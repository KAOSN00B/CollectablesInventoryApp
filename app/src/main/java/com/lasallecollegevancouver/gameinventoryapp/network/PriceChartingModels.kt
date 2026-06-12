package com.lasallecollegevancouver.gameinventoryapp.network

import com.google.gson.annotations.SerializedName

// Response wrapper for the PriceCharting product search endpoint
data class PriceChartingSearchResponse(
    val products: List<PriceChartingProduct>?
)

// A single product returned from PriceCharting search or lookup
data class PriceChartingProduct(
    // Unique PriceCharting product ID — store this to refresh prices later
    val id: Int,

    @SerializedName("product-name")
    val productName: String,

    // The console or platform name (e.g. "Nintendo Switch", "PlayStation 5")
    @SerializedName("console-name")
    val consoleName: String?,

    // Prices are returned in cents — divide by 100 to get dollars
    @SerializedName("loose-price")
    val loosePrice: Int?,

    @SerializedName("complete-price")
    val completePrice: Int?,

    @SerializedName("new-price")
    val newPrice: Int?,

    @SerializedName("graded-price")
    val gradedPrice: Int?
) {
    // Convenience helpers that convert cents to dollars
    fun loosePriceDollars() = (loosePrice ?: 0) / 100.0
    fun completePriceDollars() = (completePrice ?: 0) / 100.0
    fun newPriceDollars() = (newPrice ?: 0) / 100.0
    fun gradedPriceDollars() = (gradedPrice ?: 0) / 100.0

    // Returns the most relevant price for display — prefers complete, falls back to loose
    fun bestPrice(): Double = when {
        completePrice != null && completePrice > 0 -> completePriceDollars()
        loosePrice != null && loosePrice > 0 -> loosePriceDollars()
        newPrice != null && newPrice > 0 -> newPriceDollars()
        else -> 0.0
    }
}

// Response wrapper for the single-product endpoint
data class PriceChartingProductResponse(
    val id: Int,
    @SerializedName("product-name") val productName: String,
    @SerializedName("console-name") val consoleName: String?,
    @SerializedName("loose-price") val loosePrice: Int?,
    @SerializedName("complete-price") val completePrice: Int?,
    @SerializedName("new-price") val newPrice: Int?,
    @SerializedName("graded-price") val gradedPrice: Int?
) {
    fun loosePriceDollars() = (loosePrice ?: 0) / 100.0
    fun completePriceDollars() = (completePrice ?: 0) / 100.0
    fun newPriceDollars() = (newPrice ?: 0) / 100.0
    fun gradedPriceDollars() = (gradedPrice ?: 0) / 100.0
    fun bestPrice(): Double = when {
        completePrice != null && completePrice > 0 -> completePriceDollars()
        loosePrice != null && loosePrice > 0 -> loosePriceDollars()
        newPrice != null && newPrice > 0 -> newPriceDollars()
        else -> 0.0
    }
}
