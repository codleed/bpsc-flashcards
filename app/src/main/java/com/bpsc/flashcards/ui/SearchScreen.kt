package com.bpsc.flashcards.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bpsc.flashcards.R
import com.bpsc.flashcards.data.AppLang
import com.bpsc.flashcards.data.Card
import com.bpsc.flashcards.data.Deck
import com.bpsc.flashcards.data.MIXED_DECK_ID
import com.bpsc.flashcards.ui.theme.InkType

data class SearchHit(val card: Card, val deck: Deck)

/** Matches the query against both languages of the card text. */
fun searchCards(decks: List<Deck>, query: String, lang: AppLang): List<SearchHit> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    val hits = mutableListOf<SearchHit>()
    for (deck in decks) {
        // "Mixed Practice" repeats every card from the real decks. Searching it too
        // returns each match twice and reuses the same LazyColumn key, which crashes.
        if (deck.id == MIXED_DECK_ID) continue
        for (card in deck.cards) {
            val hay = listOfNotNull(card.q, card.a, card.qUr, card.aUr)
                .joinToString("\u0000").lowercase()
            if (hay.contains(q)) hits.add(SearchHit(card, deck))
        }
    }
    // Cards whose text in the current language matches come first, then deck order.
    val currentFirst = lang == AppLang.UR
    return hits.sortedWith(
        compareByDescending<SearchHit> { hit ->
            val text = if (currentFirst) hit.card.qUr ?: hit.card.q else hit.card.q
            text.lowercase().contains(q)
        }.thenBy { it.deck.id }.thenBy { it.card.n },
    )
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    strings: AppStrings,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(scheme.surface)
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    placeholder,
                    style = InkType.body,
                    color = scheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = scheme.onSurface,
                ),
                cursorBrush = SolidColor(scheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Text(
                strings.clearSearch,
                style = InkType.eyebrow,
                color = scheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onClear)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
fun SearchEmpty(strings: AppStrings) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            strings.searchEmptyTitle,
            style = InkType.deckTitle,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            strings.searchEmptyBody,
            style = InkType.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun SearchResultRow(
    hit: SearchHit,
    strings: AppStrings,
    lang: AppLang,
    onOpenCard: (deckId: Int, cardId: String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val urdu = lang == AppLang.UR
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(scheme.surface)
            .border(1.dp, scheme.outlineVariant, MaterialTheme.shapes.medium)
            .clickable { onOpenCard(hit.deck.id, hit.card.id) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(hit.deck.tag, style = InkType.eyebrow, color = scheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(
                hit.card.q(lang),
                style = if (urdu) InkType.questionUrdu.copy(fontSize = 18.sp, lineHeight = 30.sp)
                else InkType.deckTitle,
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${hit.deck.title} · ${strings.cardPosition(hit.card.n, hit.deck.cards.size)}",
                style = InkType.counter,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}