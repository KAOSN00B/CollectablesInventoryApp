package com.lasallecollegevancouver.gameinventoryapp.ui.collection

data class CollectionEntry(
    val sourceId: Int,
    val category: String,
    val title: String,
    val meta: String,
    val condition: String,
    val estimatedValue: Double,
    val dateAdded: Long
)
