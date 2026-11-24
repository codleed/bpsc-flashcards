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
  1: { title: "The Interview", desc: "Intro, hardware, the web, current affairs", tag: "MIXED" },
  2: { title: "Hardware & Security", desc: "PC parts, networking, cybersecurity", tag: "IT" },
  3: { title: "Pakistan GK", desc: "Provinces, telecom, teacher qualities", tag: "GK" },
  4: { title: "Islamiyat", desc: "Iman, Qurbani, Hijri history, Hadith", tag: "ISLAMIYAT" },
  5: { title: "Current Affairs", desc: "Civics, economy, digital signature", tag: "GK" },
  6: { title: "Personal & World", desc: "Family, Hormuz, APEC, bits and bytes", tag: "MIXED" },
  7: { title: "Balochistan & Teaching", desc: "Divisions, MS Office, phishing, assessment", tag: "TEACHING" },
  8: { title: "Pedagogy & Geography", desc: "TPACK, world geography, ports", tag: "TEACHING" },
  9: { title: "IT Fundamentals", desc: "COMPUTER, viruses, first lesson, software", tag: "IT" },
  10: { title: "Islam & Digital Life", desc: "Pillars, digital citizenship, domains", tag: "ISLAMIYAT" },
  11: { title: "Personal & IT", desc: "Female panel set — family, hardware, WordPress", tag: "MIXED" },
  12: { title: "Islamiyat & GK", desc: "Female panel set — Salah, mountains, universities", tag: "GK" },
  13: { title: "Balochistan & Zhob", desc: "District, PPC, province history, first term plan", tag: "GK" },
  14: { title: "Teaching & Digital Literacy", desc: "Flipped classroom, analytical thinking, fake news", tag: "TEACHING" },
  15: { title: "Personal & Islamiyat", desc: "Female panel set — intro, Quran & Shariah, open source", tag: "MIXED" },
  16: { title: "Pakistan, Civics & IT", desc: "Hockey World Cup, federalism, Articles 25–28, RAM vs ROM", tag: "GK" },
  17: { title: "Personal, World & Science", desc: "Intro, Hormuz, algorithms, scientific method", tag: "MIXED" },
  18: { title: "IT Fundamentals & Personal", desc: "Primary memory, software types, malware, Babbage", tag: "IT" },
};

const fileRe = /^(\d+)_IT_Teacher_Interview_QA\.md$/;

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
  total += questions.length;
  decks.push({ id, ...meta, questions });
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