#!/usr/bin/env node
// Publish a new app release so installed phones can update themselves.
//
//   1. builds a signed release APK
//   2. copies it into the Demo repo as apps/web/public/app/bpsc-<code>.apk
//   3. writes apps/web/public/app/version.json from data/AppVersion.kt
//   4. commits ONLY those files, pushes, and Netlify deploys
//
//   node tools/publish-app.mjs            build + publish + push
//   node tools/publish-app.mjs --no-push  build + publish, no push
//   node tools/publish-app.mjs --dry-run  print everything, change nothing
//
// Before the first run, bump CODE/NAME in
// app/src/main/java/com/bpsc/flashcards/data/AppVersion.kt
import { execSync } from "node:child_process";
import { createHash } from "node:crypto";
import {
  existsSync,
  mkdirSync,
  readFileSync,
  copyFileSync,
  writeFileSync,
  readdirSync,
  unlinkSync,
} from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const appDir = join(here, "..");
const demoDir = join(appDir, "..", "..", "Demo");
const args = process.argv.slice(2);
const dryRun = args.includes("--dry-run");
const noPush = args.includes("--no-push");

const run = (cmd, opts = {}) => {
  console.log("+", cmd);
  if (!dryRun) execSync(cmd, { stdio: "inherit", ...opts });
};

// --- read the version the app will report -----------------------------------
const versionSrc = readFileSync(
  join(appDir, "app/src/main/java/com/bpsc/flashcards/data/AppVersion.kt"),
  "utf8",
);
const versionCode = Number(/CODE:\s*Int\s*=\s*(\d+)/.exec(versionSrc)?.[1]);
const versionName = /NAME:\s*String\s*=\s*"([^"]+)"/.exec(versionSrc)?.[1];
if (!versionCode || !versionName) {
  console.error("Could not read version from data/AppVersion.kt");
  process.exit(1);
}
console.log(`Publishing version ${versionName} (code ${versionCode})`);

// --- build the signed release APK -------------------------------------------
run("gradlew.bat assembleRelease", { cwd: appDir, shell: process.platform === "win32" });

const apk = join(appDir, "app/build/outputs/apk/release/app-release.apk");
if (!existsSync(apk)) {
  console.error(`Release APK not found at ${apk}`);
  process.exit(1);
}

const bytes = readFileSync(apk);
const sha256 = createHash("sha256").update(bytes).digest("hex");
const sizeMb = (bytes.length / (1024 * 1024)).toFixed(1);
console.log(`APK ${sizeMb} MB  sha256 ${sha256}`);

// --- stage into the web app so Netlify serves it ----------------------------
const webAppDir = join(demoDir, "apps/web/public/app");
const notes = process.env.RELEASE_NOTES?.trim() || `Version ${versionName}`;
const versionJson = {
  versionCode,
  versionName,
  notes,
  url: `/app/bpsc-${versionCode}.apk`,
  sha256,
};

if (!existsSync(demoDir)) {
  console.error(`Demo repo not found at ${demoDir}`);
  process.exit(1);
}
if (!dryRun) {
  mkdirSync(webAppDir, { recursive: true });
  copyFileSync(apk, join(webAppDir, `bpsc-${versionCode}.apk`));
  writeFileSync(join(webAppDir, "version.json"), JSON.stringify(versionJson, null, 2) + "\n");
}

// Keep only the newest few APKs; old ones are dead weight on the CDN.
const old = readdirSync(webAppDir)
  .filter((f) => /^bpsc-\d+\.apk$/.test(f))
  .map((f) => ({ f, code: Number(/bpsc-(\d+)\.apk/.exec(f)[1]) }))
  .filter((x) => x.code !== versionCode)
  .sort((a, b) => b.code - a.code)
  .slice(3);
for (const { f, code } of old) {
  console.log(`removing old bpsc-${code}.apk`);
  if (!dryRun) unlinkSync(join(webAppDir, f));
}

// --- commit only the files this script owns ---------------------------------
const owned = "apps/web/public/app";
run(`git -C "${demoDir}" add -- ${owned}`);

const staged = dryRun
  ? owned
  : execSync(`git -C "${demoDir}" diff --cached --name-only`, { encoding: "utf8" }).trim();
if (!staged) {
  console.log("No release changes to publish.");
  process.exit(0);
}
console.log("staged:\n" + staged);

run(`git -C "${demoDir}" commit -m "release: app ${versionName} (${versionCode})"`);

if (noPush) {
  console.log(dryRun ? "dry-run finished — nothing changed." : "Committed. Push any time with: git push");
  process.exit(0);
}

run(`git -C "${demoDir}" push`);
console.log("Pushed. Netlify will serve the new APK and version.json in about a minute.");
console.log("Installed phones will show the update banner on their next launch.");