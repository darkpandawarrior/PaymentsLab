# PaymentsLab — Design Spec

**Date:** 2026-07-02
**Status:** Approved by Siddharth (brainstorming session, section-by-section)
**Repo:** `/Users/darkpandawarrior/Repos/Android/PaymentsLab` (public GitHub target)

## 1. Purpose

A portfolio flagship that proves Lead-level Android system design through the hardest
integration domain on the platform: payments. Four goals, in priority order:

1. **Architecture showcase** — multi-module KMP architecture, one module per payment
   provider behind a unified abstraction, enforced boundaries, real testing strategy, CI.
2. **Living integration catalog** — a public reference covering the Android payments
   ecosystem: what each provider takes to integrate, sandbox feasibility, gotchas.
3. **Library groundwork** — `core:payments-api` is written so it extracts into a
   publishable KMP artifact later. Not published in v1.
4. **Interview reference** — per-provider docs, payload visualizations, and lifecycle
   walkthroughs that double as Siddharth's own payment-architecture study material.

**App concept:** an *Integration Lab*. Home screen catalogs every provider/flow with
status badges (`Sandbox-ready`, `KYC-gated — documented only`, `Coming soon`). Each
provider opens a Lab screen that executes and step-visualizes the full lifecycle —
order created → SDK invoked → callback → server verification → webhook → terminal state —
rendering actual (redacted) request/response payloads at each step. A secondary
**Demo Checkout** mode (cart → pay sheet) reuses the same provider registry for a
product-like demo.

## 2. V1 Scope

### Providers (all verified sandbox-feasible with zero business KYC, research 2026-07-02)

| Provider | Integration | Why |
|---|---|---|
| Razorpay | Drop-in Checkout (`com.razorpay:checkout` ≥1.6.40) | India anchor; instant `rzp_test_` keys; HMAC-SHA256 signature verification story |
| Cashfree | Nextgen SDK (`com.cashfree.pg:api` 2.3.x) | Second India gateway; sandbox **UPI simulator** (rare) |
| Raw UPI intent | No SDK — `upi://pay` + ACTION_VIEW chooser | Platform-level differentiator; NPCI chooser rules; "client response is unverifiable" lesson |
| Stripe | PaymentSheet (`com.stripe:paymentsheet` 23.x) | Global anchor; Compose-native; 3DS2 challenge demo; idempotency keys |
| Google Pay | TEST env via Stripe as gateway (`play-services-wallet`) | Lives inside `provider:stripe` — it is a payment method on Stripe, not a gateway |

### Documented but NOT integrated (KYC/partner-gated — catalog entries + docs only)

PhonePe PG, Juspay HyperSDK, PayU, Paytm All-in-One, CCAvenue/Easebuzz/Instamojo,
Samsung Pay. Each gets a `docs/` write-up: what onboarding requires, integration shape,
why it can't be solo-demoed.

### Lifecycle coverage per provider (v1)

Order creation (server-side) → payment execution → server-side verification →
webhook receipt → status polling with backoff → process-death recovery (pending-payment
journal in Room survives app kill). Refunds, tokenization/saved cards, UPI Autopay/
mandates, and Play Billing are later milestones (§10).

### Out of scope for v1

Play Billing (needs Play Console listing), Braintree (headless v5 — v1.x milestone),
iOS app target (modules are KMP-ready; no iOS entry point shipped), publishing
`payments-api` to Maven, real (live-mode) payments.

## 3. Architecture

### Decisions

- **Monorepo**: Android/KMP app + Ktor backend in one repo — one coherent story.
- **Full KMP now**: core modules are KMP from day one (approved over Android-only).
- **DI: Koin 4.x** (not Hilt) — consistent with Mileway/Kursi, KMP-compatible in
  commonMain, and keeps `payments-api` extractable (Hilt would Android-lock it).
- **Provider-plugin pattern**: each provider is a self-contained Gradle module
  contributing a `PaymentGateway` Koin definition; a registry collects them. Features
  depend only on the registry + api. Adding provider N+1 touches no existing code.
- **Boundaries enforced mechanically** via dependency-guard, not convention.

### Module map

