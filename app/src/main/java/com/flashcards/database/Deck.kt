package com.flashcards.database

import androidx.room.*
import com.flashcards.db
import com.flashcards.emptyIfNull
import com.flashcards.maxByOrRandom
import com.flashcards.minByOrRandom
import com.flashcards.nullIfEmpty
import java.io.*
import java.lang.Integer.min
import java.time.Instant
import java.time.ZoneId
import java.util.Scanner
import kotlin.math.ln
import kotlin.math.pow

@Entity(tableName = "deck")
data class Deck(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "text_font_size") val textFontSize: Int = 64,
    @ColumnInfo(name = "read_front") val readFront: Boolean = false,
    @ColumnInfo(name = "front_locale") val frontLocale: String = "",
    @ColumnInfo(name = "read_back") val readBack: Boolean = false,
    @ColumnInfo(name = "back_locale") val backLocale: String = "",
    @ColumnInfo(name = "read_hint") val readHint: Boolean = false,
    @ColumnInfo(name = "hint_locale") val hintLocale: String = "",
    @ColumnInfo(name = "use_hint_as_pronunciation") val useHintAsPronunciation: Boolean = false,
    @ColumnInfo(name = "activate_cards_per_day") val activateCardsPerDay: Int = 0,
    @ColumnInfo(name = "last_card_activation") var lastCardActivation: Long = 0
) {
    fun import(stream: InputStream): Int {
        Scanner(stream).use { scanner ->
            var count = 0
            while (scanner.hasNextLine()) {
                val line = scanner.nextLine()
                val fields = line.split("\t")
                if (fields.size < 2) continue
                val card = Card(
                    0, id, false, fields[0], fields[1],
                    fields.getOrNull(2)?.nullIfEmpty())
                if (db().card().getByFront(id, card.front) != null) continue
                db().card().insert(card)
                count++
            }
            return count
        }
    }

    fun export(stream: OutputStream) {
        PrintWriter(stream).use { out ->
            db().card().getAll(id).forEach {
                out.println("${it.front}\t${it.back}\t${it.hint.emptyIfNull()}")
            }
        }
    }

    fun getRandomCard(): Pair<Card, Boolean> {
        activateCardsIfDue()
        val removeLastCount = min(5, db().card().countActive(id) - 1)
        if (removeLastCount < 0) throw Exception("Deck is empty")
        val lastN = db().flash().getLastN(id, removeLastCount)
        return db().card().getActive(id)
            .filter { !lastN.any { f -> it.id == f.cardID } }
            .map { listOf(Pair(it, false), Pair(it, true)) }
            .flatten().minByOrRandom { flashValueFunction(it.first, it.second) }
    }

    private fun flashValueFunction(card: Card, isBack: Boolean): Double {
        val ALPHA = 1 / Math.E
        val EXPECTED_TIME: Long = 5_000     // 5 seconds

        // f should be positive, decreasing, with an asymptote at y=0
        fun f(timeElapsed: Long) = 0.5.pow(timeElapsed.toDouble() / EXPECTED_TIME)
        // g should be increasing, and g(0)=0
        fun g(timeSince: Long) = ln(timeSince.toDouble() / EXPECTED_TIME + 1)

        var avgScore = 0.0
        var last: Flash? = null
        fun lastFlashTime() = last?.createdAt ?: card.createdAt
        for (flash in db().flash().getAllFromCard(card.id, isBack)) {
            val since = flash.createdAt - lastFlashTime()
            val score = (if (flash.isCorrect) f(flash.timeElapsed) else 0.0) * g(since)

            // Average score will be near 0 for new cards
            avgScore = ALPHA * score + (1 - ALPHA) * avgScore
            last = flash
        }
        return avgScore / g(System.currentTimeMillis() - lastFlashTime())
    }

    private fun activateCardsIfDue() {
        if (activateCardsPerDay <= 0) return
        val date1 = Instant.ofEpochMilli(lastCardActivation).atZone(ZoneId.systemDefault()).toLocalDate()
        val date2 = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault()).toLocalDate()
        if (date1.isEqual(date2)) return
        db().card().getInactive(id, activateCardsPerDay).forEach {
            db().card().update(it.copy(isActive = true))
        }
        lastCardActivation = System.currentTimeMillis()
        db().deck().update(this)
    }

    companion object {
        val dummy = Deck(0, "Deck")
    }
}

@Dao
interface DeckDao {
    @Query("SELECT * FROM deck WHERE id = :id")
    fun getByID(id: Int): Deck?
    @Query("SELECT * FROM deck")
    fun getAll(): List<Deck>

    @Insert fun insert(dbo: Deck): Long
    @Update fun update(dbo: Deck)
    @Delete fun delete(dbo: Deck)
}