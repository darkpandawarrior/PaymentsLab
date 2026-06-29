<div align="center">

# PaymentsLab

### An Integration Lab for the Android payments ecosystem — every gateway behind one abstraction, with a live look at what actually happens on each transaction.

[![CI](https://github.com/darkpandawarrior/PaymentsLab/actions/workflows/ci.yml/badge.svg)](https://github.com/darkpandawarrior/PaymentsLab/actions/workflows/ci.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-7F52FF?logo=kotlin&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose%20MP-1.11.1-4285F4?logo=jetpackcompose&logoColor=white)
![Ktor](https://img.shields.io/badge/Ktor-3.5.1-087CFA?logo=ktor&logoColor=white)
![Modules](https://img.shields.io/badge/modules-19-success)

</div>

---

## Why PaymentsLab

Payments is the hardest integration surface on Android: every gateway ships a different SDK, most of
them are Activity-callback-era, the client can lie about the outcome, and the interesting logic
(signatures, webhooks, idempotency, recovery) lives on the server. That makes it a perfect subject
for a **systems** showcase rather than a UI one.

PaymentsLab runs — and **step-by-step visualizes** — real payment flows across multiple providers,
all behind a single `PaymentGateway` abstraction, backed by a small Ktor server that does the order
creation, signature verification and webhook reconciliation a real integration requires.

I built it to be three things at once:

1. **An architecture showcase** — a full Kotlin Multiplatform, multi-module design where each
   provider is an isolated, swappable Gradle module, the domain core is unit-tested against fakes,
   and the messy SDK reality is confined behind a coroutine-friendly host.
2. **A living catalog** — every provider the app touches is documented: what integrating it takes,
   whether a solo developer can demo it in sandbox, and the gotchas.
3. **A teaching reference** — the Lab renders each payment's lifecycle as a live timeline, showing
   the actual (redacted) payloads at every hop, so the *why* of payment architecture is visible.

## The one idea worth stealing

> **A client-side `Success` is a hint, never proof.** Only the server — after signature verification
> and webhook reconciliation — decides the true state.

Everything in the design flows from that: a server that owns price and truth, a client that always
confirms before trusting, a **journal written to Room *before* the SDK launches** so a process death
mid-payment is always recoverable, and a **redaction layer** so no secret or PII ever renders or logs.

## Highlights

- 🧩 **19-module KMP architecture.** One Gradle module per provider, contributed into a registry via
  Koin `getAll<PaymentGateway>()` — adding gateway *N+1* touches no existing code. Feature modules
  never depend on each other; they meet only at the `:app` composition root.
- 🔌 **One contract, four real SDKs — plus a generic hosted-webview archetype.** Razorpay, Cashfree,
  Stripe (+ Google Pay) and a raw UPI intent flow all implement the same tiny `PaymentGateway`
  interface. The Activity-callback SDKs are bridged into suspending coroutines by a `PaymentHost`
  that never leaks an `Activity` upward. `provider:hosted-webview` covers the whole class of
  gateways with no native SDK (redirect-and-return-URL checkout, e.g. MoMo-style flows) behind the
  same contract.
- 🪪 **Env-backed credentials that auto-degrade honestly.** `core:config` resolves each gateway's
  sandbox keys from `PLAB_<GATEWAY>_<MODE>_<KEY>` env vars; a gateway with no resolved credentials
  auto-degrades from `SANDBOX_READY` to `MOCK_MODE` instead of silently pretending to work — the Lab
  stays demoable and honest with zero real credentials configured.
- 🛡️ **Server is the source of truth.** A companion Ktor server creates orders (**price resolved
  server-side**), verifies payments with **real HMAC-SHA256** (Razorpay), and reconciles idempotent,
  signature-checked webhooks — the client callback is only ever treated as a hint.
- ⏱️ **Process-death recovery, for real.** Every in-flight payment is journaled to Room *before* the
  SDK opens; on cold start the orchestrator finds unresolved payments and reconciles them.
- 🔎 **Redaction by default.** A single choke point masks any secret/PII-shaped field before it can
  be rendered in the Lab timeline or written to a log.
- 🔐 **VAPT-grade security suite.** `core:security` — real Android Keystore AES-256-GCM at-rest
  encryption, `FLAG_SECURE` on payment screens (blocks screenshots/recording), device-integrity
  checks (root/Magisk, emulator, debugger), and a certificate-pinning config.
- ♻️ **Pure, replayable state machine.** The lifecycle is a pure `(State, Event) -> Effects` reducer
  (zero coroutines/DI/IO); the orchestrator just executes its effects. A payment's path is a
  recorded event log that replays byte-for-byte identically — the auditing property money movement
  wants.
- 🧪 **A real quality gate.** ktlint + detekt across all 19 modules (including KMP `commonMain`),
  fake-based unit tests for the orchestrator/ViewModels/backend, run on every push via GitHub Actions.

## Provider status matrix

The whole app is honest about what a solo developer can actually run without a business account.

| Provider | Category | Region | Sandbox (no KYC)? | In v1 | Notes |
|---|---|---|:--:|:--:|---|
| **Razorpay** | Gateway | India | ✅ | ✅ | Drop-in checkout; real HMAC-SHA256 signature verification |
| **Cashfree** | Gateway | India | ✅ | ✅ | Nextgen SDK; sandbox UPI simulator |
| **UPI intent** | Platform flow | India | ✅ | ✅ | Raw `upi://` intent, no SDK; client response is *unverifiable* by design |
| **Stripe** | Gateway | Global | ✅ | ✅ | PaymentSheet; 3DS2; Google Pay rides Stripe as the gateway |
| **Google Pay** | Method | Global | ✅ | ✅ | `ENVIRONMENT_TEST`, processed via Stripe |
| PhonePe PG | Gateway | India | ❌ | 📄 | MID needs business KYC — documented only |
| Juspay HyperSDK | Orchestrator | India | ❌ | 📄 | Credentials are B2B-issued — documented only |
| PayU / Paytm | Gateway | India | ⚠️ | 📄 | Later milestone / documented |
| Play Billing | IAP | — | ⚠️ | 🔜 | Needs a Play Console listing — later milestone |

✅ runnable in sandbox · 📄 catalog + docs only (gated) · 🔜 planned

## Screens & flows

Two modes, one engine:

- **Integration Lab** — the home screen catalogs every provider with a status badge. Tapping one
  opens a lab that executes the full lifecycle and renders it as a **live timeline**: order created →
  SDK launched → client result → server verification → settled, each step expandable to its actual
  redacted payload. This is the teaching surface — the sequence, and the fact that a client `Success`
  still has to pass through *Verifying* before it's trusted, is the whole point.
- **Demo Checkout** — a product checkout (cart → pay) that reuses the exact same provider registry
  and orchestrator, with an inline mini-timeline so the "normal" flow is explained as it runs.
- **History** — the transaction log, streamed from the Room journal.

Motion is deliberate, not decorative: a shimmer on the `MOCK_MODE` badge, `SuccessBurst`/
`FailureShake` terminal feedback, a scramble-to-mask `RedactionReveal`, and an animated
`PaymentFlowDiagram` that visualizes the client-success-is-not-server-truth trust boundary as a
moving packet. Every animated component reads `LocalReducedMotion`, so system-level reduce-motion
is honored everywhere, not bolted onto one screen.

The Lab timeline — the app's centerpiece — rendered as a deterministic Roborazzi screenshot:

<div align="center">
<img src="docs/screenshots/step_timeline_light.png" alt="Payment lifecycle timeline" width="320" />
</div>

> Screenshots are generated on the JVM (Robolectric, no emulator) and committed to
> [`docs/screenshots/`](docs/screenshots/) — see `ScreenshotCatalogTest`. Refresh with
> `./gradlew :app:recordRoborazziDebug`.

Catalog → provider lab → settled, stitched from the same committed Roborazzi frames (no emulator
was used to record this — see [`docs/demo/`](docs/demo/) for how it's built):

<div align="center">
<img src="docs/demo/android_flow.gif" alt="Catalog to settled payment flow" width="320" />
</div>

### Also runs on iOS

`ios/shared` packages the KMP-safe surface (archetype C hosted-webview + archetype D mobile-money)
into a real `.framework`, consumed by a genuine Xcode project at `ios/iosApp/`. The screenshot below
is a real iOS Simulator run of the same Compose UI, not a mockup:

<div align="center">
<img src="docs/screenshots/ios_catalog.png" alt="The same catalog UI running on iOS" width="320" />
</div>

**Native-SDK gateways aren't Android-only either — most of them ship real iOS SDKs.** All five —
Stripe, Razorpay, Cashfree, Omise, and Square — are built as real native-SDK integrations on iOS,
the same pattern each time: a small Kotlin interface Kotlin/Native exports as a plain Objective-C
protocol, implemented in Swift against the vendor's real SDK (Kotlin/Native can't cinterop against
a Swift-only framework directly, so the direction has to run this way). Google Pay has no iOS
equivalent at all — Apple Pay is a separate Apple product, not a Google Pay port.

| Gateway | iOS SDK | Docs |
|---|---|---|
| Stripe | `StripePaymentSheet` (SPM, `26.1.0`) | [stripe-ios.md](docs/providers/stripe-ios.md) |
| Razorpay | `RazorpayCheckout` (SPM, `razorpay-pod` `1.5.4`) | [razorpay-ios.md](docs/providers/razorpay-ios.md) |
| Cashfree | `CashfreePGUISDK` Drop Checkout (SPM, `core-ios-sdk`) | [cashfree-ios.md](docs/providers/cashfree-ios.md) |
| Omise | `OmiseSDK` manual tokenization (SPM, `5.6.3`) | [omise-ios.md](docs/providers/omise-ios.md) |
| Square | `SQIPCardEntryViewController` (CocoaPods, `1.6.7` — no SPM distribution exists) | [square-ios.md](docs/providers/square-ios.md) |

<div align="center">
<img src="docs/screenshots/ios_catalog_all_native.png" alt="Stripe, Razorpay, Cashfree, Omise and Square all showing in the iOS catalog, real SDKs linked" width="320" />
</div>

Build it yourself — **Square's CocoaPods dependency means the workspace, not the bare project, is
now the entry point**:

```bash
cd ios/iosApp
pod install   # needs Ruby >=3.0 for CocoaPods; see docs/providers/square-ios.md if your system Ruby is older
xcodebuild -workspace iosApp.xcworkspace -scheme iosApp -sdk iphonesimulator build
```

The `EmbedKotlinFramework` build phase runs the Gradle framework build automatically; the first
`xcodebuild` also resolves the four SPM-based SDKs.

## Architecture

The heart of the design is a deliberately tiny, platform-agnostic contract, with all the messy
platform reality pushed to the edges.

```kotlin
interface PaymentGateway {
    val id: GatewayId
    val meta: GatewayMeta
    suspend fun prepare(created: CreatedOrder): PreparedPayment
    suspend fun pay(host: PaymentHost, prepared: PreparedPayment): PaymentResult
}
```

- **`PaymentHost`** is opaque in `commonMain` — it carries no platform types, so the contract stays
  multiplatform. On Android the concrete host owns the `ComponentActivity` + ActivityResult plumbing
  and bridges each SDK's callback back into the suspending `pay()` call. This indirection is what lets
  Compose-era, coroutine-based feature code drive Activity-callback-era gateway SDKs without leaking
  Activity references upward.
- **`PaymentOrchestrator`** (in `core:orchestration`) coordinates one payment across four
  collaborators — the registry, the backend (server truth), the gateway SDK (client hint) and the
  journal (crash insurance) — and emits a `Flow<PaymentStep>` the Lab renders as a timeline. Because
  every collaborator is an interface, the whole flow is exercised in `commonTest` with fakes: no
  Android, no network, no real SDK.
- **The backend** mirrors the same plugin shape: thin Ktor routes over per-provider `GatewayAdapter`s.

### Module map

```
app/                       Android composition root — Koin wiring, navigation, AndroidPaymentHost
backend/                   Ktor JVM server — orders, verify, webhooks, status (server = truth)
core/
  payments-api/            The frozen contract: PaymentGateway, PaymentResult, PaymentHost,
                           PaymentBackend, PendingPaymentJournal, PaymentStep, Redactor   (KMP + jvm)
  config/                  Env-backed credential resolution (PLAB_* keys), MOCK_MODE auto-degrade  (KMP)
  protocol/                @Serializable wire DTOs shared with the backend's JVM target    (KMP + jvm)
  orchestration/           the effectful shell + fsm/ — a PURE (State,Event)->Effects reducer it
                           drives; the tested heart (journal-first, server-as-truth, replayable)
  network/                 Ktor client implementing PaymentBackend
  data/                    Room KMP journal (process-death recovery)
  designsystem/            Compose Multiplatform theme, tokens, StepTimeline, Motion Kit
                           (shimmer, terminal feedback, flow diagram), PayloadCard
  security/                Keystore AES-256-GCM store, FLAG_SECURE, device-integrity, pinning config
  common/                  UiText, KMP logging, initKoin()/platformModule() (iOS-ready DI entry point)
provider/
  razorpay/  cashfree/  upi-intent/  stripe/    one module per gateway, behind the contract
  hosted-webview/          generic archetype for SDK-less, redirect-and-return-URL gateways
feature/
  lab/                     catalog home + live per-provider lab timeline
  checkout-demo/           product checkout that reuses the same registry (the "explained" mode)
  history/                 transaction log from the Room journal
app/ .../work/             PaymentReconciliationWorker — WorkManager process-death reconciliation
build-logic/               convention plugins (kmp.library / kmp.compose / cmp.feature / …)
```

### Data flow (one payment)

1. UI → `PaymentOrchestrator.pay(host, gatewayId, catalogItemId)`.
2. Orchestrator → backend `POST /orders`. **Journal-first:** a pending row is written to Room
   *before* the SDK launches — the process-death insurance.
3. `gateway.prepare()` → `gateway.pay(host, …)` → the SDK UI / UPI chooser.
4. The callback is mapped to a `PaymentResult`. **The client result is a hint, never truth.**
5. Orchestrator → backend `verify` (signature check) and/or polls `GET /payments/{id}` with backoff.
   Server state — updated by webhooks — is the single source of truth.
6. Terminal state → journal row resolved → history; the Lab timeline renders every step + payload.

## Tech stack

| Layer | Technology |
|---|---|
| Language | Kotlin **2.4.0** (K2) |
| UI | Compose Multiplatform **1.11.1**, Material 3 |
| Build | AGP **9.2.1**, Gradle Kotlin DSL, convention plugins, version catalog |
| DI | Koin **4.2.2** (multiplatform) |
| Client networking | Ktor **3.5.1** client (OkHttp + Darwin engines) + kotlinx-serialization |
| Backend | Ktor **3.5.1** server (Netty), in-memory store (swappable for Exposed/SQLite) |
| Database | Room **2.8.4** (KMP, bundled SQLite) — the pending-payment journal |
| Concurrency | Coroutines + Flow (no LiveData); `kotlinx-datetime`, immutable collections |
| Provider SDKs | Razorpay Checkout, Cashfree PG (api + ui), Stripe PaymentSheet, Play Services Wallet |
| Security | Android Keystore (AES-256-GCM), FLAG_SECURE, device-integrity, OkHttp CertificatePinner |
| Background | WorkManager (payment reconciliation), Koin-backed WorkerFactory |
| Testing | JUnit, MockK, Turbine, kotlinx-coroutines-test, Koin-Test, **Roborazzi** screenshots; fake-first |
| Quality | detekt **1.23.8**, ktlint, **dependency-guard**, **Kover** coverage floor, Compose stability config |
| Shrinking | **R8** full-mode (release minify + resource shrink; APK 42M→13M), payment-SDK keep rules, mapping.txt |
| Observability | `CrashReporter` abstraction (Napier default; Crashlytics/Sentry = one-line DI swap) |
| Variants | `debug` / `release` / **`vapt`** (flips security bypass flags); per-env `BACKEND_URL` |
| Release | Fastlane (versioning + build lanes), `release.yml` (tag → GitHub Release + R8 mapping) |
| Targets | Android (compileSdk **37**, minSdk **24**) + iOS-ready KMP core; **JDK 21** |

## Getting started

> **JDK 21 is required.** detekt 1.23.x can't run on JDK 23+, so the whole build is pinned to 21.
> If your default `java` is newer, install one (`brew install openjdk@21`) and either point
> `org.gradle.java.home` in `gradle.properties` at it (as committed) or run with
> `JAVA_HOME=$(…/jdk-21) ./gradlew`.

```bash
git clone https://github.com/darkpandawarrior/PaymentsLab.git
cd PaymentsLab

# 1. Start the backend (in-memory store; sandbox/test adapters) — serves on :8080
./gradlew :backend:run

# 2. Build & install the app (points at 10.0.2.2:8080 from the emulator)
./gradlew :app:installDebug
```

Sandbox credentials are read from environment variables (see `backend/.env.example`); the app only
ever ships publishable keys, never secrets. Real end-to-end payment execution needs a device plus
each provider's sandbox keys — the SDK integrations compile and the flows are fully wired regardless.

Every gateway below the original three (Razorpay/Stripe/Cashfree) follows one convention —
`PLAB_<GATEWAY>_<MODE>_<KEY>` — resolved by `core:config`'s `EnvCredentialStore`. Unset → the
gateway auto-degrades to `MOCK_MODE`; set → it upgrades to real, no code change either way.
`backend/.env.example` predates the Tier-1 real-SDK batch, so the current full set is:

| Gateway | Vars | Self-serve sandbox? |
|---|---|---|
| Paystack | `PLAB_PAYSTACK_TEST_SECRET_KEY` | Yes — paystack.com |
| PayPal | `PLAB_PAYPAL_TEST_CLIENT_ID`, `_CLIENT_SECRET` | Yes — developer.paypal.com |
| Square | `PLAB_SQUARE_TEST_APPLICATION_ID`, `_ACCESS_TOKEN`, `_LOCATION_ID` | Yes — developer.squareup.com/apps |
| Omise | `PLAB_OMISE_TEST_PUBLIC_KEY`, `_SECRET_KEY` | Yes — dashboard.omise.co/signup |

<details>
<summary><b>All build &amp; tooling commands</b></summary>

```bash
# The full local + CI gate: ktlint + detekt (all modules) + every unit test
./gradlew fastGate

# Build the debug APK
./gradlew :app:assembleDebug

# Individual test suites
./gradlew :backend:test                          # Ktor testApplication (real HMAC, webhook idempotency)
./gradlew :core:orchestration:testAndroidHostTest # the tested heart, against fakes
./gradlew :provider:razorpay:testDebugUnitTest    # SDK-callback → PaymentResult mapping

# Static analysis only
./gradlew ktlintCheck detekt
```

</details>

## Testing & quality

- **The tested heart.** `PaymentOrchestrator` is covered end-to-end in `commonTest` with fakes —
  happy path timeline, journal-before-launch ordering, server-overrides-client-success, pending→poll,
  cancellation, and cold-start recovery. No Android, no network, no real SDK.
- **Fakes, not mocks.** ViewModels and the orchestrator are tested against fake repositories/gateways
  that implement the real interfaces; mocks are reserved for external services.
- **Backend.** Ktor `testApplication` asserts server-authoritative pricing, a **real** Razorpay
  HMAC-SHA256 pass/fail, idempotent webhook dedup, and 400s on unknown item/gateway.
- **Static analysis.** ktlint and detekt run across every module — detekt is pointed at the KMP
  `commonMain`/`androidMain` source sets, not just `src/main`, so the core and features are actually
  analyzed.
- **CI.** `.github/workflows/ci.yml` provisions JDK 21 and runs `fastGate` + `:app:assembleDebug` on
  every push and PR.

## Security posture

- Secret keys live only in backend environment variables; the APK ships publishable keys only.
  `backend/.env.example` documents every credential; nothing sensitive is hardcoded.
- Client success callbacks are never trusted — the server verifies, and webhooks reconcile.
- Webhook signatures are verified before processing, and handlers are idempotent (event-id dedup).
- Idempotency keys on order creation; the app retries the *status check*, never the charge.
- The `Redactor` allowlist masks any secret/PII-shaped field before it is rendered or logged.
- **At-rest:** `core:security` stores saved tokens with Android Keystore AES-256-GCM (non-exportable
  key, TEE/StrongBox-backed). **On-screen:** payment routes are `FLAG_SECURE`. **Device:** launch-time
  root/emulator/debugger inspection. **Transport:** a certificate-pinning config (placeholder pins
  here — the localhost backend is intentionally unpinned; the pattern is real).

## Roadmap

- Braintree (headless v5) · PayU (shared test creds) · Paytm All-in-One
- Refunds (full/partial) and saved-cards backed by RBI network tokens
- UPI Autopay / e-mandates + Stripe Billing subscriptions
- Google Play Billing v8 (needs a Play Console listing)
- iOS app entry point (the KMP core already compiles for iOS today)
- Real certificate pins + a signing config to make the release/Play Fastlane lanes live
- Extract & publish `core:payments-api` as a standalone KMP library

## iOS readiness

The core is Kotlin Multiplatform and **compiles for iOS today** (`payments-api`, `protocol`,
`common`, `orchestration` all build `iosArm64` / `iosSimulatorArm64`). The provider SDKs are
Android-only by nature, so the contract stays in `commonMain` and only the implementations are
`androidMain`-shaped. v1 ships the Android app; an iOS entry point is a roadmap item, not a rewrite.

---

<div align="center">
<sub>Built as a portfolio flagship — a systems-level look at Android payments, not just a checkout screen.</sub>
</div>
