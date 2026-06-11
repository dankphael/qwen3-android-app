package com.example.qwen3.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qwen3.ui.viewmodels.DiagnosticsViewModel

@Composable
fun DiagnosticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DiagnosticsViewModel = viewModel { DiagnosticsViewModel() },
) {
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val modelInfo by viewModel.modelInfo.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDiagnostics()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnostics") },
                navigationIcon = {
                    IconButton(onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            DiagnosticsSection(title = "Device")
            DiagnosticsItem("RAM", deviceInfo.ramMb)
            DiagnosticsItem("CPU Cores", deviceInfo.cores.toString())
            DiagnosticsItem("Tier", deviceInfo.tier)
            DiagnosticsItem("GPU Backend", deviceInfo.gpuBackend)

            DiagnosticsSection(title = "Model")
            DiagnosticsItem("Current", modelInfo.currentModel)
            DiagnosticsItem("Context Size", modelInfo.contextSize.toString())
            DiagnosticsItem("Decode Threads", modelInfo.decodeThreads.toString())
            DiagnosticsItem("Batch Threads", modelInfo.batchThreads.toString())
            DiagnosticsItem("Batch Size", modelInfo.batchSize.toString())
        }
    }
}

@Composable
private fun DiagnosticsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun DiagnosticsItem(label: String, value: String) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

data class DeviceInfoState(
    val ramMb: String = "N/A",
    val cores: Int = 0,
    val tier: String = "UNKNOWN",
    val gpuBackend: String = "cpu",
)

data class ModelInfoState(
    val currentModel: String = "None",
    val contextSize: Int = 0,
    val decodeThreads: Int = 0,
    val batchThreads: Int = 0,
    val batchSize: Int = 0,
)
