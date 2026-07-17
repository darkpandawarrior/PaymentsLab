#!/usr/bin/env bash
# Regenerates ONLY the <!-- AUTOGEN:x --> spans in README.md from source-of-truth in code.
# Hand-written prose outside the markers is never touched. See the Mileway twin for notes.
set -euo pipefail
cd "$(dirname "$0")/.."

README="README.md"
SETTINGS="settings.gradle.kts"
SHOTS_DIR="docs/screenshots"

# grep -c exits 1 (not 0) when a pattern matches zero lines — under `set -e` that aborts the whole
# script. `|| true` keeps the "0" grep already prints on stdout while swallowing the non-zero exit,
# so a module class dropping to zero (e.g. the last :provider: include removed) no longer breaks it.

# --- local modules: `include(...)` in this repo's settings ---
local_total=$(grep -c '^include(' "$SETTINGS" || true)
local_features=$(grep -c '^include(":feature:' "$SETTINGS" || true)
local_cores=$(grep -c '^include(":core:' "$SETTINGS" || true)
local_providers=$(grep -c '^include(":provider:' "$SETTINGS" || true)
# whatever is left (app, iosApp, backend/server, …)
local_other=$(( local_total - local_features - local_cores - local_providers ))

# --- composed modules: substituted from includeBuild(external/kmp-toolkit) ---
# Each `substitute(module("com.siddharth.kmp:X")).using(project(...))` is one composed module.
# Those mapped to a `:provider:` project are payment gateways; the rest are shared core libraries.
composed_total=$(grep -cE 'substitute\(module\("com\.siddharth\.kmp:' "$SETTINGS" || true)
composed_providers=$(grep -cE 'using\(project\(":provider:' "$SETTINGS" || true)
composed_cores=$(( composed_total - composed_providers ))

grand_total=$(( local_total + composed_total ))
shots=$(find "$SHOTS_DIR" -maxdepth 1 -name '*.png' | wc -l | tr -d ' ')

badge="<!-- AUTOGEN:badge -->
![Modules](https://img.shields.io/badge/modules-${grand_total}-success)
<!-- /AUTOGEN:badge -->"

stats="<!-- AUTOGEN:stats -->
> **At a glance** — **${grand_total}-module** KMP architecture: **${local_total} local** (${local_cores} core · ${local_features} feature · ${local_other} app/iOS/backend) + **${composed_total} composed** via \`includeBuild(external/kmp-toolkit)\` (${composed_cores} shared core · ${composed_providers} payment-provider gateways), **${shots}** deterministic Roborazzi screenshots. *Numbers auto-generated from \`settings.gradle.kts\` by \`scripts/gen-readme.sh\`.*
<!-- /AUTOGEN:stats -->"

replace_block() {   # $1=tag  $2=replacement (marker lines included)
  TAG="$1" REPL="$2" perl -0777 -i -pe '
    s/<!-- AUTOGEN:\Q$ENV{TAG}\E -->.*?<!-- \/AUTOGEN:\Q$ENV{TAG}\E -->/$ENV{REPL}/s;
  ' "$README"
}

replace_block "badge" "$badge"
replace_block "stats" "$stats"
echo "[gen-readme] total=$grand_total (local=$local_total: ${local_cores}c/${local_features}f/${local_providers}p/${local_other}o + composed=$composed_total: ${composed_cores}c/${composed_providers}p) shots=$shots"
