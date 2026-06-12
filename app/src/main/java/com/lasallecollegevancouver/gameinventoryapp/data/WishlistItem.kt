package com.lasallecollegevancouver.gameinventoryapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Represents an item the user wants to buy — any category can be on the wishlist
@Entity(tableName = "wishlist")
data class WishlistItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    // "GAME", "CONSOLE", "COMIC", "TCG", "TOY", "LEGO"
    val type: String,
    // Platform or category detail (e.g. "Nintendo Switch", "Marvel", "Pokemon")
    val platform: String,
    // The price the user is willing to pay
    val targetPrice: Double,
    // The current market price fetched from PriceCharting
    val currentMarketPrice: Double,
    val notes: String,
    val priceChartingId: Int? = null,
    val dateAdded: Long = System.currentTimeMillis()
)
