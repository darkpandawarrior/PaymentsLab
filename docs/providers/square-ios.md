# Square on iOS

- **Region:** Global
- **Archetype:** A (native SDK — `SQIPCardEntryViewController`, real `SquareInAppPaymentsSDK` `1.6.7`
  via CocoaPods)
- **Status shipped:** `MOCK_MODE` (same auto-degrade honesty as Android — no live sandbox
  credentials were available this session either)
- **Docs:** https://developer.squareup.com/docs/in-app-payments-sdk/what-it-does

## The one gateway that needed CocoaPods, not SPM

Square's iOS SDK (`SquareInAppPaymentsSDK`) has **no Swift Package Manager distribution** —
confirmed by checking every Square iOS SDK repo for a `Package.swift` and finding none. It's
CocoaPods-only, latest `1.6.7` on CocoaPods trunk.

CocoaPods needs Ruby ≥3.0; this machine's system Ruby was `2.6.10`, and installing CocoaPods
against it failed outright:

```
ERROR:  Error installing cocoapods:
    The last version of ffi (>= 1.15.0) to support your Ruby & RubyGems was 1.17.4.
    ffi requires Ruby version >= 3.0, < 4.1.dev. The current ruby version is 2.6.10.210.
```

Fixed by installing a second, newer Ruby via Homebrew (`brew install ruby` → Ruby 4.0.5, installed
to `/opt/homebrew/opt/ruby` — additive, doesn't touch or replace `/usr/bin/ruby`), then
`gem install cocoapods` against that Ruby. One more real snag: `pod install` crashed on a Unicode
normalization error until `LANG=en_US.UTF-8`/`LC_ALL=en_US.UTF-8` were set (CocoaPods' own install
output warns about exactly this, easy to miss).

**Practical consequence: `ios/iosApp` now needs `iosApp.xcworkspace`, not `iosApp.xcodeproj`.**
CocoaPods generates a `Pods.xcodeproj` and a workspace tying it together with the app project —
from this gateway onward, build with:

```bash
cd ios/iosApp
PATH="/opt/homebrew/opt/ruby/bin:/opt/homebrew/lib/ruby/gems/4.0.0/bin:$PATH" pod install
xcodebuild -workspace iosApp.xcworkspace -scheme iosApp -sdk iphonesimulator build
```

## Architecture

Same Swift-implements-Kotlin-interface boundary as the other four. `SquareCheckoutHostImpl.swift`
implements `SquareCheckoutHost` against the real SDK — and unlike Omise, Square's iOS SDK ships a
genuine ready-made card-entry UI (`SQIPCardEntryViewController`), the same UX story as Android's
`CardEntry` activity:

```swift
SQIPInAppPaymentsSDK.squareApplicationID = applicationId
let cardEntryViewController = SQIPCardEntryViewController(theme: theme)
cardEntryViewController.delegate = self
// delegate: cardEntryViewController(_:didObtain:completionHandler:) hands back cardDetails.nonce
```

## Verified

Built via `xcodebuild -workspace` (not `-project` — see above), CocoaPods resolved the real SDK,
installed + launched alongside Stripe/Razorpay/Cashfree/Omise in an iPhone 17 Pro Simulator with no
crash — Square shows in the catalog, `Global` region count updated from 2 to 3
(`docs/screenshots/ios_catalog_all_native.png`). Not verified: actually tapping through to open
card entry and submit a real card — no touch-injection tool available for iOS Simulator in this
environment.
