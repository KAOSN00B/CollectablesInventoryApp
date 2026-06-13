import { Router, Request, Response, NextFunction } from "express";
import { z } from "zod";
import prisma from "../lib/prisma";

// Zod schemas for write endpoints
const addItemSchema = z.object({
  catalogItemId: z.number().int().positive().optional().nullable(),
  type: z.enum(["GAME", "CONSOLE"]),
  title: z.string().min(1, "Title is required"),
  platform: z.string().min(1, "Platform is required"),
  condition: z.enum(["LOOSE", "CIB", "NEW", "POOR"]),
  purchasePrice: z.number().min(0).optional(),
  estimatedValue: z.number().min(0).optional(),
  notes: z.string().optional().nullable(),
  forTrade: z.boolean().optional(),
});

const updateItemSchema = z.object({
  condition: z.enum(["LOOSE", "CIB", "NEW", "POOR"]).optional(),
  purchasePrice: z.number().min(0).optional(),
  estimatedValue: z.number().min(0).optional(),
  notes: z.string().optional().nullable(),
  forTrade: z.boolean().optional(),
});

const router = Router();

// Generate a random 5-character alphanumeric public code (e.g. "X7K2P")
function generatePublicCode(): string {
  const characters = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no ambiguous chars like 0/O, 1/I
  let code = "";
  for (let i = 0; i < 5; i++) {
    code += characters.charAt(Math.floor(Math.random() * characters.length));
  }
  return code;
}

// POST /collections — create a new anonymous collection
router.post("/", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { displayName } = req.body as { displayName?: string };

    // Retry generating until we get a unique code (collision is extremely rare but possible)
    let publicCode = generatePublicCode();
    let attempts = 0;
    while (attempts < 10) {
      const existing = await prisma.collection.findUnique({ where: { publicCode } });
      if (!existing) break;
      publicCode = generatePublicCode();
      attempts++;
    }

    const collection = await prisma.collection.create({
      data: { publicCode, displayName: displayName ?? null },
    });

    res.status(201).json(collection);
  } catch (err) {
    next(err);
  }
});

// GET /collections/:code — get a collection by its public code
router.get("/:code", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const code = String(req.params.code).toUpperCase();

    const collection = await prisma.collection.findUnique({
      where: { publicCode: code },
      include: {
        items: { orderBy: { createdAt: "desc" } },
        wishlist: { orderBy: { id: "asc" } },
      },
    });

    if (!collection) {
      res.status(404).json({ error: "Collection not found" });
      return;
    }

    res.json(collection);
  } catch (err) {
    next(err);
  }
});

// ---------- ITEMS ----------

// GET /collections/:code/items
router.get("/:code/items", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const code = String(req.params.code).toUpperCase();

    const collection = await prisma.collection.findUnique({
      where: { publicCode: code },
    });
    if (!collection) {
      res.status(404).json({ error: "Collection not found" });
      return;
    }

    const items = await prisma.collectionItem.findMany({
      where: { collectionId: collection.id },
      orderBy: { createdAt: "desc" },
    });

    res.json(items);
  } catch (err) {
    next(err);
  }
});

// POST /collections/:code/items
router.post("/:code/items", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const code = String(req.params.code).toUpperCase();

    const parsed = addItemSchema.safeParse(req.body);
    if (!parsed.success) {
      res.status(400).json({ error: "Invalid request", details: parsed.error.flatten().fieldErrors });
      return;
    }

    const collection = await prisma.collection.findUnique({
      where: { publicCode: code },
    });
    if (!collection) {
      res.status(404).json({ error: "Collection not found" });
      return;
    }

    const { catalogItemId, type, title, platform, condition, purchasePrice, estimatedValue, notes, forTrade } = parsed.data;

    const item = await prisma.collectionItem.create({
      data: {
        collectionId: collection.id,
        catalogItemId: catalogItemId ?? null,
        type,
        title,
        platform,
        condition,
        purchasePrice: purchasePrice ?? 0,
        estimatedValue: estimatedValue ?? 0,
        notes: notes ?? null,
        forTrade: forTrade ?? false,
      },
    });

    res.status(201).json(item);
  } catch (err) {
    next(err);
  }
});

// PUT /collections/:code/items/:id
router.put("/:code/items/:id", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const code = String(req.params.code).toUpperCase();
    const itemId = parseInt(String(req.params.id), 10);

    const parsed = updateItemSchema.safeParse(req.body);
    if (!parsed.success) {
      res.status(400).json({ error: "Invalid request", details: parsed.error.flatten().fieldErrors });
      return;
    }

    const collection = await prisma.collection.findUnique({
      where: { publicCode: code },
    });
    if (!collection) {
      res.status(404).json({ error: "Collection not found" });
      return;
    }

    const existing = await prisma.collectionItem.findFirst({
      where: { id: itemId, collectionId: collection.id },
    });
    if (!existing) {
      res.status(404).json({ error: "Item not found" });
      return;
    }

    const { condition, purchasePrice, estimatedValue, notes, forTrade } = parsed.data;

    const updated = await prisma.collectionItem.update({
      where: { id: itemId },
      data: {
        condition: condition ?? existing.condition,
        purchasePrice: purchasePrice ?? existing.purchasePrice,
        estimatedValue: estimatedValue ?? existing.estimatedValue,
        notes: notes !== undefined ? notes : existing.notes,
        forTrade: forTrade !== undefined ? forTrade : existing.forTrade,
      },
    });

    res.json(updated);
  } catch (err) {
    next(err);
  }
});

