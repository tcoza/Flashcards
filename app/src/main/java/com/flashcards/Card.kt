package com.flashcards

class Card(var front: String = "", var back: String = "") {
    var hint: String? = null
    var time: Long = 0L     // Time spent in millis

    fun save() = "$front\t$back\t${hint.emptyIfNull()}"

    companion object {
        fun load(string: String): Card? {
            val fields = string.split("\t")
            if (fields.size < 2) return null
            return Card(fields[0], fields[1]).apply {
                hint = fields.getOrNull(2)?.nullIfEmpty()
            }
        }
    }
}