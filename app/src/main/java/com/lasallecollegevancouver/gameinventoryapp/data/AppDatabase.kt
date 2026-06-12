package com.lasallecollegevancouver.gameinventoryapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Version 2 adds: priceChartingId + lastPriceCheck to games and consoles,
// and creates the new collectibles and wishlist tables
@Database(
    entities = [Game::class, Console::class, Collectible::class, WishlistItem::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun gameDao(): GameDao
    abstract fun consoleDao(): ConsoleDao
    abstract fun collectibleDao(): CollectibleDao
    abstract fun wishlistDao(): WishlistDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        // Migration from v1 (Phase 1) to v2 (Phase 2)
        // Adds new columns to existing tables and creates the two new tables
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add price tracking columns to existing tables
                database.execSQL("ALTER TABLE games ADD COLUMN priceChartingId INTEGER")
                database.execSQL("ALTER TABLE games ADD COLUMN lastPriceCheck INTEGER")
                database.execSQL("ALTER TABLE consoles ADD COLUMN priceChartingId INTEGER")
                database.execSQL("ALTER TABLE consoles ADD COLUMN lastPriceCheck INTEGER")

                // Create the collectibles table for Comics, TCG, Toys, and LEGO
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS collectibles (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        name TEXT NOT NULL,
                        condition TEXT NOT NULL,
                        purchasePrice REAL NOT NULL,
                        estimatedValue REAL NOT NULL,
                        notes TEXT NOT NULL,
                        dateAdded INTEGER NOT NULL,
                        priceChartingId INTEGER,
                        lastPriceCheck INTEGER,
                        issueNumber TEXT,
                        publisher TEXT,
                        series TEXT,
                        grade TEXT,
                        cardSet TEXT,
                        rarity TEXT,
                        tcgGame TEXT,
                        quantity INTEGER,
                        franchise TEXT,
                        brand TEXT,
                        isSealed INTEGER,
                        setNumber TEXT,
                        theme TEXT,
                        hasBox INTEGER,
                        hasInstructions INTEGER,
                        isComplete INTEGER
                    )
                """.trimIndent())

                // Create the wishlist table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS wishlist (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        type TEXT NOT NULL,
                        platform TEXT NOT NULL,
                        targetPrice REAL NOT NULL,
                        currentMarketPrice REAL NOT NULL,
                        notes TEXT NOT NULL,
                        priceChartingId INTEGER,
                        dateAdded INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        // Returns the single shared database instance, applying the migration if needed
        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "game_inventory_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
