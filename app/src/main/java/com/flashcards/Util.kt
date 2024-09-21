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