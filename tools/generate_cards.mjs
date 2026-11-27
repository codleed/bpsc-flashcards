// Parses every NN_IT_Teacher_Interview_QA.md (and, where present, the matching
// NN_IT_Teacher_Interview_QA.json export) in the BPSC folder into:
//   - flashcards-app/app/src/main/assets/questions.json (bundled in the APK, first-run/offline copy)
//   - ../Demo/apps/web/public/questions.json          (served live at bpsc-qbank.netlify.app)
// New markdown files are picked up automatically; add a deckMeta entry to name them nicely.
// A matching JSON export (meta + qa rows) takes precedence: it carries the clean
// English question plus both Urdu fields, so regeneration cannot lose them.
// Usage: node tools/generate_cards.mjs  [--quiet]
import { readdirSync, readFileSync, writeFileSync, mkdirSync, existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const repoRoot = process.env.BPSC_DIR ?? join(here, "..", ".."); // BPSC/ — holds the md files
const outFile = join(here, "..", "app", "src", "main", "assets", "questions.json");
const webOutFile = join(repoRoot, "..", "Demo", "apps", "web", "public", "questions.json");
const quiet = process.argv.includes("--quiet");

// Optional per-deck titles. Any file without an entry gets a default title and a warning.
const deckMeta = {
  1: { title: "The Interview", desc: "Intro, hardware, the web, current affairs", tag: "MIXED", titleUr: "انٹرویو", descUr: "تعارف، ہارڈویئر، ویب، موجودہ معاملات", tagUr: "مختلط" },
  2: { title: "Hardware & Security", desc: "PC parts, networking, cybersecurity", tag: "IT", titleUr: "ہارڈویئر اور سیکیورٹی", descUr: "پی سی کے حصے، نیٹ ورکنگ، سائبر سیکیورٹی", tagUr: "آئی ٹی" },
  3: { title: "Pakistan GK", desc: "Provinces, telecom, teacher qualities", tag: "GK", titleUr: "پاکستان جنرل نالج", descUr: "صوبے، ٹیلی کام، اچھے استاد کی خوبیاں", tagUr: "جنرل نالج" },
  4: { title: "Islamiyat", desc: "Iman, Qurbani, Hijri history, Hadith", tag: "ISLAMIYAT", titleUr: "اسلامیات", descUr: "ایمان، قربانی، ہجری تاریخ، حدیث", tagUr: "اسلامیات" },
  5: { title: "Current Affairs", desc: "Civics, economy, digital signature", tag: "GK", titleUr: "موجودہ معاملات", descUr: "شہریت، معیشت، ڈیجیٹل دستخط", tagUr: "جنرل نالج" },
  6: { title: "Personal & World", desc: "Family, Hormuz, APEC, bits and bytes", tag: "MIXED", titleUr: "ذاتی اور دنیا", descUr: "خاندان، ہرمز، اپیک، بٹس اور بائٹس", tagUr: "مختلط" },
  7: { title: "Balochistan & Teaching", desc: "Divisions, MS Office, phishing, assessment", tag: "TEACHING", titleUr: "بلوچستان اور تدریس", descUr: "ڈویژنز، ایم ایس آفس، فشنگ، جائزہ", tagUr: "تدریس" },
  8: { title: "Pedagogy & Geography", desc: "TPACK, world geography, ports", tag: "TEACHING", titleUr: "تدریسی طریقے اور جغرافیہ", descUr: "TPACK، عالمی جغرافیہ، بندرگاہیں", tagUr: "تدریس" },
  9: { title: "IT Fundamentals", desc: "COMPUTER, viruses, first lesson, software", tag: "IT", titleUr: "آئی ٹی بنیادی باتیں", descUr: "کمپیوٹر، وائرس، پہلی کلاس، سافٹ ویئر", tagUr: "آئی ٹی" },
  10: { title: "Islam & Digital Life", desc: "Pillars, digital citizenship, domains", tag: "ISLAMIYAT", titleUr: "اسلام اور ڈیجیٹل زندگی", descUr: "ارکانِ اسلام، ڈیجیٹل شہریت، ڈومینز", tagUr: "اسلامیات" },
  11: { title: "Personal & IT", desc: "Female panel set — family, hardware, WordPress", tag: "MIXED", titleUr: "ذاتی اور آئی ٹی (خاتون پینل)", descUr: "خاندان، ہارڈویئر، ورڈپریس", tagUr: "مختلط" },
  12: { title: "Islamiyat & GK", desc: "Female panel set — Salah, mountains, universities", tag: "GK", titleUr: "اسلامیات اور جنرل نالج (خاتون پینل)", descUr: "نماز، پہاڑ، یونیورسٹیاں", tagUr: "جنرل نالج" },
  13: { title: "Balochistan & Zhob", desc: "District, PPC, province history, first term plan", tag: "GK", titleUr: "بلوچستان اور ژوب", descUr: "ضلع، پی پی سی، صوبائی تاریخ، پہلے تین ماہ کا منصوبہ", tagUr: "جنرل نالج" },
  14: { title: "Teaching & Digital Literacy", desc: "Flipped classroom, analytical thinking, fake news", tag: "TEACHING", titleUr: "تدریس اور ڈیجیٹل خواندگی", descUr: "فلیپڈ کلاسروم، تجزیاتی سوچ، جھوٹی خبریں", tagUr: "تدریس" },
  15: { title: "Personal & Islamiyat", desc: "Female panel set — intro, Quran & Shariah, open source", tag: "MIXED", titleUr: "ذاتی اور اسلامیات (خاتون پینل)", descUr: "تعارف، قرآن اور شریعت، اوپن سورس", tagUr: "مختلط" },
  16: { title: "Pakistan, Civics & IT", desc: "Hockey World Cup, federalism, Articles 25–28, RAM vs ROM", tag: "GK", titleUr: "پاکستان، شہریت اور آئی ٹی", descUr: "ہاکی ورلڈ کپ، وفاقیت، آئین کے آرٹیکل 25–28، RAM اور ROM", tagUr: "جنرل نالج" },
  17: { title: "Personal, World & Science", desc: "Intro, Hormuz, algorithms, scientific method", tag: "MIXED", titleUr: "ذاتی، دنیا اور سائنس", descUr: "تعارف، ہرمز، الگورتھم، سائنسی طریقہ", tagUr: "مختلط" },
  18: { title: "IT Fundamentals & Personal", desc: "Primary memory, software types, malware, Babbage", tag: "IT", titleUr: "آئی ٹی بنیادی باتیں اور ذاتی", descUr: "پرائمری میموری، سافٹ ویئر کی اقسام، بدنیتی سافٹ ویئر، چارلس بیبیج", tagUr: "آئی ٹی" },
  19: { title: "Hub Interview (Female)", desc: "Intro, Islamiyat, geography, IT basics", tag: "MIXED", titleUr: "حب انٹرویو (خاتون)", descUr: "تعارف، اسلامیات، جغرافیہ، آئی ٹی بنیادی باتیں", tagUr: "مختلط" },
  20: { title: "Kalat Panel — Pedagogy", desc: "Flipped class, TPACK, lab management, Islamiyat", tag: "TEACHING", titleUr: "قلات پینل — تدریسی طریقے", descUr: "فلیپڈ کلاس، TPACK، لیب کا انتظام، اسلامیات", tagUr: "تدریس" },
  21: { title: "Kharan Interview (Female)", desc: "Seerah, Kharan, MS Office, cybersecurity", tag: "MIXED", titleUr: "خاران انٹرویو (خاتون)", descUr: "سیرت، خاران، ایم ایس آفس، سائبر سیکیورٹی", tagUr: "مختلط" },
  22: { title: "Islamiat, Pakistan Movement & IT", desc: "Pillars, Two-Nation Theory, databases, programming", tag: "MIXED", titleUr: "اسلامیات، تحریکِ پاکستان اور آئی ٹی", descUr: "ارکانِ اسلام، دو قومی نظریہ، ڈیٹابیس، پروگرامنگ", tagUr: "مختلط" },
  23: { title: "Kalat Zone Panel A", desc: "Intro, Khuzdar, prophets of Surah Yusuf, IT basics", tag: "MIXED", titleUr: "قلات زون پینل اے", descUr: "تعارف، خضدار، سورۃ یوسف کے انبیاء، آئی ٹی بنیادی باتیں", tagUr: "مختلط" },
  24: {
    title: "Most Repeated Questions",
    desc: "Asked across the most panels, ranked by how often they came up",
    tag: "MOST ASKED",
    titleUr: "سب سے زیادہ پوچھے گئے سوالات",
    descUr: "جو سب سے زیادہ پینلز میں آئے، تعداد کے حساب سے ترتیب دیے گئے",
    tagUr: "اہم سوالات",
  },
  25: { title: "Nasirabad Panel A", desc: "Seerah, superpowers, computer basics, constitutional law", tag: "MIXED", titleUr: "نصیر آباد پینل اے", descUr: "سیرت، سپر پاورز، کمپیوٹر بنیادی باتیں، آئینی قوانین", tagUr: "مختلط" },
  26: { title: "Nasirabad Panel B (Female)", desc: "Year of the Elephant, Aam-e-Huzn, hardware, SAARC", tag: "MIXED", titleUr: "نصیر آباد پینل بی (خاتون)", descUr: "عام الفیل، عام حزن، ہارڈویئر، سارک", tagUr: "مختلط" },
  27: { title: "Jhalmagsi Interview (Female)", desc: "Salat, Azaan, Balochistan districts, NFC, AI", tag: "MIXED", titleUr: "جھل مگسی انٹرویو (خاتون)", descUr: "نماز، اذان، بلوچستان کے اضلاع، این ایف سی، اے آئی", tagUr: "مختلط" },
  28: { title: "Zhob Panel — Namaz & GK", desc: "Salat meaning, Azaan translation, Taunsa, Faisalabad, search engines", tag: "GK", titleUr: "ژوب پینل — نماز اور جنرل نالج", descUr: "نماز کا مطلب، اذان کی ترجمہ، تونسہ، فیصل آباد، سرچ انجن", tagUr: "جنرل نالج" },
  29: { title: "Zhob — Dost Muhammad (20 min)", desc: "Aqaid, fasting and Hajj, Constitution, IT fundamentals, cybersecurity", tag: "MIXED", titleUr: "ژوب — دوست محمد (20 منٹ)", descUr: "عقائد، روزہ اور حج، آئین، آئی ٹی بنیادی باتیں، سائبر سیکیورٹی", tagUr: "مختلط" },
  30: { title: "Zhob — IT Female (GK Mix)", desc: "Hottest place, coastlines, Dhaka, Bay'at, Aqaba, RAM, AI", tag: "MIXED", titleUr: "ژوب — آئی ٹی خاتون (جنرل نالج)", descUr: "گرم ترین جگہ، ساحلی پٹی، ڈھاکہ، بیعت عقبہ، ریم، اے آئی", tagUr: "مختلط" },
  31: { title: "Kharan — Fazal Karim (26 Nov)", desc: "Flipped classroom, Kharan schools and projects, geography, online teaching", tag: "KHARAN", titleUr: "خاران — فضل کریم (26 نومبر)", descUr: "فلیپڈ کلاس، خاران کے اسکول اور منصوبے، جغرافیہ، آن لائن تدریس", tagUr: "خاران" },
  32: { title: "Zhob — Islamabad & Web Basics", desc: "Islamabad history, Sain Kamal Khan Sherani, Iqbal's address, HTTP/HTTPS", tag: "MIXED", titleUr: "ژوب — اسلام آباد اور ویب بنیادیں", descUr: "اسلام آباد کی تاریخ، سائیں کمال خان شیرانی، اقبال کا خطاب، ایچ ٹی ٹی پی/ایچ ٹی ٹی پی ایس", tagUr: "مختلط" },
  124: { title: "Kalat — Short Discussion", desc: "Arkan, Hijrat, Pakistan Movement, database, computer basics", tag: "MIXED", titleUr: "قلات — مختصر گفتگو", descUr: "ارکان، ہجرت، تحریک پاکستان، ڈیٹابیس، کمپیوٹر بنیادی باتیں", tagUr: "مختلط" },
};

// The "most asked" deck is not a numbered interview source — it is a ranked
// repetition list of questions already in the bank, so it gets an id of its own
// and is appended after the markdown-driven decks.
const REPEATED_DECK_ID = 24;
const REPEATED_FILE = "repeated_questions.json";

const fileRe = /^(\d+)_IT_Teacher_Interview_QA\.md$/;
const translationsDir = join(repoRoot, "translations", "ur");

// Optional Urdu translations: any .json in translations/ur/ shaped like
// {"decks":[{"id":N,"title":"…","desc":"…","questions":[{"n":1,"q":"…","a":"…"}]}]}
// (deck-export format; files may group several decks).
function loadUrduAll() {
  const byDeck = new Map(); // id -> {title, desc, cards: Map(n -> {q, a})}
  if (!existsSync(translationsDir)) return byDeck;
  for (const f of readdirSync(translationsDir)) {
    if (!f.endsWith(".json")) continue;
    let j;
    try {
      j = JSON.parse(readFileSync(join(translationsDir, f), "utf8"));
    } catch (e) {
      console.warn(`urdu: ${f} is not valid JSON (${e.message}) — skipped`);
      continue;
    }
    for (const d of j.decks ?? []) {
      if (!d || !d.id) continue;
      let entry = byDeck.get(d.id);
      if (!entry) {
        entry = { title: null, desc: null, cards: new Map() };
        byDeck.set(d.id, entry);
      }
      if (d.title) entry.title = d.title;
      if (d.desc) entry.desc = d.desc;
      for (const c of d.questions ?? []) {
        if (c.n && c.q && c.a) entry.cards.set(Number(c.n), { q: c.q, a: c.a });
      }
    }
  }
  return byDeck;
}

// Bilingual export files (<id>_IT_Teacher_Interview_QA.json) carry meta + qa
// rows with q_en, a_en, q_ur and a_ur. When one exists it is the source of
// truth for that deck, so the merge survives every regeneration.
function loadDeckJson(id) {
  const p = join(repoRoot, `${id}_IT_Teacher_Interview_QA.json`);
  if (!existsSync(p)) return null;
  let j;
  try {
    j = JSON.parse(readFileSync(p, "utf8"));
  } catch (e) {
    console.warn(`deck ${id}: invalid JSON (${e.message}) — falling back to markdown`);
    return null;
  }
  if (!Array.isArray(j.qa) || j.qa.length === 0) {
    console.warn(`deck ${id}: JSON has no qa rows — falling back to markdown`);
    return null;
  }
  const questions = j.qa.map((row, i) => {
    const n = Number(String(row.id ?? "").split("-")[1]) || i + 1;
    if (!row.q_en || !row.a_en) {
      throw new Error(`deck ${id}: JSON row ${row.id ?? n} is missing q_en/a_en`);
    }
    const card = { n, q: row.q_en, a: row.a_en };
    if (row.q_ur && row.a_ur) {
      card.q_ur = row.q_ur;
      card.a_ur = row.a_ur;
    }
    return card;
  });
  console.log(`deck ${id}: loaded ${questions.length} cards from JSON`);
  return questions;
}

// The "most repeated questions" deck. Source is repeated_questions.json: a
// ranked list (rank 1 = asked in the most panels) with English and Urdu text
// already written out. These deliberately repeat cards from the numbered decks —
// that is the point of the deck — so the app shows them as their own cards.
function loadRepeatedQuestions() {
  const p = join(repoRoot, REPEATED_FILE);
  if (!existsSync(p)) return null;
  let j;
  try {
    j = JSON.parse(readFileSync(p, "utf8"));
  } catch (e) {
    console.warn(`repeated deck: invalid JSON (${e.message}) — skipped`);
    return null;
  }
  const rows = Array.isArray(j.questions) ? j.questions : [];
  if (rows.length === 0) {
    console.warn("repeated deck: no questions — skipped");
    return null;
  }
  const seen = new Set();
  const cards = rows.map((r, i) => {
    const q = String(r.question ?? "").trim();
    const a = String(r.answer ?? "").trim();
    if (!q || !a) throw new Error(`repeated deck: row ${r.rank ?? i + 1} is missing question/answer`);
    // Keep the file's own ranking; that order is the deck's value.
    const n = Number(r.rank) || i + 1;
    if (seen.has(q.toLowerCase())) return null;
    seen.add(q.toLowerCase());
    const card = { n, q, a };
    if (r.question_ur && r.answer_ur) {
      card.q_ur = String(r.question_ur).trim();
      card.a_ur = String(r.answer_ur).trim();
    }
    return card;
  }).filter(Boolean);
  console.log(`repeated deck: loaded ${cards.length} cards, ${cards.filter((c) => c.q_ur).length} with Urdu`);
  return cards;
}

function clean(s) {
  return s
    .replace(/<br\s*\/?>/gi, " ")
    .replace(/\*\*(.+?)\*\*/g, "$1")
    .replace(/\*(.+?)\*/g, "$1")
    .replace(/`(.+?)`/g, "$1")
    .replace(/\|/g, "·")
    .replace(/\s+/g, " ")
    .trim();
}

const rowRe = /^\|\s*(\d+)\s*\|(.*)\|(.*)\|\s*$/;

const files = readdirSync(repoRoot)
  .map((f) => fileRe.exec(f))
  .filter(Boolean)
  .map((m) => {
    const n = Number(m[1]);
    // Deck id 24 belongs to the generated "Most Repeated" deck, so a markdown
    // file with that number is published as 124 instead of being silently dropped.
    return { id: n === REPEATED_DECK_ID ? n + 100 : n, f: m[0] };
  })
  .filter((f) => existsSync(join(repoRoot, f.f)))
  .sort((a, b) => a.id - b.id);

if (files.length === 0) {
  console.error(`No numbered markdown files found in ${repoRoot}`);
  process.exit(1);
}

const decks = [];
let total = 0;

const urduAll = loadUrduAll();

for (const { id, f } of files) {
  let questions = loadDeckJson(id);
  if (!questions) {
    const lines = readFileSync(join(repoRoot, f), "utf8").split(/\r?\n/);
    questions = [];
    for (const line of lines) {
      const m = rowRe.exec(line);
      if (!m) continue;
      const n = Number(m[1]);
      const q = clean(m[2]);
      const a = clean(m[3]);
      if (!q || !a) throw new Error(`${f} row ${n}: empty cell`);
      questions.push({ n, q, a });
    }
    if (questions.length === 0) throw new Error(`${f}: no Q&A table rows found`);
  }
  const meta = deckMeta[id] ?? {
    title: `Deck ${id}`,
    desc: "Your own interview Q&A",
    tag: "MIXED",
  };
  if (!deckMeta[id]) {
    console.warn(`deck ${id}: no deckMeta entry — using placeholder "${meta.title}". Add one in tools/generate_cards.mjs to rename it.`);
  }
  if (questions.length !== 50) {
    console.warn(`deck ${id}: ${questions.length} cards (expected the usual 50)`);
  }
  const urEntry = urduAll.get(id);
  const urdu = urEntry?.cards ?? new Map();
  const titleUr = urEntry?.title ?? meta.titleUr ?? null;
  const descUr = urEntry?.desc ?? meta.descUr ?? null;
  const questionsUr = questions.map((qn) => {
    const t = urdu.get(qn.n);
    if (!t) return qn;
    return { ...qn, q_ur: t.q, a_ur: t.a };
  });
  const urduCount = questionsUr.filter((qn) => qn.q_ur && qn.a_ur).length;
  if (urduCount === 0) {
    console.warn(`deck ${id}: no Urdu translation — cards will show English.`);
  } else if (urduCount !== questions.length) {
    console.warn(`deck ${id}: Urdu covers ${urduCount}/${questions.length} cards — the rest fall back to English.`);
  }
  if (urduCount > 0) {
    console.log(`deck ${id}: urdu ${urduCount}/${questions.length}`);
  }
  total += questions.length;
  decks.push({ id, title: meta.title, desc: meta.desc, tag: meta.tag, titleUr, descUr, tagUr: meta.tagUr ?? null, questions: questionsUr });
}

// Appended last so the numbered decks keep their positions...
const repeatedCards = loadRepeatedQuestions();
const repeatedCardDeck = repeatedCards
  ? {
      id: REPEATED_DECK_ID,
      title: deckMeta[REPEATED_DECK_ID].title,
      desc: deckMeta[REPEATED_DECK_ID].desc,
      tag: deckMeta[REPEATED_DECK_ID].tag,
      titleUr: deckMeta[REPEATED_DECK_ID].titleUr,
      descUr: deckMeta[REPEATED_DECK_ID].descUr,
      tagUr: deckMeta[REPEATED_DECK_ID].tagUr,
      questions: repeatedCards,
    }
  : null;
if (repeatedCardDeck) {
  decks.push(repeatedCardDeck);
  total += repeatedCards.length;
}

// Display order: the most-repeated deck goes FIRST — it is the highest-value
// revision set, so it is what should be on screen when the app opens. Deck ids
// stay tied to their source files (24 is not a numbered interview source), so
// only the array order changes; everything in the app looks cards up by id.
const decksOut = repeatedCardDeck
  ? [
      repeatedCardDeck,
      ...decks.filter((d) => d.id !== REPEATED_DECK_ID),
    ]
  : decks;

const json = JSON.stringify({ generated: "from md", decks: decksOut });

mkdirSync(dirname(outFile), { recursive: true });
writeFileSync(outFile, json, "utf8");
let webNote = " (Demo repo not found, skipped web copy)";
if (existsSync(webOutFile)) {
  mkdirSync(dirname(webOutFile), { recursive: true });
  writeFileSync(webOutFile, json, "utf8");
  webNote = " + web copy";
}
if (!quiet) {
  console.log(`OK: ${decks.length} decks, ${total} cards -> app assets (${(json.length / 1024).toFixed(0)} KB JSON)${webNote}`);
}