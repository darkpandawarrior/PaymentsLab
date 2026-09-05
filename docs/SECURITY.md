# Security posture

PaymentsLab-KMP treats security as **defense in depth**, with a clear-eyed view of what each layer buys.
The single most important idea comes first:

> **The authoritative security is server-side.** A payment is only trusted after the backend verifies
> its signature and reconciles it against provider webhooks (see the README's "server is truth"
> section and `core:orchestration`). Everything below *raises the cost* of an attack on the client —
> it does not, and cannot, replace server-side verification. On-device anti-tamper is a speed bump for
> a determined attacker who controls the device; the server is the wall.

## Layers

| Layer | Where | What it stops | Honest limits |
|---|---|---|---|
| **Server verification** | `backend/`, `core:orchestration` | A forged/replayed client "success" — the server checks the real signature and webhook state | The real security boundary; everything else is defense in depth |
| **Redaction** | `core:payments-api` `Redactor` | Secrets/PII leaking into the Lab UI or logs | Allowlist-by-pattern; new secret-shaped keys must be covered |
| **At-rest encryption** | `core:security` `KeystoreSecureStore` | Reading a saved token off a lost/stolen device's storage | Key is non-exportable (TEE/StrongBox-backed); a rooted live device can still ask the Keystore to decrypt |
| **Screen protection** | `SecureScreen`, `AppSecurityManager` | Screenshots, screen recording/casting, recents-thumbnail leaks, tapjacking/overlay attacks | Framework-enforced and reliable, per-window |
| **Transport** | `network_security_config.xml`, `PaymentCertificatePinning` | User-CA MITM (system-only trust anchors) and, with real pins, proxy interception of provider APIs | Pins here are placeholders; the localhost dev backend is intentionally unpinned |
| **Device integrity** | `DeviceIntegrity`, anti-debug/hook/SSL detectors | Casual root/emulator/debugger/Frida/Xposed/SSL-bypass — raises attacker effort, feeds risk decisions | **Best-effort.** These run in the attacker-controlled process and are defeatable; they are signal, not a guarantee |

## Detection vs enforcement (why they're separate)

`SecurityAuditor.audit()` (a `suspend` call, run off the main thread) answers **"what did we find?"** —
a `SecurityAudit` of raw signals. `SecurityPolicy.evaluate(audit, posture)` answers **"what should we
do?"** — an `ALLOW` / `WARN` / `BLOCK` `SecurityDecision` for the configured `SecurityPosture` (strict
in release, lenient in debug). Keeping the two apart means the app chooses its stance per build without
touching detection code, and the decision is a pure, exhaustively-tested function.

## VAPT bypass flags

A penetration test often *must* run on a rooted/hooked/debuggable device. `BuildConfig.BYPASS_*` →
`SecurityConfig.bypass*` let a dedicated compliance-test variant exclude a category from the compromise
gate **while still detecting and logging it** (diagnostics stay accurate). Every flag defaults to
`false`; they are never `true` in a real release.

## Not implemented (deliberately)

Native anti-Frida/anti-hook via JNI, string-obfuscation, and dynamic self-integrity checks were left
out on purpose: they are easily bypassed, add significant complexity, and read as security theatre in a
codebase whose thesis is that the server is the real authority. The bar we aim for is *credible,
Lead-level client hardening* — not the illusion of an un-tamperable client.
