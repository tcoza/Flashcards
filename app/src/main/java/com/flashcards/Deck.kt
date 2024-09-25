package com.flashcards

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.room.*
import java.io.*
import java.util.Scanner

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
        val ALPHA = 0.3
        val EXPECTED_TIME: Long = 5_000     // 5 seconds

        var average = 0.0
        lateinit var finalFlash: Flash
        for (flash in db().flash().getAllFromCard(card.id, isBack)) {
            var score = Math.pow(0.5, flash.timeElapsed.toDouble() / EXPECTED_TIME)
            if (!flash.isCorrect) score = 0.0
            average = ALPHA * (1 - score) + (1 - ALPHA) * average
            finalFlash = flash
        }
        val since = System.currentTimeMillis() - finalFlash.createdAt
        return Math.log(since.toDouble()) * average
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