// Test harness for the in-app updater. Serves a fake "newer release" so the
// update banner and download path can be exercised without publishing anything.
//
//   node tools/mock-update.mjs
//
// A debug build reads http://10.0.2.2:8787/version.json (10.0.2.2 is the host
// from an Android emulator). Point the phone at the host LAN IP instead if
// testing on a real device, and change the debug URL in AppUpdater.kt.
import { createServer } from "node:http";
import { readFileSync, existsSync } from "node:fs";
import { createHash } from "node:crypto";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const apkPath = process.argv[2] ||
  join(here, "../app/build/outputs/apk/release/app-release.apk");

if (!existsSync(apkPath)) {
  console.error(`APK not found: ${apkPath}`);
  process.exit(1);
}

const bytes = readFileSync(apkPath);
const sha256 = createHash("sha256").update(bytes).digest("hex");
// Pretend to be version 99 so any real install is "older".
const manifest = {
  versionCode: 99,
  versionName: "9.9",
  notes: "Test update — this is a local mock, not a real release.",
  url: "/bpsc-99.apk",
  sha256,
};

createServer((req, res) => {
  if (req.url === "/version.json") {
    res.writeHead(200, { "Content-Type": "application/json", "Cache-Control": "no-cache" });
    res.end(JSON.stringify(manifest, null, 2));
    console.log("served version.json");
  } else if (req.url === "/bpsc-99.apk") {
    res.writeHead(200, {
      "Content-Type": "application/vnd.android.package-archive",
      "Content-Length": bytes.length,
    });
    res.end(bytes);
    console.log(`served APK (${(bytes.length / 1048576).toFixed(1)} MB)`);
  } else {
    res.writeHead(404).end();
  }
}).listen(8787, () => {
  console.log(`mock update server on http://localhost:8787`);
  console.log(`serving version 9.9 (code 99), apk ${(bytes.length / 1048576).toFixed(1)} MB`);
});