package com.paymentslab.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.coroutines.MainScope

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val koin = initWebKoin()
    startMockCheckoutAutoResolver(relay = koin.get(), scope = MainScope())
    ComposeViewport(document.body!!) {
        WebAppRoot()
    }
}
