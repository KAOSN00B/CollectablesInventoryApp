import "dotenv/config";
import express from "express";
import cors from "cors";
import prisma from "./lib/prisma";
import collectionsRouter from "./routes/collections";
import catalogRouter from "./routes/catalog";
import publicRouter from "./routes/public";

const app = express();
const PORT = process.env.PORT ?? 3000;

app.use(cors());
app.use(express.json());

// ---- Routes ----
app.use("/collections", collectionsRouter);
app.use("/catalog", catalogRouter);
app.use("/c", publicRouter);

// Keepalive endpoint — cron-job.org pings this every 14 minutes to prevent cold starts
app.get("/health", (_req, res) => {
  res.json({ status: "ok", timestamp: new Date().toISOString() });
});

// ---- Startup ----
async function startServer() {
  // Enable pg_trgm extension for fuzzy search — idempotent, safe to run on every startup
  await prisma.$executeRaw`CREATE EXTENSION IF NOT EXISTS pg_trgm`;

  // Create a GIN index on searchableText if it doesn't exist — makes fuzzy search fast
  await prisma.$executeRaw`
    CREATE INDEX IF NOT EXISTS catalog_items_searchable_gin
    ON catalog_items USING GIN ("searchableText" gin_trgm_ops)
  `;

  app.listen(PORT, () => {
    console.log(`CollectOS backend running on port ${PORT}`);
  });
}

startServer().catch((error) => {
  console.error("Failed to start server:", error);
  process.exit(1);
});
