package com.example.qwen3.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Shared Compose Multiplatform root. Phase 5 navigation scaffold with tabs for:
 * ChatList, Models, Notes, Settings. ChatDetail dialog for per-chat messaging.
 * Diagnostics accessible from ChatList toolbar.
 */
@Composable
fun App() {
    MaterialTheme {
        AppNavigation()
    }
}
