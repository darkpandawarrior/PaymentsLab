# Release runbook

## Versioning model (three-tier)

Source of truth = repo-root `VERSION` + `BUILD_NUMBER` + `MILESTONE` files, combined with live git
state. Everything else is *derived* by `scripts/version.sh` — nothing below is ever hand-typed in a
build file:

| Value | Formula | Used as |
|---|---|---|
| **MARKETING** | `YYYY.M.MILESTONE` (e.g. `2026.7.4`) | Android release `versionName`; iOS `CFBundleShortVersionString` (≤3 int components — App Store hard limit) |
| **BUILDCODE** | `BUILD_NUMBER (base) + commitCount` | Android `versionCode`; iOS `CFBundleVersion` |
| **FINGERPRINT** | `YYYY.0M.0W.MILESTONE.commitCount` (e.g. `2026.07.03.4.127`) | git tag (`v<FINGERPRINT>`), GitHub release title, `BuildConfig.FINGERPRINT`, debug `versionNameSuffix` |

`YYYY`/`M`/`0M` = year/month/zero-padded month, `0W` = zero-padded ISO week-of-year, `commitCount` =
`git rev-list --count HEAD`.

`scripts/version.sh` is the single implementation — `app/build.gradle.kts` calls it via
`providers.exec` for Android, the `ios-ipa` job in `.github/workflows/github-release.yml` calls it
to pass `MARKETING_VERSION`/`CURRENT_PROJECT_VERSION` overrides to `xcodebuild` (which flow into
`ios/iosApp/iosApp/Info.plist`'s `$(MARKETING_VERSION)`/`$(CURRENT_PROJECT_VERSION)` tokens), and
`fastlane/Fastfile`'s `current_version_code` calls it so the Play changelog filename always matches
the shipped `versionCode`.

Debug builds get `versionNameSuffix = "-<FINGERPRINT>"` so two CI builds from the same day are
distinguishable at a glance; release builds ship the bare MARKETING versionName.

## Cutting a release

```bash
# 1. Bump the milestone (a new release line) — or --version X.Y.Z for semver bookkeeping only,
#    or --build-base N to rebase versionCode. All optional; running with no flags just prints
#    the current stamp.
scripts/bump_version.sh --milestone

# 2. Commit the bump.
git add MILESTONE && git commit -m "chore(release): bump milestone"

# 3. Tag HEAD with the FINGERPRINT scripts/version.sh now reports (github-release.yml re-verifies
#    this exact match before building anything).
tag="v$(scripts/version.sh | sed -n 's/^FINGERPRINT=//p')"
git tag "$tag" && git push origin "$tag"
```

Pushing the tag triggers `.github/workflows/github-release.yml`: it verifies the tag matches the
live FINGERPRINT, builds every task in the `RELEASE_BUILD_CMD` repo variable (default
`assembleDebug assembleRelease` — PaymentsLab-KMP has no product flavors), attaches every APK produced
(named `PaymentsLab-KMP-<tag>-<variant>.apk`, `-unsigned` included if no keystore secret is
configured), zips `docs/screenshots` + diffs it against the previous release, creates/updates the
GitHub Release, then (macOS job, best-effort) archives an **unsigned** iOS `.ipa`-shaped artifact
gated on `ios/iosApp/*.xcodeproj` existing.

`.github/workflows/release.yml` ("Release verification build") is a separate, manual
(`workflow_dispatch`-only) sanity check — full gate + release APK + R8 mapping as workflow
artifacts, publishes nothing. It used to also auto-publish on the same `v*` tag push as
github-release.yml, which raced the two `gh`/`softprops` release-creation calls against each other;
it's manual-only now so there's exactly one tag-triggered publisher.

Store deployment (`play-deploy.yml`, `amazon-appstore-deploy.yml`, `aptoide-deploy.yml`,
`huawei-appgallery-deploy.yml`, `samsung-galaxy-store-deploy.yml`, `indus-deploy.yml`,
`fdroid-deploy.yml`) is separate again — each is gated on its own store-specific secret (e.g.
`AMAZON_APPSTORE_CLIENT_ID`) and mostly triggers on `release: published` (fired automatically once
github-release.yml creates the release) or manual `workflow_dispatch`. They all build the **release**
variant, never debug.

## Owner-only secrets

These are dormant until set (repo Settings → Secrets and variables → Actions). Every workflow that
needs them checks presence first and no-ops cleanly otherwise.

### Canonical Android signing secrets (used by every deploy workflow that signs)

| Secret | Purpose |
|---|---|
| `ANDROID_KEYSTORE_B64` | base64 of the upload/release `.jks` keystore |
| `ANDROID_KEYSTORE_PASSWORD` | keystore password |
| `ANDROID_KEY_ALIAS` | key alias inside the keystore |
| `ANDROID_KEY_PASSWORD` | key password |

(Previously these were unprefixed `KEYSTORE_B64`/`KEYSTORE_PASSWORD`/`KEY_ALIAS`/`KEY_PASSWORD` —
renamed to the `ANDROID_*` set across every deploy workflow for consistency; nothing else changed.)

Without a keystore, `assembleRelease` falls back to the debug signing key (see
`app/build.gradle.kts`'s `hasReleaseSigning`) — every store's `assembleRelease`/`bundleRelease` step
still produces an installable artifact, just not one the real stores will accept.

### Per-target secrets

| Workflow | Secrets |
|---|---|
| `play-deploy.yml` | `PLAYSTORE_CREDS_B64` (base64 Play service-account JSON) + the canonical Android signing set |
| `amazon-appstore-deploy.yml` | `AMAZON_APPSTORE_CLIENT_ID`, `AMAZON_APPSTORE_CLIENT_SECRET`, `AMAZON_APPSTORE_APP_ID` + signing set |
| `aptoide-deploy.yml` | `APTOIDE_API_KEY` + signing set |
| `huawei-appgallery-deploy.yml` | `HUAWEI_CLIENT_ID`, `HUAWEI_CLIENT_KEY`, `HUAWEI_APP_ID` + signing set |
| `samsung-galaxy-store-deploy.yml` | `SAMSUNG_ACCESS_TOKEN`, `SAMSUNG_SERVICE_ACCOUNT_ID`, `SAMSUNG_CONTENT_ID` + signing set |
| `indus-deploy.yml` | `INDUS_API_KEY`, `INDUS_APP_ID` (builds an unsigned AAB today — add the signing set to `bundleRelease` before relying on this one) |
| `fdroid-deploy.yml` | signing set only (manual `workflow_dispatch`, reproducible `-Pfdroid` build) |
| `github-release.yml` | none required — builds with whatever signing is available, ships `-unsigned` APKs otherwise. Optional: `RELEASE_SCRUBLIST_B64` (second-layer leak scrub for release notes) |
| iOS lanes (`fastlane/Fastfile` `ios` platform, if added) | `APPSTORE_AUTH_KEY_B64`, `ASC_KEY_ID`, `ASC_ISSUER_ID`, `MATCH_PASSWORD`, `MATCH_GIT_PRIVATE_KEY` |

`RELEASE_BUILD_CMD` and `IOS_SCHEME` are repo **variables** (not secrets, Settings → Variables), only
needed if the defaults (`assembleDebug assembleRelease`, first non-Tests scheme) don't fit.
