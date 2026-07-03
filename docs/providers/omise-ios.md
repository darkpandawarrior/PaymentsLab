# Omise on iOS

- **Region:** SEA
- **Archetype:** A (native SDK — `OmiseSDK`, real `omise-ios` `5.6.3` via SPM)
- **Status shipped:** `MOCK_MODE` (same auto-degrade honesty as Android — no live sandbox
  credentials were available this session either)
- **Docs:** https://github.com/omise/omise-ios

## Architecture

Same Swift-implements-Kotlin-interface boundary as Stripe. Unlike Android's `CreditCardActivity`,
**Omise's iOS SDK ships no ready-made card-entry UI** — its public API is manual tokenization only:

```swift
let omiseSDK = OmiseSDK(publicKey: publicKey)
let payload = CreateTokenPayload(card: CreateTokenPayload.Card(name:number:expirationMonth:expirationYear:securityCode:))
omiseSDK.client.createToken(payload: payload) { result in /* .success(Token) / .failure(Error) */ }
```

`OmiseCheckoutHostImpl.swift` presents a small SwiftUI card form of this app's own (not
vendor-provided) and calls the real SDK for the actual tokenization request when the user taps Pay
— the card-entry chrome is custom, the tokenization call is real. `omise-ios`'s `Package.swift`
declares a transitive dependency on `omise-ios-3ds` (pinned `2.4.0` by Omise, resolved automatically
by SPM) for 3DS challenge support.

## Verified

Built via `xcodebuild`, SPM resolved the real SDK and its 3DS transitive dependency, installed +
launched alongside Stripe/Razorpay/Cashfree with no crash. Not verified: actually submitting a card
through the custom form and confirming a real token comes back — no touch-injection tool available
for iOS Simulator in this environment, and mock mode means the client never reaches this code path
without live Omise credentials anyway (matches Android's `OmiseGateway`).
