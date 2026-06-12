package com.lasallecollegevancouver.gameinventoryapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Room entity — each instance of this class becomes one row in the "games" table
@Entity(tableName = "games")
data class Game(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val platform: String,
    val genre: String,
    val condition: String,
    val completionStatus: String,
    val purchasePrice: Double,
    val estimatedValue: Double,
    val notes: String,
    // Stored as milliseconds since epoch so it is sortable without a type converter
    val dateAdded: Long = System.currentTimeMillis(),
    // PriceCharting product ID — stored after first lookup so we can refresh price later
    val priceChartingId: Int? = null,
    // Epoch ms of the last time we fetched a price from PriceCharting
    val lastPriceCheck: Long? = null
)
