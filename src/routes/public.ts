import { Router, Request, Response } from "express";
import prisma from "../lib/prisma";

const router = Router();

// GET /c/:code — public read-only collection page (plain HTML, no React needed)
router.get("/:code", async (req: Request, res: Response) => {
  const code = String(req.params.code).toUpperCase();

  const collection = await prisma.collection.findUnique({
    where: { publicCode: code },
    include: {
      items: { orderBy: [{ platform: "asc" }, { title: "asc" }] },
      wishlist: { orderBy: [{ isGrail: "desc" }, { title: "asc" }] },
    },
  });

  if (!collection) {
    res.status(404).send(`
      <!DOCTYPE html>
      <html lang="en">
      <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <title>Collection Not Found — CollectOS</title>
        <style>
          body { font-family: system-ui, sans-serif; background: #0f0f0f; color: #e0e0e0;
                 display: flex; align-items: center; justify-content: center; min-height: 100vh; margin: 0; }
          .card { background: #1a1a1a; border-radius: 12px; padding: 40px; text-align: center; max-width: 400px; }
          h1 { color: #ff5555; margin-top: 0; }
          p { color: #999; }
        </style>
      </head>
      <body>
        <div class="card">
          <h1>Collection Not Found</h1>
          <p>No collection exists for code <strong>${escapeHtml(code)}</strong>.</p>
          <p>Check the code and try again, or create your own collection in the CollectOS app.</p>
        </div>
      </body>
      </html>
    `);
    return;
  }

  // Group items by platform for display
  const itemsByPlatform: Record<string, typeof collection.items> = {};
  for (const item of collection.items) {
    if (!itemsByPlatform[item.platform]) itemsByPlatform[item.platform] = [];
    itemsByPlatform[item.platform].push(item);
  }

  // Calculate total value
  const totalValue = collection.items.reduce(
    (sum, item) => sum + item.estimatedValue,
    0
  );

  const displayName = collection.displayName ?? `Collection ${collection.publicCode}`;
  const itemCount = collection.items.length;

  // Build platform sections HTML
  const platformSections = Object.entries(itemsByPlatform)
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([platform, items]) => {
      const rows = items
        .map(
          (item) => `
          <tr>
            <td>${escapeHtml(item.title)}</td>
            <td><span class="badge badge-${item.condition.toLowerCase()}">${escapeHtml(item.condition)}</span></td>
            <td>$${item.estimatedValue.toFixed(2)}</td>
            ${item.forTrade ? "<td><span class=\"trade-badge\">FOR TRADE</span></td>" : "<td></td>"}
          </tr>`
        )
        .join("");

      return `
        <div class="platform-section">
          <h2>${escapeHtml(platform)} <span class="platform-count">${items.length} item${items.length !== 1 ? "s" : ""}</span></h2>
          <table>
            <thead><tr><th>Title</th><th>Condition</th><th>Value</th><th></th></tr></thead>
            <tbody>${rows}</tbody>
          </table>
        </div>`;
    })
    .join("");

  // Build wishlist section HTML
  const wishlistRows = collection.wishlist
    .map(
      (item) => `
      <tr>
        <td>${item.isGrail ? '<span class="grail-badge">&#9733; GRAIL</span> ' : ""}${escapeHtml(item.title)}</td>
        <td>${escapeHtml(item.platform)}</td>
        <td>$${item.currentEstimatedValue.toFixed(2)}</td>
        <td>$${item.targetPrice.toFixed(2)}</td>
      </tr>`
    )
    .join("");

  const wishlistSection =
    collection.wishlist.length > 0
      ? `
      <div class="platform-section">
        <h2>Wishlist <span class="platform-count">${collection.wishlist.length} item${collection.wishlist.length !== 1 ? "s" : ""}</span></h2>
        <table>
          <thead><tr><th>Title</th><th>Platform</th><th>Current Value</th><th>Target Price</th></tr></thead>
          <tbody>${wishlistRows}</tbody>
        </table>
      </div>`
      : "";

  res.send(`
    <!DOCTYPE html>
    <html lang="en">
    <head>
      <meta charset="UTF-8" />
      <meta name="viewport" content="width=device-width, initial-scale=1.0" />
      <title>${escapeHtml(displayName)} — CollectOS</title>
      <style>
        *, *::before, *::after { box-sizing: border-box; }
        body { font-family: system-ui, -apple-system, sans-serif; background: #0f0f0f; color: #e0e0e0;
               margin: 0; padding: 24px 16px; line-height: 1.5; }
        .container { max-width: 900px; margin: 0 auto; }
        header { margin-bottom: 32px; }
        .logo { font-size: 13px; letter-spacing: 0.1em; color: #666; text-transform: uppercase; margin-bottom: 8px; }
        h1 { margin: 0 0 8px; font-size: 2rem; color: #fff; }
        .meta { color: #888; font-size: 14px; }
        .stats { display: flex; gap: 24px; margin: 24px 0; flex-wrap: wrap; }
        .stat { background: #1a1a1a; border-radius: 10px; padding: 16px 24px; flex: 1; min-width: 140px; }
        .stat-value { font-size: 1.5rem; font-weight: 700; color: #7ec8ff; }
        .stat-label { font-size: 12px; color: #888; text-transform: uppercase; letter-spacing: 0.05em; }
        .platform-section { margin-bottom: 32px; }
        h2 { font-size: 1.1rem; color: #ccc; border-bottom: 1px solid #2a2a2a; padding-bottom: 8px; margin-bottom: 12px; }
        .platform-count { font-size: 0.8rem; color: #666; font-weight: 400; margin-left: 8px; }
        table { width: 100%; border-collapse: collapse; font-size: 14px; }
        th { text-align: left; color: #666; font-size: 12px; text-transform: uppercase; letter-spacing: 0.05em;
             padding: 6px 12px 6px 0; border-bottom: 1px solid #222; }
        td { padding: 8px 12px 8px 0; border-bottom: 1px solid #1a1a1a; color: #ddd; }
        .badge { font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 4px; letter-spacing: 0.05em; }
        .badge-loose { background: #2a2a00; color: #ccaa00; }
        .badge-cib   { background: #002a1a; color: #00cc88; }
        .badge-new   { background: #001a2a; color: #00aaff; }
        .badge-poor  { background: #2a0000; color: #cc4444; }
        .trade-badge { font-size: 10px; font-weight: 700; padding: 2px 8px; border-radius: 4px;
                       background: #1a1a00; color: #aaaa00; letter-spacing: 0.05em; }
        .grail-badge { font-size: 11px; font-weight: 700; color: #ffcc00; }
        footer { margin-top: 48px; text-align: center; color: #444; font-size: 12px; }
      </style>
    </head>
    <body>
      <div class="container">
        <header>
          <div class="logo">CollectOS</div>
          <h1>${escapeHtml(displayName)}</h1>
          <div class="meta">Code: ${escapeHtml(collection.publicCode)} &nbsp;&middot;&nbsp; ${itemCount} item${itemCount !== 1 ? "s" : ""}</div>
        </header>

        <div class="stats">
          <div class="stat">
            <div class="stat-value">${itemCount}</div>
            <div class="stat-label">Items Owned</div>
          </div>
          <div class="stat">
            <div class="stat-value">$${totalValue.toFixed(2)}</div>
            <div class="stat-label">Total Value</div>
          </div>
          <div class="stat">
            <div class="stat-value">${Object.keys(itemsByPlatform).length}</div>
            <div class="stat-label">Platforms</div>
          </div>
        </div>

        ${platformSections}
        ${wishlistSection}

        <footer>
          <p>Shared via CollectOS &nbsp;&middot;&nbsp; No personal data stored</p>
        </footer>
      </div>
    </body>
    </html>
  `);
});

// Prevent XSS in user-supplied strings rendered into HTML
function escapeHtml(text: string): string {
  return text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

export default router;
