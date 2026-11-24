#!/usr/bin/env node
// Trigger this whenever markdown files are added or changed in BPSC\.
// Regenerates questions.json (app bundle + web copy), commits ONLY the two card
// files in the Demo repo (your other work-in-progress stays untouched), pushes,
// and lets Netlify deploy.
//
//   node tools/publish-cards.mjs            regenerate + commit + push
//   node tools/publish-cards.mjs --no-push  regenerate + commit, no push
//   node tools/publish-cards.mjs --dry-run  print everything, change nothing
import { execSync } from "node:child_process";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { existsSync } from "node:fs";

const here = dirname(fileURLToPath(import.meta.url));
const demoDir = join(here, "..", "..", "..", "Demo");
const args = process.argv.slice(2);
const dryRun = args.includes("--dry-run");
const noPush = args.includes("--no-push");
const onlyCommit = args.includes("--commit-only") || noPush;

const run = (cmd) => {
  console.log("+", cmd);
  if (!dryRun) execSync(cmd, { stdio: "inherit" });
};

run(`node "${join(here, "generate_cards.mjs")}"`);

if (!existsSync(demoDir)) {
  console.error(`Demo repo not found at ${demoDir}`);
  process.exit(1);
}

// Stage only the two files this script owns, so unrelated WIP is never swept in.
run(`git -C "${demoDir}" add -- apps/web/public/questions.json netlify.toml`);

const staged = dryRun
  ? "apps/web/public/questions.json netlify.toml"
  : execSync(`git -C "${demoDir}" diff --cached --name-only`, { encoding: "utf8" }).trim();
if (!staged) {
  console.log("No card changes to publish.");
  process.exit(0);
}
console.log("staged:\n" + staged);

const msg = `cards: update question bank (${new Date().toISOString().slice(0, 10)})`;
run(`git -C "${demoDir}" commit -m "${msg}"`);

if (onlyCommit) {
  console.log(dryRun ? "dry-run finished — nothing changed." : "Committed. Push any time with: git push");
  process.exit(0);
}

run(`git -C "${demoDir}" push`);
console.log("Pushed. If Netlify builds from main, the live cards update in about a minute.");
console.log("If you deploy by CLI instead: cd Demo && npx netlify deploy --prod");