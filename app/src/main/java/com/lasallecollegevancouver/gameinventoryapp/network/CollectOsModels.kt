package com.lasallecollegevancouver.gameinventoryapp.network

import com.google.gson.annotations.SerializedName

// --- Collection ---

data class Collection(
    val id: Int,
    val publicCode: String,
    val displayName: String?,
    val createdAt: String,
    val items: List<CollectionItem> = emptyList(),
    val wishlist: List<WishlistItem> = emptyList()
)

data class CreateCollectionRequest(
    val displayName: String?
)

// --- Collection Items ---

data class CollectionItem(
    val id: Int,
    val collectionId: Int,
    val catalogItemId: Int?,
    val type: String,
    val title: String,
    val platform: String,
    val condition: String,
    val purchasePrice: Double,
    val estimatedValue: Double,
    val notes: String?,
    val forTrade: Boolean,
    val createdAt: String,
    val updatedAt: String,
    // TCG-specific fields — null for game/console items
    val tcgGame: String? = null,
    val tcgSet: String? = null,
    val tcgSetCode: String? = null,
    val tcgCardNumber: String? = null,
    val tcgRarity: String? = null,
    val tcgIsFoil: Boolean? = null,
    val tcgExternalId: String? = null,
    val quantity: Int? = null
)

data class AddItemRequest(
    val catalogItemId: Int?,
    val type: String,
    val title: String,
    val platform: String,
    val condition: String,
    val purchasePrice: Double,
    val estimatedValue: Double,
    val notes: String?,
    val forTrade: Boolean,
    // TCG-specific fields — omit entirely for game/console items
    val tcgGame: String? = null,
    val tcgSet: String? = null,
    val tcgSetCode: String? = null,
    val tcgCardNumber: String? = null,
    val tcgRarity: String? = null,
    val tcgIsFoil: Boolean? = null,
    val tcgExternalId: String? = null,
    val quantity: Int? = null,
    // Optional — if set, the backend adds this item to the binder automatically
    val binderId: Int? = null
)

data class UpdateItemRequest(
    val condition: String?,
    val purchasePrice: Double?,
    val estimatedValue: Double?,
    val notes: String?,
    val forTrade: Boolean?,
    val tcgIsFoil: Boolean? = null,
    val quantity: Int? = null
)

// --- Catalog ---

data class CatalogItem(
    val id: Int,
    val type: String,
    val title: String,
    val platform: String,
    val upc: String?,
    val looseValue: Double,
    val cibValue: Double,
    val newValue: Double,
    val genre: String?,
    val releaseYear: Int?,
    val imageUrl: String?
)

data class CommunityStats(
    val catalogItemId: Int,
    val ownedByCollectors: Int,
    val availableForTrade: Int
)

data class PlatformCount(
    val platform: String,
    val count: Int
)

// --- Wishlist ---

data class WishlistItem(
    val id: Int,
    val collectionId: Int,
    val catalogItemId: Int?,
    val title: String,
    val platform: String,
    val targetPrice: Double,
    val currentEstimatedValue: Double,
    val notes: String?,
    val isGrail: Boolean
)

data class AddWishlistRequest(
    val catalogItemId: Int?,
    val title: String,
    val platform: String,
    val targetPrice: Double,
    val currentEstimatedValue: Double,
    val notes: String?,
    val isGrail: Boolean
)

data class UpdateWishlistRequest(
    val targetPrice: Double?,
    val currentEstimatedValue: Double?,
    val notes: String?,
    val isGrail: Boolean?
)

// --- Binders ---

data class Binder(
    val id: Int,
    val collectionId: Int,
    val name: String,
    val createdAt: String,
    val itemCount: Int = 0,
    val totalValue: Double = 0.0
)

data class BinderDetail(
    val id: Int,
    val collectionId: Int,
    val name: String,
    val createdAt: String,
    val items: List<CollectionItem> = emptyList()
)

data class CreateBinderRequest(val name: String)

data class AddToBinderRequest(val itemId: Int)
