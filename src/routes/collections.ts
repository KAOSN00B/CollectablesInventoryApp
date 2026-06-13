import { Router, Request, Response } from "express";
import prisma from "../lib/prisma";

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
router.post("/", async (req: Request, res: Response) => {
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
});

// GET /collections/:code — get a collection by its public code
router.get("/:code", async (req: Request, res: Response) => {
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
});

// ---------- ITEMS ----------

// GET /collections/:code/items
router.get("/:code/items", async (req: Request, res: Response) => {
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
});

// POST /collections/:code/items
router.post("/:code/items", async (req: Request, res: Response) => {
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
    type,
    title,
    platform,
    condition,
    purchasePrice,
    estimatedValue,
    notes,
    forTrade,
  } = req.body as {
    catalogItemId?: number;
    type: string;
    title: string;
    platform: string;
    condition: string;
    purchasePrice?: number;
    estimatedValue?: number;
    notes?: string;
    forTrade?: boolean;
  };

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
});

// PUT /collections/:code/items/:id
router.put("/:code/items/:id", async (req: Request, res: Response) => {
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

  const { condition, purchasePrice, estimatedValue, notes, forTrade } =
    req.body as {
      condition?: string;
      purchasePrice?: number;
      estimatedValue?: number;
      notes?: string;
      forTrade?: boolean;
    };

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
});

// DELETE /collections/:code/items/:id
router.delete("/:code/items/:id", async (req: Request, res: Response) => {
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
});

// ---------- WISHLIST ----------

// GET /collections/:code/wishlist
router.get("/:code/wishlist", async (req: Request, res: Response) => {
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
});

// POST /collections/:code/wishlist
router.post("/:code/wishlist", async (req: Request, res: Response) => {
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
});

// PUT /collections/:code/wishlist/:id
router.put("/:code/wishlist/:id", async (req: Request, res: Response) => {
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
});

// DELETE /collections/:code/wishlist/:id
router.delete("/:code/wishlist/:id", async (req: Request, res: Response) => {
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
});

export default router;
