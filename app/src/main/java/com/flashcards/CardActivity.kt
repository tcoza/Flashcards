package com.flashcards

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import com.flashcards.ui.theme.FlashcardsTheme

class CardActivity : ComponentActivity() {
    companion object {
        const val DECK_ID_INT = "DECK_ID"
    }

    lateinit var deck: Deck
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deck = db().deck().getByID(intent.extras?.getInt(DECK_ID_INT)!!)!!
        setContent { FlashcardsTheme { Content() } }
    }

    override fun onPause() {
        super.onPause()
        resumeStopwatch = stopwatch.isRunning
        stopwatch.pause()
    }

    override fun onResume() {
        super.onResume()
        if (resumeStopwatch) stopwatch.start()
    }

    val stopwatch = Stopwatch()
    var resumeStopwatch = false

    var card by mutableStateOf(Card.dummy)
    var showBack  = false

    @Preview
    @Composable
    fun Content() {
        val fontSize = 64.sp
        val buttonFontSize = 24.sp

        var showHint = remember { mutableStateOf(true) }
        var showFront = remember { mutableStateOf(true) }
        var showBack = remember { mutableStateOf(true) }
        fun cardDone() = showFront.value && showBack.value
        fun nextCard() {
            val pair = deck.getRandomCard()
            card = pair.first
            this.showBack = pair.second
            showFront.value = !this.showBack
            showHint.value = card.hint == null
            showBack.value = this.showBack
            stopwatch.start()
        }
        if (!isPreview()) LaunchedEffect(Unit) { nextCard() }
        if (deck.size == 0) finish()
        if (db().card().getByID(card.id) == null) nextCard()
        if (cardDone() && stopwatch.isRunning) {
            db().flash().insert(Flash(0,
                card.id,
                System.currentTimeMillis(),
                this.showBack,
                stopwatch.getElapsedTimeMillis(),
                true))
            stopwatch.reset()
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        if (showHint.value) {
                            showFront.value = true
                            showBack.value = true
                        } else showHint.value = true
                    })
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { _, _ -> },
                        onDragStart = {},
                        onDragEnd = { if (cardDone()) nextCard() })
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            @Composable
            fun ButtonOrText(text: String, buttonText: String, state: MutableState<Boolean>) {
                Box(Modifier.fillMaxWidth()) {
                    Text(text,
                        // fontSize directly will do wierd things with multiline
                        style = TextStyle(fontSize = fontSize),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .hideIf(!state.value)
                        )
                    Button(modifier = Modifier
                        .align(Alignment.Center)
                        .hideIf(state.value),
                        onClick = { state.value = true }) {
                        Text(buttonText, fontSize = buttonFontSize)
                    }
                }
            }

            Button(modifier = Modifier.hideIf(!cardDone()),
                onClick = {
                    if (!cardDone()) return@Button
                    Intent(this@CardActivity, EditCardActivity::class.java).apply {
                        putExtra(EditCardActivity.DECK_ID_INT, deck.id)
                        putExtra(EditCardActivity.CARD_ID_INT, card.id)
                        this@CardActivity.startActivity(this)
                    }
                }
            ) {
                Text("Edit", fontSize = buttonFontSize)
            }

            Spacer(Modifier.weight(1f))
            ButtonOrText(card.front, "Show front", showFront)
            Spacer(Modifier.weight(1f))
            if (card.hint != null) {
                ButtonOrText(card.hint!!, "Show hint", showHint)
                Spacer(Modifier.weight(1f))
            }
            ButtonOrText(card.back, "Show back", showBack)
            Spacer(Modifier.weight(2f))

            Text(when {
                    !showHint.value -> "Tap to show hint"
                    !showFront.value -> "Tap to show front"
                    !showBack.value -> "Tap to show back"
                    else -> "Swipe for next"
                },
                fontSize = buttonFontSize)
        }
    }
}

