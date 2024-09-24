package com.flashcards

import androidx.room.Database
import androidx.room.*

@Database(entities = [Deck::class, Card::class, Flash::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deck(): DeckDao
    abstract fun card(): CardDao
    abstract fun flash(): FlashDao
}





@Dao
interface FlashDao {
    @Query("SELECT * FROM flash WHERE card_id = :card_id")
    fun getAllFromCard(card_id: Int): List<Flash>

    @Query("SELECT * FROM flash JOIN card ON card_id = card.id WHERE deck_id = :deck_id")
    fun getAllFromDeck(deck_id: Int): List<Flash>

    @Query("SELECT * FROM flash WHERE card_id = :card_id ORDER BY created_at DESC LIMIT 1")
    fun getLast(card_id: Int): Flash?

    @Insert fun insert(dbo: Flash)
}

@Entity(tableName = "flash")
data class Flash(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "card_id") val cardID: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "is_back") val isBack: Boolean,
    @ColumnInfo(name = "time_elapsed") val timeElapsed: Long,
    @ColumnInfo(name = "is_correct") val isCorrect: Boolean
) {
    fun getScore() = if (isCorrect) Math.pow(0.5, timeElapsed.toDouble() / 10_000) else 0
}