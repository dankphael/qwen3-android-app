package com.example.qwen3.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qwen3.ui.viewmodels.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel { SettingsViewModel() },
) {
    val temperature by viewModel.temperature.collectAsState()
    val maxTokens by viewModel.maxTokens.collectAsState()
    val gpuEnabled by viewModel.gpuEnabled.collectAsState()
    val systemPrompt by viewModel.systemPrompt.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SettingSection(title = "Generation")
            SettingItem(
                label = "Temperature",
                value = "%.2f".format(temperature),
            ) {
                Slider(
                    value = temperature,
                    onValueChange = { viewModel.setTemperature(it) },
                    valueRange = 0f..2f,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            SettingItem(
                label = "Max Tokens",
                value = maxTokens.toString(),
            ) {
                Slider(
                    value = maxTokens.toFloat(),
                    onValueChange = { viewModel.setMaxTokens(it.toInt()) },
                    valueRange = 100f..2048f,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SettingSection(title = "Hardware")
            SettingItem(
                label = "Enable GPU",
                value = if (gpuEnabled) "On" else "Off",
            ) {
                Switch(
                    checked = gpuEnabled,
                    onCheckedChange = { viewModel.setGpuEnabled(it) },
                )
            }

            SettingSection(title = "System Prompt")
            TextField(
                value = systemPrompt,
                onValueChange = { viewModel.setSystemPrompt(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                minLines = 4,
            )
        }
    }
}

@Composable
private fun SettingSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingItem(
    label: String,
    value: String,
    content: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content()
    }
}
