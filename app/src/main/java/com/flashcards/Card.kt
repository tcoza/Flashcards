package com.flashcards

import androidx.room.*

@Entity(tableName = "card")
data class Card(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "deck_id") val deck_id: Int,
    @ColumnInfo(name = "front", ) val front: String,
    @ColumnInfo(name = "back") val back: String,
    @ColumnInfo(name = "hint") val hint: String?,
    @ColumnInfo(name = "created_at") val created_at: Long
) {
    companion object {
        val dummy = Card(-1, -1, "Front", "Back", null, 0)
        fun empty() = Card(0, 0, "", "", null, 0L)
    }
}

@Dao
interface CardDao {
    @Query("SELECT * FROM card WHERE id = :id")
    fun getByID(id: Int): Card?
    @Query("SELECT * FROM card WHERE front = :front")
    fun getByFront(front: String): Card?
    @Query("SELECT * FROM card WHERE deck_id = :deck_id")
    fun getAll(deck_id: Int): List<Card>
    @Query("SELECT COUNT(*) FROM card WHERE deck_id = :deck_id")
    fun count(deck_id: Int): Int

    @Insert fun insert(dbo: Card)
    @Update fun update(dbo: Card)
    @Delete fun delete(dbo: Card)
}