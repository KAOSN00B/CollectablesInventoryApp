package com.lasallecollegevancouver.gameinventoryapp.network.rawg

import retrofit2.http.GET
import retrofit2.http.Query

interface RawgApiService {

    // Search by title — returns top result, page_size=1 keeps API usage minimal
    @GET("games")
    suspend fun searchGames(
        @Query("search") query: String,
        @Query("key") apiKey: String,
        @Query("page_size") pageSize: Int = 1
    ): RawgSearchResponse
}
