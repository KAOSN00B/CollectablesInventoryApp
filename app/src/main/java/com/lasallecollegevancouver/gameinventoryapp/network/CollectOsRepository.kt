package com.lasallecollegevancouver.gameinventoryapp.network

// Single access point for all CollectOS API calls — fragments call this, never the service directly
class CollectOsRepository {

    private val api = RetrofitClient.collectOsService

    // --- Collections ---

    suspend fun createCollection(displayName: String?): Collection {
        return api.createCollection(CreateCollectionRequest(displayName))
    }

    suspend fun getCollection(publicCode: String): Collection {
        return api.getCollection(publicCode)
    }

    // --- Items ---

    suspend fun getItems(publicCode: String): List<CollectionItem> {
        return api.getItems(publicCode)
    }

    suspend fun addItem(publicCode: String, request: AddItemRequest): CollectionItem {
        return api.addItem(publicCode, request)
    }

    suspend fun updateItem(publicCode: String, itemId: Int, request: UpdateItemRequest): CollectionItem {
        return api.updateItem(publicCode, itemId, request)
    }

    suspend fun deleteItem(publicCode: String, itemId: Int) {
        api.deleteItem(publicCode, itemId)
    }

    // --- Catalog ---

    suspend fun searchCatalog(query: String, platform: String? = null): List<CatalogItem> {
        return api.searchCatalog(query, platform)
    }

    suspend fun lookupBarcode(upc: String): CatalogItem? {
        return try {
            api.lookupBarcode(upc)
        } catch (exception: Exception) {
            null
        }
    }

    suspend fun getCatalogItem(catalogItemId: Int): CatalogItem {
        return api.getCatalogItem(catalogItemId)
    }

    suspend fun getCommunityStats(catalogItemId: Int): CommunityStats? {
        return try {
            api.getCommunityStats(catalogItemId)
        } catch (exception: Exception) {
            null
        }
    }

    suspend fun getPlatformCounts(): List<PlatformCount> {
        return try {
            api.getPlatformCounts()
        } catch (exception: Exception) {
            emptyList()
        }
    }

    // --- Wishlist ---

    suspend fun getWishlist(publicCode: String): List<WishlistItem> {
        return api.getWishlist(publicCode)
    }

    suspend fun addWishlistItem(publicCode: String, request: AddWishlistRequest): WishlistItem {
        return api.addWishlistItem(publicCode, request)
    }

    suspend fun updateWishlistItem(publicCode: String, itemId: Int, request: UpdateWishlistRequest): WishlistItem {
        return api.updateWishlistItem(publicCode, itemId, request)
    }

    suspend fun deleteWishlistItem(publicCode: String, itemId: Int) {
        api.deleteWishlistItem(publicCode, itemId)
    }
}
