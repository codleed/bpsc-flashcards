# BPSC Flashcards

A native Android app (Kotlin + Jetpack Compose, Material 3) for practicing the BPSC IT Teacher interview Q&A bank as flash cards. All 900 cards across 18 decks are bundled, and the app refreshes cards live from a hosted JSON file so new questions reach installed phones without reinstalling the app.

## The app

- Deck list with per-deck and overall progress (mastered / to revisit).
- Tap a card to flip it: question on the front, the speakable answer on the back.
- **PREV / NEXT** under every card to move through the deck at your own pace (dimmed at the ends).
- **Search** on the home screen across every question and answer, in both languages — e.g. "Arkan". Tapping a result opens that deck at that card (browse mode, with a "start practice" option at the end).
- Mark **GOT IT** (mastered) or **AGAIN** (kept in the round). Progress persists on the device.
- Light "ink & paper" and dark "night revision" themes.

## Language and theme

The deck list has two toggles: **English / اردو** and **light / dark**. Both persist on the device.

- **Urdu affects card text only** — with اردو selected, each card's question and answer render in Urdu (right-aligned, in the bundled **Jameel Noori Nastaleeq** font). The screen text — titles, labels, buttons — stays English.
- Urdu content lives in `../translations/ur/*.json` (deck-export format: `{"decks":[{"id":N,"questions":[{"n":1,"q":"…","a":"…"}]}]}`). Cards without a translation fall back to English.
- Regenerating merges those translations into `questions.json` as `q_ur`/`a_ur` next to the English text.
- The same `questions.json` (Urdu fields included) is what the live-update fetch delivers.

## Live card updates

Cards are content, not code. On every launch the app:

1. Shows the cached/bundled cards instantly (works fully offline).
2. Fetches `questions.json` from `REMOTE_CARDS_URL` (top of `app/src/main/java/com/bpsc/flashcards/data/Bank.kt`).
3. Saves it locally and swaps it in. A failed fetch keeps the last good copy and shows a retry hint.

To host your own copy, serve any `questions.json` matching the deck schema and point the constant at it. The JSON is generated from the source markdown files (see below).

## App updates (self-hosted)

The app is not on Play, so it updates itself from the same Netlify site as the cards.

On every launch, on the deck list, the app fetches
`https://bpsc-qbank.netlify.app/app/version.json`. Nothing appears unless the
published `versionCode` is higher than the installed one. When it is, a
**non-blocking banner** shows the new version number and your release notes with:

- **DOWNLOAD** — fetches the APK into the app's own cache, verifies its SHA-256
  against the manifest, then turns into **INSTALL**.
- **INSTALL** — hands the APK to the system installer over a `FileProvider`
  (no storage permission needed). Android will ask once to allow "install unknown
  apps" for this app.
- **LATER** — hides the banner for this launch only. Studying is never blocked.

A failed check or download is silent; the banner simply does not appear.

Tapping **INSTALL** installs in place, so progress is kept. That only works
because release builds are signed with `keystore/bpsc-release.jks`.

### Publishing a release

1. Bump the version in `app/src/main/java/com/bpsc/flashcards/data/AppVersion.kt`
   (`CODE` must always increase; `NAME` is what users see). Gradle reads that same
   file, so the APK and the manifest can never disagree.
2. `node tools/publish-app.mjs` — builds the signed release APK, copies it to
   `Demo/apps/web/public/app/bpsc-<code>.apk`, writes `app/version.json` with the
   checksum, commits only those files, and pushes.
3. Netlify serves it; installed phones show the banner on their next launch.

Use `RELEASE_NOTES="…" node tools/publish-app.mjs` to set the note text, or it
defaults to "Version X.Y".

**Back up the keystore.** `keystore/` is gitignored on purpose. If you lose it,
installed phones can never update in place again — they would have to uninstall
and reinstall, losing their saved progress.

The release build blocks cleartext HTTP; a debug build reads a local manifest so
you can rehearse the flow with `node tools/mock-update.mjs`.

## Fixing search crash (2026-11-25)

Searching any word that matched a card crashed the app with
`IllegalArgumentException: Key "d3-n25" was already used`. Cause: `Bank.parse()`
prepends a synthetic "Mixed Practice" deck (id 0) that repeats every card, so each
match appeared twice and Compose rejected the duplicate `LazyColumn` keys. Fixes:

- `searchCards()` skips the synthetic deck (in `SearchScreen.kt`).
- Result keys are now composite (`deck.id:card.id`).
- Totals exclude the synthetic deck, so the header counts 1,100 not 2,200.

## Urdu deck metadata (2026-11-25)

`Bank.kt` read `title_ur`/`desc_ur`/`tag_ur`, but `questions.json` uses
`titleUr`/`descUr`/`tagUr`, so Urdu deck titles/descriptions never displayed.
`DeckRow` still only renders the English title/desc; the fixed fields are read and
available for a future Urdu chrome pass.

## Building

Requirements: JDK 17, Android SDK (compileSdk 35, build-tools 35.0.0).

```bash
./gradlew assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk

./gradlew assembleRelease
# -> app/build/outputs/apk/release/app-release.apk  (signed, see keystore/)
```

## Regenerating the card data

The generator parses numbered markdown files (`NN_IT_Teacher_Interview_QA.md`) — any new number is picked up automatically — and writes both the bundled asset and a copy for the web host.

```bash
node tools/generate_cards.mjs                     # reads ../.. by default
BPSC_DIR=/path/to/markdown/folder node tools/generate_cards.mjs   # or point it anywhere
```

Urdu translations are picked up from `<BPSC_DIR>/translations/ur/*.json` (deck-export format). Decks 19–24 carry their Urdu inside their own bilingual source file instead (see below).

A new deck without a `deckMeta` entry in `tools/generate_cards.mjs` gets a placeholder title and a warning — add one line there to name it.

### Sources that are not markdown

- **`NN_IT_Teacher_Interview_QA.json`** (decks 19–24): a bilingual export with `meta` + `qa` rows (`id`, `q_roman`, `q_en`, `q_ur`, `a_en`, `a_ur`). It wins over the markdown table, so regenerating cannot drop the Urdu text. These files are the only place decks 19–22 keep their Urdu — if they go missing, those decks silently fall back to English-only from the markdown.
- **`repeated_questions.json`**: builds deck 24, "Most Repeated Questions". Rows carry `rank`, `question`, `answer`, `question_ur`, `answer_ur`. It is a ranked repetition of questions already in the bank, so the same question legitimately appears twice — once in its source deck and once here. Display order is **first**, ahead of every numbered deck, because it is the highest-value revision set; its id stays 24, since ids map to source files and everything looks cards up by id.