import { Router, Request, Response, NextFunction } from "express";
import prisma from "../lib/prisma";

const router = Router();

// GET /catalog/search?q=zelda&platform=N64&limit=20
// Uses pg_trgm similarity search for fuzzy matching
router.get("/search", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const query = String(req.query.q ?? "").trim();
    const platform = String(req.query.platform ?? "").trim();
    const limit = Math.min(parseInt(String(req.query.limit ?? "20"), 10), 50);

    if (query.length < 2) {
      res.status(400).json({ error: "Query must be at least 2 characters" });
      return;
    }

    const searchPattern = `%${query}%`;

    // Use pg_trgm for fuzzy matching on the searchableText column
    // Falls back to ILIKE if similarity is low — covers both exact substring and fuzzy
    let results;
    if (platform) {
      results = await prisma.$queryRaw<
        { id: number; type: string; title: string; platform: string; upc: string | null; looseValue: number; cibValue: number; newValue: number; genre: string | null; releaseYear: number | null; imageUrl: string | null }[]
      >`
        SELECT id, type, title, platform, upc, "looseValue", "cibValue", "newValue", genre, "releaseYear", "imageUrl"
        FROM catalog_items
        WHERE platform = ${platform}
          AND (
            "searchableText" ILIKE ${searchPattern}
            OR word_similarity(${query}, title) > 0.5
          )
        ORDER BY word_similarity(${query}, platform) DESC, word_similarity(${query}, "searchableText") DESC, word_similarity(${query}, title) DESC
        LIMIT ${limit}
      `;
    } else {
      results = await prisma.$queryRaw<
        { id: number; type: string; title: string; platform: string; upc: string | null; looseValue: number; cibValue: number; newValue: number; genre: string | null; releaseYear: number | null; imageUrl: string | null }[]
      >`
        SELECT id, type, title, platform, upc, "looseValue", "cibValue", "newValue", genre, "releaseYear", "imageUrl"
        FROM catalog_items
        WHERE "searchableText" ILIKE ${searchPattern}
           OR word_similarity(${query}, title) > 0.5
        ORDER BY word_similarity(${query}, platform) DESC, word_similarity(${query}, "searchableText") DESC, word_similarity(${query}, title) DESC
        LIMIT ${limit}
      `;
    }

    res.json(results);
  } catch (err) {
    next(err);
  }
});

// GET /catalog/barcode/:upc — look up a game by barcode
router.get("/barcode/:upc", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const upc = String(req.params.upc);

    const item = await prisma.catalogItem.findFirst({
      where: { upc },
    });

    if (!item) {
      res.status(404).json({ error: "No catalog item found for this barcode" });
      return;
    }

    res.json(item);
  } catch (err) {
    next(err);
  }
});

// GET /catalog/platforms — list all platforms and their catalog counts
// Used by Android for the completion tracker
router.get("/platforms", async (_req: Request, res: Response, next: NextFunction) => {
  try {
    const platformCounts = await prisma.$queryRaw<
      { platform: string; count: bigint }[]
    >`
      SELECT platform, COUNT(*) as count
      FROM catalog_items
      WHERE type = 'GAME'
      GROUP BY platform
      ORDER BY platform ASC
    `;

    // Convert BigInt to number for JSON serialization
    const result = platformCounts.map((row) => ({
      platform: row.platform,
      count: Number(row.count),
    }));

    res.json(result);
  } catch (err) {
    next(err);
  }
});

// GET /catalog/:id — single catalog item detail
router.get("/:id", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const id = parseInt(String(req.params.id), 10);
    if (isNaN(id)) {
      res.status(400).json({ error: "Invalid catalog item ID" });
      return;
    }

    const item = await prisma.catalogItem.findUnique({ where: { id } });
    if (!item) {
      res.status(404).json({ error: "Catalog item not found" });
      return;
    }

    res.json(item);
  } catch (err) {
    next(err);
  }
});

// GET /catalog/:id/community — ownership count and trade availability count
// Anonymous aggregate — no personal data exposed
router.get("/:id/community", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const catalogItemId = parseInt(String(req.params.id), 10);
    if (isNaN(catalogItemId)) {
      res.status(400).json({ error: "Invalid catalog item ID" });
      return;
    }

    const [ownershipCount, tradeCount] = await Promise.all([
      prisma.collectionItem.count({ where: { catalogItemId } }),
      prisma.collectionItem.count({ where: { catalogItemId, forTrade: true } }),
    ]);

    res.json({
      catalogItemId,
      ownedByCollectors: ownershipCount,
      availableForTrade: tradeCount,
    });
  } catch (err) {
    next(err);
  }
});

export default router;
