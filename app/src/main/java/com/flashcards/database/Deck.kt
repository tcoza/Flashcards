package com.flashcards.database

import androidx.compose.runtime.MutableState
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
import kotlin.math.max
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
                if (db().card().getByBack(id, card.back) != null) continue
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

    fun getRandomCard(value: MutableState<Double>? = null): Pair<Card, Boolean> {
        activateCardsIfDue()
        val removeLastCount = min(5, db().card().countActive(id) - 1)
        if (removeLastCount < 0) throw Exception("Deck is empty")
        val lastN = db().flash().getLastN(id, removeLastCount)
        return db().card().getActive(id)
            .map { listOf(Pair(it, false), Pair(it, true)) }
            .flatten().map { Pair(it, flashValueFunction(it.first, it.second)) }
            .apply { value?.value = this.minOf { it.second } }
            .filter { !lastN.any { f -> it.first.first.id == f.cardID } }
            .minByOrRandom { it.second }.first
    }

    // Map<(cardID, isBack), (value, lastFlashTime)>
    @Ignore private val cache = mutableMapOf<Pair<Int, Boolean>, Pair<Double, Long>>()
    @Ignore private var lastFlashInCacheID: Int = -1
    private fun flashValueFunction(card: Card, isBack: Boolean): Double {
        val ALPHA = 1 / Math.E
        val EXPECTED_TIME: Long = 5_000     // 5 seconds

        // f should be positive, decreasing, with an asymptote at y=0
        fun f(timeElapsed: Long) = 0.5.pow(timeElapsed.toDouble() / EXPECTED_TIME)
        // g should be increasing, and g(0)=0
        fun g(timeSince: Long) = ln(timeSince.toDouble() / EXPECTED_TIME + 1)

        // Update cache
        for (flash in db().flash().getAfter(id, lastFlashInCacheID)) {
            val key = Pair(flash.cardID, flash.isBack)
            if (!cache.containsKey(key)) cache[key] = Pair(0.0,
                db().card().getByID(flash.cardID)!!.createdAt)
            val since = flash.createdAt - cache[key]!!.second
            var score = (if (flash.isCorrect) f(flash.timeElapsed) else 0.0) * g(since)
            score = ALPHA * score + (1 - ALPHA) * cache[key]!!.first
            cache[key] = Pair(score, flash.createdAt)
            lastFlashInCacheID = max(lastFlashInCacheID, flash.id)
        }

        val entry = cache[Pair(card.id, isBack)] ?: Pair(0.0, card.createdAt)
        return entry.first / g(System.currentTimeMillis() - entry.second)
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