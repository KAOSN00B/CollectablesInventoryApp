package com.lasallecollegevancouver.gameinventoryapp.network

import retrofit2.http.GET
import retrofit2.http.Query

// Retrofit interface for the PriceCharting REST API
interface PriceChartingApiService {

    // Search for products by name — returns a list of matches
    // token is the API key sent as a query parameter
    @GET("products")
    suspend fun searchProducts(
        @Query("q") query: String,
        @Query("status") status: String = "price",
        @Query("token") token: String
    ): PriceChartingSearchResponse

    // Look up a product by its UPC barcode
    @GET("products")
    suspend fun searchByBarcode(
        @Query("q") upc: String,
        @Query("id") id: String = "",
        @Query("status") status: String = "price",
        @Query("token") token: String
    ): PriceChartingSearchResponse

    // Fetch full pricing details for a known product by its PriceCharting ID
    @GET("product")
    suspend fun getProductById(
        @Query("id") productId: Int,
        @Query("status") status: String = "price",
        @Query("token") token: String
    ): PriceChartingProductResponse
}
