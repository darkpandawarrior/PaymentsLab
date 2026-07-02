# PayU (India + LATAM)

- **Region:** India / LATAM
- **Archetype:** A (native SDK, India) / C (REST, LATAM) in reality — per `research-notes.md`
  (verified 2026-07-02): India has a live modern SDK (`in.payu:payu-checkout-pro:3.3.14`) with test
  key/salt available pre-KYC via dashboard test mode — a genuine real-upgrade candidate; LATAM is
  REST-only, no mobile SDK. This demo runs both through the generic archetype-C mock checkout path
  (same simplification as PhonePe/Worldpay — a verified backend order-creation integration is future
  work for the India SDK path specifically, since it's the one with real self-serve test access).
- **Status shipped:** `MOCK_MODE`.
- **Docs:** https://docs.payu.in/docs/android-checkoutpro-sdk (India); https://developers.payulatam.com (LATAM).
