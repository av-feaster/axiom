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
import com.axiom.android.sdk.AxiomMode
import com.axiom.android.sdk.ui.theme.AxiomTheme as AxiomUiTheme
import com.axiom.android.sdk.ui.AxiomBottomSheet
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
                    mode = AxiomMode.Managed,
                    enableLogging = true
                )
            )
            Log.i(TAG, "AxiomSDK initialized successfully")
            statusMessage = "Axiom SDK Initialized Successfully"
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
                        statusMessage = statusMessage
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleAppUI(
    statusMessage: String
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
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "SDK Status",
                        style = MaterialTheme.typography.titleMedium,
                        color = AxiomUiTheme.colors.textPrimary
                    )
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = AxiomUiTheme.colors.textSecondary
                    )
                }
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
                        text = "Instructions",
                        style = MaterialTheme.typography.titleMedium,
                        color = AxiomUiTheme.colors.textPrimary
                    )
                    Text(
                        text = "The AxiomBottomSheet is rendered automatically in Managed mode. Tap the floating pill at the bottom to interact with the SDK.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AxiomUiTheme.colors.textSecondary
                    )
                }
            }
        }
        
        // Add AxiomBottomSheet - single UI entry point
        AxiomBottomSheet()
    }
}