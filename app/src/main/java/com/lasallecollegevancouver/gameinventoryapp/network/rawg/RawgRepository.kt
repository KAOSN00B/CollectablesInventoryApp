package com.lasallecollegevancouver.gameinventoryapp.network.rawg

import com.lasallecollegevancouver.gameinventoryapp.BuildConfig

class RawgRepository(private val api: RawgApiService) {

    // Simple in-memory cache so the same title never hits the API twice in one session
    private val cache = mutableMapOf<String, String?>()

    // Returns the background_image URL for a game title, or null if not found
    suspend fun getCoverUrl(title: String): String? {
        cache[title]?.let { return it }

        return try {
            val response = api.searchGames(
                query = title,
                apiKey = BuildConfig.RAWG_API_KEY
            )
            val url = response.results.firstOrNull()?.backgroundImage
            cache[title] = url
            url
        } catch (exception: Exception) {
            null
        }
    }
}
