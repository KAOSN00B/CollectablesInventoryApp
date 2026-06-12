package com.lasallecollegevancouver.gameinventoryapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

// Data Access Object â€” all database operations for the wishlist table
@Dao
interface WishlistDao {

    // Returns all wishlist items ordered newest first
    @Query("SELECT * FROM wishlist ORDER BY dateAdded DESC")
    suspend fun getAllWishlistItems(): List<WishlistItem>

    // Returns a single wishlist item by its primary key
    @Query("SELECT * FROM wishlist WHERE id = :itemId")
    suspend fun getWishlistItemById(itemId: Int): WishlistItem?

    @Insert
    suspend fun insert(item: WishlistItem): Long

    @Update
    suspend fun update(item: WishlistItem): Int

    @Delete
    suspend fun delete(item: WishlistItem): Int
}
