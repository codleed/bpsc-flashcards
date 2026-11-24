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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bpsc.flashcards.data.CardsStatus
import com.bpsc.flashcards.data.CardStatus
import com.bpsc.flashcards.data.Deck
import com.bpsc.flashcards.ui.theme.InkType
import com.bpsc.flashcards.ui.theme.warmAccent

@Composable
fun DecksScreen(
    decks: List<Deck>,
    states: Map<String, CardStatus>,
    cardsStatus: CardsStatus,
    onRefresh: () -> Unit,
    onOpenDeck: (Int) -> Unit,
) {
    val allCards = decks.flatMap { it.cards }
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
                        "REVISION · BPSC INTERVIEW",
                        style = InkType.eyebrow,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "IT Teacher Interview",
                        style = InkType.appTitle,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "${allCards.size} questions answered in short speakable lines, exactly as you wrote them. Tap a card to flip it.",
                        style = InkType.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    CardSyncStatus(cardsStatus = cardsStatus, onRefresh = onRefresh)
                    Spacer(Modifier.height(26.dp))
                    MasteryRow(known = known, total = allCards.size, again = again)
                    Spacer(Modifier.height(28.dp))
                }
            }
            items(decks, key = { it.id }) { deck ->
                DeckRow(
                    deck = deck,
                    known = deck.cards.count { states[it.id] == CardStatus.KNOWN },
                    again = deck.cards.count { states[it.id] == CardStatus.AGAIN },
                    onOpenDeck = onOpenDeck,
                )
                Spacer(Modifier.height(12.dp))
            }
            item {
                Spacer(Modifier.height(20.dp))
                Text(
                    "Marking AGAIN keeps a card in the round; marking GOT IT masters it. Progress is saved on this device.",
                    style = InkType.eyebrow,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun CardSyncStatus(cardsStatus: CardsStatus, onRefresh: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val (text, color) = when (cardsStatus) {
        is CardsStatus.Loading -> "CHECKING FOR CARD UPDATES…" to scheme.onSurfaceVariant
        is CardsStatus.Updated ->
            (if (cardsStatus.changed) "CARDS UPDATED · ${cardsStatus.total} QUESTIONS"
            else "CARDS UP TO DATE · ${cardsStatus.total} QUESTIONS") to scheme.primary
        is CardsStatus.Stale -> "OFFLINE COPY · TAP TO RETRY" to warmAccent()
        CardsStatus.Idle -> "CARDS LOADED LOCALLY" to scheme.outline
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
private fun MasteryRow(known: Int, total: Int, again: Int) {
    val scheme = MaterialTheme.colorScheme
    Column {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            Text("MASTERED", style = InkType.eyebrow, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text("$known / $total", style = InkType.counter, color = scheme.onSurface)
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
            Text("$again cards marked again", style = InkType.eyebrow, color = warmAccent())
        }
    }
}

@Composable
private fun DeckRow(deck: Deck, known: Int, again: Int, onOpenDeck: (Int) -> Unit) {
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
            Text(deck.title, style = InkType.deckTitle, color = scheme.onSurface)
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
                "$known / $total",
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
                Text("$again again", style = InkType.eyebrow, color = warmAccent())
            }
        }
    }
}