import "dotenv/config";
import { PrismaClient } from "@prisma/client";
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

async function main() {
  const seedDir = path.join(__dirname, "seed-data", "games");
  const entries = loadCatalogEntries(seedDir);

  let inserted = 0;
  for (let i = 0; i < entries.length; i += BATCH_SIZE) {
    const batch = entries.slice(i, i + BATCH_SIZE);
    const result = await prisma.catalogItem.createMany({
      data: batch,
      skipDuplicates: true,
    });
    inserted += result.count;
    console.log(
      `  Batch ${Math.floor(i / BATCH_SIZE) + 1}: ${result.count} new rows inserted (${i + batch.length}/${entries.length} processed)`
    );
  }

  const total = await prisma.catalogItem.count();
  console.log(`\nSeed complete. ${inserted} new rows inserted. catalog_items now has ${total} rows.`);
}

main()
  .catch((error) => {
    console.error("Seed failed:", error);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
