// Parses every NN_IT_Teacher_Interview_QA.md in the BPSC folder into:
//   - flashcards-app/app/src/main/assets/questions.json (bundled in the APK, first-run/offline copy)
//   - ../Demo/apps/web/public/questions.json          (served live at bpsc-qbank.netlify.app)
// New markdown files are picked up automatically; add a deckMeta entry to name them nicely.
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
};

const fileRe = /^(\d+)_IT_Teacher_Interview_QA\.md$/;
const translationsDir = join(repoRoot, "translations", "ur");

// Optional Urdu translations per deck: translations/ur/NN.json = {"cards":[{"n":1,"q":"...","a":"..."}]}
function loadUrdu(id) {
  const f = join(translationsDir, `${String(id).padStart(2, "0")}.json`);
  if (!existsSync(f)) return new Map();
  const map = new Map();
  for (const c of JSON.parse(readFileSync(f, "utf8")).cards ?? []) {
    if (c.n && c.q && c.a) map.set(Number(c.n), { q: c.q, a: c.a });
  }
  return map;
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
  .map((m) => ({ id: Number(m[1]), f: m[0] }))
  .filter((f) => existsSync(join(repoRoot, f.f)))
  .sort((a, b) => a.id - b.id);

if (files.length === 0) {
  console.error(`No numbered markdown files found in ${repoRoot}`);
  process.exit(1);
}

const decks = [];
let total = 0;

for (const { id, f } of files) {
  const lines = readFileSync(join(repoRoot, f), "utf8").split(/\r?\n/);
  const questions = [];
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
  const urdu = loadUrdu(id);
  const questionsUr = questions.map((qn) => {
    const t = urdu.get(qn.n);
    if (!t) return qn;
    return { ...qn, q_ur: t.q, a_ur: t.a };
  });
  if (urdu.size > 0) {
    console.log(`deck ${id}: urdu ${urdu.size}/${questions.length}`);
  }
  total += questions.length;
  decks.push({ id, ...meta, questions: questionsUr });
}

const json = JSON.stringify({ generated: "from md", decks });

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