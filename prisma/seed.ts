import "dotenv/config";
import { Prisma, PrismaClient } from "@prisma/client";
import { PrismaPg } from "@prisma/adapter-pg";
import * as fs from "fs";
import * as path from "path";

const adapter = new PrismaPg({ connectionString: process.env.DATABASE_URL });
const prisma = new PrismaClient({ adapter });

interface CatalogEntry {
  type: string;
  title: string;
  platform: string;
  upc: string | null;
  looseValue: number;
  cibValue: number;
  newValue: number;
  genre: string | null;
  releaseYear: number | null;
  searchableText: string;
}

function loadCatalogEntries(seedDir: string): CatalogEntry[] {
  const files = fs
    .readdirSync(seedDir)
    .filter((file) => file.endsWith(".json") && !file.startsWith("_"))
    .sort();

  console.log(`Found ${files.length} platform files to seed.`);

  const entries: CatalogEntry[] = [];
  for (const file of files) {
    const filePath = path.join(seedDir, file);
    const raw = fs.readFileSync(filePath, "utf-8");

    try {
      const parsed = JSON.parse(raw);
      if (!Array.isArray(parsed)) {
        console.log(`  ${file.padEnd(24)} -> skipped (not an array)`);
        continue;
      }
      entries.push(...parsed);
      console.log(`  ${file.replace(".json", "").padEnd(24)} -> ${parsed.length} entries loaded`);
    } catch {
      console.log(`  ${file.padEnd(24)} -> skipped (invalid JSON)`);
    }
  }

  return entries;
}

const BATCH_SIZE = 500;

async function pruneUnreferencedGameCatalogRows() {
  const deleted = await prisma.$executeRaw`
    DELETE FROM catalog_items ci
    WHERE ci.type = 'GAME'
      AND NOT EXISTS (
        SELECT 1 FROM collection_items collection_item
        WHERE collection_item."catalogItemId" = ci.id
      )
      AND NOT EXISTS (
        SELECT 1 FROM wishlist_items wishlist_item
        WHERE wishlist_item."catalogItemId" = ci.id
      )
  `;

  console.log(`Pruned ${deleted} unreferenced GAME catalog rows before reseeding.`);
}

async function bulkUpsertCatalogEntries(entries: CatalogEntry[]) {
  let affected = 0;

  for (let i = 0; i < entries.length; i += BATCH_SIZE) {
    const batch = entries.slice(i, i + BATCH_SIZE);
    const rows = batch.map((entry) => Prisma.sql`(
      ${entry.type},
      ${entry.title},
      ${entry.platform},
      ${entry.upc},
      ${entry.looseValue},
      ${entry.cibValue},
      ${entry.newValue},
      ${entry.genre},
      ${entry.releaseYear},
      ${entry.searchableText}
    )`);

    const result = await prisma.$executeRaw`
      INSERT INTO catalog_items
        (type, title, platform, upc, "looseValue", "cibValue", "newValue", genre, "releaseYear", "searchableText")
      VALUES ${Prisma.join(rows)}
      ON CONFLICT (title, platform) DO UPDATE SET
        type = EXCLUDED.type,
        upc = EXCLUDED.upc,
        "looseValue" = EXCLUDED."looseValue",
        "cibValue" = EXCLUDED."cibValue",
        "newValue" = EXCLUDED."newValue",
        genre = EXCLUDED.genre,
        "releaseYear" = EXCLUDED."releaseYear",
        "searchableText" = EXCLUDED."searchableText"
    `;

    affected += result;
    console.log(
      `  Batch ${Math.floor(i / BATCH_SIZE) + 1}: ${result} rows inserted/updated (${i + batch.length}/${entries.length} processed)`
    );
  }

  return affected;
}

async function main() {
  const seedDir = path.join(__dirname, "seed-data", "games");
  const entries = loadCatalogEntries(seedDir);

  await pruneUnreferencedGameCatalogRows();
  const affected = await bulkUpsertCatalogEntries(entries);

  const total = await prisma.catalogItem.count();
  console.log(`\nSeed complete. ${affected} catalog rows inserted/updated. catalog_items now has ${total} rows.`);
}

main()
  .catch((error) => {
    console.error("Seed failed:", error);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });