package com.bpsc.flashcards

import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.bpsc.flashcards.data.AppLang
import com.bpsc.flashcards.data.AppPrefs
import com.bpsc.flashcards.data.Bank
import com.bpsc.flashcards.data.ProgressStore
import com.bpsc.flashcards.data.ThemeMode
import com.bpsc.flashcards.ui.DecksScreen
import com.bpsc.flashcards.ui.LocalAppStrings
import com.bpsc.flashcards.ui.StudyScreen
import com.bpsc.flashcards.ui.stringsFor
import com.bpsc.flashcards.ui.theme.BpscFlashTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bank = Bank(applicationContext)
        val store = ProgressStore(applicationContext)
        val systemDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val prefs = AppPrefs(applicationContext, systemDark)
        val animationsOff = Settings.Global.getFloat(
            contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f

        setContent {
            val lang by prefs.lang.collectAsState()
            val theme by prefs.theme.collectAsState()
            CompositionLocalProvider(
                LocalAppStrings provides stringsFor(lang),
                LocalLayoutDirection provides if (lang == AppLang.UR) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                BpscFlashTheme(darkTheme = theme == ThemeMode.DARK) {
                    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                        AppRoot(
                            bank = bank,
                            store = store,
                            prefs = prefs,
                            lang = lang,
                            theme = theme,
                            reduceMotion = animationsOff,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppRoot(
    bank: Bank,
    store: ProgressStore,
    prefs: AppPrefs,
    lang: AppLang,
    theme: ThemeMode,
    reduceMotion: Boolean,
) {
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
            lang = lang,
            theme = theme,
            onToggleLang = { prefs.toggleLang() },
            onToggleTheme = { prefs.toggleTheme() },
            onRefresh = { scope.launch { bank.refresh() } },
            onOpenDeck = { deckId = it },
        )
    } else {
        val deck = remember(open) { bank.deck(open) }
        BackHandler { deckId = null }
        StudyScreen(
            deck = deck,
            states = states,
            lang = lang,
            reduceMotion = reduceMotion,
            onMark = { id, status -> store.mark(id, status) },
            onResetDeck = { store.resetDeck(deck) },
            onExit = { deckId = null },
        )
    }
}