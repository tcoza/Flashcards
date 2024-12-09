package com.flashcards

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flashcards.database.AppDatabase
import com.flashcards.database.Deck
import com.flashcards.database.Flash
import com.flashcards.database.getStatsString
import com.flashcards.ui.theme.FlashcardsTheme
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalUnit
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    private lateinit var cardActivityLauncher: ActivityResultLauncher<Intent>
    private lateinit var backupDatabaseLauncher: ActivityResultLauncher<String>
    private lateinit var restoreDatabaseLauncher: ActivityResultLauncher<Array<String>>

    val DATABASE_MIME_TYPE = "application/x-sqlite3"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FlashcardsTheme { Scaffold() } }

        backupDatabaseLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument(DATABASE_MIME_TYPE)) {it?.let { uri ->
            contentResolver.openOutputStream(uri)!!.use { output ->
                FileInputStream(getDatabasePath(AppDatabase.DB_NAME)).use { input ->
                    input.copyTo(output) } }
            showToast("Success!")
        }}
        restoreDatabaseLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let { uri ->
            try {
                db().close()
                contentResolver.openInputStream(uri)!!.use { input ->
                    FileOutputStream(getDatabasePath(AppDatabase.DB_NAME)).use { output ->
                        input.copyTo(output) } }
                showToast("Success!")
            }
            finally {
                Application.instance.openDatabase()
                refreshDecks()
            }
        }}
        cardActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            if (it.data == null) return@registerForActivityResult
            showToast(it.data?.getStringExtra(FlashActivity.STATS_STR) ?: return@registerForActivityResult, Toast.LENGTH_LONG)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshDecks()
    }

    fun refreshDecks() {
        decks.clear()
        decks.addAll(db().deck().getAll())
        recountDue = !recountDue
    }

    private var decks = mutableStateListOf<Deck>()
    private var recountDue by mutableStateOf(false)

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Scaffold() {
        Scaffold(
            topBar = { TopBar() },
            content = { Content(it) },
            floatingActionButton = {
                Button(onClick = {
                    InputBox("Enter deck name:") {
                        var deck = Deck(0, it ?: return@InputBox)
                        deck = deck.copy(id = db().deck().insert(deck).toInt())
                        decks.add(deck)
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC))
                ) { Text("+ New deck") }})
    }

    @Composable
    @Preview(showBackground = true)
    fun Content(paddingValues: PaddingValues = PaddingValues(0.dp)) {
        if (isPreview()) decks.add(Deck(0, "Expressions & Interjections & Other nonsense"))
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val selected = remember { mutableStateOf(-1) }
            for (index in decks.indices) {
                DeckRow(index, selected)
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(64.dp))
        }
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TopBar() {
        var expanded by remember { mutableStateOf(false) }
        TopAppBar(
            title = { Text("Flashcards") },
            actions = {
                Box {
                    IconButton(onClick = { expanded = true }) { Icon(Icons.Default.MoreVert, contentDescription = "More actions") }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem({ Text("Backup database")}, onClick = {
                            expanded = false; backupDatabaseLauncher.launch("${AppDatabase.DB_NAME}.db") })
                        DropdownMenuItem({ Text("Restore database")}, onClick = {
                            expanded = false; restoreDatabaseLauncher.launch(arrayOf("*/*")) })
                    }
                }
            }
        )
    }

    @Composable
    fun DeckRow(index: Int, selected: MutableState<Int>) {
        val deck = decks[index]
        Column(Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF9B86FC))
            .clickable {
                cardActivityLauncher.launch(
                    Intent(this@MainActivity, FlashActivity::class.java).apply {
                        putExtra(DECK_ID_INT, deck.id)
                    })
            }
            .padding(16.dp)
        ) {
            val isSelected = selected.value == index
            Row(Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(deck.name, modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp))
                val count = db().card().count(deck.id)
                val countActive = db().card().countActive(deck.id)
                var countDue by remember { mutableStateOf<Int?>(null) }
                LaunchedEffect(deck, recountDue) {
                    while (true) {
                        countDue = deck.countDue()
                        delay(5_000)   // 5 s
                    }
                }
                Text("${countDue ?: "?"}/$countActive/$count", softWrap = false)
                Spacer(Modifier.width(8.dp))
                SmallButton(if (isSelected) "▲" else "▼") {
                    selected.value = if (isSelected) -1 else index
                }
            }
            if (isSelected) {
                val todayStart = LocalDate.now().toEpochMilli()
                val flashes = db().flash().getAllFromDeck(deck.id, todayStart)
                if (flashes.any()) {
                    Spacer(Modifier.height(12.dp))
                    val showDialog = remember { mutableStateOf(false) }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Today:")
                        Spacer(Modifier.weight(1f))
                        Text(flashes.getStatsString())
                        Spacer(Modifier.width(8.dp))
                        SmallButton("...") { showDialog.value = true }
                        StatsDialog(deck, showDialog)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SmallButton("View Cards") {
                        Intent(this@MainActivity, CardsActivity::class.java).apply {
                            putExtra(DECK_ID_INT, deck.id)
                            this@MainActivity.startActivity(this)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    SmallButton("Options") {
                        Intent(this@MainActivity, DeckActivity::class.java).apply {
                            putExtra(DECK_ID_INT, deck.id)
                            this@MainActivity.startActivity(this)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    SmallButton("+ Add cards", Color(0xFF11BB22)) {
                        Intent(this@MainActivity, EditCardActivity::class.java).apply {
                            putExtra(DECK_ID_INT, deck.id)
                            this@MainActivity.startActivity(this)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun SmallButton(text: String, color: Color = Color(0xFF8B76EC), onClick: () -> Unit) {
        Box(
            modifier = Modifier
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color)
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp)
        ) {
            Text(text, Modifier.align(Alignment.Center))
        }
    }

    fun InputBox(message: String, callback: (String?) -> Unit) {
        val editText = EditText(this)
        AlertDialog.Builder(this)
            .setMessage(message)
            .setView(editText)
            .setPositiveButton("OK") { _, _ -> callback(editText.text.toString()) }
            .setNegativeButton("Cancel") { _, _ -> callback(null) }
            .create()
            .show()
    }

    @Composable
    fun StatsDialog(deck: Deck, show: MutableState<Boolean>) {
        if (show.value) AlertDialog(
            modifier = Modifier.heightIn(max = 512.dp),
            title = { Text("Stats: ${deck.name}") },
            onDismissRequest = { show.value = false },
            confirmButton = {},
            text = {
                val firstTime = db().flash().getFirstOfDeck(deck.id)!!.createdAt
                val minDate = Instant.ofEpochMilli(firstTime).atZone(ZoneId.systemDefault()).toLocalDate()
                var date = LocalDate.now()
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    do {
                        val since = date.toEpochMilli()
                        val until = date.plusDays(1).toEpochMilli()
                        val flashes = db().flash().getAllFromDeck(deck.id, since, until)
                        if (!flashes.isEmpty())
                            Row(Modifier.fillMaxWidth()) {
                                Text("${date.dayOfMonth}/${date.monthValue}")
                                Spacer(Modifier.weight(1f))
                                Text(flashes.getStatsString())
                            }
                        date = date.minusDays(1)
                    } while (!(date).isBefore(minDate))
                }
            })
    }
}
