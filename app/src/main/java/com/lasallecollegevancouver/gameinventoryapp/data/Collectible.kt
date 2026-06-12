package com.lasallecollegevancouver.gameinventoryapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Single table for all non-game/console collectibles (Comics, TCG, Toys, LEGO)
// Category-specific fields are nullable — only the relevant ones are set per item
@Entity(tableName = "collectibles")
data class Collectible(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    // "COMIC", "TCG", "TOY", or "LEGO" — determines which fields are shown in the UI
    val type: String,

    // Universal display name used across all types
    val name: String,

    val condition: String,
    val purchasePrice: Double,
    val estimatedValue: Double,
    val notes: String,
    val dateAdded: Long = System.currentTimeMillis(),
    val priceChartingId: Int? = null,
    val lastPriceCheck: Long? = null,

    // --- Comic-specific fields ---
    val issueNumber: String? = null,
    val publisher: String? = null,
    val series: String? = null,
    // e.g. "Raw", "CGC 9.8", "CBCS 9.6"
    val grade: String? = null,

    // --- TCG-specific fields ---
    // e.g. "Base Set", "Scarlet & Violet"
    val cardSet: String? = null,
    // e.g. "Common", "Rare", "Holo Rare", "Secret Rare"
    val rarity: String? = null,
    // e.g. "Pokemon", "Magic: The Gathering", "Yu-Gi-Oh"
    val tcgGame: String? = null,
    // Number of copies of this card owned
    val quantity: Int? = null,

    // --- Toy/Figure-specific fields ---
    // e.g. "Star Wars", "Marvel", "Nintendo"
    val franchise: String? = null,
    // e.g. "Hasbro", "NECA", "Funko"
    val brand: String? = null,
    // True if still in original sealed packaging
    val isSealed: Boolean? = null,

    // --- LEGO-specific fields ---
    // Official LEGO set number (e.g. "75192")
    val setNumber: String? = null,
    // e.g. "Star Wars", "Technic", "City"
    val theme: String? = null,
    val hasBox: Boolean? = null,
    val hasInstructions: Boolean? = null,
    // True if all pieces are present
    val isComplete: Boolean? = null
)
