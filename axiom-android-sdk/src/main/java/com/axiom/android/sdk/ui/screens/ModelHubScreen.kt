package com.axiom.android.sdk.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.axiom.android.sdk.AxiomSDK
import com.axiom.android.sdk.domain.DownloadState
import com.axiom.android.sdk.domain.ModelDownloadState
import com.axiom.android.sdk.domain.ModelUIItem
import com.axiom.android.sdk.domain.withActiveDownload
import com.axiom.android.sdk.ui.components.AppCard
import com.axiom.android.sdk.ui.components.DownloadProgressCard
import com.axiom.android.sdk.ui.theme.AxiomTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val TAG = "ModelHubScreen"

/**
 * Model Hub Screen - central screen for all model management
 * Shows all models with download/delete/select actions
 */
@Composable
fun ModelHubScreen(
    onModelSelected: (String) -> Unit,
    onImportModel: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val modelManager = AxiomSDK.getModelManager()
    var models by remember { mutableStateOf<List<ModelUIItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    val activeDownload by modelManager.getActiveDownloadFlow().collectAsState()

    val displayModels = remember(models, activeDownload) {
        models.withActiveDownload(activeDownload)
    }

    // Load models on composition
    LaunchedEffect(Unit) {
        isLoading = true
        Log.i(TAG, "Loading available models...")
        try {
            val availableModels = modelManager.getAvailableModels()
            models = availableModels
            Log.i(TAG, "Loaded ${availableModels.size} available models")
            availableModels.forEach { model ->
                Log.d(TAG, "Model: ${model.name} (${model.id}) - State: ${model.downloadState}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load models", e)
        }
        isLoading = false
    }

    // Refresh models only when download reaches terminal state (Completed or Failed)
    // Using snapshotFlow with distinctUntilChanged on state class type
    LaunchedEffect(Unit) {
        snapshotFlow { activeDownload?.state?.javaClass }
            .distinctUntilChanged()
            .collect { _ ->
                val state = activeDownload?.state
                if (state is DownloadState.Completed || state is DownloadState.Failed) {
                    try {
                        models = modelManager.getAvailableModels()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to refresh models after download", e)
                    }
                }
            }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    Log.i(TAG, "Import model clicked")
                    onImportModel()
                },
                containerColor = AxiomTheme.colors.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Import Model",
                    tint = AxiomTheme.colors.textPrimary
                )
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(displayModels, key = { it.id }) { model ->
                    val isDownloading = model.downloadState is ModelDownloadState.Downloading ||
                        model.downloadState is ModelDownloadState.Paused ||
                        model.downloadState is ModelDownloadState.Updating

                    if (isDownloading) {
                        DownloadProgressCard(
                            model = model,
                            onDownload = {
                                Log.i(TAG, "Download requested for model: ${model.name} (${model.id})")
                                modelManager.download(model.id)
                            },
                            onPauseResume = {
                                Log.i(TAG, "Pause/Resume requested for model: ${model.name} (${model.id})")
                                when (model.downloadState) {
                                    is ModelDownloadState.Downloading -> modelManager.pause()
                                    is ModelDownloadState.Paused -> modelManager.resume()
                                    else -> {}
                                }
                            },
                            onCancel = {
                                Log.i(TAG, "Cancel requested for model: ${model.name} (${model.id})")
                                modelManager.cancel()
                            },
                            onRemove = {
                                Log.i(TAG, "Remove requested for model: ${model.name} (${model.id})")
                                scope.launch {
                                    modelManager.delete(model.id)
                                    models = modelManager.getAvailableModels()
                                }
                            }
                        )
                    } else {
                        AppCard(
                            model = model,
                            onAddToApp = {
                                Log.i(TAG, "Add to app requested for model: ${model.name} (${model.id})")
                                modelManager.download(model.id)
                            },
                            onDelete = {
                                Log.i(TAG, "Delete requested for model: ${model.name} (${model.id})")
                                scope.launch {
                                    modelManager.delete(model.id)
                                    models = modelManager.getAvailableModels()
                                }
                            },
                            onLaunchChat = {
                                Log.i(TAG, "Launch chat requested for model: ${model.name} (${model.id})")
                                scope.launch {
                                    try {
                                        // Initialize engine with selected model before navigating
                                        val modelPath = "${context.filesDir.absolutePath}/models/${model.id}.gguf"
                                        val config = com.axiom.core.LLMConfig(
                                            modelPath = modelPath,
                                            contextSize = 1024,
                                            temperature = 0.7f,
                                            topK = 40,
                                            topP = 0.95f,
                                            repeatPenalty = 1.0f,
                                            threads = Runtime.getRuntime().availableProcessors(),
                                            maxTokens = 512
                                        )
                                        val engineManager = com.axiom.android.sdk.engine.EngineManager
                                        val success = engineManager.initialize(config)
                                        if (success) {
                                            Log.i(TAG, "Engine initialized successfully with model: ${model.id}")
                                            onModelSelected(model.id)
                                        } else {
                                            Log.e(TAG, "Failed to initialize engine with model: ${model.id}")
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error initializing engine", e)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
