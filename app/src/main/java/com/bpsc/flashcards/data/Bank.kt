package com.bpsc.flashcards.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Card deck is content, not code: fetched from the deployed cards file, cached locally. */
const val REMOTE_CARDS_URL: String = "https://bpsc-qbank.netlify.app/questions.json"

/** Synthetic deck that repeats every card; it must not be counted or searched twice. */
const val MIXED_DECK_ID: Int = 0

/** The ranked "most asked" deck. It is the deck worth seeing first, so it leads the home. */
const val MOST_REPEATED_DECK_ID: Int = 24

data class Card(
    val deckId: Int,
    val n: Int,
    val q: String,
    val a: String,
    val qUr: String? = null,
    val aUr: String? = null,
) {
    val id: String get() = "d$deckId-n$n"

    val hasUrdu: Boolean get() = qUr != null && aUr != null

    fun q(lang: AppLang): String = if (lang == AppLang.UR) qUr ?: q else q

    fun a(lang: AppLang): String = if (lang == AppLang.UR) aUr ?: a else a
}

data class Deck(
    val id: Int,
    val title: String,
    val desc: String,
    val tag: String,
    val cards: List<Card>,
    val titleUr: String? = null,
    val descUr: String? = null,
    val tagUr: String? = null,
) {
    val hasUrduMeta: Boolean get() = titleUr != null

    val urduCards: Int get() = cards.count { it.hasUrdu }

    fun title(lang: AppLang): String = if (lang == AppLang.UR) titleUr ?: title else title

    fun desc(lang: AppLang): String = if (lang == AppLang.UR) descUr ?: desc else desc

    fun tag(lang: AppLang): String = if (lang == AppLang.UR) tagUr ?: tag else tag
}

sealed interface CardsStatus {
    data object Idle : CardsStatus
    data object Loading : CardsStatus
    /** Fetch succeeded. `changed` = the file differed from what we already had. */
    data class Updated(val changed: Boolean, val total: Int) : CardsStatus
    /** Fetch failed; operating on the local copy. */
    data class Stale(val reason: String) : CardsStatus
}

class Bank(context: Context) {

    private val appContext = context.applicationContext

    private val _decks = MutableStateFlow(loadLocal())
    val decks: StateFlow<List<Deck>> = _decks.asStateFlow()

    private val _status = MutableStateFlow<CardsStatus>(CardsStatus.Idle)
    val status: StateFlow<CardsStatus> = _status.asStateFlow()

    private fun cacheFile(): File = File(appContext.filesDir, "cards.json")

    private fun bundledText(): String =
        appContext.assets.open("questions.json").bufferedReader().use { it.readText() }

    private fun localText(): String =
        if (cacheFile().exists()) cacheFile().readText() else bundledText()

    private fun loadLocal(): List<Deck> = parse(localText())

    /** Fetch the latest card file, cache it locally, and swap it in. */
    suspend fun refresh() {
        if (_status.value is CardsStatus.Loading) return
        _status.value = CardsStatus.Loading
        try {
            val text = withContext(Dispatchers.IO) { download() }
            if (text == null) {
                _status.value = CardsStatus.Stale("no connection")
                return
            }
            val previous = localText()
            cacheFile().writeText(text)
            _decks.value = parse(text)
            _status.value = CardsStatus.Updated(
                changed = previous != text,
                total = _decks.value.filter { it.id != MIXED_DECK_ID }.sumOf { it.cards.size },
            )
        } catch (e: Exception) {
            _status.value = CardsStatus.Stale(e.message ?: "download failed")
        }
    }

    private fun download(): String? = try {
        val connection = URL(REMOTE_CARDS_URL).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.requestMethod = "GET"
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } finally {
            connection.disconnect()
        }
    } catch (e: Exception) {
        null
    }

    private fun parse(text: String): List<Deck> {
        val arr: JSONArray = JSONObject(text).getJSONArray("decks")
        val parsed = List(arr.length()) { i ->
            val d = arr.getJSONObject(i)
            val id = d.getInt("id")
            val qs: JSONArray = d.getJSONArray("questions")
            Deck(
                id = id,
                title = d.getString("title"),
                desc = d.getString("desc"),
                tag = d.getString("tag"),
                cards = List(qs.length()) { j ->
                    val q = qs.getJSONObject(j)
                    Card(
                        deckId = id,
                        n = q.getInt("n"),
                        q = q.getString("q"),
                        a = q.getString("a"),
                        qUr = q.optString("q_ur").ifBlank { null },
                        aUr = q.optString("a_ur").ifBlank { null },
                    )
                },
                // Note the camelCase keys: the published questions.json uses
                // titleUr/descUr/tagUr, not title_ur/desc_ur/tag_ur.
                titleUr = d.optString("titleUr").ifBlank { null },
                descUr = d.optString("descUr").ifBlank { null },
                tagUr = d.optString("tagUr").ifBlank { null },
            )
        }
        val total = parsed.sumOf { it.cards.size }
        val mixed = Deck(
            MIXED_DECK_ID,
            "Mixed Practice",
            "All $total cards, shuffled",
            "ALL",
            parsed.flatMap { it.cards },
            titleUr = "آمیخ مشق",
            descUr = "تمام $total کارڈز، بے ترتیب",
            tagUr = "سب",
        )
        return listOf(mixed) + parsed
    }

    fun deck(id: Int): Deck = _decks.value.first { it.id == id }
}