package com.flashcards.database

import androidx.room.*

@Entity(tableName = "card")
data class Card(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "deck_id") val deckID: Int,
    @ColumnInfo(name = "is_active") val isActive: Boolean = false,
    @ColumnInfo(name = "front") val front: String,
    @ColumnInfo(name = "back") val back: String,
    @ColumnInfo(name = "hint") val hint: String? = null,
    @ColumnInfo(name = "use_hint_as_pronunciation") val useHintAsPronunciation: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        val dummy = Card(-1, -1, true, "Front", "Back")
    }
}

@Dao
interface CardDao {
    @Query("SELECT * FROM card WHERE id = :id")
    fun getByID(id: Int): Card?

    @Query("SELECT * FROM card WHERE deck_id = :deck_id AND front = :front")
    fun getByFront(deck_id: Int, front: String): Card?

    @Query("SELECT * FROM card WHERE deck_id = :deck_id AND back = :back")
    fun getByBack(deck_id: Int, back: String): Card?

    @Query("SELECT * FROM card " +
            "WHERE deck_id = :deck_id " +
            "AND is_active = 1 " +
            "ORDER BY card.created_at DESC")
    fun getActive(deck_id: Int): List<Card>

    @Query("SELECT * FROM card " +
            "WHERE deck_id = :deck_id " +
            "ORDER BY card.created_at DESC")
    fun getAll(deck_id: Int): List<Card>

    @Query("SELECT COUNT(*) FROM card WHERE deck_id = :deck_id")
    fun count(deck_id: Int): Int

    @Query("SELECT COUNT(*) FROM card " +
            "WHERE deck_id = :deck_id " +
            "AND is_active = 1")
    fun countActive(deck_id: Int): Int

    @Query("SELECT * FROM card " +
            "WHERE deck_id = :deck_id " +
            "AND is_active = 0 " +
            "ORDER BY created_at " +
            "LIMIT :n")
    fun getInactive(deck_id: Int, n: Int): List<Card>

    @Query("SELECT * FROM card " +
            "WHERE deck_id = :deck_id " +
            "ORDER BY card.created_at DESC " +
            "LIMIT 1")
    fun getLast(deck_id: Int): Card?

//    @Query("SELECT * FROM card " +
//            "WHERE deck_id = :deck_id " +
//            "AND id NOT IN (" +
//            "SELECT DISTINCT card.id FROM card " +
//            "JOIN flash ON card.id = flash.card_id " +
//            "WHERE deck_id = :deck_id)")
//    fun getUnflashed(deck_id: Int): List<Card>

    @Insert fun insert(dbo: Card): Long
    @Update fun update(dbo: Card)
    @Delete fun delete(dbo: Card)
}