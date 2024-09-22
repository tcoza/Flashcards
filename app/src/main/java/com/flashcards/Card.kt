package com.flashcards

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class Card(front: String = "", back: String = "") {
    var front by mutableStateOf("")
    var back by mutableStateOf("")
    var hint by mutableStateOf<String?>(null)
    var time by mutableStateOf(0L)   // Time spent in millis
    init { this.front = front; this.back = back }

    fun save() = "$front\t$back\t${hint.emptyIfNull()}\t$time"

    fun addTime(time: Long) { this.time = (ALPHA * time + (1 - ALPHA) * this.time).toLong() }

    companion object {
        const val ALPHA = 0.5

        fun load(string: String): Card? {
            val fields = string.split("\t")
            if (fields.size < 2) return null
            return Card(fields[0], fields[1]).apply {
                hint = fields.getOrNull(2)?.nullIfEmpty()
                time = fields.getOrNull(3)?.toLong() ?: 0L
            }
        }
    }
}