# provider:hosted-webview

Archetype-C gateway: renders a hosted checkout page in a Compose WebView and resolves on
return-URL redirect.

## Code-review checklist

- SSL/certificate errors fail closed — `SslFailClosedWebViewClient.onReceivedSslError` always
  calls `handler.cancel()` and reports a failure outcome. Never call `handler.proceed()`.
