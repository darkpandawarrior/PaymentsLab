#!/usr/bin/env bash
# Single source of the three-tier version stamp (MARKETING / BUILDCODE / FINGERPRINT), computed from
# the repo-root VERSION + BUILD_NUMBER + MILESTONE files plus live git state. Shared by:
#   - app/build.gradle.kts (Android versionName/versionCode/BuildConfig.FINGERPRINT, via providers.exec)
#   - .github/workflows/github-release.yml's iOS job (MARKETING_VERSION/CURRENT_PROJECT_VERSION overrides)
#   - scripts/bump_version.sh (prints the result after a bump)
#
# MARKETING  = YYYY.M.MILESTONE            (<=3 int components — iOS CFBundleShortVersionString limit)
# BUILDCODE  = BUILD_NUMBER(base) + commitCount
# FINGERPRINT = YYYY.0M.0W.MILESTONE.commitCount
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

milestone="$(tr -d '[:space:]' < MILESTONE)"
build_number_base="$(tr -d '[:space:]' < BUILD_NUMBER)"
# A shallow clone reports 1 commit, so versionCode silently becomes 2. That is exactly what
# shipped: every APK published to F-Droid carried versionCode 2, so no client could ever offer
# an upgrade. actions/checkout defaults to fetch-depth 1. Refuse rather than guess.
commit_count="$(git rev-list --count HEAD)"
if [ "$(git rev-parse --is-shallow-repository)" = "true" ]; then
  echo "error: shallow clone. git rev-list reports $commit_count commits, so versionCode would" >&2
  echo "       be $((commit_count + 1)). Set \`fetch-depth: 0\` on actions/checkout." >&2
  exit 1
fi

year="$(date +%Y)"
month_padded="$(date +%m)"
month="$((10#$month_padded))"
week_padded="$(date +%V)"

marketing="${year}.${month}.${milestone}"
buildcode="$((build_number_base + commit_count))"
fingerprint="${year}.${month_padded}.${week_padded}.${milestone}.${commit_count}"

echo "MARKETING=${marketing}"
echo "BUILDCODE=${buildcode}"
echo "FINGERPRINT=${fingerprint}"
