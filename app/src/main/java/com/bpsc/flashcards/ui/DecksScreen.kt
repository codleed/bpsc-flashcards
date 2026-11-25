package com.bpsc.flashcards.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bpsc.flashcards.R
import com.bpsc.flashcards.data.AppLang
import com.bpsc.flashcards.data.CardsStatus
import com.bpsc.flashcards.data.CardStatus
import com.bpsc.flashcards.data.Deck
import com.bpsc.flashcards.data.MIXED_DECK_ID
import com.bpsc.flashcards.data.ReleaseInfo
import com.bpsc.flashcards.data.ThemeMode
import com.bpsc.flashcards.data.UpdateStatus
import com.bpsc.flashcards.ui.theme.InkType
import com.bpsc.flashcards.ui.theme.warmAccent

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
    var query by remember { mutableStateOf("") }
    val hits = remember(decks, query, lang) { searchCards(decks, query, lang) }
    val allCards = decks.filter { it.id != MIXED_DECK_ID }.flatMap { it.cards }
    val known = allCards.count { states[it.id] == CardStatus.KNOWN }
    val again = allCards.count { states[it.id] == CardStatus.AGAIN }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 40.dp),
        ) {
            item {
                Column {
                    Text(
                        strings.appEyebrow,
                        style = InkType.eyebrow,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        strings.appTitle,
                        style = InkType.appTitle,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        strings.appIntro(allCards.size),
                        style = InkType.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    CardSyncStatus(cardsStatus = cardsStatus, strings = strings, onRefresh = onRefresh)
                    Spacer(Modifier.height(20.dp))

                    if (showUpdate && updateRelease != null) {
                        UpdateBanner(
                            release = updateRelease,
                            status = updateStatus,
                            onDownload = onDownloadUpdate,
                            onInstall = onInstallUpdate,
                            onDismiss = onDismissUpdate,
                        )
                        Spacer(Modifier.height(20.dp))
                    }

                    TogglesRow(
                        lang = lang,
                        theme = theme,
                        strings = strings,
                        onToggleLang = onToggleLang,
                        onToggleTheme = onToggleTheme,
                    )

                    Spacer(Modifier.height(26.dp))
                    MasteryRow(
                        known = known,
                        total = allCards.size,
                        again = again,
                        strings = strings,
                    )
                    Spacer(Modifier.height(20.dp))
                    SearchBar(
                        query = query,
                        onQueryChange = { query = it },
                        onClear = { query = "" },
                        strings = strings,
                        placeholder = strings.searchPlaceholder,
                    )
                    Spacer(Modifier.height(28.dp))
                }
            }
            if (query.isNotBlank()) {
                if (hits.isEmpty()) {
                    item { SearchEmpty(strings = strings) }
                } else {
                    item {
                        Text(
                            strings.resultsCount(hits.size),
                            style = InkType.eyebrow,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                    }
                    items(hits, key = { "${it.deck.id}:${it.card.id}" }) { hit ->
                        SearchResultRow(
                            hit = hit,
                            strings = strings,
                            lang = lang,
                            onOpenCard = onOpenCard,
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            } else {
                items(decks, key = { it.id }) { deck ->
                    DeckRow(
                        deck = deck,
                        strings = strings,
                        known = deck.cards.count { states[it.id] == CardStatus.KNOWN },
                        again = deck.cards.count { states[it.id] == CardStatus.AGAIN },
                        onOpenDeck = onOpenDeck,
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
            item {
                Spacer(Modifier.height(20.dp))
                Text(
                    strings.footerNote,
                    style = InkType.eyebrow,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun TogglesRow(
    lang: AppLang,
    theme: ThemeMode,
    strings: AppStrings,
    onToggleLang: () -> Unit,
    onToggleTheme: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1.4f)) {
            Text(strings.languageLabel, style = InkType.eyebrow, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(6.dp))
            SegmentedControl(
                options = listOf(strings.english, strings.urdu),
                selectedIndex = if (lang == AppLang.UR) 1 else 0,
                onSelect = { onToggleLang() },
            )
        }
        Column(Modifier.weight(1f)) {
            Text(strings.themeLabel, style = InkType.eyebrow, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(6.dp))
            SegmentedControl(
                options = listOf(strings.light, strings.dark),
                icons = listOf(R.drawable.ic_sun, R.drawable.ic_moon),
                selectedIndex = if (theme == ThemeMode.DARK) 1 else 0,
                onSelect = { onToggleTheme() },
            )
        }
    }
}

@Composable
private fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    icons: List<Int>? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(scheme.surface)
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(3.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) scheme.primary else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { if (!selected) onSelect(index) }
                    .padding(vertical = 9.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    icons?.getOrNull(index)?.let { icon ->
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                            tint = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        label,
                        style = InkType.eyebrow,
                        color = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun CardSyncStatus(
    cardsStatus: CardsStatus,
    strings: AppStrings,
    onRefresh: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val (text, color) = when (cardsStatus) {
        is CardsStatus.Loading -> strings.checking to scheme.onSurfaceVariant
        is CardsStatus.Updated ->
            (if (cardsStatus.changed) strings.cardsUpdated(cardsStatus.total)
            else strings.cardsUpToDate(cardsStatus.total)) to scheme.primary
        is CardsStatus.Stale -> strings.offlineCopy to warmAccent()
        CardsStatus.Idle -> strings.loadedLocally to scheme.outline
    }
    val clickable = cardsStatus !is CardsStatus.Loading
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .then(if (clickable) Modifier.clickable(onClick = onRefresh) else Modifier)
            .padding(vertical = 4.dp),
    ) {
        Text(text, style = InkType.eyebrow, color = color)
    }
}

@Composable
private fun MasteryRow(known: Int, total: Int, again: Int, strings: AppStrings) {
    val scheme = MaterialTheme.colorScheme
    Column {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            Text(
                strings.mastered,
                style = InkType.eyebrow,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                strings.masteredOf(known, total),
                style = InkType.counter,
                color = scheme.onSurface,
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(scheme.outlineVariant),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((known / total.toFloat()).coerceIn(0f, 1f))
                    .background(scheme.primary),
            )
        }
        if (again > 0) {
            Spacer(Modifier.height(6.dp))
            Text(strings.againBadge(again), style = InkType.eyebrow, color = warmAccent())
        }
    }
}

@Composable
private fun DeckRow(
    deck: Deck,
    strings: AppStrings,
    known: Int,
    again: Int,
    onOpenDeck: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val total = deck.cards.size
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(scheme.surface)
            .border(1.dp, scheme.outlineVariant, MaterialTheme.shapes.medium)
            .clickable { onOpenDeck(deck.id) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(deck.tag, style = InkType.eyebrow, color = scheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(
                deck.title,
                style = InkType.deckTitle,
                color = scheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                deck.desc,
                style = InkType.body,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
            Text(
                strings.masteredOf(known, total),
                style = InkType.counter,
                color = if (known == total) scheme.primary else scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .width(72.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(scheme.outlineVariant),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((known / total.toFloat()).coerceIn(0f, 1f))
                        .background(scheme.primary),
                )
            }
            if (again > 0) {
                Spacer(Modifier.height(5.dp))
                Text(
                    strings.againBadge(again),
                    style = InkType.eyebrow,
                    color = warmAccent(),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}