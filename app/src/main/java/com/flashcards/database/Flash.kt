package com.flashcards.database

import androidx.compose.material3.Text
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

@Dao
interface FlashDao {
    @Query("SELECT flash.* FROM flash " +
            "WHERE card_id = :card_id " +
            "AND is_back = :is_back " +
            "ORDER BY flash.created_at")
    fun getAllFromCard(card_id: Int, is_back: Boolean): List<Flash>

    @Query("SELECT flash.* FROM flash " +
            "JOIN card ON card_id = card.id " +
            "WHERE deck_id = :deck_id " +
            "ORDER BY flash.created_at " +
            "LIMIT 1")
    fun getFirstOfDeck(deck_id: Int): Flash?

    @Query("SELECT flash.* FROM flash " +
            "WHERE card_id = :card_id " +
            "AND is_back = :is_back " +
            "ORDER BY created_at DESC " +
            "LIMIT 1")
    fun getLast(card_id: Int, is_back: Boolean): Flash?

    @Query("SELECT flash.* FROM flash " +
            "JOIN card ON card_id = card.id " +
            "WHERE deck_id = :deck_id " +
            "ORDER BY flash.created_at DESC " +
            "LIMIT :n")
    fun getLastN(deck_id: Int, n: Int): List<Flash>

    @Query("SELECT flash.* FROM flash " +
            "JOIN card ON card_id = card.id " +
            "WHERE deck_id = :deck_id " +
            "AND flash.id > :flash_id")
    fun getAfter(deck_id: Int, flash_id: Int): List<Flash>

    @Query("SELECT flash.* FROM flash " +
            "JOIN card ON card.id = flash.card_id " +
            "WHERE deck_id = :deck_id " +
            "AND flash.created_at >= :since " +
            "AND flash.created_at < :until " +
            "ORDER BY flash.created_at")
    fun getAllFromDeck(deck_id: Int, since: Long = 0, until: Long = System.currentTimeMillis()): List<Flash>

    @Insert
    fun insert(dbo: Flash)
}

@Entity(tableName = "flash")
data class Flash(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "card_id") val cardID: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "is_back") val isBack: Boolean,
    @ColumnInfo(name = "time_elapsed") val timeElapsed: Long,
    @ColumnInfo(name = "is_correct") val isCorrect: Boolean
)

fun Iterable<Flash>.getStatsString(): String {
    val total = count()
    val accurate = filter { it.isCorrect }.size
    val accurateRate = accurate * 100 / total
    val accurateAvgTime = filter { it.isCorrect }.map { it.timeElapsed }.average()
    val accurateAvgTimeStr = String.format("%.1f", accurateAvgTime / 1000)
    return "$accurate/$total ($accurateRate%), $accurateAvgTimeStr s/a.c."
}