// DELETE /collections/:code/items/:id
router.delete("/:code/items/:id", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const code = String(req.params.code).toUpperCase();
    const itemId = parseInt(String(req.params.id), 10);

    const collection = await prisma.collection.findUnique({
      where: { publicCode: code },
    });
    if (!collection) {
      res.status(404).json({ error: "Collection not found" });
      return;
    }

    const existing = await prisma.collectionItem.findFirst({
      where: { id: itemId, collectionId: collection.id },
    });
    if (!existing) {
      res.status(404).json({ error: "Item not found" });
      return;
    }

    await prisma.collectionItem.delete({ where: { id: itemId } });
    res.status(204).send();
  } catch (err) {
    next(err);
  }
});

// ---------- WISHLIST ----------

// GET /collections/:code/wishlist
router.get("/:code/wishlist", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const code = String(req.params.code).toUpperCase();

    const collection = await prisma.collection.findUnique({
      where: { publicCode: code },
    });
    if (!collection) {
      res.status(404).json({ error: "Collection not found" });
      return;
    }

    const wishlist = await prisma.wishlistItem.findMany({
      where: { collectionId: collection.id },
      orderBy: [{ isGrail: "desc" }, { id: "asc" }],
    });

    res.json(wishlist);
  } catch (err) {
    next(err);
  }
});

// POST /collections/:code/wishlist
router.post("/:code/wishlist", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const code = String(req.params.code).toUpperCase();

    const collection = await prisma.collection.findUnique({
      where: { publicCode: code },
    });
    if (!collection) {
      res.status(404).json({ error: "Collection not found" });
      return;
    }

    const {
      catalogItemId,
      title,
      platform,
      targetPrice,
      currentEstimatedValue,
      notes,
      isGrail,
    } = req.body as {
      catalogItemId?: number;
      title: string;
      platform: string;
      targetPrice?: number;
      currentEstimatedValue?: number;
      notes?: string;
      isGrail?: boolean;
    };

    const wishlistItem = await prisma.wishlistItem.create({
      data: {
        collectionId: collection.id,
        catalogItemId: catalogItemId ?? null,
        title,
        platform,
        targetPrice: targetPrice ?? 0,
        currentEstimatedValue: currentEstimatedValue ?? 0,
        notes: notes ?? null,
        isGrail: isGrail ?? false,
      },
    });

    res.status(201).json(wishlistItem);
  } catch (err) {
    next(err);
  }
});

// PUT /collections/:code/wishlist/:id
router.put("/:code/wishlist/:id", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const code = String(req.params.code).toUpperCase();
    const itemId = parseInt(String(req.params.id), 10);

    const collection = await prisma.collection.findUnique({
      where: { publicCode: code },
    });
    if (!collection) {
      res.status(404).json({ error: "Collection not found" });
      return;
    }

    const existing = await prisma.wishlistItem.findFirst({
      where: { id: itemId, collectionId: collection.id },
    });
    if (!existing) {
      res.status(404).json({ error: "Wishlist item not found" });
      return;
    }

    const { targetPrice, currentEstimatedValue, notes, isGrail } = req.body as {
      targetPrice?: number;
      currentEstimatedValue?: number;
      notes?: string;
      isGrail?: boolean;
    };

    const updated = await prisma.wishlistItem.update({
      where: { id: itemId },
      data: {
        targetPrice: targetPrice ?? existing.targetPrice,
        currentEstimatedValue:
          currentEstimatedValue ?? existing.currentEstimatedValue,
        notes: notes !== undefined ? notes : existing.notes,
        isGrail: isGrail !== undefined ? isGrail : existing.isGrail,
      },
    });

    res.json(updated);
  } catch (err) {
    next(err);
  }
});

// DELETE /collections/:code/wishlist/:id
router.delete("/:code/wishlist/:id", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const code = String(req.params.code).toUpperCase();
    const itemId = parseInt(String(req.params.id), 10);

    const collection = await prisma.collection.findUnique({
      where: { publicCode: code },
    });
    if (!collection) {
      res.status(404).json({ error: "Collection not found" });
      return;
    }

    const existing = await prisma.wishlistItem.findFirst({
      where: { id: itemId, collectionId: collection.id },
    });
    if (!existing) {
      res.status(404).json({ error: "Wishlist item not found" });
      return;
    }

    await prisma.wishlistItem.delete({ where: { id: itemId } });
    res.status(204).send();
  } catch (err) {
    next(err);
  }
});

export default router;
