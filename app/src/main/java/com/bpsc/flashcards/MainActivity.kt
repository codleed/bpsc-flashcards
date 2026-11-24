package com.bpsc.flashcards

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.bpsc.flashcards.data.Bank
import com.bpsc.flashcards.data.ProgressStore
import com.bpsc.flashcards.ui.DecksScreen
import com.bpsc.flashcards.ui.StudyScreen
import com.bpsc.flashcards.ui.theme.BpscFlashTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bank = Bank(applicationContext)
        val store = ProgressStore(applicationContext)
        val animationsOff = Settings.Global.getFloat(
            contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f

        setContent {
            BpscFlashTheme {
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    AppRoot(bank = bank, store = store, reduceMotion = animationsOff)
                }
            }
        }
    }
}

@Composable
fun AppRoot(bank: Bank, store: ProgressStore, reduceMotion: Boolean) {
    val decks by bank.decks.collectAsState()
    val states by store.states.collectAsState()
    val cardsStatus by bank.status.collectAsState()
    val scope = rememberCoroutineScope()
    var deckId by remember { mutableStateOf<Int?>(null) }

    // Pull the newest cards whenever the app comes to the deck list.
    LaunchedEffect(deckId) {
        if (deckId == null) scope.launch { bank.refresh() }
    }

    val open = deckId
    if (open == null) {
        DecksScreen(
            decks = decks,
            states = states,
            cardsStatus = cardsStatus,
            onRefresh = { scope.launch { bank.refresh() } },
            onOpenDeck = { deckId = it },
        )
    } else {
        val deck = remember(open) { bank.deck(open) }
        BackHandler { deckId = null }
        StudyScreen(
            deck = deck,
            states = states,
            reduceMotion = reduceMotion,
            onMark = { id, status -> store.mark(id, status) },
            onResetDeck = { store.resetDeck(deck) },
            onExit = { deckId = null },
        )
    }
}