package com.bpsc.flashcards.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bpsc.flashcards.R
import com.bpsc.flashcards.data.AppLang
import com.bpsc.flashcards.data.CardsStatus
import com.bpsc.flashcards.data.CardStatus
import com.bpsc.flashcards.data.Deck
import com.bpsc.flashcards.data.MIXED_DECK_ID
import com.bpsc.flashcards.data.MOST_REPEATED_DECK_ID
import com.bpsc.flashcards.data.ReleaseInfo
import com.bpsc.flashcards.data.ThemeMode
import com.bpsc.flashcards.data.UpdateStatus
import com.bpsc.flashcards.ui.theme.Instrument
import com.bpsc.flashcards.ui.theme.LocalPaperColors
import com.bpsc.flashcards.ui.theme.PaperColors
import com.bpsc.flashcards.ui.theme.PaperType
import com.bpsc.flashcards.ui.theme.NooriNastaliq
import com.bpsc.flashcards.ui.theme.rememberPaperColors
import java.text.NumberFormat
import java.util.Locale

/**
 * Home — the paper artboard from Home-html, built as Compose.
 *
 * Faithful to that mockup's values: 24dp gutters, 28dp green hero, 24dp sand
 * cards, Fraunces display type, marigold actions, and the 232x64 ink nav pill
 * floating over the content. Anything below the artboard's fold (the rest of the
 * 29 decks, search results, settings) continues the same material rather than
 * inventing a second one.
 */

private enum class HomeSection { DECKS, SEARCH }

private fun count(n: Int): String = NumberFormat.getIntegerInstance(Locale.US).format(n)

/** "IT Teacher Interview" -> "IT Teacher\nInterview", matching the artboard's two lines. */
private fun headline(title: String): String {
    val words = title.split(" ").filter { it.isNotBlank() }
    return if (words.size >= 3) {
        words.take(2).joinToString(" ") + "\n" + words.drop(2).joinToString(" ")
    } else {
        title
    }
}

private fun Deck.known(states: Map<String, CardStatus>): Int =
    cards.count { states[it.id] == CardStatus.KNOWN }

private fun Deck.again(states: Map<String, CardStatus>): Int =
    cards.count { states[it.id] == CardStatus.AGAIN }

