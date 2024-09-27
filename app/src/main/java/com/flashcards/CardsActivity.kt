package com.flashcards

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flashcards.ui.theme.FlashcardsTheme
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*

class CardsActivity : ComponentActivity() {
    var deck: Deck = Deck.dummy
    var cards = mutableStateListOf<Card>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deck = db().deck().getByID(intent.extras?.getInt(DECK_ID_INT)!!)!!
        setContent { FlashcardsTheme { Content() } }
    }

    override fun onResume() {
        super.onResume()
        cards.clear()
        cards.addAll(db().card().getAll(deck.id))
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Preview
    @Composable
    fun Content() {
        Column(Modifier.padding(16.dp)) {
            var searchString by remember { mutableStateOf("") }
            OutlinedTextField(value = searchString,
                onValueChange = { searchString = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search")} )
            LazyColumn(Modifier.fillMaxSize()) {
                val filtered = cards.filter {
                    arrayOf(it.front, it.back, it.hint.emptyIfNull()).any {
                        it.contains(searchString, true)
                    }
                }
                items(filtered) { card ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                Intent(this@CardsActivity, EditCardActivity::class.java).apply {
                                    putExtra(DECK_ID_INT, deck.id)
                                    putExtra(CARD_ID_INT, card.id)
                                    this@CardsActivity.startActivity(this)
                                }
                            }) {
                        for (item in arrayOf(card.front, card.hint.emptyIfNull(), card.back))
                            Text(text = item, modifier = Modifier
                                .weight(1f)
                                .padding(8.dp))

                    }
                }
            }
        }
    }
}