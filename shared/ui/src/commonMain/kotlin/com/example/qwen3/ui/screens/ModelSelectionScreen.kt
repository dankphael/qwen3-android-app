package com.example.qwen3.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Row
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qwen3.ui.viewmodels.ModelSelectionViewModel

@Composable
fun ModelSelectionScreen(
    viewModel: ModelSelectionViewModel = viewModel { ModelSelectionViewModel() },
) {
    val availableModels by viewModel.availableModels.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Select Model") })
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            items(availableModels) { model ->
                ModelSelectionItem(
                    modelName = model.name,
                    modelSize = "${model.sizeGb}GB",
                    isSelected = model.name == selectedModel,
                    onSelect = {
                        viewModel.selectModel(model.name)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectModel(model.name) }
                        .padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun ModelSelectionItem(
    modelName: String,
    modelSize: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                text = modelName,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = modelSize,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

data class ModelItem(
    val name: String,
    val sizeGb: Double,
    val minRamMb: Int,
    val minCores: Int,
)
