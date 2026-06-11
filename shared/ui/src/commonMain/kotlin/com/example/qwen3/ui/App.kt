package com.example.qwen3.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.qwen3.core.ModelMetadata

/**
 * Shared Compose Multiplatform root. This is the Phase 0 placeholder that renders
 * on both Android and iOS, proving the shared-UI pipeline. The full screen-by-screen
 * port (ChatList → ChatDetail → Settings → Diagnostics → ModelSelection → Notes)
 * lands in Phase 5.
 */
@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Qwen3 Chat — Kotlin Multiplatform",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "${ModelMetadata.ALL.size} models available · shared core online",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
