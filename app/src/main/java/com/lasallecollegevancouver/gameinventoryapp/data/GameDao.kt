package com.lasallecollegevancouver.gameinventoryapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

// Data Access Object â€” defines all database operations for the games table
@Dao
interface GameDao {

    // Returns all games ordered newest first
    @Query("SELECT * FROM games ORDER BY dateAdded DESC")
    suspend fun getAllGames(): List<Game>

    // Returns a single game by its primary key, or null if not found
    @Query("SELECT * FROM games WHERE id = :gameId")
    suspend fun getGameById(gameId: Int): Game?

    // Inserts a new game row; Room auto-generates the id
    @Insert
    suspend fun insertGame(game: Game): Long

    // Updates an existing game row matched by its id
    @Update
    suspend fun updateGame(game: Game): Int

    // Deletes the game row matched by its id
    @Delete
    suspend fun deleteGame(game: Game): Int

    // Returns the total estimated value of the entire games collection
    @Query("SELECT COALESCE(SUM(estimatedValue), 0.0) FROM games")
    suspend fun getTotalValue(): Double

    // Returns the total number of games
    @Query("SELECT COUNT(*) FROM games")
    suspend fun getCount(): Int

    // Returns the 5 most recently added games for the Dashboard preview
    @Query("SELECT * FROM games ORDER BY dateAdded DESC LIMIT 5")
    suspend fun getRecentGames(): List<Game>
}
