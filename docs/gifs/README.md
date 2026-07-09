# Flow GIFs

Each GIF here walks one real multi-screen journey. Every frame is a **real rendered pixel** — a
deterministic JVM Roborazzi capture from [`docs/screenshots/`](../screenshots/) (Robolectric, no
emulator; see `ScreenshotCatalogTest`), stitched with ffmpeg. Nothing is mocked or hand-drawn.

| GIF | Journey | Frames |
|---|---|---|
| `explore_verify_flow.gif` | Home → provider catalog → provider lab running → server-verified settle | `home_screen_dashboard` → `lab_home_screen_catalog` → `provider_lab_screen_running` → `provider_lab_screen_settled_success` |
| `checkout_flow.gif` | Pick product & gateway → order summary → paying → settled | `checkout_screen_order_summary` → `checkout_screen_paying` → `checkout_screen_settled_success` |
| `activity_flow.gif` | Full transaction journal → filtered to successes | `history_screen_all` → `history_screen_with_filters` |

## Rebuild

1. Refresh the source frames (any Compose/theme change legitimately changes these):

   ```bash
   ./gradlew :app:recordRoborazziDebug
   ```

2. Re-stitch each flow with the reusable ffmpeg script
   ([`scripts/make-flow-gif.sh`](../../scripts/make-flow-gif.sh) — palettegen/paletteuse for clean
   color, ~1.6s per frame, a short crossfade):

   ```bash
   S=docs/screenshots
   WIDTH=360 bash scripts/make-flow-gif.sh docs/gifs/explore_verify_flow.gif \
     $S/home_screen_dashboard.png $S/lab_home_screen_catalog.png \
     $S/provider_lab_screen_running.png $S/provider_lab_screen_settled_success.png
   WIDTH=360 bash scripts/make-flow-gif.sh docs/gifs/checkout_flow.gif \
     $S/checkout_screen_order_summary.png $S/checkout_screen_paying.png $S/checkout_screen_settled_success.png
   WIDTH=360 bash scripts/make-flow-gif.sh docs/gifs/activity_flow.gif \
     $S/history_screen_all.png $S/history_screen_with_filters.png
   ```

## iOS

`../screenshots/ios_catalog*.png` are real `xcrun simctl io <device> screenshot` captures of
`ios/iosApp` running in an iPhone 17 Pro Simulator (iOS 26), built via
`xcodebuild -scheme iosApp -sdk iphonesimulator`. They're single frames — `simctl` has no touch
injection to drive a second screen state non-interactively, so a real iOS screenshot beats a
fabricated multi-frame sequence.
