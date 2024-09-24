package com.flashcards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
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
        const val DECK_ID_INT = "DECK_ID"
        const val CARD_ID_INT = "CARD_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val deck_id = (intent?.extras?.getInt(DECK_ID_INT))!!
        deck = db().deck().getByID(deck_id)!!
        val card_id = intent?.extras?.getInt(CARD_ID_INT, -1) ?: -1
        isNew = (card_id == -1)
        card = if (isNew) Card.empty()
            else db().card().getByID(card_id)!!
        setContent { FlashcardsTheme { Content() } }
    }

    var deck: Deck = Deck.dummy
    var card: Card = Card.dummy
    var isNew: Boolean = false

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    @Preview(showBackground = true)
    fun Content() {
        val cardIndex = intent?.extras?.getInt(CARD_ID_INT, -1) ?: -1

        var front by remember { mutableStateOf(card.front) }
        var back by remember { mutableStateOf(card.back) }
        var hint by remember { mutableStateOf(card.hint.emptyIfNull()) }

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
                if (!isNew) {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red),
                        onClick = {
                            db().card().delete(card)
                            finish()
                        }
                    ) {
                        Text("Delete")
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = {
                    if (isNew) {
                        db().card().insert(Card(0, deck.id,
                            front, back, hint.nullIfEmpty(),
                            System.currentTimeMillis()))
                        front = ""; back = ""; hint = ""
                        focusRequester.requestFocus()
                    }
                    else {
                        db().card().update(card.copy(
                            front = front,
                            back = back,
                            hint = hint.nullIfEmpty()))
                        finish()
                    }
                }) {
                    Text(if (isNew) "Add" else "Save")
                }
            }
        }
    }
}