package com.flashcards

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flashcards.ui.theme.FlashcardsTheme
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.flashcards.database.Card
import com.flashcards.database.Deck

class CardsActivity : ComponentActivity() {
    private lateinit var exportDeckLauncher: ActivityResultLauncher<String>
    private lateinit var importDeckLauncher: ActivityResultLauncher<Array<String>>
    val EXPORT_MIME_TYPE = "text/plain"

    var deck: Deck = Deck.dummy
    var cards = mutableStateListOf<Card>()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deck = db().deck().getByID(intent.extras?.getInt(DECK_ID_INT)!!)!!
        setContent { FlashcardsTheme { Scaffold(topBar = { TopBar() }, content = { Content(it) }) } }

        importDeckLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let { uri ->
            contentResolver.openInputStream(uri)!!.use {
                showToast("Imported ${deck.import(it)} cards")
                refreshCards()
            }
        }}
        exportDeckLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument(EXPORT_MIME_TYPE)) {it?.let { uri ->
            contentResolver.openOutputStream(uri)!!.use {
                deck.export(it)
                val count = db().card().count(deck.id)
                showToast("Exported ${count} cards")
            }
        }}
    }

    override fun onResume() {
        super.onResume()
        refreshCards()
    }

    fun refreshCards() {
        cards.clear()
        cards.addAll(db().card().getAll(deck.id))
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    @Preview
    @Composable
    fun Content(paddingValues: PaddingValues = PaddingValues()) {
        Column(Modifier.padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.Bottom) {
            var searchString by remember { mutableStateOf("") }
            val preview = isPreview()   // Who knows...
            LazyColumn(Modifier.weight(1f)) {
                val filtered = cards.filter {
                    arrayOf(it.front, it.back, it.hint.emptyIfNull()).any {
                        it.contains(searchString, true)
                    }
                }
                stickyHeader {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(8.dp)
                    ) {
                        Text("Front", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text("Back", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text("Active",
                            modifier = Modifier.width(56.dp),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center)
                    }
                }
                items(if (preview) listOf(Card.dummy, Card.dummy) else filtered) { card ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                Intent(this@CardsActivity, EditCardActivity::class.java).apply {
                                    putExtra(DECK_ID_INT, deck.id)
                                    putExtra(CARD_ID_INT, card.id)
                                    this@CardsActivity.startActivity(this)
                                }
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        for (item in arrayOf(card.front, card.back))
                            Text(text = item, modifier = Modifier.weight(1f))
                        Checkbox(checked = card.isActive, modifier = Modifier.size(56.dp, 24.dp),
                            onCheckedChange = { checked ->
                                val index = cards.indexOf(card)
                                db().card().getByID(card.id)!!.let {
                                    cards[index] = it.copy(isActive = checked)
                                    db().card().update(cards[index])
                                }
                            })
                    }
                }
            }
            OutlinedTextField(value = searchString,
                onValueChange = { searchString = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search")} )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TopBar() {
        var expanded by remember { mutableStateOf(false) }
        TopAppBar(
            title = { Text("Cards: ${deck.name}") },
            actions = {
                Box {
                    IconButton(onClick = { expanded = true }) { Icon(Icons.Default.MoreVert, contentDescription = "More actions") }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem({ Text("Import cards")}, onClick = {
                            expanded = false; importDeckLauncher.launch(arrayOf(EXPORT_MIME_TYPE))
                        })
                        DropdownMenuItem({ Text("Export cards")}, onClick = {
                            expanded = false; exportDeckLauncher.launch("${deck.name}.txt")
                        })
                    }
                }
            }
        )
    }
}