package com.flashcards

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalInspectionMode

fun Modifier.hideIf(condition: Boolean) = this.alpha(if (condition) 0f else 1f)
@Composable fun isPreview() = LocalInspectionMode.current
fun Context.showToast(text: String, duration: Int = Toast.LENGTH_SHORT) = Toast.makeText(this, text, duration).show()

fun String.nullIfEmpty() = if (this == "") null else this
fun String?.emptyIfNull() = this ?: ""

fun <T> Iterable<T>.random() = this.elementAt((this.count() * Math.random()).toInt())
fun <T, R: Comparable<R>> Iterable<T>.maxByOrRandom(selector: (T) -> R): T {
    val values = this.map { Pair(it, selector.invoke(it)) }
    val max = values.maxOf { it.second }
    return values.filter { it.second == max }.random().first
}