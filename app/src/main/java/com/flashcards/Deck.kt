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
        val options = mutableMapOf<Pair<Card, Boolean>, Flash?>()
        db().card().getAll(id).forEach {
            options[Pair(it, false)] = db().flash().getLast(it.id, false)
            options[Pair(it, true)] = db().flash().getLast(it.id, true)
        }
        val new = options.filter { it.value == null }.map { it.key }
        if (!new.isEmpty()) return new.random()

//        println("List:")
//        options.toList()
//            .sortedBy { flashValueFunction(it.second!!) }
//            .forEach({ println(it.first.first.front + ": " + flashValueFunction(it.second!!)) })
        return options.maxBy { flashValueFunction(it.value!!) }.key
    }

    private fun flashValueFunction(flash: Flash): Double = flash.run {
        //val expectedTime = 5_000     // 5 seconds

        //val score = if (isCorrect) Math.pow(0.5, timeElapsed.toDouble() / expectedTime) else 0.0
        var since = (System.currentTimeMillis() - createdAt).toDouble()
        return Math.sqrt(since) * timeElapsed * if (isCorrect) 1 else 2 //(1 - score)
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