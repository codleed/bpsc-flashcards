# BPSC Flashcards

A native Android app (Kotlin + Jetpack Compose, Material 3) for practicing the BPSC IT Teacher interview Q&A bank as flash cards. All 900 cards across 18 decks are bundled, and the app refreshes cards live from a hosted JSON file so new questions reach installed phones without reinstalling the app.

## The app

- Deck list with per-deck and overall progress (mastered / to revisit).
- Tap a card to flip it: question on the front, the speakable answer on the back.
- Mark **GOT IT** (mastered) or **AGAIN** (kept in the round). Progress persists on the device.
- Light "ink & paper" and dark "night revision" themes.

## Live card updates

Cards are content, not code. On every launch the app:

1. Shows the cached/bundled cards instantly (works fully offline).
2. Fetches `questions.json` from `REMOTE_CARDS_URL` (top of `app/src/main/java/com/bpsc/flashcards/data/Bank.kt`).
3. Saves it locally and swaps it in. A failed fetch keeps the last good copy and shows a retry hint.

To host your own copy, serve any `questions.json` matching the deck schema and point the constant at it. The JSON is generated from the source markdown files (see below).

## Building

Requirements: JDK 17, Android SDK (compileSdk 35, build-tools 35.0.0).

```bash
./gradlew assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk
```

## Regenerating the card data

The generator parses numbered markdown files (`NN_IT_Teacher_Interview_QA.md`) — any new number is picked up automatically — and writes both the bundled asset and a copy for the web host.

```bash
node tools/generate_cards.mjs                     # reads ../.. by default
BPSC_DIR=/path/to/markdown/folder node tools/generate_cards.mjs   # or point it anywhere
```

A new deck without a `deckMeta` entry in `tools/generate_cards.mjs` gets a placeholder title and a warning — add one line there to name it.