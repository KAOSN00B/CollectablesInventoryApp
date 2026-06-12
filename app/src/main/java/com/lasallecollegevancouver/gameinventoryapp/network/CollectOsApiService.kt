package com.lasallecollegevancouver.gameinventoryapp.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// Retrofit interface matching every endpoint on the CollectOS backend
interface CollectOsApiService {

    // --- Collections ---

    @POST("collections")
    suspend fun createCollection(@Body request: CreateCollectionRequest): Collection

    @GET("collections/{code}")
    suspend fun getCollection(@Path("code") code: String): Collection

    // --- Collection Items ---

    @GET("collections/{code}/items")
    suspend fun getItems(@Path("code") code: String): List<CollectionItem>

    @POST("collections/{code}/items")
    suspend fun addItem(
        @Path("code") code: String,
        @Body request: AddItemRequest
    ): CollectionItem

    @PUT("collections/{code}/items/{id}")
    suspend fun updateItem(
        @Path("code") code: String,
        @Path("id") itemId: Int,
        @Body request: UpdateItemRequest
    ): CollectionItem

    @DELETE("collections/{code}/items/{id}")
    suspend fun deleteItem(
        @Path("code") code: String,
        @Path("id") itemId: Int
    ): Response<Unit>

    // --- Catalog ---

    @GET("catalog/search")
    suspend fun searchCatalog(
        @Query("q") query: String,
        @Query("platform") platform: String? = null,
        @Query("limit") limit: Int = 20
    ): List<CatalogItem>

    @GET("catalog/barcode/{upc}")
    suspend fun lookupBarcode(@Path("upc") upc: String): CatalogItem

    @GET("catalog/{id}")
    suspend fun getCatalogItem(@Path("id") catalogItemId: Int): CatalogItem

    @GET("catalog/{id}/community")
    suspend fun getCommunityStats(@Path("id") catalogItemId: Int): CommunityStats

    @GET("catalog/platforms")
    suspend fun getPlatformCounts(): List<PlatformCount>

    // --- Wishlist ---

    @GET("collections/{code}/wishlist")
    suspend fun getWishlist(@Path("code") code: String): List<WishlistItem>

    @POST("collections/{code}/wishlist")
    suspend fun addWishlistItem(
        @Path("code") code: String,
        @Body request: AddWishlistRequest
    ): WishlistItem

    @PUT("collections/{code}/wishlist/{id}")
    suspend fun updateWishlistItem(
        @Path("code") code: String,
        @Path("id") itemId: Int,
        @Body request: UpdateWishlistRequest
    ): WishlistItem

    @DELETE("collections/{code}/wishlist/{id}")
    suspend fun deleteWishlistItem(
        @Path("code") code: String,
        @Path("id") itemId: Int
    ): Response<Unit>
}
