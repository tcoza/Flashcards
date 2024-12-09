package com.flashcards.database

import android.content.Context
import androidx.room.Database
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.Locale

@Database(
    entities = [Deck::class, Card::class, Flash::class],
    exportSchema = false,
    version = 7)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deck(): DeckDao
    abstract fun card(): CardDao
    abstract fun flash(): FlashDao

    companion object {
        const val DB_NAME = "database"
        fun build(context: Context) =
            Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                .allowMainThreadQueries()
                .addMigrations(*migrations)
                .build()
    }
}

/**
 * ATTENTION:
 * Use INT or INTEGER for Boolean fields
 * Use TEXT for String fields
 */

fun SupportSQLiteDatabase.addColumn(table: String, column: String, type: String, default: String, nullable: Boolean = false) =
    this.execSQL("ALTER TABLE $table ADD COLUMN $column $type ${if (nullable) "" else "NOT"} NULL DEFAULT $default")

val migrations = arrayOf(
    object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) = database.run {
            addColumn("card", "is_active", "INT", "1")
            addColumn("deck", "text_font_size", "INT", "64")
            addColumn("deck", "read_front", "INT", "0")
            addColumn("deck", "front_locale", "TEXT", "''")
            addColumn("deck", "read_back", "INT", "0")
            addColumn("deck", "back_locale", "TEXT", "''")
            addColumn("deck", "use_hint_as_pronunciation", "INT", "0")
            addColumn("deck", "activate_cards_per_day", "INT", "0")
            addColumn("deck", "last_card_activation", "INT", "0")
        }
    },
    object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) = database.run {
            addColumn("deck", "hint_locale", "TEXT", "''")
        }
    },
    object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) = database.run {
            addColumn("deck", "read_hint", "INT", "0")
        }
    },
    object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) = database.run {
            addColumn("deck", "target_time", "INT", Deck(name = "").targetTime.toString())
        }
    },
    object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) = database.run {
            addColumn("deck", "show_hint", "INT", "1")
            addColumn("deck", "show_back", "INT", "0")
            addColumn("deck", "font_size", "INT", Deck(name = "").fontSize.toString())
        }
    },
    object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) = database.run {
            this.execSQL("ALTER TABLE deck DROP COLUMN use_hint_as_pronunciation")
            addColumn("card", "use_hint_as_pronunciation", "INT", "0")
        }
    }
)