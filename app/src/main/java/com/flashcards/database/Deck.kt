package com.flashcards.database

import androidx.room.*
import com.flashcards.db
import com.flashcards.emptyIfNull
import com.flashcards.*
import com.flashcards.nullIfEmpty
import java.io.*
import java.lang.Integer.min
import java.time.Instant
import java.time.ZoneId
import java.util.Scanner
import kotlin.math.ln
import kotlin.math.log
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
    @ColumnInfo(name = "activate_cards_per_day") val activateCardsPerDay: Int = 0,
    @ColumnInfo(name = "last_card_activation") var lastCardActivation: Long = 0,
    @ColumnInfo(name = "target_time") val targetTime: Long = 5_000,      // 5s
    @ColumnInfo(name = "show_hint") val showHint: Boolean = true,
    @ColumnInfo(name = "show_back") val showBack: Boolean = false,       // In flashes
    @ColumnInfo(name = "font_size") val fontSize: Int = 64,
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

    @Ignore private val DONT_SHOW_LAST_N = 5
    fun getNextFlash(onlyDue: Boolean): Flash? {
        activateCardsIfDue()
        val currentTimeMillis = System.currentTimeMillis()
        return disableUpdateCache {
            getPossibleFlashes()
            .map { Pair(it, timeDue(it)) }
            .filter { !onlyDue || it.second <= currentTimeMillis }
            .run {
                if (isEmpty()) return@disableUpdateCache null
                val cardsCount = distinctBy { it.first.cardID }.count()
                val lastCount = min(DONT_SHOW_LAST_N, cardsCount - 1)
                val last = db().flash().getLastN(id, lastCount).map { it.cardID }
                filter { !last.contains(it.first.cardID) }
            }
            .minBy { it.second }.first
        }
    }

    // timeDue(flash) <= currentTimeMillis
    fun isDue(flash: Flash, currentTimeMillis: Long = System.currentTimeMillis()) = timeDue(flash) <= currentTimeMillis
    fun countDue(currentTimeMillis: Long = System.currentTimeMillis()) =
        disableUpdateCache { getPossibleFlashes().count { isDue(it, currentTimeMillis) } }

    private fun isBackOptions() = if (showBack) listOf(false, true) else listOf(false)
    private fun getPossibleFlashes() =
        System.currentTimeMillis().let { currentTimeMillis ->
            db().card().getActive(id).map {
                isBackOptions().map { back ->
                Flash(cardID = it.id, isBack = back, createdAt = currentTimeMillis)
            }}.flatten()
        }

    // f should be positive, decreasing, with an asymptote at y=0
    private fun f(timeElapsed: Long) = 2.0.pow(-timeElapsed.toDouble() / targetTime)
    private fun f_inv(value: Double) =
        if (value == 0.0) Long.MAX_VALUE
        else (-log(value, 2.0) * targetTime).toLong()
    // g should be increasing, and g(0)=0
    private fun g(timeSince: Long) = ln(timeSince.toDouble() / targetTime + 1)
    private fun g_inv(value: Double) = ((Math.E.pow(value) - 1) * targetTime).toLong()

    @Ignore public var autoUpdateCache: Boolean = true
    // Map<(cardID, isBack), CacheEntry>
    private class CacheEntry(val value: Double = 0.0, val lastFlashTime: Long)
    @Ignore private val cache = mutableMapOf<Pair<Int, Boolean>, CacheEntry>()
    @Ignore private var lastFlashInCacheID: Int = -1
    fun resetCache() { cache.clear(); lastFlashInCacheID = -1 }
    fun updateCache() {
        for (flash in db().flash().getAfter(id, lastFlashInCacheID)) {
            val key = Pair(flash.cardID, flash.isBack)
            cache[key] = nextEntry(cache[key] ?: CacheEntry(lastFlashTime = flash.card().createdAt), flash)
            lastFlashInCacheID = max(lastFlashInCacheID, flash.id)
        }
    }
    private fun nextEntry(entry: CacheEntry, flash: Flash): CacheEntry {
        val ALPHA = 1 / Math.E
        val since = flash.createdAt - entry.lastFlashTime
        var score = 0.0; if (flash.isCorrect) score = f(flash.timeElapsed) * g(since)
        score = ALPHA * score + (1 - ALPHA) * entry.value
        return CacheEntry(score, flash.createdAt)
    }

    // first: x(correct) * f(timeElapsed) * g(timeSincePrevFlash), exponential rolling average
    // second: lastFlash?.createdAt ?: card.createdAt
    private fun getCacheEntry(flash: Flash): CacheEntry {
        if (autoUpdateCache) updateCache()
        return cache[Pair(flash.cardID, flash.isBack)] ?: CacheEntry(lastFlashTime = flash.card().createdAt)
    }

    private fun <T> disableUpdateCache(action: () -> T): T {
        val save = autoUpdateCache
        try {
            if (autoUpdateCache) updateCache()
            autoUpdateCache = false
            return action.invoke()
        }
        finally { autoUpdateCache = save }
    }

    // Time at which expectedTimeElapsed() returns targetTime
    private fun timeDue(entry: CacheEntry) = g_inv(entry.value / f(targetTime)) + entry.lastFlashTime
    fun timeDue(flash: Flash) = timeDue(getCacheEntry(flash)) + if (flash.isBack) 1 else 0
    fun timeDue(card: Card) = isBackOptions().map { Flash(cardID = card.id, isBack = it) }.minOf { timeDue(it) }
    // Time due if flash is added.
    fun nextTimeDue(flash: Flash) = timeDue(nextEntry(getCacheEntry(flash), flash))

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