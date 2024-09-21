package com.flashcards

import android.content.Context
import androidx.compose.ui.platform.LocalInspectionMode
import java.io.*
import java.util.Scanner

class Deck(var name: String) {
    val cards = mutableListOf<Card>()

    fun save(context: Context) = context.openFileOutput(fileName, Context.MODE_PRIVATE).use { export(it) }

    fun load(context: Context) {
        cards.clear()
        try { context.openFileInput(fileName).use { import(it) } }
        catch (_: FileNotFoundException) {}
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

    fun getRandomCardIndex() = (Math.random() * cards.size).toInt()

    private val fileName
        get() = "$fileNamePrefix$name$fileNameSuffix"

    companion object {
        public const val MIME_TYPE = "text/plain"
        private const val fileNamePrefix = "deck_"
        private const val fileNameSuffix = ".txt"

        fun Context.getDecks(load: Boolean = false): Array<Deck> {
            return this.fileList()
                .filter { filename -> filename.startsWith(fileNamePrefix) }
                .filter { filename -> filename.endsWith(fileNameSuffix) }
                .map { filename -> Deck(filename.removePrefix(fileNamePrefix).removeSuffix(fileNameSuffix))
                    .apply { if (load) load(this@getDecks) } }
                .toTypedArray()
        }
    }
}