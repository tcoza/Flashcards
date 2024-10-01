package com.flashcards

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.NumberPicker
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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flashcards.database.AppDatabase
import com.flashcards.database.Deck
import com.flashcards.ui.theme.FlashcardsTheme
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.NumberFormatException
import java.util.Locale

class DeckActivity : ComponentActivity() {
    private lateinit var exportDeckLauncher: ActivityResultLauncher<String>
    private lateinit var importDeckLauncher: ActivityResultLauncher<Array<String>>
    private var deck: Deck = Deck.dummy      // Argument to above

    val EXPORT_MIME_TYPE = "text/plain"

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deck = db().deck().getByID(intent.getIntExtra(DECK_ID_INT, -1))!!
        setContent { FlashcardsTheme { Scaffold(topBar = { TopBar() }, content = { Content(it) }) } }

        importDeckLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let { uri ->
            contentResolver.openInputStream(uri)!!.use {
                showToast("Imported ${deck.import(it)} cards")
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

    fun getLocales() = sequence {
        yield(Pair(Locale.ENGLISH, "English"))
        yield(Pair(Locale.FRENCH, "French"))
        yield(Pair(Locale.CHINESE, "Chinese"))
        yield(Pair(Locale.GERMAN, "German"))
        yield(Pair(Locale.ITALIAN, "Italian"))
        yield(Pair(Locale.JAPANESE, "Japanese"))
        yield(Pair(Locale.KOREAN, "Korean"))
    }.toList()

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    @Preview(showBackground = true)
    fun Content(paddingValues: PaddingValues = PaddingValues(0.dp)) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(8.dp)
        ) {
            var name by remember { mutableStateOf(deck.name) }
            var readFront = remember { mutableStateOf(deck.readFront) }
            var readBack = remember { mutableStateOf(deck.readBack) }
            var useHintAsPronunciation = remember { mutableStateOf(deck.useHintAsPronunciation) }
            var frontLocale = remember { mutableStateOf(deck.frontLocale) }
            var backLocale = remember { mutableStateOf(deck.backLocale) }
            var hintLocale = remember { mutableStateOf(deck.hintLocale) }
            var activateCardsPerDay by remember { mutableStateOf(deck.activateCardsPerDay) }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Deck name") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontSize = 32.sp)
            )
            @Composable
            fun LabeledSwitch(text: String, value: MutableState<Boolean>, lang: MutableState<String>) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("$text:", fontSize = 20.sp, modifier = Modifier.weight(1f))
                    Switch(checked = value.value, onCheckedChange = { value.value = it }, Modifier.weight(1f))
                    Spinner("Language",
                        getLocales().map { Pair(it.first.toString(), it.second) },
                        lang, Modifier.weight(2f))
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Text-to-speech", fontSize = 20.sp)
            Spacer(Modifier.height(8.dp))
            LabeledSwitch("Front", readFront, frontLocale)
            Spacer(Modifier.height(4.dp))
            LabeledSwitch("Back", readBack, backLocale)
            Spacer(Modifier.height(4.dp))
            LabeledSwitch("Hint*", useHintAsPronunciation, hintLocale)
            Spacer(Modifier.height(4.dp))
            Text("* Use hint as pronunciation for front")
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val fontSize = 20.sp
                Text("Auto-activate ", fontSize = fontSize)
                TextField(
                    value = activateCardsPerDay.toString(),
                    onValueChange = {
                        try { activateCardsPerDay = it.toInt() }
                        catch (_: NumberFormatException) {}
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.size(64.dp, 56.dp)
                )
                Text(" cards every day", fontSize = fontSize)
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
                Button(onClick = {
                    deck = db().deck().getByID(deck.id)!!
                    deck = deck.copy(
                        name = name,
                        activateCardsPerDay = activateCardsPerDay,
                        readFront = readFront.value,
                        readBack = readBack.value,
                        useHintAsPronunciation = useHintAsPronunciation.value,
                        frontLocale = frontLocale.value,
                        backLocale = backLocale.value,
                        hintLocale = hintLocale.value
                    )
                    db().deck().update(deck)
                    finish()
                }) { Text("Save") }
            }
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Spinner(
        label: String,
        options: List<Pair<String, String>>,
        selected: MutableState<String>,
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
}