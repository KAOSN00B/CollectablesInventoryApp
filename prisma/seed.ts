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

async function main() {
  const seedDir = path.join(__dirname, "seed-data", "games");
  const entries = loadCatalogEntries(seedDir);

  let upserted = 0;
  for (const entry of entries) {
    await prisma.catalogItem.upsert({
      where: {
        title_platform: {
          title: entry.title,
          platform: entry.platform,
        },
      },
      update: {
        type: entry.type,
        upc: entry.upc,
        looseValue: entry.looseValue,
        cibValue: entry.cibValue,
        newValue: entry.newValue,
        genre: entry.genre,
        releaseYear: entry.releaseYear,
        searchableText: entry.searchableText,
      },
      create: entry,
    });
    upserted += 1;

    if (upserted % 500 === 0) {
      console.log(`  ${upserted}/${entries.length} catalog entries upserted...`);
    }
  }

  const total = await prisma.catalogItem.count();
  console.log(`\nSeed complete. ${upserted} entries upserted. catalog_items now has ${total} rows.`);
}

main()
  .catch((error) => {
    console.error("Seed failed:", error);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
