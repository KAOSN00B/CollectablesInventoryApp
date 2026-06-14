package com.lasallecollegevancouver.gameinventoryapp.network.rawg

import com.google.gson.annotations.SerializedName

data class RawgSearchResponse(
    val results: List<RawgGame>
)

data class RawgGame(
    val id: Int,
    val name: String,
    @SerializedName("background_image") val backgroundImage: String?
)
