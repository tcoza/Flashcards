package com.flashcards

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flashcards.ui.theme.FlashcardsTheme

class MainActivity : ComponentActivity() {
    private lateinit var openFileLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var saveFileLauncher: ActivityResultLauncher<String>
    private var deck: Deck? = null      // Argument

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Deck.load(this)
        setContent { FlashcardsTheme { Content() } }

        openFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let { uri ->
            if (deck == null) return@registerForActivityResult
            contentResolver.openInputStream(uri)!!.use {
                this.showToast("Imported ${deck?.import(it)} cards")
                deck?.save(this@MainActivity);
            }
        }}
        saveFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument(Deck.MIME_TYPE)) {it?.let { uri ->
            if (deck == null) return@registerForActivityResult
            contentResolver.openOutputStream(uri)!!.use {
                deck?.export(it)
                this.showToast("Exported ${deck?.cards?.size} cards")
            }
        }}
    }

    @Composable
    @Preview(showBackground = true)
    fun Content() {
        val scrollState = rememberScrollState()
        Column(
            Modifier
                .fillMaxSize()
                .padding(8.dp)
                .verticalScroll(scrollState)
        ) {
            val selected = remember { mutableStateOf(-1) }
            for (index in 0 until Deck.list.size) {
                DeckRow(index, selected)
                Spacer(Modifier.height(8.dp))
            }
            Column(Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFBB86FC))
                .clickable {
                    InputBox("Enter deck name:") {
                        Deck.create(this@MainActivity, it ?: return@InputBox)
                    }
                }
                .padding(16.dp)
            ) {
                Text("+ New deck")
            }
        }
    }

    @Composable
    fun DeckRow(index: Int, selected: MutableState<Int>) {
        val deck = Deck.list[index]
        Column(Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF9B86FC))
            .clickable {
                Intent(this@MainActivity, CardActivity::class.java).apply {
                    putExtra(CardActivity.DECK_NAME_STR, deck.name)
                    this@MainActivity.startActivity(this)
                }
            }
            .padding(16.dp)
        ) {
            val isSelected = selected.value == index
            Row(Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${deck.name} (${deck.cards.size})")
                Spacer(modifier = Modifier.weight(1f))
                SmallButton(if (isSelected) "▲" else "▼") {
                    selected.value = if (isSelected) -1 else index
                }
            }
            if (isSelected) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SmallButton("+ Add Cards") {
                        Intent(this@MainActivity, EditCardActivity::class.java).apply {
                            putExtra(EditCardActivity.DECK_NAME_STR, deck.name)
                            this@MainActivity.startActivity(this)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    SmallButton("Import") {
                        this@MainActivity.deck = deck
                        openFileLauncher.launch(arrayOf(Deck.MIME_TYPE))
                    }
                    Spacer(Modifier.width(8.dp))
                    SmallButton("Export") {
                        this@MainActivity.deck = deck
                        saveFileLauncher.launch("${deck.name}.txt")
                    }
                    Spacer(Modifier.width(8.dp))
                    SmallButton("Delete", Color(0xFFBB1122)) {
                        AlertDialog.Builder(this@MainActivity).apply {
                            setMessage("Delete ${deck.name}?")
                            setPositiveButton("Yes") { _, _ ->
                                deck.delete(this@MainActivity)
                                selected.value = -1
                            }
                            setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                            create()
                            show()
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
}
