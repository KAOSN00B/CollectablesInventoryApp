import { Router, Request, Response, NextFunction } from "express";
import { z } from "zod";
import prisma from "../lib/prisma";

const GAME_CONDITIONS = ["LOOSE", "CIB", "NEW", "POOR"] as const;
const TCG_CONDITIONS = ["NM", "LP", "MP", "HP", "DMG"] as const;

// Zod schemas for write endpoints
const addItemSchema = z
  .object({
    catalogItemId: z.number().int().positive().optional().nullable(),
    type: z.enum(["GAME", "CONSOLE", "TCG", "COLLECTIBLE"]),
    title: z.string().min(1, "Title is required"),
    platform: z.string().min(1, "Platform is required"),
    condition: z.string().min(1, "Condition is required"),
    purchasePrice: z.number().min(0).optional(),
    estimatedValue: z.number().min(0).optional(),
    notes: z.string().optional().nullable(),
    forTrade: z.boolean().optional(),
    // TCG-specific fields — omitted for game/console items
    tcgGame: z.enum(["MTG", "POKEMON", "YUGIOH"]).optional().nullable(),
    tcgSet: z.string().optional().nullable(),
    tcgSetCode: z.string().optional().nullable(),
    tcgCardNumber: z.string().optional().nullable(),
    tcgRarity: z.string().optional().nullable(),
    tcgIsFoil: z.boolean().optional().nullable(),
    tcgExternalId: z.string().optional().nullable(),
    quantity: z.number().int().min(1).optional().nullable(),
    binderId: z.number().int().positive().optional().nullable(),
  })
  .superRefine((data, ctx) => {
    // Enforce the correct condition scale per item type
    if (data.type === "GAME" || data.type === "CONSOLE") {
      if (!GAME_CONDITIONS.includes(data.condition as typeof GAME_CONDITIONS[number])) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: `For type ${data.type}, condition must be one of: ${GAME_CONDITIONS.join(", ")}`,
          path: ["condition"],
        });
      }
    } else if (data.type === "TCG") {
      if (!TCG_CONDITIONS.includes(data.condition as typeof TCG_CONDITIONS[number])) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: `For type TCG, condition must be one of: ${TCG_CONDITIONS.join(", ")}`,
          path: ["condition"],
        });
      }
    }
    // COLLECTIBLE: no condition restriction
  });

const updateItemSchema = z.object({
  condition: z.string().optional(),
  purchasePrice: z.number().min(0).optional(),
  estimatedValue: z.number().min(0).optional(),
  notes: z.string().optional().nullable(),
  forTrade: z.boolean().optional(),
  tcgIsFoil: z.boolean().optional().nullable(),
  quantity: z.number().int().min(1).optional().nullable(),
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

    const {
      catalogItemId, type, title, platform, condition,
      purchasePrice, estimatedValue, notes, forTrade,
      tcgGame, tcgSet, tcgSetCode, tcgCardNumber, tcgRarity,
      tcgIsFoil, tcgExternalId, quantity, binderId,
    } = parsed.data;

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
        tcgGame: tcgGame ?? null,
        tcgSet: tcgSet ?? null,
        tcgSetCode: tcgSetCode ?? null,
        tcgCardNumber: tcgCardNumber ?? null,
        tcgRarity: tcgRarity ?? null,
        tcgIsFoil: tcgIsFoil ?? null,
        tcgExternalId: tcgExternalId ?? null,
        quantity: quantity ?? null,
      },
    });

    // If the caller provided a binder ID, assign this item to that binder
    if (binderId) {
      const binder = await prisma.binder.findFirst({
        where: { id: binderId, collectionId: collection.id },
      });
      if (binder) {
        await prisma.binderItem.create({
          data: { binderId, collectionItemId: item.id },
        });
      }
    }

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

    const { condition, purchasePrice, estimatedValue, notes, forTrade, tcgIsFoil, quantity } = parsed.data;

    const updated = await prisma.collectionItem.update({
      where: { id: itemId },
      data: {
        condition: condition ?? existing.condition,
        purchasePrice: purchasePrice ?? existing.purchasePrice,
        estimatedValue: estimatedValue ?? existing.estimatedValue,
        notes: notes !== undefined ? notes : existing.notes,
        forTrade: forTrade !== undefined ? forTrade : existing.forTrade,
        tcgIsFoil: tcgIsFoil !== undefined ? tcgIsFoil : existing.tcgIsFoil,
        quantity: quantity !== undefined ? quantity : existing.quantity,
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

// ---------- BINDERS ----------

// GET /collections/:code/binders — list binders with item count and total value
router.get("/:code/binders", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const code = String(req.params.code).toUpperCase();

    const collection = await prisma.collection.findUnique({
      where: { publicCode: code },
    });
    if (!collection) {
      res.status(404).json({ error: "Collection not found" });
      return;
    }

    const binders = await prisma.binder.findMany({
      where: { collectionId: collection.id },
      orderBy: { createdAt: "asc" },
      include: {
        items: {
          include: { collectionItem: true },
        },
      },
    });

    // Compute item count and total value for each binder
    const bindersWithStats = binders.map((binder) => ({
      id: binder.id,
      collectionId: binder.collectionId,
      name: binder.name,
      createdAt: binder.createdAt,
      itemCount: binder.items.length,
      totalValue: binder.items.reduce((sum, bi) => {
        const item = bi.collectionItem;
        const qty = item.quantity ?? 1;
        return sum + item.estimatedValue * qty;
      }, 0),
    }));

    res.json(bindersWithStats);
  } catch (err) {
    next(err);
  }
});

// POST /collections/:code/binders — create a new binder
router.post("/:code/binders", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const code = String(req.params.code).toUpperCase();
    const { name } = req.body as { name?: string };

    if (!name || name.trim().length === 0) {
      res.status(400).json({ error: "Binder name is required" });
      return;
    }

    const collection = await prisma.collection.findUnique({
      where: { publicCode: code },
    });
    if (!collection) {
      res.status(404).json({ error: "Collection not found" });
      return;
    }

    const binder = await prisma.binder.create({
      data: { collectionId: collection.id, name: name.trim() },
    });

    res.status(201).json({ ...binder, itemCount: 0, totalValue: 0 });
  } catch (err) {
    next(err);
  }
});

