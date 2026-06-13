import "dotenv/config";
import express, { NextFunction, Request, Response } from "express";
import cors from "cors";
import helmet from "helmet";
import rateLimit from "express-rate-limit";
import prisma from "./lib/prisma";
import collectionsRouter from "./routes/collections";
import catalogRouter from "./routes/catalog";
import publicRouter from "./routes/public";

const app = express();
const PORT = process.env.PORT ?? 3000;

// Security headers — sets X-Frame-Options, X-Content-Type-Options, CSP, HSTS, etc.
app.use(helmet());

// Global rate limit — 100 requests per 15 minutes per IP
const globalRateLimit = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 100,
  standardHeaders: true,
  legacyHeaders: false,
});
app.use(globalRateLimit);

// Stricter rate limit on search — pg_trgm queries are expensive
const searchRateLimit = rateLimit({
  windowMs: 60 * 1000,
  max: 30,
  standardHeaders: true,
  legacyHeaders: false,
});
app.use("/catalog/search", searchRateLimit);

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

// Global error handler — must have 4 params so Express recognizes it as error middleware
app.use((err: Error, _req: Request, res: Response, _next: NextFunction) => {
  console.error(err);
  res.status(500).json({ error: "Internal server error" });
});

startServer().catch((error) => {
  console.error("Failed to start server:", error);
  process.exit(1);
});
