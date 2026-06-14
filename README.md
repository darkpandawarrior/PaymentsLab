<div align="center">

# PaymentsLab

### An Integration Lab for the Android payments ecosystem — every gateway behind one abstraction, with a live look at what actually happens on each transaction.

[![CI](https://github.com/darkpandawarrior/PaymentsLab/actions/workflows/ci.yml/badge.svg)](https://github.com/darkpandawarrior/PaymentsLab/actions/workflows/ci.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-7F52FF?logo=kotlin&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose%20MP-1.11.1-4285F4?logo=jetpackcompose&logoColor=white)
![Ktor](https://img.shields.io/badge/Ktor-3.5.0-087CFA?logo=ktor&logoColor=white)
![Modules](https://img.shields.io/badge/modules-16-success)

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

- 🧩 **16-module KMP architecture.** One Gradle module per provider, contributed into a registry via
  Koin `getAll<PaymentGateway>()` — adding gateway *N+1* touches no existing code. Feature modules
  never depend on each other; they meet only at the `:app` composition root.
- 🔌 **One contract, four real SDKs.** Razorpay, Cashfree, Stripe (+ Google Pay) and a raw UPI intent
  flow all implement the same tiny `PaymentGateway` interface. The Activity-callback SDKs are bridged
  into suspending coroutines by a `PaymentHost` that never leaks an `Activity` upward.
- 🛡️ **Server is the source of truth.** A companion Ktor server creates orders (**price resolved
  server-side**), verifies payments with **real HMAC-SHA256** (Razorpay), and reconciles idempotent,
  signature-checked webhooks — the client callback is only ever treated as a hint.
- ⏱️ **Process-death recovery, for real.** Every in-flight payment is journaled to Room *before* the
  SDK opens; on cold start the orchestrator finds unresolved payments and reconciles them.
- 🔎 **Redaction by default.** A single choke point masks any secret/PII-shaped field before it can
  be rendered in the Lab timeline or written to a log.
- 🧪 **A real quality gate.** ktlint + detekt across all 16 modules (including KMP `commonMain`),
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

> Screenshot baselines are generated by the Roborazzi screenshot suite (roadmap item) and will land
> in `docs/screenshots/`.

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
  protocol/                @Serializable wire DTOs shared with the backend's JVM target    (KMP + jvm)
  orchestration/           PaymentOrchestrator — the tested heart (journal-first, server-as-truth)
  network/                 Ktor client implementing PaymentBackend
  data/                    Room KMP journal (process-death recovery)
  designsystem/            Compose Multiplatform theme, tokens, StepTimeline, PayloadCard
  common/                  UiText, KMP logging
provider/
  razorpay/  cashfree/  upi-intent/  stripe/    one module per gateway, behind the contract
feature/
  lab/                     catalog home + live per-provider lab timeline
  checkout-demo/           product checkout that reuses the same registry (the "explained" mode)
  history/                 transaction log from the Room journal
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
| Client networking | Ktor **3.5.0** client (OkHttp + Darwin engines) + kotlinx-serialization |
| Backend | Ktor **3.5.0** server (Netty), in-memory store (swappable for Exposed/SQLite) |
| Database | Room **2.8.4** (KMP, bundled SQLite) — the pending-payment journal |
| Concurrency | Coroutines + Flow (no LiveData); `kotlinx-datetime`, immutable collections |
| Provider SDKs | Razorpay Checkout, Cashfree PG (api + ui), Stripe PaymentSheet, Play Services Wallet |
| Testing | JUnit, MockK, Turbine, kotlinx-coroutines-test, Koin-Test; fake-first |
| Quality | detekt **1.23.8**, ktlint, dependency-guard |
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

## Roadmap

- Braintree (headless v5) · PayU (shared test creds) · Paytm All-in-One
- Refunds (full/partial) and saved-cards backed by RBI network tokens
- UPI Autopay / e-mandates + Stripe Billing subscriptions
- Google Play Billing v8 (needs a Play Console listing)
- iOS app entry point (the KMP core already compiles for iOS today)
- Roborazzi screenshot suite → `docs/screenshots/`
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
