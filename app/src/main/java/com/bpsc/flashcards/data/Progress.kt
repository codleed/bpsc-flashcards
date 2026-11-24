package com.bpsc.flashcards.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CardStatus { KNOWN, AGAIN }

class ProgressStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("bpsc_progress", Context.MODE_PRIVATE)

    private val _states = MutableStateFlow(read())
    val states: StateFlow<Map<String, CardStatus>> = _states.asStateFlow()

    private fun read(): Map<String, CardStatus> =
        (prefs.getStringSet(KEY, emptySet()) ?: emptySet())
            .mapNotNull { entry ->
                val idx = entry.indexOf(':')
                if (idx <= 0) return@mapNotNull null
                val status = runCatching { CardStatus.valueOf(entry.substring(0, idx)) }.getOrNull()
                    ?: return@mapNotNull null
                entry.substring(idx + 1) to status
            }
            .toMap()

    private fun write(map: Map<String, CardStatus>) {
        prefs.edit().putStringSet(KEY, map.map { (id, s) -> "${s.name}:$id" }.toSet()).apply()
        _states.value = map
    }

    fun mark(cardId: String, status: CardStatus) {
        write(_states.value + (cardId to status))
    }

    fun resetCards(ids: Collection<String>) {
        val next = _states.value.toMutableMap()
        ids.forEach { next.remove(it) }
        write(next)
    }

    fun resetDeck(deck: Deck) = resetCards(deck.cards.map { it.id })

    fun resetAll() = write(emptyMap())

    private companion object {
        const val KEY = "status"
    }
}