package com.axiom.sample

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import android.util.Log
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.annotation.RequiresPermission
import com.axiom.android.sdk.AxiomSDK
import com.axiom.android.sdk.AxiomSDKConfig
import com.axiom.android.sdk.ui.theme.AxiomTheme as AxiomUiTheme
import com.axiom.android.sdk.ui.AxiomBottomSheet
import com.axiom.android.sdk.ui.components.MyModelsBottomSheet
import com.axiom.android.sdk.domain.ModelUIItem
import com.axiom.android.sdk.domain.ModelDownloadState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    private var statusMessage by mutableStateOf("Axiom AI SDK Sample")
    private var isNetworkConnected by mutableStateOf(true)
    private var snackbarMessage by mutableStateOf<String?>(null)
    private var snackbarColor by mutableStateOf(Color.Red)
    private var showAspectsBottomSheet by mutableStateOf(false)
    private var modelUIItems by mutableStateOf<List<ModelUIItem>>(emptyList())

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "MainActivity onCreate started")
        
        // Set the window to fit system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Initialize AxiomSDK
        try {
            AxiomSDK.initialize(
                application = application,
                config = AxiomSDKConfig(
                    context = this,
                    enableLogging = true
                )
            )
            Log.i(TAG, "AxiomSDK initialized successfully")
            statusMessage = "Axiom SDK Initialized Successfully"
            
            // Initialize mock model data
            modelUIItems = listOf(
                ModelUIItem(
                    id = "llama-3-8b",
                    name = "Llama 3 8B",
                    description = "General purpose language model",
                    size = "4.7 GB",
                    version = "1.0",
                    downloadState = ModelDownloadState.Installed
                ),
                ModelUIItem(
                    id = "mistral-7b",
                    name = "Mistral 7B",
                    description = "High-performance language model",
                    size = "4.1 GB",
                    version = "1.0",
                    downloadState = ModelDownloadState.NotStarted
                ),
                ModelUIItem(
                    id = "gemma-7b",
                    name = "Gemma 7B",
                    description = "Google's open language model",
                    size = "5.2 GB",
                    version = "1.0",
                    downloadState = ModelDownloadState.Downloading(0.35f, "2.4 MB/s", 1820000000L, 5200000000L)
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AxiomSDK", e)
            statusMessage = "SDK Initialization Failed: ${e.message}"
        }
        
        setContent {
            AxiomUiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AxiomUiTheme.colors.background
                ) {
                    SampleAppUI(
                        statusMessage = statusMessage,
                        showAspectsBottomSheet = showAspectsBottomSheet,
                        onAspectsBottomSheetShow = { showAspectsBottomSheet = true },
                        onAspectsBottomSheetDismiss = { showAspectsBottomSheet = false },
                        modelUIItems = modelUIItems,
                        onModelDownload = { modelId -> 
                            // Handle model download/pause/resume
                            Log.i(TAG, "Model action: $modelId")
                        },
                        onImportLocalModel = {
                            // Handle import local model
                            Log.i(TAG, "Import local model")
                        },
                        onOptimizeStorage = {
                            // Handle optimize storage
                            Log.i(TAG, "Optimize storage")
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleAppUI(
    statusMessage: String,
    showAspectsBottomSheet: Boolean,
    onAspectsBottomSheetShow: () -> Unit,
    onAspectsBottomSheetDismiss: () -> Unit,
    modelUIItems: List<ModelUIItem>,
    onModelDownload: (String) -> Unit,
    onImportLocalModel: () -> Unit,
    onOptimizeStorage: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Axiom AI SDK Sample") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AxiomUiTheme.colors.backgroundSecondary,
                    titleContentColor = AxiomUiTheme.colors.textPrimary
                )
            )
        },
        floatingActionButton = {
            if (!showAspectsBottomSheet) {
                FloatingActionButton(
                    onClick = onAspectsBottomSheetShow,
                    containerColor = AxiomUiTheme.colors.primary,
                    contentColor = AxiomUiTheme.colors.textPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Open Aspects")
                }
            }
        }
    ) { paddingValues ->
        val view = LocalView.current
        val windowInsets = WindowInsets.systemBars
        val imeInsets = WindowInsets.ime
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(windowInsets)
                .windowInsetsPadding(imeInsets)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = AxiomUiTheme.colors.backgroundTertiary
                )
            ) {
                Text(
                    text = statusMessage,
                    modifier = Modifier.padding(16.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    color = AxiomUiTheme.colors.textPrimary
                )
            }

            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = AxiomUiTheme.colors.backgroundTertiary
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Axiom AI SDK Sample",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        color = AxiomUiTheme.colors.textPrimary
                    )
                    Text(
                        "This sample demonstrates the Axiom Android SDK integration.",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = AxiomUiTheme.colors.textSecondary
                    )
                    Text(
                        "• Single dependency integration",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = AxiomUiTheme.colors.textSecondary
                    )
                    Text(
                        "• ChatGPT-like bottom sheet UI",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = AxiomUiTheme.colors.textSecondary
                    )
                    Text(
                        "• Local GGUF model execution",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = AxiomUiTheme.colors.textSecondary
                    )
                    Text(
                        "• Customizable design system",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = AxiomUiTheme.colors.textSecondary
                    )
                }
            }
        }
    }
    
    // Add AxiomBottomSheet for chat functionality
    AxiomBottomSheet()
    
    // My Models Bottom Sheet
    if (showAspectsBottomSheet) {
        MyModelsBottomSheet(
            models = modelUIItems,
            onImportLocalModel = onImportLocalModel,
            onOptimizeStorage = onOptimizeStorage,
            onModelPauseResume = onModelDownload,
            onDismiss = onAspectsBottomSheetDismiss
        )
    }
}