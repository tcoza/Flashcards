package com.flashcards

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import com.flashcards.database.Card
import com.flashcards.database.Deck
import com.flashcards.database.Flash
import com.flashcards.database.getStatsString
import com.flashcards.ui.theme.FlashcardsTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.Closeable
import java.util.Locale
import kotlin.math.min

class FlashActivity : ComponentActivity() {
    companion object {
        const val STATS_STR = "STATS"
        const val POST_DONE_TIME = 1000L  // ms
    }

    var deck: Deck = Deck.dummy
    var onlyDue: Boolean = false

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deck = db().deck().getByID(intent.extras?.getInt(DECK_ID_INT)!!)!!
        if (deck.readFront) frontTTS = MyTTS(this, deck.frontLocale)
        if (deck.readBack) backTTS = MyTTS(this, deck.backLocale)
        if (deck.readHint || deck.hintLocale.isNotEmpty()) hintTTS = MyTTS(this, deck.hintLocale)
        onlyDue = true //deck.getNextFlash(true) != null
        setContent { FlashcardsTheme { Scaffold(content = { Content(it) }) } }     // Scaffold for dark theme
    }

    override fun onPause() {
        super.onPause()
        resumeStopwatch = stopwatch.isRunning
        stopwatch.pause()
    }

    override fun onResume() {
        super.onResume()
        if (resumeStopwatch) stopwatch.start()
        refreshCard()
    }

    val stopwatch = Stopwatch()
    var resumeStopwatch = false

    var flash by mutableStateOf<Flash?>(null)
    val flashes = mutableListOf<Flash>()
    var card by mutableStateOf(Card.dummy)
    private fun refreshCard() { card = flash?.let { db().card().getByID(it.cardID) } ?: Card.dummy }
    val showHint = mutableStateOf(true)
    val showFront = mutableStateOf(true)
    val showBack = mutableStateOf(true)
    fun getCardDone() = showFront.value && showBack.value

    @Preview
    @Composable
    fun Content(paddingValues: PaddingValues = PaddingValues()) {
        val buttonFontSize = 24.sp

        fun nextCard() {
            deck.getNextFlash(onlyDue).let {
                if (it == null) {
                    finish()
                    return@nextCard
                }
                flash = it
                refreshCard()
            }
            showFront.value = !flash!!.isBack
            showHint.value = !deck.showHint || card.hint == null
            showBack.value = flash!!.isBack
            stopwatch.restart()
            if (flash!!.isBack) speakBack() else speakFront()
        }
        if (!isPreview()) {
            if (db().card().countActive(deck.id) == 0) { finish(); return }
            if (card === Card.dummy || db().card().getByID(card.id) == null) nextCard()
        }
        LaunchedEffect(showFront.value) { if (showFront.value) speakFront() }
        LaunchedEffect(showBack.value) { if (showBack.value) speakBack() }
        LaunchedEffect(showHint.value) { if (showHint.value) speakHint() }

        val cardDone = getCardDone()
        val postDoneStopwatch = remember { Stopwatch() }
        var showResultButtons by remember { mutableStateOf(false) }
        if (cardDone && stopwatch.isRunning) {
            stopwatch.pause()
            postDoneStopwatch.restart()
            showResultButtons = false
        }

        var progress by remember { mutableStateOf(0f) }
        LinearProgressIndicator(progress = progress, Modifier.fillMaxWidth())
        LaunchedEffect(Unit) {
            while (true) {
                showResultButtons = postDoneStopwatch.getElapsedTimeMillis() >= POST_DONE_TIME
                progress = stopwatch.getElapsedTimeMillis() / deck.targetTime.toFloat()
                progress = min(progress, 1f)
                delay(20)
            }
        }

        val tapDetector: suspend PointerInputScope.() -> Unit = {
            if (!cardDone) detectTapGestures(onTap = {
                showFront.value = true
                showBack.value = showBack.value || showHint.value
                showHint.value = true
            })
            else detectTapGestures(onDoubleTap = {
                Intent(this@FlashActivity, EditCardActivity::class.java).apply {
                    putExtra(DECK_ID_INT, deck.id)
                    putExtra(CARD_ID_INT, card.id)
                    this@FlashActivity.startActivity(this)
                }
            })
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .pointerInput(cardDone, tapDetector),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            @Composable
            fun ButtonOrText(text: String, buttonText: String, state: MutableState<Boolean>) {
                Box(Modifier.fillMaxWidth()) {
                    Text(text,
                        // fontSize directly will do wierd things with multiline
                        style = TextStyle(fontSize = deck.fontSize.sp),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .hideIf(!state.value)
                            .pointerInput(cardDone, tapDetector),
                        )
                    Button(modifier = Modifier
                        .align(Alignment.Center)
                        .hideIf(state.value),
                        onClick = { state.value = true }) {
                        Text(buttonText, fontSize = buttonFontSize)
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            ButtonOrText(card.front, "Show front", showFront)
            Spacer(Modifier.weight(1f))
            if (deck.showHint && card.hint != null) {
                ButtonOrText(card.hint!!, "Show hint", showHint)
                Spacer(Modifier.weight(1f))
            }
            ButtonOrText(card.back, "Show back", showBack)
            Spacer(Modifier.weight(1f))

            if (cardDone && showResultButtons) {
                Row {
                    @Composable
                    fun ResultButton(value: Boolean) {
                        val color = if (value) Color.Green else Color.Red
                        val text = if (value) "✔" else "✘"
                        Button({
                            if (flashes.lastOrNull() !== flash)
                                flash!!.copy(
                                    timeElapsed = stopwatch.getElapsedTimeMillis(),
                                    isCorrect = value
                                ).apply {
                                    this.copy(id = db().flash().insert(this).toInt()).apply {
                                        Log.d("flash", this.toString())
                                        flashes.add(this)
                                    }
                                }
                            else
                                flash!!.copy(isCorrect = value).apply {
                                    db().flash().update(this)
                                    Log.d("flash", this.toString())
                                    deck.resetCache()
                                }
                            nextCard()
                        },
                            colors = ButtonDefaults.buttonColors(containerColor = color),
                            modifier = Modifier.size(96.dp)
                        ) {
                            Text(text, fontSize = 48.sp)
                        }
                    }
                    Spacer(Modifier.weight(2f))
                    ResultButton(value = false)
                    Spacer(Modifier.weight(1f))
                    ResultButton(value = true)
                    Spacer(Modifier.weight(2f))
                }
            }
            else if (!cardDone && ((showFront.value && deck.readFront) || (showBack.value && deck.readBack))) {
                Button({
                    if (showFront.value) speakFront()
                    if (showBack.value) speakBack()
                }, modifier = Modifier.size(96.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.speaker),
                        contentDescription = "Speaker",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            else Spacer(Modifier.size(96.dp))   // To avoid change of layout within a flash

//            Text(when {
//                    !showHint.value -> "Tap to show hint"
//                    !showFront.value -> "Tap to show front"
//                    !showBack.value -> "Tap to show back"
//                    else -> "Swipe for next"
//                },
//                fontSize = buttonFontSize,
//                modifier = Modifier.hideIf(cardDone()))
        }
    }

    @Deprecated("Deprecated in Java")
//    override fun onBackPressed() {
//        if (getCardDone() || flashes.isEmpty()) {
//            super.onBackPressed()
//        }
//        else {
//            flash = flashes.last()
//            showFront.value = true
//            showBack.value = true
//            refreshCard()
//        }
//    }

    override fun finish() {
        setResult(Activity.RESULT_OK, Intent().apply {
            if (flashes.any()) putExtra(STATS_STR, flashes.getStatsString())
        })
        super.finish()
    }

    private var frontTTS: MyTTS? = null
    private var hintTTS: MyTTS? = null
    private var backTTS: MyTTS? = null

    private fun speakBack() { backTTS?.speak(card.back) }
    private fun speakHint() { if (deck.readHint && card.hint != null) hintTTS?.speak(card.hint!!) }
    private fun speakFront() {
        if (!deck.readFront) return
        if (card.useHintAsPronunciation && card.hint != null) hintTTS?.speak(card.hint!!)
        else frontTTS?.speak(card.front)
    }

    override fun onDestroy() {
        frontTTS?.close()
        backTTS?.close()
        hintTTS?.close()
        super.onDestroy()
    }
}

class MyTTS(context: Context, locale: String) : TextToSpeech.OnInitListener, Closeable {
    private val tts: TextToSpeech
    private val locale: Locale
    private var ttsInitd: Boolean = false

    init {
        tts = TextToSpeech(context, this, "com.google.android.tts")
        this.locale =
            Locale.getAvailableLocales().firstOrNull { it.toString() == locale }
                ?: throw Exception("Invalid locale: '$locale'")
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.e("TTS", "Initialization failed")
            return
        }

        val result = tts.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("TTS", "Language not supported")
            return
        }

        ttsInitd = true
    }

    public fun speak(text: String) {
        CoroutineScope(Dispatchers.Main).launch {
            var i = 0
            while (i++ < 10 && !ttsInitd) delay(100)
            if (!ttsInitd) { Log.e("TTS", "TTS not initialized"); return@launch }
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    override fun close() {
        tts.stop()
        tts.shutdown()
    }
}