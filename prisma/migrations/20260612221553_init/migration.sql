-- CreateTable
CREATE TABLE "collections" (
    "id" SERIAL NOT NULL,
    "publicCode" TEXT NOT NULL,
    "displayName" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "collections_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "catalog_items" (
    "id" SERIAL NOT NULL,
    "type" TEXT NOT NULL,
    "title" TEXT NOT NULL,
    "platform" TEXT NOT NULL,
    "upc" TEXT,
    "looseValue" DOUBLE PRECISION NOT NULL DEFAULT 0,
    "cibValue" DOUBLE PRECISION NOT NULL DEFAULT 0,
    "newValue" DOUBLE PRECISION NOT NULL DEFAULT 0,
    "genre" TEXT,
    "releaseYear" INTEGER,
    "imageUrl" TEXT,
    "searchableText" TEXT NOT NULL,

    CONSTRAINT "catalog_items_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "collection_items" (
    "id" SERIAL NOT NULL,
    "collectionId" INTEGER NOT NULL,
    "catalogItemId" INTEGER,
    "type" TEXT NOT NULL,
    "title" TEXT NOT NULL,
    "platform" TEXT NOT NULL,
    "condition" TEXT NOT NULL,
    "purchasePrice" DOUBLE PRECISION NOT NULL DEFAULT 0,
    "estimatedValue" DOUBLE PRECISION NOT NULL DEFAULT 0,
    "notes" TEXT,
    "forTrade" BOOLEAN NOT NULL DEFAULT false,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "collection_items_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "wishlist_items" (
    "id" SERIAL NOT NULL,
    "collectionId" INTEGER NOT NULL,
    "catalogItemId" INTEGER,
    "title" TEXT NOT NULL,
    "platform" TEXT NOT NULL,
    "targetPrice" DOUBLE PRECISION NOT NULL DEFAULT 0,
    "currentEstimatedValue" DOUBLE PRECISION NOT NULL DEFAULT 0,
    "notes" TEXT,
    "isGrail" BOOLEAN NOT NULL DEFAULT false,

    CONSTRAINT "wishlist_items_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "collections_publicCode_key" ON "collections"("publicCode");

-- AddForeignKey
ALTER TABLE "collection_items" ADD CONSTRAINT "collection_items_collectionId_fkey" FOREIGN KEY ("collectionId") REFERENCES "collections"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "collection_items" ADD CONSTRAINT "collection_items_catalogItemId_fkey" FOREIGN KEY ("catalogItemId") REFERENCES "catalog_items"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "wishlist_items" ADD CONSTRAINT "wishlist_items_collectionId_fkey" FOREIGN KEY ("collectionId") REFERENCES "collections"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "wishlist_items" ADD CONSTRAINT "wishlist_items_catalogItemId_fkey" FOREIGN KEY ("catalogItemId") REFERENCES "catalog_items"("id") ON DELETE SET NULL ON UPDATE CASCADE;