```
payments-lab/
├── build-logic/convention/       # paymentslab.kmp.library / .cmp.feature /
│                                 #   .android.provider / .test  (adapted from Mileway)
├── gradle/libs.versions.toml     # AGP 9.2.1, Kotlin 2.4.0, CMP 1.11.1, Ktor 3.5,
│                                 #   Koin 4.2.x, Room 2.8.x, kotlinx-serialization
├── core/
│   ├── payments-api/             # PaymentGateway contract, sealed results, capability
│   │                             #   flags. commonMain-pure. Future library.
│   ├── protocol/                 # @Serializable order/verify/webhook/status DTOs +
│   │                             #   redaction layer. Shared with backend via jvm target.
│   ├── network/                  # Ktor client + backend API surface
│   ├── data/                     # Room KMP: transactions, pending-payment journal
│   ├── designsystem/             # theme, tokens, StepTimeline, PayloadCard components
│   └── common/                   # logging, UiText, result/coroutine utils
├── provider/
│   ├── razorpay/  cashfree/  upi-intent/  stripe/
│   │                             # androidMain implementations of the commonMain
│   │                             #   contract; own their SDK dep, Koin module,
│   │                             #   Lab metadata, and a per-module README
├── feature/
│   ├── lab/                      # catalog home + per-provider lab screen
│   ├── checkout-demo/            # cart → pay sheet over the same registry
│   └── history/                  # transaction log from Room
├── app/                          # Android composition root; startKoin + registry
└── backend/                      # Ktor server (JVM), depends on core:protocol
```

### KMP target matrix

| Module | Targets | Notes |
|---|---|---|
| `payments-api`, `common`, `protocol` | android + jvm + iosArm64/iosSimulatorArm64 | jvm target is imported by the backend — DTOs never drift |
| `data` | android + ios | Room 2.8 KMP (pattern proven in Mileway) |
| `network` | android + jvm + ios | Ktor engines per platform |
| `designsystem`, `feature:*` | CMP via `cmp.feature` convention plugin | Android is the only shipped target in v1 |
| `provider:*` | Android library, androidMain impls | SDKs are Android-only; contract stays commonMain |
| `backend` | JVM | Ktor 3.5 + Netty |

## 4. The PaymentGateway Contract (`core:payments-api`, commonMain)

```kotlin
interface PaymentGateway {
    val id: GatewayId                         // "razorpay", "upi_intent", ...
    val meta: GatewayMeta                     // name, badge, capabilities, docs link
    suspend fun prepare(order: OrderRef): PreparedPayment    // session tokens etc.
    fun launch(host: PaymentHost, prepared: PreparedPayment) // hands off to SDK/intent
    val results: Flow<GatewayEvent>
}

sealed interface PaymentResult {
    data class Success(val paymentId: String, val raw: RedactedPayload) : PaymentResult
    data class Failure(val code: FailureCode, val message: UiText,
                       val raw: RedactedPayload) : PaymentResult
    data class Pending(val reason: PendingReason) : PaymentResult  // UPI SUBMITTED
    data object Cancelled : PaymentResult
}
```

**`PaymentHost`** abstracts the Activity/launcher problem: every Indian gateway SDK is
Activity-callback-era. The host wraps ActivityResult contracts, leaks no Activity
references, and delivers results across configuration change and recreation. This
wrapping problem is a headline README feature — it is where Compose-era architecture
meets legacy SDK reality.

**Registry:** each `provider:*` module contributes a Koin definition; the app module
assembles `PaymentGatewayRegistry` (ordered, capability-filterable). Features consume
only the registry.

## 5. Payment Data Flow

1. UI → `PaymentOrchestrator` (commonMain): `startPayment(gatewayId, catalogItemId)`.
2. Orchestrator → backend `POST /orders`. **Journal-first:** a pending-payment row is
   written to Room *before* the SDK launches — process-death insurance.
3. `gateway.prepare()` → `gateway.launch(host, prepared)` → SDK UI / UPI chooser.
4. Callback → mapped to `PaymentResult`. **Client result is a hint, never truth.**
5. Orchestrator → backend `POST /payments/{id}/verify` (signature check) and/or polls
   `GET /payments/{id}`. Backend state (updated by webhooks) is the single source of truth.
6. Terminal state → journal row resolved → history; Lab timeline renders every step
   with its redacted payload.

**Process-death recovery:** on cold start the orchestrator reads unresolved journal rows
and resumes verification/polling. UPI `SUBMITTED` limbo uses WorkManager-backed polling
with exponential backoff (androidMain impl of a commonMain `StatusPoller` interface).

## 6. Backend (`backend/`, Ktor 3.5 + Netty)

Thin routes over per-provider `GatewayAdapter` implementations — deliberately mirroring
the app's plugin architecture.

