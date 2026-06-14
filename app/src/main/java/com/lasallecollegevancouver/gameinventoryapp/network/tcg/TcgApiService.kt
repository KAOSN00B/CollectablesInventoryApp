package com.lasallecollegevancouver.gameinventoryapp.network.tcg

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

// Scryfall — Magic: The Gathering
// Base URL: https://api.scryfall.com/
// No API key required. unique=prints returns every printing of the card.
interface ScryfallApiService {
    @GET("cards/search")
    suspend fun searchCards(
        @Query("q") query: String,
        @Query("unique") unique: String = "prints",
        @Query("order") order: String = "released"
    ): Response<ScryfallSearchResponse>
}

// Pokémon TCG API
// Base URL: https://api.pokemontcg.io/v2/
// API key is optional — Retrofit omits the header when null, API still works unauthenticated.
interface PokemonTcgApiService {
    @GET("cards")
    suspend fun searchCards(
        @Query("q") query: String,
        @Query("pageSize") pageSize: Int = 50,
        @Header("X-Api-Key") apiKey: String?
    ): Response<PokemonTcgSearchResponse>
}

// YGOProDeck — Yu-Gi-Oh!
// Base URL: https://db.ygoprodeck.com/api/v7/
// No API key required. fname = fuzzy name search.
interface YgoProApiService {
    @GET("cardinfo.php")
    suspend fun searchCards(
        @Query("fname") name: String
    ): Response<YgoProResponse>

    @GET("cardinfo.php")
    suspend fun getCardByExactName(
        @Query("name") name: String
    ): Response<YgoProResponse>
}