@Composable
fun DecksScreen(
    decks: List<Deck>,
    states: Map<String, CardStatus>,
    cardsStatus: CardsStatus,
    lang: AppLang,
    theme: ThemeMode,
    onToggleLang: () -> Unit,
    onToggleTheme: () -> Unit,
    onRefresh: () -> Unit,
    onOpenDeck: (Int) -> Unit,
    onOpenCard: (deckId: Int, cardId: String) -> Unit,
    updateRelease: ReleaseInfo? = null,
    updateStatus: UpdateStatus = UpdateStatus.Idle,
    showUpdate: Boolean = false,
    onDownloadUpdate: () -> Unit = {},
    onInstallUpdate: () -> Unit = {},
    onDismissUpdate: () -> Unit = {},
) {
    val strings = LocalAppStrings.current
    val colors = rememberPaperColors(dark = theme == ThemeMode.DARK)
    var section by remember { mutableStateOf(HomeSection.DECKS) }
    var query by remember { mutableStateOf("") }
    var settingsOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = section == HomeSection.SEARCH) {
        section = HomeSection.DECKS
        query = ""
    }

    CompositionLocalProvider(LocalPaperColors provides colors) {
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.paper),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .widthIn(max = 460.dp)
                    .fillMaxHeight(),
            ) {
                if (section == HomeSection.SEARCH) {
                    SearchBody(
                        decks = decks,
                        states = states,
                        lang = lang,
                        query = query,
                        onQueryChange = { query = it },
                        onOpenCard = onOpenCard,
                    )
                } else {
                    HomeBody(
                        decks = decks,
                        states = states,
                        cardsStatus = cardsStatus,
                        lang = lang,
                        strings = strings,
                        showUpdate = showUpdate,
                        updateRelease = updateRelease,
                        updateStatus = updateStatus,
                        onDownloadUpdate = onDownloadUpdate,
                        onInstallUpdate = onInstallUpdate,
                        onDismissUpdate = onDismissUpdate,
                        onRefresh = onRefresh,
                        onOpenDeck = onOpenDeck,
                        onOpenCard = onOpenCard,
                    )
                }

                HomeNav(
                    section = section,
                    colors = colors,
                    onDecks = {
                        section = HomeSection.DECKS
                        query = ""
                    },
                    onSearch = { section = HomeSection.SEARCH },
                    onSettings = { settingsOpen = true },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )

                if (settingsOpen) {
                    SettingsSheet(
                        lang = lang,
                        theme = theme,
                        colors = colors,
                        onToggleLang = onToggleLang,
                        onToggleTheme = onToggleTheme,
                        onDismiss = { settingsOpen = false },
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- home

@Composable
private fun HomeBody(
    decks: List<Deck>,
    states: Map<String, CardStatus>,
    cardsStatus: CardsStatus,
    lang: AppLang,
    strings: AppStrings,
    showUpdate: Boolean,
    updateRelease: ReleaseInfo?,
    updateStatus: UpdateStatus,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onDismissUpdate: () -> Unit,
    onRefresh: () -> Unit,
    onOpenDeck: (Int) -> Unit,
    onOpenCard: (deckId: Int, cardId: String) -> Unit,
) {
    val colors = LocalPaperColors.current

    val mixed = decks.firstOrNull { it.id == MIXED_DECK_ID }
    val repeated = decks.firstOrNull { it.id == MOST_REPEATED_DECK_ID }
    // The artboard's two-up cards are literally the bank's first two decks.
    val featuredA = decks.firstOrNull { it.id == 1 }
    val featuredB = decks.firstOrNull { it.id == 2 }
    val rest = decks.filter {
        it.id != MIXED_DECK_ID &&
            it.id != MOST_REPEATED_DECK_ID &&
            it.id != featuredA?.id &&
            it.id != featuredB?.id
    }

    val total = mixed?.cards?.size ?: decks.filter { it.id != MIXED_DECK_ID }.sumOf { it.cards.size }
    val mastered = mixed?.known(states) ?: 0
    val again = mixed?.again(states) ?: 0
    val firstAgain = mixed?.cards?.firstOrNull { states[it.id] == CardStatus.AGAIN }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 124.dp),
    ) {
        item {
            Text("Revision · BPSC interview", style = PaperType.eyebrow, color = colors.muted)
            Spacer(Modifier.height(8.dp))
            Text(headline(strings.appTitle), style = PaperType.title, color = colors.ink)
        }

        if (showUpdate && updateRelease != null) {
            item {
                Spacer(Modifier.height(18.dp))
                UpdateBanner(
                    release = updateRelease,
                    status = updateStatus,
                    onDownload = onDownloadUpdate,
                    onInstall = onInstallUpdate,
                    onDismiss = onDismissUpdate,
                )
            }
        }

        item {
            Spacer(Modifier.height(20.dp))
            MixedHeroCard(
                mixed = mixed,
                mastered = mastered,
                again = again,
                total = total,
                lang = lang,
                onContinue = { onOpenDeck(MIXED_DECK_ID) },
                onReview = {
                    val card = firstAgain
                    if (card != null) onOpenCard(MIXED_DECK_ID, card.id) else onOpenDeck(MIXED_DECK_ID)
                },
            )
        }

        item {
            Spacer(Modifier.height(22.dp))
            Text("Decks", style = PaperType.eyebrow, color = colors.muted)
        }

        if (repeated != null) {
            item {
                Spacer(Modifier.height(12.dp))
                FeaturedDeck(
                    deck = repeated,
                    known = repeated.known(states),
                    again = repeated.again(states),
                    lang = lang,
                    onOpen = { onOpenDeck(repeated.id) },
                )
            }
        }

        if (featuredA != null || featuredB != null) {
            item {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    featuredA?.let { deck ->
                        DeckTile(
                            deck = deck,
                            known = deck.known(states),
                            lang = lang,
                            yellow = true,
                            modifier = Modifier.weight(1f),
                            onOpen = { onOpenDeck(deck.id) },
                        )
                    }
                    featuredB?.let { deck ->
                        DeckTile(
                            deck = deck,
                            known = deck.known(states),
                            lang = lang,
                            yellow = false,
                            modifier = Modifier.weight(1f),
                            onOpen = { onOpenDeck(deck.id) },
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(22.dp))
            Text("More decks", style = PaperType.eyebrow, color = colors.muted)
        }

        items(rest, key = { it.id }) { deck ->
            Spacer(Modifier.height(10.dp))
            DeckRow(
                deck = deck,
                known = deck.known(states),
                lang = lang,
                onOpen = { onOpenDeck(deck.id) },
            )
        }

        item {
            Spacer(Modifier.height(20.dp))
            SyncLine(status = cardsStatus, total = total, onRetry = onRefresh)
            Spacer(Modifier.height(12.dp))
            Text(strings.footerNote, style = PaperType.small, color = colors.muted)
        }
    }
}

@Composable
private fun MixedHeroCard(
    mixed: Deck?,
    mastered: Int,
    again: Int,
    total: Int,
    lang: AppLang,
    onContinue: () -> Unit,
    onReview: () -> Unit,
) {
    val colors = LocalPaperColors.current
    val title = mixed?.title(lang) ?: "Mixed Practice"

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(colors.green)
            .padding(22.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                title,
                style = PaperType.heroTitle,
                color = colors.cream,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).alignByBaseline(),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "All ${count(total)} cards, shuffled",
                style = PaperType.small,
                color = colors.onGreenMuted,
                maxLines = 1,
                modifier = Modifier.alignByBaseline(),
            )
        }

        Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.Bottom) {
            Text(count(mastered), style = PaperType.heroNumber, color = colors.cream)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.padding(bottom = 4.dp)) {
                Text("of ${count(total)}", style = PaperType.small, color = colors.onGreenMuted)
                Text("mastered", style = PaperType.small, color = colors.onGreenMuted)
            }
        }

        Spacer(Modifier.height(16.dp))
        PaperBar(
            progress = if (total > 0) mastered.toFloat() / total else 0f,
            track = colors.onGreenLine,
            fill = colors.yellow,
            height = 4.dp,
        )

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(colors.yellow)
                    .clickable(onClick = onContinue)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Continue", style = PaperType.button, color = colors.onYellow)
                Spacer(Modifier.width(8.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = colors.onYellow,
                    modifier = Modifier.size(18.dp),
                )
            }
            Box(
                Modifier
                    .width(118.dp)
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .border(1.5.dp, colors.onGreenBorder, RoundedCornerShape(25.dp))
                    .clickable(onClick = onReview),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (again > 0) "Review $again" else "Review",
                    style = PaperType.buttonMedium,
                    color = colors.cream,
                )
            }
        }
    }
}

