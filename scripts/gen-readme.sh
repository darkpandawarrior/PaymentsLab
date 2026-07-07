#!/usr/bin/env bash
# Regenerates ONLY the <!-- AUTOGEN:x --> spans in README.md from source-of-truth in code.
# Hand-written prose outside the markers is never touched. See the Mileway twin for notes.
set -euo pipefail
cd "$(dirname "$0")/.."

README="README.md"
SETTINGS="settings.gradle.kts"
SHOTS_DIR="docs/screenshots"

modules=$(grep -c '^include(' "$SETTINGS")
providers=$(grep -c '^include(":provider:' "$SETTINGS")
features=$(grep -c '^include(":feature:' "$SETTINGS")
cores=$(grep -c '^include(":core:' "$SETTINGS")
shots=$(find "$SHOTS_DIR" -maxdepth 1 -name '*.png' | wc -l | tr -d ' ')

badge="<!-- AUTOGEN:badge -->
![Modules](https://img.shields.io/badge/modules-${modules}-success)
<!-- /AUTOGEN:badge -->"

stats="<!-- AUTOGEN:stats -->
> **At a glance** — **${modules}-module** KMP architecture (${providers} provider · ${features} feature · ${cores} core), **${shots}** deterministic Roborazzi screenshots. *Numbers auto-generated from \`settings.gradle.kts\` by \`scripts/gen-readme.sh\`.*
<!-- /AUTOGEN:stats -->"

replace_block() {   # $1=tag  $2=replacement (marker lines included)
  TAG="$1" REPL="$2" perl -0777 -i -pe '
    s/<!-- AUTOGEN:\Q$ENV{TAG}\E -->.*?<!-- \/AUTOGEN:\Q$ENV{TAG}\E -->/$ENV{REPL}/s;
  ' "$README"
}

replace_block "badge" "$badge"
replace_block "stats" "$stats"
echo "[gen-readme] modules=$modules providers=$providers features=$features cores=$cores shots=$shots"
