package com.flashcards

import androidx.room.*
import java.io.*
import java.util.Scanner
import kotlin.math.ln
import kotlin.math.pow

@Entity(tableName = "deck")
data class Deck(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String
) {
    val size: Int
        get() = db().card().count(id)

    fun import(stream: InputStream): Int {
        Scanner(stream).use { scanner ->
            var count = 0
            while (scanner.hasNextLine()) {
                val line = scanner.nextLine()
                val fields = line.split("\t")
                if (fields.size < 2) continue
                val card = Card(
                    0, id, fields[0], fields[1],
                    fields.getOrNull(2)?.nullIfEmpty(),
                    System.currentTimeMillis())
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
        val options = db().card().getAll(id)
            .map { listOf(Pair(it, false), Pair(it, true)) }
            .flatten()
        val unflashed = options.filter { db().flash().getLast(it.first.id, it.second) == null }
        if (!unflashed.isEmpty()) return unflashed.random()

//        println("List:")
//        options.toList()
//            .sortedBy { flashValueFunction(it.first, it.second) }
//            .forEach({ println(it.toString()) })
        return options.maxBy { flashValueFunction(it.first, it.second) }
    }

    // Assumes there are flashes
    private fun flashValueFunction(card: Card, isBack: Boolean): Double {
        val ALPHA = 1 / Math.E
        val EXPECTED_TIME: Long = 5_000     // 5 seconds

        var first = true
        var avgScore = 0.0
        lateinit var finalFlash: Flash
        for (flash in db().flash().getAllFromCard(card.id, isBack)) {
            var score = if (!flash.isCorrect) 0.0
                else 0.5.pow(flash.timeElapsed.toDouble() / EXPECTED_TIME)
            // Average score will be near 0 for new cards
            avgScore = ALPHA * score + (1 - ALPHA) * avgScore
            finalFlash = flash
            first = false
        }
        val since = System.currentTimeMillis() - finalFlash.createdAt
        return ln(since.toDouble()) * (avgScore - 1).pow(2) / avgScore
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