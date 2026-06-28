# Demo media

Both assets here are built from **real, rendered pixels** — nothing hand-mocked or drawn.

## `android_flow.gif`

Stitched from three of the committed Roborazzi screenshots in
[`docs/screenshots/`](../screenshots/) (`lab_home_screen_catalog.png` →
`provider_lab_screen_running.png` → `provider_lab_screen_settled_success.png`), each one a real
Compose render captured on the JVM via Robolectric (see `ScreenshotCatalogTest`). No Android
emulator/device was available in the environment this was built in, so a live screen recording
wasn't possible — this is the honest substitute: real frames, not synthetic ones.

Rebuild after `./gradlew :app:recordRoborazziDebug` refreshes the source screenshots:

```bash
python3 - <<'EOF'
from PIL import Image

frames = [
    ("../screenshots/lab_home_screen_catalog.png", 1800),
    ("../screenshots/provider_lab_screen_running.png", 1500),
    ("../screenshots/provider_lab_screen_settled_success.png", 2200),
]
imgs, durations = [], []
for path, dur in frames:
    im = Image.open(path).convert("RGBA")
    bg = Image.new("RGBA", im.size, (255, 255, 255, 255))
    bg.paste(im, (0, 0), im)
    imgs.append(bg.convert("RGB"))
    durations.append(dur)

imgs[0].save("android_flow.gif", save_all=True, append_images=imgs[1:], duration=durations, loop=0, optimize=True)
EOF
```

## `../screenshots/ios_catalog.png`

A real `xcrun simctl io <device> screenshot` capture of `ios/iosApp` running in an iPhone 17 Pro
Simulator (iOS 26), built via `xcodebuild -scheme iosApp -sdk iphonesimulator`. Only one frame —
`simctl` has no touch/tap injection to drive a second screen state non-interactively, so a
multi-frame iOS GIF would need either a physical interaction pass in Xcode or UI-testing
automation, neither done here. A single real screenshot beats a fabricated multi-frame sequence.
