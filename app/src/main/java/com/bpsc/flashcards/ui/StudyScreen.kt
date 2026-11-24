package com.bpsc.flashcards.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bpsc.flashcards.R
import com.bpsc.flashcards.data.Card
import com.bpsc.flashcards.data.CardStatus
import com.bpsc.flashcards.data.Deck
import com.bpsc.flashcards.ui.theme.InkType
import com.bpsc.flashcards.ui.theme.warmAccent
import kotlin.random.Random

@Composable
fun StudyScreen(
    deck: Deck,
    states: Map<String, CardStatus>,
    reduceMotion: Boolean,
    onMark: (String, CardStatus) -> Unit,
    onResetDeck: () -> Unit,
    onExit: () -> Unit,
) {
    var session by remember(deck.id) { mutableIntStateOf(0) }
    var round by remember(deck.id, session) { mutableIntStateOf(1) }
    var seed by remember(deck.id, session, round) { mutableStateOf(0L) }
    val queue: List<Card> = remember(deck.id, session, round, seed) {
        buildQueue(deck, states, onlyAgain = round > 1, rnd = Random(if (seed == 0L) System.nanoTime() else seed))
    }
    var index by remember(deck.id, session, round, seed) { mutableIntStateOf(0) }
    var flipped by remember(deck.id, session, round, seed) { mutableStateOf(false) }
    var pending by remember(deck.id, session, round, seed) { mutableStateOf<CardStatus?>(null) }

    // Advance to the next card only after the flip-back animation settles.
    LaunchedEffect(flipped) {
        if (!flipped) {
            val status = pending
            if (status != null) {
                pending = null
                queue.getOrNull(index)?.let { onMark(it.id, status) }
                index++
            }
        }
    }

    val sessionDone = index >= queue.size
    val knownInDeck = deck.cards.count { states[it.id] == CardStatus.KNOWN }
    val againInDeck = deck.cards.count { states[it.id] == CardStatus.AGAIN }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 20.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onExit) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = "Back to decks",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Text(
                    deck.title,
                    style = InkType.counter,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (!sessionDone) {
                    Text(
                        "${index + 1} / ${queue.size}",
                        style = InkType.counter,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(
                            if (queue.isEmpty()) 0f
                            else (index / queue.size.toFloat()).coerceIn(0f, 1f),
                        )
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
            Spacer(Modifier.height(4.dp))

            if (sessionDone) {
                SessionSummary(
                    deck = deck,
                    known = knownInDeck,
                    again = againInDeck,
                    onRevisit = { round++ },
                    onResetAndPractice = {
                        onResetDeck()
                        session++
                    },
                    onExit = onExit,
                )
            } else {
                Box(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = 4.dp),
                ) {
                    FlipCard(
                        card = queue[index],
                        deckTag = deck.tag,
                        flipped = flipped,
                        reduceMotion = reduceMotion,
                        onFlip = { flipped = !flipped },
                    )
                }
                ActionBar(
                    flipped = flipped,
                    onAgain = {
                        pending = CardStatus.AGAIN
                        flipped = false
                    },
                    onGotIt = {
                        pending = CardStatus.KNOWN
                        flipped = false
                    },
                )
            }
        }
    }
}

private fun buildQueue(
    deck: Deck,
    states: Map<String, CardStatus>,
    onlyAgain: Boolean,
    rnd: Random,
): List<Card> {
    val pool = deck.cards.filter { card ->
        val status = states[card.id]
        if (onlyAgain) status == CardStatus.AGAIN else status != CardStatus.KNOWN
    }
    return pool.shuffled(rnd)
}

@Composable
private fun FlipCard(
    card: Card,
    deckTag: String,
    flipped: Boolean,
    reduceMotion: Boolean,
    onFlip: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = if (reduceMotion) snap() else tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "flip",
    )
    val density = LocalDensity.current.density
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 16f * density
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFlip,
            ),
    ) {
        if (rotation <= 90f) {
            CardFace {
                Column(
                    Modifier.fillMaxSize().padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "QUESTION",
                        style = InkType.eyebrow,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        card.q,
                        style = InkType.question,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(28.dp))
                    Text(
                        "TAP TO REVEAL",
                        style = InkType.eyebrow,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        } else {
            Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) {
                CardFace {
                    Column(
                        Modifier.fillMaxSize().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            "ANSWER · $deckTag",
                            style = InkType.eyebrow,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            card.a,
                            style = InkType.answer,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardFace(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .shadow(6.dp, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
    ) {
        content()
    }
}

@Composable
private fun ActionBar(
    flipped: Boolean,
    onAgain: () -> Unit,
    onGotIt: () -> Unit,
) {
    if (flipped) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onAgain,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = warmAccent()),
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Text("AGAIN", style = InkType.eyebrow)
            }
            Button(
                onClick = onGotIt,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Text("GOT IT", style = InkType.eyebrow)
            }
        }
    } else {
        Box(
            Modifier.fillMaxWidth().height(84.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Tap the card to reveal the answer",
                style = InkType.eyebrow,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun SessionSummary(
    deck: Deck,
    known: Int,
    again: Int,
    onRevisit: () -> Unit,
    onResetAndPractice: () -> Unit,
    onExit: () -> Unit,
) {
    val complete = again == 0
    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (complete) "SESSION COMPLETE" else "ROUND COMPLETE",
            style = InkType.eyebrow,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            if (complete) "All ${deck.cards.size} cards marked known"
            else "$again cards to revisit",
            style = InkType.appTitle,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            if (complete) "${deck.title} is mastered. Come back in a few days to keep it fresh."
            else "Review the ones that tripped you up, then finish the deck.",
            style = InkType.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        if (complete) {
            PrimaryButton("BACK TO DECKS", onExit)
            Spacer(Modifier.height(12.dp))
            SecondaryButton("RESET DECK & PRACTICE AGAIN", onResetAndPractice)
        } else {
            PrimaryButton("REVISIT $again CARDS", onRevisit)
            Spacer(Modifier.height(12.dp))
            SecondaryButton("BACK TO DECKS", onExit)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "$known of ${deck.cards.size} known in this deck",
            style = InkType.eyebrow,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        Text(label, style = InkType.eyebrow)
    }
}

@Composable
private fun SecondaryButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        Text(label, style = InkType.eyebrow)
    }
}