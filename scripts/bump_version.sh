#!/usr/bin/env bash
# Bumps the repo-root version source-of-truth files (VERSION / BUILD_NUMBER / MILESTONE).
# MARKETING/BUILDCODE/FINGERPRINT are never written to a file — they're always live-computed by
# scripts/version.sh from these files + git state, so there's nothing to keep in sync by hand.
#
# Usage:
#   scripts/bump_version.sh --milestone            # MILESTONE += 1 (cut a new release line)
#   scripts/bump_version.sh --version 1.2.0         # set VERSION (semver bookkeeping)
#   scripts/bump_version.sh --build-base 5           # set BUILD_NUMBER (versionCode base)
#   scripts/bump_version.sh                          # no-op bump, just print the current stamp
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

while [ $# -gt 0 ]; do
    case "$1" in
        --milestone)
            m="$(tr -d '[:space:]' < MILESTONE)"
            echo "$((m + 1))" > MILESTONE
            echo "MILESTONE -> $(cat MILESTONE)"
            shift
            ;;
        --version)
            [ $# -ge 2 ] || { echo "--version needs an argument" >&2; exit 1; }
            echo "$2" > VERSION
            echo "VERSION -> $(cat VERSION)"
            shift 2
            ;;
        --build-base)
            [ $# -ge 2 ] || { echo "--build-base needs an argument" >&2; exit 1; }
            echo "$2" > BUILD_NUMBER
            echo "BUILD_NUMBER -> $(cat BUILD_NUMBER)"
            shift 2
            ;;
        --commit)
            # Explicit no-op: commit count is live, nothing to write.
            shift
            ;;
        *)
            echo "Usage: $0 [--milestone] [--version X.Y.Z] [--build-base N] [--commit]" >&2
            exit 1
            ;;
    esac
done

echo
scripts/version.sh
