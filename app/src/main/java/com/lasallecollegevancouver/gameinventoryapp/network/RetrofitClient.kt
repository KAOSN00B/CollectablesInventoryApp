package com.lasallecollegevancouver.gameinventoryapp.network

import com.lasallecollegevancouver.gameinventoryapp.BuildConfig

import com.lasallecollegevancouver.gameinventoryapp.config.AppConfig
import com.lasallecollegevancouver.gameinventoryapp.network.rawg.RawgApiService
import com.lasallecollegevancouver.gameinventoryapp.network.rawg.RawgRepository
import com.lasallecollegevancouver.gameinventoryapp.network.tcg.PokemonTcgApiService
import com.lasallecollegevancouver.gameinventoryapp.network.tcg.ScryfallApiService
import com.lasallecollegevancouver.gameinventoryapp.network.tcg.TcgRepository
import com.lasallecollegevancouver.gameinventoryapp.network.tcg.YgoProApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // Shared OkHttpClient with lightweight request logging in debug builds
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Collectos/1.0 (Android collector inventory app)")
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
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

    // Scryfall — Magic: The Gathering (no API key required)
    private val scryfallRetrofit = Retrofit.Builder()
        .baseUrl("https://api.scryfall.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val scryfallService: ScryfallApiService =
        scryfallRetrofit.create(ScryfallApiService::class.java)

    // Pokémon TCG API (free key from pokemontcg.io)
    private val pokemonTcgRetrofit = Retrofit.Builder()
        .baseUrl("https://api.pokemontcg.io/v2/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val pokemonTcgService: PokemonTcgApiService =
        pokemonTcgRetrofit.create(PokemonTcgApiService::class.java)

    // YGOProDeck — Yu-Gi-Oh! (no API key required)
    private val ygoProRetrofit = Retrofit.Builder()
        .baseUrl("https://db.ygoprodeck.com/api/v7/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val ygoProService: YgoProApiService =
        ygoProRetrofit.create(YgoProApiService::class.java)

    // Pre-built TcgRepository — use this in TCG fragments instead of constructing manually
    val tcgRepository: TcgRepository = TcgRepository(
        scryfallService = scryfallService,
        pokemonService = pokemonTcgService,
        ygoService = ygoProService
    )

    // RAWG.io — game cover art
    private val rawgRetrofit = Retrofit.Builder()
        .baseUrl("https://api.rawg.io/api/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val rawgService: RawgApiService = rawgRetrofit.create(RawgApiService::class.java)

    val rawgRepository: RawgRepository = RawgRepository(rawgService)
}
