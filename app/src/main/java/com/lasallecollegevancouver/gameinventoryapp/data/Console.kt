package com.lasallecollegevancouver.gameinventoryapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Room entity — each instance of this class becomes one row in the "consoles" table
@Entity(tableName = "consoles")
data class Console(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val brand: String,
    val model: String,
    val condition: String,
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
