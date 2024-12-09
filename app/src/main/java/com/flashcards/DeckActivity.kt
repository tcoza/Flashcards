package com.flashcards

import android.app.AlertDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flashcards.database.Deck
import com.flashcards.ui.theme.FlashcardsTheme
import java.lang.NumberFormatException
import java.util.Locale

class DeckActivity : ComponentActivity() {
    private var deck: Deck = Deck.dummy      // Argument to above

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deck = db().deck().getByID(intent.getIntExtra(DECK_ID_INT, -1))!!
        setContent { FlashcardsTheme { Scaffold(topBar = { TopBar() }, content = { Content(it) }) } }
    }

    fun getLocales() = sequence {
        yield(Pair(Locale.ENGLISH, "English"))
        yield(Pair(Locale.FRENCH, "French"))
        yield(Pair(Locale.CHINESE, "Chinese"))
        yield(Pair(Locale.GERMAN, "German"))
        yield(Pair(Locale.ITALIAN, "Italian"))
        yield(Pair(Locale.JAPANESE, "Japanese"))
        yield(Pair(Locale.KOREAN, "Korean"))
    }.toList()

    val fontSize = 20.sp

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    @Preview(showBackground = true)
    fun Content(paddingValues: PaddingValues = PaddingValues(0.dp)) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            var name by remember { mutableStateOf(deck.name) }
            val readFront = remember { mutableStateOf(deck.readFront) }
            val readBack = remember { mutableStateOf(deck.readBack) }
            val readHint = remember { mutableStateOf(deck.readHint) }
            val frontLocale = remember { mutableStateOf(deck.frontLocale) }
            val backLocale = remember { mutableStateOf(deck.backLocale) }
            val hintLocale = remember { mutableStateOf(deck.hintLocale) }
            var activateCardsPerDayStr by remember { mutableStateOf(deck.activateCardsPerDay.toString()) }
            var targetTimeStr by remember { mutableStateOf((deck.targetTime / 1000f).toString()) }
            var showHint by remember { mutableStateOf(deck.showHint) }
            var showBack by remember { mutableStateOf(deck.showBack) }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Deck name") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontSize = 24.sp)
            )
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Target time per card: ", fontSize = fontSize)
                Spacer(Modifier.weight(1f))
                TextField(
                    value = targetTimeStr,
                    onValueChange = { targetTimeStr = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.size(96.dp, 56.dp)
                )
                Text(" s", fontSize = fontSize)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Auto-activate ", fontSize = fontSize)
                TextField(
                    value = activateCardsPerDayStr,
                    onValueChange = { activateCardsPerDayStr = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.size(64.dp, 56.dp)
                )
                Text(" cards/day", fontSize = fontSize)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Enable hint on flashes: ", fontSize = fontSize)
                Spacer(Modifier.weight(1f))
                Switch(checked = showHint, onCheckedChange = { showHint = it })
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Enable back-first flashes: ", fontSize = fontSize)
                Spacer(Modifier.weight(1f))
                Switch(checked = showBack, onCheckedChange = { showBack = it })
            }
            Spacer(Modifier.height(24.dp))


            GroupBox("Text-to-speech", Modifier.fillMaxWidth()) {
                LabeledSwitch("Front", readFront, frontLocale)
                Spacer(Modifier.height(4.dp))
                LabeledSwitch("Hint", readHint, hintLocale)
                Spacer(Modifier.height(4.dp))
                LabeledSwitch("Back", readBack, backLocale)
            }

            Spacer(Modifier.height(24.dp))

            Row(Modifier.fillMaxWidth()) {
                Button(colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    onClick = {
                        AlertDialog.Builder(this@DeckActivity).apply {
                            setMessage("Delete ${deck.name}?")
                            setPositiveButton("Yes") { _, _ ->
                                db().deck().delete(deck)
                                finish()
                            }
                            setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                            create()
                            show()
                        }
                    }
                ) { Text("Delete") }
                Spacer(Modifier.weight(1f))
                Button(
                    enabled =
                        targetTimeStr.toFloatOrNull() != null &&
                        activateCardsPerDayStr.toIntOrNull() != null,
                    onClick = {
                    deck = db().deck().getByID(deck.id)!!
                    deck = deck.copy(
                        name = name,
                        targetTime = (targetTimeStr.toFloat() * 1000).toLong(),
                        activateCardsPerDay = activateCardsPerDayStr.toInt(),
                        readFront = readFront.value,
                        readBack = readBack.value,
                        readHint = readHint.value,
                        frontLocale = frontLocale.value,
                        backLocale = backLocale.value,
                        hintLocale = hintLocale.value,
                        showBack = showBack,
                        showHint = showHint
                    )
                    db().deck().update(deck)
                    finish()
                }) { Text("Save") }
            }
        }
    }

    @Composable
    fun LabeledSwitch(text: String, value: MutableState<Boolean>, lang: MutableState<String>) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("$text:", fontSize = fontSize, modifier = Modifier.weight(1f))
            Switch(checked = value.value, onCheckedChange = { value.value = it }, Modifier.weight(1f))
            Spinner("Language",
                getLocales().map { Pair(it.first.toString(), it.second) },
                lang, Modifier.weight(2f))
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TopBar() {
        var expanded by remember { mutableStateOf(false) }
        TopAppBar(
            title = { Text("Deck options") },
            actions = {
                Box {
                    IconButton(onClick = { expanded = true }) { Icon(Icons.Default.MoreVert, contentDescription = "More actions") }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                    }
                }
            }
        )
    }

    @Composable
    fun GroupBox(
        title: String,
        modifier: Modifier = Modifier,
        content: @Composable ColumnScope.() -> Unit
    ) {
        Column(
            modifier = modifier
                .border(1.dp, Color.Gray, shape = RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}