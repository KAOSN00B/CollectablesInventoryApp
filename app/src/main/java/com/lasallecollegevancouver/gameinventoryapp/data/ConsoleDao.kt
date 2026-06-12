package com.lasallecollegevancouver.gameinventoryapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

// Data Access Object â€” defines all database operations for the consoles table
@Dao
interface ConsoleDao {

    // Returns all consoles ordered newest first
    @Query("SELECT * FROM consoles ORDER BY dateAdded DESC")
    suspend fun getAllConsoles(): List<Console>

    // Returns a single console by its primary key, or null if not found
    @Query("SELECT * FROM consoles WHERE id = :consoleId")
    suspend fun getConsoleById(consoleId: Int): Console?

    // Inserts a new console row; Room auto-generates the id
    @Insert
    suspend fun insertConsole(console: Console): Long

    // Updates an existing console row matched by its id
    @Update
    suspend fun updateConsole(console: Console): Int

    // Deletes the console row matched by its id
    @Delete
    suspend fun deleteConsole(console: Console): Int

    // Returns the total estimated value of the entire consoles collection
    @Query("SELECT COALESCE(SUM(estimatedValue), 0.0) FROM consoles")
    suspend fun getTotalValue(): Double

    // Returns the total number of consoles
    @Query("SELECT COUNT(*) FROM consoles")
    suspend fun getCount(): Int
}