@Composable
private fun FeaturedDeck(deck: Deck, known: Int, again: Int, lang: AppLang, onOpen: () -> Unit) {
    val colors = LocalPaperColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(colors.sand)
            .clickable(onClick = onOpen)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Most asked", style = PaperType.tag, color = colors.greenText)
            Spacer(Modifier.height(2.dp))
            Text(
                deck.title(lang),
                style = PaperType.featuredTitle,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                deck.desc(lang),
                style = PaperType.small,
                color = colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(horizontalAlignment = Alignment.End) {
            Row {
                Text(
                    count(known),
                    style = PaperType.bigCount,
                    color = colors.ink,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    "/${count(deck.cards.size)}",
                    style = PaperType.small,
                    color = colors.muted,
                    modifier = Modifier.alignByBaseline(),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                if (again > 0) "$again again" else "Not started",
                style = PaperType.micro,
                color = colors.greenText,
            )
        }
    }
}

@Composable
private fun DeckTile(
    deck: Deck,
    known: Int,
    lang: AppLang,
    yellow: Boolean,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
) {
    val colors = LocalPaperColors.current
    val background = if (yellow) colors.yellow else colors.inkCard
    val primary = if (yellow) colors.onYellow else colors.cream
    val secondary = if (yellow) colors.onYellowMuted else colors.onInkMuted
    val track = if (yellow) colors.onYellowLine else colors.onInkLine
    val fill = if (yellow) colors.onYellow else colors.yellow

    Column(
        modifier
            .height(136.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(background)
            .clickable(onClick = onOpen)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(deck.tag(lang), style = PaperType.tag, color = secondary)
            Spacer(Modifier.height(4.dp))
            Text(
                deck.title(lang),
                style = PaperType.cardTitle,
                color = primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column {
            Text(
                "${count(known)} / ${count(deck.cards.size)}",
                style = PaperType.count,
                color = primary,
            )
            Spacer(Modifier.height(8.dp))
            PaperBar(
                progress = if (deck.cards.isNotEmpty()) known.toFloat() / deck.cards.size else 0f,
                track = track,
                fill = fill,
                height = 3.dp,
            )
        }
    }
}

@Composable
private fun DeckRow(deck: Deck, known: Int, lang: AppLang, onOpen: () -> Unit) {
    val colors = LocalPaperColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.sand)
            .clickable(onClick = onOpen)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(deck.tag(lang), style = PaperType.tag, color = colors.greenText)
            Spacer(Modifier.height(2.dp))
            Text(
                deck.title(lang),
                style = PaperType.featuredTitle.copy(fontSize = 17.sp, lineHeight = 22.sp),
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            "${count(known)} / ${count(deck.cards.size)}",
            style = PaperType.count,
            color = colors.muted,
        )
    }
}

@Composable
private fun PaperBar(progress: Float, track: Color, fill: Color, height: Dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(track),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(fill),
        )
    }
}

