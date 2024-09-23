package com.flashcards

import androidx.room.Database
import androidx.room.*

@Database(entities = [DeckDbo::class, CardDbo::class, FlashDbo::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deck(): DeckDao
    abstract fun card(): CardDao
    abstract fun flash(): FlashDao
}

fun android.content.Context.getDB() =
    Room.databaseBuilder(this, AppDatabase::class.java, "database").build()

@Dao
interface DeckDao {
    @Query("SELECT * FROM deck WHERE id = :id")
    fun getByID(id: Int): Deck?
    @Query("SELECT * FROM deck")
    fun getAll(): List<Deck>

    @Insert fun insert(dbo: DeckDbo)
    @Update fun update(dbo: DeckDbo)
    @Delete fun delete(dbo: DeckDbo)
}

@Dao
interface CardDao {
    @Query("SELECT * FROM card WHERE id = :id")
    fun getByID(id: Int): Card?
    @Query("SELECT * FROM card WHERE deck_id = :deck_id")
    fun getAll(deck_id: Int): List<Card>

    @Insert fun insert(dbo: CardDbo)
    @Update fun update(dbo: CardDbo)
    @Delete fun delete(dbo: CardDbo)
}

@Dao
interface FlashDao {
    @Query("SELECT * FROM flash WHERE card_id = :card_id")
    fun getAllFromCard(card_id: Int): List<FlashDbo>

    @Query("SELECT * FROM flash JOIN card ON card_id = card.id WHERE deck_id = :deck_id")
    fun getAllFromDeck(deck_id: Int): List<FlashDbo>

    @Insert fun insert(dbo: FlashDbo)
}

@Entity(tableName = "deck")
data class DeckDbo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String
)

@Entity(tableName = "card")
data class CardDbo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "deck_id") val deck_id: Int,
    @ColumnInfo(name = "front") val front: String,
    @ColumnInfo(name = "back") val back: String,
    @ColumnInfo(name = "hint") val hint: String?,
    @ColumnInfo(name = "created_at") val created_at: Long
)

@Entity(tableName = "flash")
data class FlashDbo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "card_id") val card_id: Int,
    @ColumnInfo(name = "created_at") val created_at: Long,
    @ColumnInfo(name = "time_elapsed") val time_elapsed: Long,
    @ColumnInfo(name = "accurate") val accurate: Boolean
)