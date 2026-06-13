-- Make catalog seeding idempotent by allowing upserts on title + platform.
CREATE UNIQUE INDEX IF NOT EXISTS "catalog_items_title_platform_key"
ON "catalog_items" ("title", "platform");
