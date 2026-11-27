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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.bpsc.flashcards.data.AppLang
import com.bpsc.flashcards.data.AppPrefs
import com.bpsc.flashcards.data.AppUpdater
import com.bpsc.flashcards.data.Bank
import com.bpsc.flashcards.data.ProgressStore
import com.bpsc.flashcards.data.ThemeMode
import com.bpsc.flashcards.data.UpdateStatus
import com.bpsc.flashcards.ui.DecksScreen
import com.bpsc.flashcards.ui.StudyScreen
import com.bpsc.flashcards.ui.theme.BpscFlashTheme
import com.bpsc.flashcards.ui.theme.DarkPaper
import com.bpsc.flashcards.ui.theme.LightPaper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bank = Bank(applicationContext)
        val store = ProgressStore(applicationContext)
        val updater = AppUpdater(applicationContext)
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
            // App chrome stays English in every language; `lang` only flips card text.
            BpscFlashTheme(darkTheme = theme == ThemeMode.DARK) {
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    AppRoot(
                        bank = bank,
                        store = store,
                        prefs = prefs,
                        updater = updater,
                        lang = lang,
                        theme = theme,
                        reduceMotion = animationsOff,
                    )
                }
            }
        }
    }
}

data class StudyTarget(val deckId: Int, val cardId: String? = null)

@Composable
fun AppRoot(
    bank: Bank,
    store: ProgressStore,
    prefs: AppPrefs,
    updater: AppUpdater,
    lang: AppLang,
    theme: ThemeMode,
    reduceMotion: Boolean,
) {
    val decks by bank.decks.collectAsState()
    val states by store.states.collectAsState()
    val cardsStatus by bank.status.collectAsState()
    val updateStatus by updater.status.collectAsState()
    val updateRelease by updater.release.collectAsState()
    val scope = rememberCoroutineScope()
    var target by remember { mutableStateOf<StudyTarget?>(null) }
    var updateDismissed by remember { mutableStateOf(false) }

    // The window is edge-to-edge on Android 15 (targetSdk 35), so the system bar
    // icons need to flip with in-app light/dark — the activity theme stays light.
    val view = LocalView.current
    LaunchedEffect(theme) {
        val window = (view.context as? android.app.Activity)?.window ?: return@LaunchedEffect
        WindowCompat.getInsetsController(window, view).apply {
            val light = theme != ThemeMode.DARK
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = light
        }
        // targetSdk 35 draws edge-to-edge; match the 3-button nav strip to the
        // home paper instead of leaving the light theme's grey strip under a dark app.
        window.navigationBarColor = (if (theme == ThemeMode.DARK) DarkPaper.paper else LightPaper.paper).toArgb()
        window.isNavigationBarContrastEnforced = false
    }

    // Pull the newest cards and check for a newer app version whenever we land on
    // the deck list. Both are network calls that never block the UI.
    LaunchedEffect(target) {
        if (target == null) {
            scope.launch { bank.refresh() }
            updater.check()
        }
    }

    val open = target
    if (open == null) {
        DecksScreen(
            decks = decks,
            states = states,
            cardsStatus = cardsStatus,
            lang = lang,
            theme = theme,
            updateRelease = updateRelease,
            updateStatus = updateStatus,
            showUpdate = !updateDismissed && updateRelease != null,
            onToggleLang = { prefs.toggleLang() },
            onToggleTheme = { prefs.toggleTheme() },
            onRefresh = { scope.launch { bank.refresh() } },
            onOpenDeck = { deckId -> target = StudyTarget(deckId) },
            onOpenCard = { deckId, cardId -> target = StudyTarget(deckId, cardId) },
            onDownloadUpdate = { updateRelease?.let { scope.launch { updater.download(it) } } },
            onInstallUpdate = {
                (updateStatus as? UpdateStatus.Downloaded)?.let { updater.install(it.file) }
            },
            onDismissUpdate = { updateDismissed = true },
        )
    } else {
        val deck = remember(open.deckId) { bank.deck(open.deckId) }
        BackHandler { target = null }
        StudyScreen(
            deck = deck,
            states = states,
            lang = lang,
            reduceMotion = reduceMotion,
            onMark = { id, status -> store.mark(id, status) },
            onResetDeck = { store.resetDeck(deck) },
            onExit = { target = null },
            startCardId = open.cardId,
        )
    }
}