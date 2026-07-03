package com.paymentslab.ios.shared

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** Swift calls this once, after [doInitKoin], to get the root view controller. */
fun mainViewController(): UIViewController = ComposeUIViewController { AppRoot() }
