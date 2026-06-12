package com.lasallecollegevancouver.gameinventoryapp.network

import com.lasallecollegevancouver.gameinventoryapp.BuildConfig

// Wraps the PriceChartingApiService and provides clean result types to the UI
class PriceChartingRepository {

    private val service = RetrofitClient.priceChartingService
    private val apiKey = BuildConfig.PRICE_CHARTING_API_KEY

    // Searches PriceCharting by item name — returns a list of matching products or empty list on error
    suspend fun searchByName(query: String): List<PriceChartingProduct> {
        return try {
            val response = service.searchProducts(query = query, token = apiKey)
            response.products ?: emptyList()
        } catch (exception: Exception) {
            emptyList()
        }
    }

    // Looks up a product by UPC barcode — returns the first match or null
    suspend fun searchByBarcode(upc: String): PriceChartingProduct? {
        return try {
            val response = service.searchProducts(query = upc, token = apiKey)
            response.products?.firstOrNull()
        } catch (exception: Exception) {
            null
        }
    }

    // Fetches the current price for a product we already have a PriceCharting ID for
    // Returns null if the lookup fails (e.g. no internet)
    suspend fun refreshPrice(priceChartingId: Int): PriceChartingProductResponse? {
        return try {
            service.getProductById(productId = priceChartingId, token = apiKey)
        } catch (exception: Exception) {
            null
        }
    }
}
