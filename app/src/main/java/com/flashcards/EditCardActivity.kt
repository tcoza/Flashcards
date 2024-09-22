package com.flashcards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import com.flashcards.ui.theme.FlashcardsTheme

class EditCardActivity : ComponentActivity() {
    companion object {
        const val DECK_NAME_STR = "DECK_NAME"
        const val CARD_INDEX_INT = "CARD_INDEX"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlashcardsTheme {
                Content()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    @Preview(showBackground = true)
    fun Content() {
        val deckName = intent?.extras?.getString(DECK_NAME_STR)
        val deck = remember { if (deckName != null) Deck.get(deckName) else Deck.dummy }
        val cardIndex = intent?.extras?.getInt(CARD_INDEX_INT, -1) ?: -1
        val isNewCard = cardIndex < 0

        val card = deck.cards.getOrElse(cardIndex) { Card() }
        var front by remember { mutableStateOf(card.front) }
        var back by remember { mutableStateOf(card.back) }
        var hint by remember { mutableStateOf(card.hint ?: "") }

        val focusRequester = FocusRequester()

        Column (
            Modifier
                .fillMaxSize()
                .padding(24.dp, 0.dp),
            verticalArrangement = Arrangement.Center) {
            Text("Deck: ${deck.name}",
                fontSize = 24.sp)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                label = { Text("Front") },
                value = front, onValueChange = { front = it },
                //onValueChange = { value -> card.value.front = value },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester))
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                label = { Text("Hint") },
                value = hint, onValueChange = { hint = it },
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                label = { Text("Back") },
                value = back, onValueChange = { back = it },
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth()) {
                if (!isNewCard) {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red),
                        onClick = {
                            deck.cards.removeAt(cardIndex)
                            deck.save(this@EditCardActivity)
                            finish()
                        }
                    ) {
                        Text("Delete")
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = {
                    card.front = front
                    card.back = back
                    card.hint = hint
                    if (isNewCard) deck.cards.add(card)
                    deck.save(this@EditCardActivity)
                    if (!isNewCard) finish()
                    else { front = ""; back = ""; hint = "" }
                    focusRequester.requestFocus()
                }) {
                    Text(if (isNewCard) "Add" else "Save")
                }
            }
        }
    }
}