// GET /collections/:code/binders/:id — get one binder with all its items
router.get("/:code/binders/:id", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const code = String(req.params.code).toUpperCase();
    const binderId = parseInt(String(req.params.id), 10);

    const collection = await prisma.collection.findUnique({
      where: { publicCode: code },
    });
    if (!collection) {
      res.status(404).json({ error: "Collection not found" });
      return;
    }

    const binder = await prisma.binder.findFirst({
      where: { id: binderId, collectionId: collection.id },
      include: {
        items: {
          include: { collectionItem: true },
          orderBy: { collectionItem: { createdAt: "desc" } },
        },
      },
    });
    if (!binder) {
      res.status(404).json({ error: "Binder not found" });
      return;
    }

    res.json({
      id: binder.id,
      collectionId: binder.collectionId,
      name: binder.name,
      createdAt: binder.createdAt,
      items: binder.items.map((bi) => bi.collectionItem),
    });
  } catch (err) {
    next(err);
  }
});

// DELETE /collections/:code/binders/:id — delete a binder (items stay in collection)
router.delete("/:code/binders/:id", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const code = String(req.params.code).toUpperCase();
    const binderId = parseInt(String(req.params.id), 10);

    const collection = await prisma.collection.findUnique({
      where: { publicCode: code },
    });
    if (!collection) {
      res.status(404).json({ error: "Collection not found" });
      return;
    }

    const binder = await prisma.binder.findFirst({
      where: { id: binderId, collectionId: collection.id },
    });
    if (!binder) {
      res.status(404).json({ error: "Binder not found" });
      return;
    }

    // Deleting the binder cascades to binder_items; the collection items themselves are untouched
    await prisma.binder.delete({ where: { id: binderId } });
    res.status(204).send();
  } catch (err) {
    next(err);
  }
});

// POST /collections/:code/binders/:id/items — add an existing item to a binder
router.post("/:code/binders/:id/items", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const code = String(req.params.code).toUpperCase();
    const binderId = parseInt(String(req.params.id), 10);
    const { itemId } = req.body as { itemId?: number };

    if (!itemId) {
      res.status(400).json({ error: "itemId is required" });
      return;
    }

    const collection = await prisma.collection.findUnique({
      where: { publicCode: code },
    });
    if (!collection) {
      res.status(404).json({ error: "Collection not found" });
      return;
    }

    const binder = await prisma.binder.findFirst({
      where: { id: binderId, collectionId: collection.id },
    });
    if (!binder) {
      res.status(404).json({ error: "Binder not found" });
      return;
    }

    const item = await prisma.collectionItem.findFirst({
      where: { id: itemId, collectionId: collection.id },
    });
    if (!item) {
      res.status(404).json({ error: "Item not found in this collection" });
      return;
    }

    // upsert so adding the same item twice is idempotent
    await prisma.binderItem.upsert({
      where: { binderId_collectionItemId: { binderId, collectionItemId: itemId } },
      create: { binderId, collectionItemId: itemId },
      update: {},
    });

    res.status(201).json({ binderId, collectionItemId: itemId });
  } catch (err) {
    next(err);
  }
});

// DELETE /collections/:code/binders/:id/items/:itemId — remove item from binder
router.delete("/:code/binders/:id/items/:itemId", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const code = String(req.params.code).toUpperCase();
    const binderId = parseInt(String(req.params.id), 10);
    const itemId = parseInt(String(req.params.itemId), 10);

    const collection = await prisma.collection.findUnique({
      where: { publicCode: code },
    });
    if (!collection) {
      res.status(404).json({ error: "Collection not found" });
      return;
    }

    await prisma.binderItem.deleteMany({
      where: { binderId, collectionItemId: itemId },
    });

    res.status(204).send();
  } catch (err) {
    next(err);
  }
});

export default router;
