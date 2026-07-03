# Square on iOS — blocked, documented honestly

- **Status:** Not built. Real, verified blocker — not skipped silently.

## Why not built

Square's In-App Payments SDK for iOS is distributed via **CocoaPods only**
(`SquareInAppPaymentsSDK` on CocoaPods trunk, latest `1.6.7`) — unlike Stripe, Razorpay, Cashfree,
and Omise, it has no Swift Package Manager distribution (confirmed: no `Package.swift` in any
Square iOS SDK repo, no SPM-compatible package found).

CocoaPods requires Ruby ≥ 3.0 (its `ffi` native-extension dependency dropped support for older
Rubies). This machine's system Ruby is `2.6.10` — installing CocoaPods here failed with:

```
ERROR:  Error installing cocoapods:
    The last version of ffi (>= 1.15.0) to support your Ruby & RubyGems was 1.17.4.
    ffi requires Ruby version >= 3.0, < 4.1.dev. The current ruby version is 2.6.10.210.
```

Unblocking this means installing a newer Ruby (via Homebrew/rbenv/asdf) — a change to the machine's
dev toolchain beyond this repo, not something to do unilaterally mid-task.

## What building it would look like (for next time)

Same pattern as the other four: a `SquareCheckoutHost` Kotlin interface in `ios/shared`, a
`SquareIosGateway` mirroring Android's `SquareGateway` contract, and a Swift
`SquareCheckoutHostImpl` implementing it against the real `SquareInAppPaymentsSDK` CocoaPods
CardEntry flow (`SQIPCardEntry.startCardEntryFlow(from:theme:)` + `SQIPCardEntryDelegate`). The only
new mechanics vs. the SPM-based integrations: a `Podfile` alongside `iosApp.xcodeproj`, running
`pod install` (which generates `iosApp.xcworkspace` — the workspace becomes the build entry point
instead of the `.xcodeproj` directly), and switching `xcodebuild -project` to `xcodebuild -workspace`
in the build command.
