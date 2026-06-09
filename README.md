# PaymentsLab

An **Integration Lab** for the Android payments ecosystem — a KMP multi-module app that runs, and
step-by-step visualizes, real payment flows across multiple gateways behind one unified
`PaymentGateway` abstraction, backed by a Ktor server that does the order creation, signature
verification, and webhook reconciliation that real payments require.

It is three things at once:

1. **An architecture showcase** — a clean multi-module Kotlin Multiplatform design where each
   payment provider is an isolated, swappable module, the domain core is fully unit-tested against
   fakes, and the messy Activity-callback SDK reality is confined behind a coroutine-friendly host.
2. **A living catalog** — every provider the app touches is documented: what it takes to integrate,
   whether a solo developer can demo it in sandbox, and the gotchas.
3. **A teaching reference** — the Lab renders each payment's lifecycle as a live timeline (order
   created → SDK launched → client result → server verification → settled), showing the actual
   (redacted) payloads at every hop, so the *why* of payment architecture is visible, not buried.

## The one idea worth stealing

> **A client-side `Success` is a hint, never proof.** Only the server — after signature
> verification and webhook reconciliation — decides the true state.

Everything in the design flows from that: a server that owns price and truth, a client that always
confirms before trusting, a journal written *before* the SDK launches so a process death mid-payment
is always recoverable, and a redaction layer so no secret or PII ever renders or logs.

## Provider status matrix

| Provider | Category | Region | Sandbox (no KYC)? | In v1 | Notes |
|---|---|---|:--:|:--:|---|
| **Razorpay** | Gateway | India | ✅ | ✅ | Drop-in checkout; real HMAC-SHA256 signature verification |
| **Cashfree** | Gateway | India | ✅ | ✅ | Nextgen SDK; sandbox UPI simulator |
| **UPI intent** | Platform flow | India | ✅ | ✅ | Raw `upi://` intent, no SDK; client response is *unverifiable* by design |
| **Stripe** | Gateway | Global | ✅ | ✅ | PaymentSheet; 3DS2; Google Pay rides Stripe as gateway |
| **Google Pay** | Method | Global | ✅ | ✅ | TEST env, processed via Stripe |
| PhonePe PG | Gateway | India | ❌ | 📄 | MID needs business KYC — documented only |
| Juspay HyperSDK | Orchestrator | India | ❌ | 📄 | Credentials B2B-issued — documented only |
| PayU / Paytm | Gateway | India | ⚠️ | 📄 | Later milestone / documented |
| Play Billing | IAP | — | ⚠️ | 🔜 | Needs Play Console listing — later milestone |

✅ runnable in sandbox · 📄 catalog + docs only (gated) · 🔜 planned

## Architecture

```
app/                       Android composition root — Koin wiring, nav, AndroidPaymentHost
backend/                   Ktor JVM server — orders, verify, webhooks, status (server = truth)
core/
  payments-api/            The frozen contract: PaymentGateway, PaymentResult, PaymentHost,
                           PaymentBackend, PendingPaymentJournal, PaymentStep, Redactor  (KMP + jvm)
  protocol/                @Serializable wire DTOs shared with the backend's JVM target  (KMP + jvm)
  orchestration/           PaymentOrchestrator — the tested heart (journal-first, server-as-truth)
  network/                 Ktor client implementing PaymentBackend
  data/                    Room KMP journal (process-death recovery)
  designsystem/            Compose Multiplatform theme, tokens, StepTimeline, PayloadCard
  common/                  UiText, logging
provider/
  razorpay/  cashfree/  upi-intent/  stripe/    one module per gateway, behind the contract
feature/
  lab/                     catalog home + live per-provider lab timeline
  checkout-demo/           product checkout that reuses the same registry (the "explained" mode)
  history/                 transaction log from the Room journal
```

**Why these choices:** full KMP (Android-first, iOS-ready) with Koin so the `payments-api` module
can be extracted into a published library later; provider-plugin modules so adding gateway N+1
touches no existing code (Koin `getAll<PaymentGateway>()` collects them); a Ktor backend because
every Lead-level payments question lives on the server side — signatures, webhooks, idempotency.

## Running it

```bash
# 1. Start the backend (in-memory store; test/sandbox adapters)
./gradlew :backend:run                 # serves on :8080

# 2. Build & install the app (points at 10.0.2.2:8080 from the emulator)
./gradlew :app:installDebug

# 3. Verification gate
./gradlew fastGate                     # ktlint + detekt + backend + core unit tests
```

Sandbox credentials are read from environment variables (see `backend/.env.example`); the app only
ever ships publishable keys, never secrets.

## Status

v1 in progress. Core contract, orchestrator (with a passing fake-based test suite), data, network,
and designsystem modules are in place; providers, backend, features, and the app shell are being
assembled. See [`docs/superpowers/specs/`](docs/superpowers/specs/) for the full design.
