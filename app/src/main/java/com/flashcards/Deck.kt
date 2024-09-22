package com.flashcards

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.platform.LocalInspectionMode
import java.io.*
import java.util.Scanner

class Deck private constructor(val name: String) {
    val cards = mutableStateListOf<Card>()

    fun save(context: Context) = context.openFileOutput(fileName, Context.MODE_PRIVATE).use { export(it) }

    fun load(context: Context) {
        cards.clear()
        try { context.openFileInput(fileName).use { import(it) } }
        catch (_: FileNotFoundException) {}
    }

    fun delete(context: Context) {
        context.deleteFile(fileName)
        list.remove(this)
    }

    fun import(stream: InputStream): Int {
        Scanner(stream).use {
            var count = 0
            while (it.hasNextLine()) {
                val card = Card.load(it.nextLine()) ?: continue
                if (cards.any {c -> c.front == card.front}) continue
                cards.add(card)
                count++
            }
            return count
        }
    }

    fun export(stream: OutputStream) {
        PrintWriter(stream).use {
            for (card in cards)
                it.println(card.save())
        }
    }

    fun getRandomCardIndex(): Int {
        cards.withIndex()
            .firstOrNull { it.value.time == 0L }
            ?.let { return it.index }
        val weights = cards.map { it.time * it.time }
        var random = (Math.random() * weights.sum()).toInt()
        var currSum: Long = 0
        for (i in weights.indices) {
            currSum += weights[i]
            if (currSum > random) return i
        }
        return -1   // Shouldn't be here
    }

    private val fileName
        get() = "$fileNamePrefix$name$fileNameSuffix"

    companion object {
        const val MIME_TYPE = "text/plain"
        private const val fileNamePrefix = "deck_"
        private const val fileNameSuffix = ".txt"

        val list = mutableStateListOf<Deck>()

        fun load(context: Context) {
            list.clear()
            context.fileList()
                .filter { it.startsWith(fileNamePrefix) }
                .map { it.removePrefix(fileNamePrefix) }
                .filter { it.endsWith(fileNameSuffix) }
                .map { it.removeSuffix(fileNameSuffix) }
                .map { Deck(it).apply { load(context) } }
                .map { list.add(it) }
        }

        fun get(name: String) = list.first { it.name == name }
        fun create(context: Context, name: String) = Deck(name).apply { save(context); list.add(this) }
        val dummy = Deck("")
    }
}