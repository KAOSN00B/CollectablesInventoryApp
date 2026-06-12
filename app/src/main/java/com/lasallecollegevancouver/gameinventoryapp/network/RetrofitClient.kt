package com.lasallecollegevancouver.gameinventoryapp.network

import com.lasallecollegevancouver.gameinventoryapp.config.AppConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // Shared OkHttpClient with logging so API calls are visible in Logcat during development
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    // CollectOS backend — points to AppConfig.API_BASE_URL (swap that one line for local vs production)
    private val collectOsRetrofit = Retrofit.Builder()
        .baseUrl(AppConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val collectOsService: CollectOsApiService =
        collectOsRetrofit.create(CollectOsApiService::class.java)

    // PriceCharting API — kept for any legacy price check flows
    private val priceChartingRetrofit = Retrofit.Builder()
        .baseUrl("https://www.pricecharting.com/api/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val priceChartingService: PriceChartingApiService =
        priceChartingRetrofit.create(PriceChartingApiService::class.java)
}
