package com.flashcards

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneId

// Intent extras
const val DECK_ID_INT = "DECK_ID"
const val CARD_ID_INT = "CARD_ID"

fun Modifier.hideIf(condition: Boolean) = this.alpha(if (condition) 0f else 1f)
@Composable fun isPreview() = LocalInspectionMode.current
fun Context.showToast(text: String, duration: Int = Toast.LENGTH_SHORT) = Toast.makeText(this, text, duration).show()

fun String.nullIfEmpty() = if (this == "") null else this
fun String?.emptyIfNull() = this ?: ""

fun LocalDate.toEpochMilli() = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun <T> Iterable<T>.random() = this.elementAt((this.count() * Math.random()).toInt())
fun <T, R: Comparable<R>> Iterable<T>.minByOrRandom(selector: (T) -> R): T = minOrMaxByOrRandom(selector, false)
fun <T, R: Comparable<R>> Iterable<T>.maxByOrRandom(selector: (T) -> R): T = minOrMaxByOrRandom(selector, true)
private fun <T, R: Comparable<R>> Iterable<T>.minOrMaxByOrRandom(selector: (T) -> R, max: Boolean): T {
    val values = this.map { Pair(it, selector.invoke(it)) }
    val value = if (max) values.maxOf { it.second } else values.minOf { it.second }
    return values.filter { it.second == value }.random().first
}

fun toHumanString(timeMs: Long): String {
    if (timeMs < 0) return "-" + toHumanString(-timeMs)
    return toHumanString(timeMs, listOf(
        Pair(1, "ms"),
        Pair(1000, "s"),
        Pair(60, "m"),
        Pair(60, "h"),
        Pair(24, "d"),
        Pair(7, "w"),
    ))
}

private fun toHumanString(value: Long, suffixes: List<Pair<Long, String>>): String {
    var value = value
    for (i in suffixes.indices) {
        value /= suffixes[i].first
        if (i+1 < suffixes.size && value >= suffixes[i+1].first) continue
        return "${value}${suffixes[i].second}"
    }
    throw AssertionError("Should not reach")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Spinner(
    label: String,
    options: List<Pair<T, String>>,
    selected: MutableState<T>,
    modifier: Modifier = Modifier) {

    // State to manage the dropdown menu visibility
    var expanded by remember { mutableStateOf(false) }

    // Box to wrap the TextField and Dropdown
    Box(modifier = modifier) {
        TextField(
            value = options.firstOrNull { it.first == selected.value }?.second ?: "",
            onValueChange = { /* No-op, since it's read-only */ },
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable(onClick = { expanded = true }) // Make the TextField clickable
        )

        // Dropdown Menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.second) },
                    onClick = {
                        selected.value = option.first
                        expanded = false
                    }
                )
            }
        }
    }
}