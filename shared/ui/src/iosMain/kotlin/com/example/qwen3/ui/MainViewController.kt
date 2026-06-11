package com.example.qwen3.ui

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** Entry point hosted by the SwiftUI iosApp shell. */
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
