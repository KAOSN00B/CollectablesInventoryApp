package com.lasallecollegevancouver.gameinventoryapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

// Data Access Object â€” all database operations for the collectibles table
@Dao
interface CollectibleDao {

    // Returns all collectibles ordered newest first
    @Query("SELECT * FROM collectibles ORDER BY dateAdded DESC")
    suspend fun getAllCollectibles(): List<Collectible>

    // Returns only collectibles of a specific type (e.g. "COMIC", "TCG")
    @Query("SELECT * FROM collectibles WHERE type = :type ORDER BY dateAdded DESC")
    suspend fun getByType(type: String): List<Collectible>

    // Returns a single collectible by its primary key
    @Query("SELECT * FROM collectibles WHERE id = :collectibleId")
    suspend fun getById(collectibleId: Int): Collectible?

    // Returns the sum of all estimated values across all collectibles
    @Query("SELECT COALESCE(SUM(estimatedValue), 0.0) FROM collectibles")
    suspend fun getTotalValue(): Double

    // Returns the sum of estimated values for a specific type
    @Query("SELECT COALESCE(SUM(estimatedValue), 0.0) FROM collectibles WHERE type = :type")
    suspend fun getTotalValueByType(type: String): Double

    // Returns the count of collectibles for a specific type
    @Query("SELECT COUNT(*) FROM collectibles WHERE type = :type")
    suspend fun getCountByType(type: String): Int

    @Insert
    suspend fun insert(collectible: Collectible): Long

    @Update
    suspend fun update(collectible: Collectible): Int

    @Delete
    suspend fun delete(collectible: Collectible): Int
}