@Composable
private fun SyncLine(status: CardsStatus, total: Int, onRetry: () -> Unit) {
    val colors = LocalPaperColors.current
    val (text, live) = when (status) {
        is CardsStatus.Loading -> "Checking for card updates…" to true
        is CardsStatus.Updated ->
            (if (status.changed) "Cards updated · ${count(total)} questions"
            else "Cards up to date · ${count(total)} questions") to true
        is CardsStatus.Stale -> "Offline copy · tap to retry" to false
        CardsStatus.Idle -> "Cards up to date · ${count(total)} questions" to true
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .then(if (!live) Modifier.clickable(onClick = onRetry) else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (live) colors.greenText else colors.yellow),
        )
        Spacer(Modifier.width(6.dp))
        Text(text, style = PaperType.small, color = colors.muted)
    }
}

// ---------------------------------------------------------------- search

@Composable
private fun SearchBody(
    decks: List<Deck>,
    states: Map<String, CardStatus>,
    lang: AppLang,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenCard: (deckId: Int, cardId: String) -> Unit,
) {
    val colors = LocalPaperColors.current
    val hits = remember(decks, query, lang) { searchCards(decks, query, lang) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focus.requestFocus()
        keyboard?.show()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 124.dp),
    ) {
        item {
            Text("Search", style = PaperType.eyebrow, color = colors.muted)
            Spacer(Modifier.height(8.dp))
            Text("Find a question", style = PaperType.title, color = colors.ink)
        }

        item {
            Spacer(Modifier.height(20.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.sand)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = null,
                    tint = colors.muted,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            "Search questions and answers…",
                            style = PaperType.search,
                            color = colors.muted,
                            maxLines = 1,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = LocalTextStyle.current.merge(PaperType.search).copy(color = colors.ink),
                        cursorBrush = SolidColor(colors.green),
                        keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus),
                    )
                }
                if (query.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(colors.paper)
                            .clickable { onQueryChange("") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = "Clear search",
                            tint = colors.muted,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }

        when {
            query.isBlank() -> item {
                Spacer(Modifier.height(20.dp))
                Text(
                    "Search every question and answer, in both languages.",
                    style = PaperType.body,
                    color = colors.muted,
                )
            }

            hits.isEmpty() -> item {
                Spacer(Modifier.height(24.dp))
                Text("No cards match", style = PaperType.featuredTitle, color = colors.ink)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Try a shorter word — search looks in every question and answer, in both languages.",
                    style = PaperType.body,
                    color = colors.muted,
                )
            }

            else -> {
                item {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        if (hits.size == 1) "1 card matches" else "${count(hits.size)} cards match",
                        style = PaperType.small,
                        color = colors.muted,
                    )
                }
                items(hits, key = { "${it.deck.id}:${it.card.id}" }) { hit ->
                    Spacer(Modifier.height(10.dp))
                    SearchRow(
                        hit = hit,
                        lang = lang,
                        onClick = { onOpenCard(hit.deck.id, hit.card.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchRow(hit: SearchHit, lang: AppLang, onClick: () -> Unit) {
    val colors = LocalPaperColors.current
    val urdu = lang == AppLang.UR
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.sand)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(hit.deck.tag(lang), style = PaperType.tag, color = colors.greenText)
        Spacer(Modifier.height(4.dp))
        Text(
            hit.card.q(lang),
            style = if (urdu) {
                PaperType.question.copy(fontFamily = NooriNastaliq, fontSize = 18.sp, lineHeight = 34.sp)
            } else {
                PaperType.question
            },
            color = colors.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${hit.deck.title(lang)} · Card ${hit.card.n} of ${hit.deck.cards.size}",
            style = PaperType.small,
            color = colors.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---------------------------------------------------------------- nav & settings

@Composable
private fun HomeNav(
    section: HomeSection,
    colors: PaperColors,
    onDecks: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .width(232.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(colors.navBg)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (section == HomeSection.DECKS) {
                NavPill("Decks", R.drawable.ic_grid, colors, onClick = {})
                NavGhost(R.drawable.ic_search, "Search questions", colors, onClick = onSearch)
            } else {
                NavPill("Search", R.drawable.ic_search, colors, onClick = {})
                NavGhost(R.drawable.ic_grid, "Back to decks", colors, onClick = onDecks)
            }
            NavGhost(R.drawable.ic_sliders, "Language and theme", colors, onClick = onSettings)
        }
    }
}

@Composable
private fun NavPill(label: String, icon: Int, colors: PaperColors, onClick: () -> Unit) {
    Row(
        Modifier
            .width(112.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(colors.navActive)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = colors.navActiveInk,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = PaperType.body.copy(fontFamily = Instrument, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
            color = colors.navActiveInk,
        )
    }
}

@Composable
private fun NavGhost(icon: Int, description: String, colors: PaperColors, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = colors.navIcon,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun SettingsSheet(
    lang: AppLang,
    theme: ThemeMode,
    colors: PaperColors,
    onToggleLang: () -> Unit,
    onToggleTheme: () -> Unit,
    onDismiss: () -> Unit,
) {
    val swallow = remember { MutableInteractionSource() }
    Box(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = swallow, indication = null, onClick = onDismiss)
            .background(Color(0x66181816)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(colors.sand)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Language & theme",
                    style = PaperType.featuredTitle.copy(fontSize = 24.sp, lineHeight = 28.sp),
                    color = colors.ink,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.paper)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "Close",
                        tint = colors.muted,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Text("LANGUAGE", style = PaperType.micro, color = colors.muted)
            Spacer(Modifier.height(8.dp))
            Segmented(
                options = listOf("English", "اردو"),
                selected = if (lang == AppLang.UR) 1 else 0,
                colors = colors,
                onSelect = { index ->
                    if ((index == 1) != (lang == AppLang.UR)) onToggleLang()
                },
            )

            Spacer(Modifier.height(20.dp))
            Text("APPEARANCE", style = PaperType.micro, color = colors.muted)
            Spacer(Modifier.height(8.dp))
            Segmented(
                options = listOf("Light", "Dark"),
                selected = if (theme == ThemeMode.DARK) 1 else 0,
                colors = colors,
                onSelect = { index ->
                    if ((index == 1) != (theme == ThemeMode.DARK)) onToggleTheme()
                },
            )

            Spacer(Modifier.height(24.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(colors.green)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text("Done", style = PaperType.button, color = colors.cream)
            }
        }
    }
}

@Composable
private fun Segmented(
    options: List<String>,
    selected: Int,
    colors: PaperColors,
    onSelect: (Int) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.paper)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val active = index == selected
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (active) colors.ink else Color.Transparent)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = PaperType.count.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = if (active) colors.paper else colors.muted,
                )
            }
        }
    }
}
