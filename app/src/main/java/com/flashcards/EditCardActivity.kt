package com.flashcards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import com.flashcards.database.Card
import com.flashcards.database.Deck
import com.flashcards.ui.theme.FlashcardsTheme

class EditCardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val deck_id = (intent?.extras?.getInt(DECK_ID_INT))!!
        deck.value = db().deck().getByID(deck_id)!!
        val card_id = intent?.extras?.getInt(CARD_ID_INT, -1) ?: -1
        card = if (card_id == -1) null else db().card().getByID(card_id)!!
        setContent { FlashcardsTheme { Content() } }
    }

    var deck = mutableStateOf(Deck.dummy)
    var card: Card? = Card.dummy

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    @Preview(showBackground = true)
    fun Content() {
        val decks = db().deck().getAll().map { Pair(it, it.name) }
        var isActive by remember { mutableStateOf(card?.isActive ?: false) }
        var front by remember { mutableStateOf((card?.front).emptyIfNull()) }
        var back by remember { mutableStateOf((card?.back).emptyIfNull()) }
        var hint by remember { mutableStateOf((card?.hint).emptyIfNull()) }
        val dupFront = db().card().getByFront(deck.value.id, front)?.let { it.id != card?.id } ?: false
        val dupBack = db().card().getByBack(deck.value.id, back)?.let { it.id != card?.id } ?: false

        LaunchedEffect(deck.value) {
            if (card != null) return@LaunchedEffect
            isActive = db().card().getLast(deck.value.id)?.isActive ?: true
        }

        val focusRequester = FocusRequester()
        val errorColors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = Color.Red,
            unfocusedBorderColor = Color.Red
        )

        Column (
            Modifier
                .fillMaxSize()
                .padding(24.dp, 0.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Spinner("Deck", decks, deck)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                label = { Text("Front") },
                value = front, onValueChange = { front = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                colors = if (dupFront) errorColors else TextFieldDefaults.outlinedTextFieldColors())
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                label = { Text("Hint") },
                value = hint, onValueChange = { hint = it },
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                label = { Text("Back") },
                value = back, onValueChange = { back = it },
                modifier = Modifier.fillMaxWidth(),
                colors = if (dupBack) errorColors else TextFieldDefaults.outlinedTextFieldColors())
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Active:", fontSize = 20.sp)
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = isActive,
                    onCheckedChange = { isActive = it }
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth()) {
                if (card != null) {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red),
                        onClick = {
                            db().card().delete(card!!)
                            finish()
                        }
                    ) {
                        Text("Delete")
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = {
                        if (card == null) {
                            db().card().insert(
                                Card(0, deck.value.id, isActive,
                                front, back, hint.nullIfEmpty())
                            )
                            front = ""; back = ""; hint = ""
                            focusRequester.requestFocus()
                        }
                        else {
                            db().card().update(card!!.copy(
                                deckID = deck.value.id,
                                isActive = isActive,
                                front = front,
                                back = back,
                                hint = hint.nullIfEmpty()))
                            finish()
                        }
                    },
                    enabled = !dupFront && !dupBack && front != "" && back != ""
                ) {
                    Text(if (card == null) "Add" else "Save")
                }
            }
        }
    }
}