| Route | Behavior |
|---|---|
| `POST /orders` | Client sends catalog item id — **price is resolved server-side** (trust-boundary lesson, surfaced in Lab UI). Creates provider order (Razorpay Orders API / Cashfree order+session / Stripe PaymentIntent / local txn ref for UPI intent). Returns `core:protocol` DTOs. |
| `POST /payments/{id}/verify` | Provider-specific: Razorpay HMAC-SHA256(order_id\|payment_id, secret), Cashfree signature, Stripe PaymentIntent retrieve. |
| `POST /webhooks/{provider}` | Webhook-signature verification → idempotent state update (event ids deduped). Stripe CLI forwards locally; Razorpay/Cashfree sandbox webhooks target the deployed instance. |
| `GET /payments/{id}` | Polling target; returns authoritative state. |

**State:** SQLite (Exposed). **Deployment:** local-first; one free-tier Render/Railway
deployment + Dockerfile so demos work without a laptop.

### Security posture (dedicated README section)

- Secret keys only in backend env vars; APK ships publishable keys only. `.env.example`
  documents every credential.
- Never trust client success callbacks; verify server-side; webhooks reconcile.
- Webhook signature verification before processing; idempotent handlers.
- Idempotency keys on order creation; retry the *status check*, never the charge.
- Redaction layer in `core:protocol`: Lab-rendered/logged payloads pass an allowlist
  serializer — no secret or PII ever renders or logs.

## 7. Error Handling

One sealed vocabulary in `payments-api`: `FailureCode` = `UserCancelled`,
`NetworkError`, `GatewayDeclined`, `VerificationFailed`, `ConfigMissing`,
`SdkError(raw)`. Each provider maps its SDK error zoo into this vocabulary; the mapping
tables are documented in each provider README. User-facing strings via `UiText`.
Providers with missing sandbox config degrade to a "configure me" Lab state, never crash.

## 8. Testing Strategy

| Layer | Approach |
|---|---|
| commonTest | Orchestrator state machine with `FakePaymentGateway` (fakes, not mocks); journal-recovery scenarios; protocol round-trip serialization |
| Android unit | Per-provider SDK-callback → `PaymentResult` mapping via faked callbacks; Turbine for flows |
| Backend | Ktor `testApplication`: real HMAC test vectors, webhook idempotency, price-tamper rejection |
| Compose UI | Lab timeline per lifecycle state; happy path per screen; Roborazzi screenshots (double as README imagery) |
| Guards | Kover coverage floor; dependency-guard locks the module graph |

TDD (superpowers/android-tdd) for the orchestrator, journal, verification, and error
mapping — the branching-logic cores.

## 9. CI & Repo Presentation

**GitHub Actions (Mileway-derived):** fast gate (ktlint, detekt, common/jvm tests,
backend tests, dependency-guard) · assemble gate (debug APK artifact) · screenshots
workflow. Single-source versioning via VERSION + BUILD_NUMBER files.

**Presentation:** root README (architecture diagram, provider status matrix — the living
catalog index); per-provider READMEs (integration steps, sandbox setup, error mapping,
gotchas — the interview reference); `docs/` gateway-comparison covering KYC-gated
providers.

## 10. Milestones

**V1 (this week):**
- D1 skeleton: convention plugins, version catalog, module graph, CI
- D2 `payments-api` + `protocol` + orchestrator + journal (TDD, commonTest)
- D3 backend: orders/verify/webhooks + Razorpay adapter + UPI ref support
- D4 `provider:upi-intent` + `provider:razorpay` + Lab UI end-to-end
- D5 `provider:stripe` (+ Google Pay TEST) + `provider:cashfree`
- D6 checkout-demo, history, polish, screenshots, READMEs
- D7 buffer, deploy backend, publish repo

**Later milestones:** Braintree headless v5 · PayU (shared test creds) · refunds +
saved-cards (RBI network tokens) screens · UPI Autopay/mandates + Stripe Billing ·
Play Billing v8 (Play Console listing) · iOS entry point (CMP) · extract + publish
`payments-api` · verify patterns against the Jugnoo reference repo (Siddharth to
provide a copy; treat as ground truth for real-world integration checks).

## 11. Key Research Findings (2026-07-02, full report in docs/ later)

- Solo-demoable sandbox, zero KYC: Razorpay, Cashfree, Stripe, Braintree, raw UPI
  intent (P2P to own VPA), Google Pay TEST. Gated: PhonePe (business KYC for MID),
  Juspay (B2B-issued credentials), Paytm (staging MID flaky), CCAvenue/Instamojo (dated
  SDKs).
- Braintree Drop-in UI deprecated Jul 2025 (v5 headless only); Checkout.com Frames
  deprecated Jun 2026; UPI Collect flow sunsets Feb 2026 — intent/QR win.
- RBI card-on-file tokenization: merchants store network tokens only; further
  compliance directions land Apr 2026.
- Play Billing v8 mandatory for updates by Aug 2026; `queryPurchaseHistory()` removed.
