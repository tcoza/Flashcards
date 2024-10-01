package com.flashcards.database

import android.content.Context
import androidx.room.Database
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.Locale

@Database(entities = [Deck::class, Card::class, Flash::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deck(): DeckDao
    abstract fun card(): CardDao
    abstract fun flash(): FlashDao

    companion object {
        const val DB_NAME = "database"
        fun build(context: Context) =
            Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                .allowMainThreadQueries()
                .addMigrations(MIGRATION_1_2)
                .addMigrations(MIGRATION_2_3)
                .addMigrations(MIGRATION_3_4)
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

val MIGRATION_1_2 = object : Migration(1, 2) {
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
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) = database.run {
        addColumn("deck", "hint_locale", "TEXT", "''")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) = database.run {
        addColumn("deck", "read_hint", "INT", "0")
    }
}