package com.axiom.sample

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import android.util.Log
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.annotation.RequiresPermission
import com.axiom.core.LLMConfig
import com.axiom.core.LLMEngine
import com.axiom.core.Model
import com.axiom.core.ModelManager
import com.axiom.llama.cpp.LlamaCppEngine
import com.axiom.models.DefaultModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
        private const val TAG_NETWORK = "NetworkMonitor"
        private const val TAG_DOWNLOAD = "ModelDownload"
        private const val TAG_GENERATION = "TextGeneration"
        private const val TAG_INIT = "EngineInit"
    }
    
    private val engine: LLMEngine = LlamaCppEngine()
    private val modelManager: ModelManager by lazy { DefaultModelManager(this) }
    
    private var isInitialized = false
    private var currentPrompt by mutableStateOf("")
    private var generatedText by mutableStateOf("")
    private var streamingText by mutableStateOf("")
    private var isLoading by mutableStateOf(false)
    private var errorMessage by mutableStateOf("")
    private var statusMessage by mutableStateOf("Axiom AI SDK Sample")
    private var useStreaming by mutableStateOf(false)
    private var availableModels by mutableStateOf<List<Model>>(emptyList())
    private var isNetworkConnected by mutableStateOf(true)
    private var snackbarMessage by mutableStateOf<String?>(null)
    private var snackbarColor by mutableStateOf(Color.Red)

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "MainActivity onCreate started")
        
        // Setup network connectivity monitoring
        Log.i(TAG_NETWORK, "Setting up network connectivity monitoring")
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.i(TAG_NETWORK, "Network available - Internet connected")
                isNetworkConnected = true
                snackbarMessage = "Internet connected"
                snackbarColor = Color.Green
                lifecycleScope.launch {
                    delay(5000)
                    snackbarMessage = null
                    Log.d(TAG_NETWORK, "Snackbar message cleared after 5s")
                }
            }
            
            override fun onLost(network: Network) {
                super.onLost(network)
                Log.w(TAG_NETWORK, "Network lost - No internet connected")
                isNetworkConnected = false
                snackbarMessage = "No internet connected"
                snackbarColor = Color.Red
            }
        }
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        
        // Check initial network state
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        isNetworkConnected = capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        Log.i(TAG_NETWORK, "Initial network state: connected=$isNetworkConnected")
        if (!isNetworkConnected) {
            snackbarMessage = "No internet connected"
            snackbarColor = Color.Red
        }
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SampleAppUI(
                        isInitialized = isInitialized,
                        currentPrompt = currentPrompt,
                        onPromptChange = { currentPrompt = it },
                        generatedText = generatedText,
                        streamingText = streamingText,
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        statusMessage = statusMessage,
                        useStreaming = useStreaming,
                        onStreamingToggle = { useStreaming = it },
                        availableModels = availableModels,
                        onInitialize = { model -> initializeEngine(model) },
                        onGenerate = { generateText() },
                        onClear = { 
                            generatedText = ""
                            streamingText = ""
                        },
                        snackbarMessage = snackbarMessage,
                        snackbarColor = snackbarColor
                    )
                }
            }
        }
        
        // Fetch available models
        lifecycleScope.launch {
            availableModels = modelManager.fetchRegistry()
        }
    }

    private fun initializeEngine(model: Model) {
        isLoading = true
        errorMessage = ""
        statusMessage = "Preparing model..."
        
        Log.i(TAG_INIT, "initializeEngine called for model: ${model.id}")
        Log.i(TAG_INIT, "Model name: ${model.name}")
        Log.d(TAG_INIT, "Model URL: ${model.downloadUrl}")
        Log.d(TAG_INIT, "Model size: ${formatSize(model.size)} bytes")

        lifecycleScope.launch {
            try {
                val localPath = withContext(Dispatchers.IO) {
                    val existing = File(filesDir, "models/${model.id}.gguf")
                    Log.d(TAG_DOWNLOAD, "Checking for existing model at: ${existing.absolutePath}")
                    Log.d(TAG_DOWNLOAD, "File exists: ${existing.isFile}, size: ${existing.length()}")
                    
                    if (existing.isFile && existing.length() > 1_000_000L) {
                        Log.i(TAG_DOWNLOAD, "Using existing model file (${existing.length()} bytes)")
                        existing.absolutePath
                    } else {
                        Log.i(TAG_DOWNLOAD, "Model not found or too small, starting download")
                        try {
                            val downloadedPath = modelManager.download(model) { progress ->
                                Log.d(TAG_DOWNLOAD, "Download progress: ${(progress * 100f).toInt()}%")
                                runOnUiThread {
                                    statusMessage =
                                        "Downloading model… ${(progress * 100f).toInt().coerceIn(0, 100)}%"
                                }
                            }
                            Log.i(TAG_DOWNLOAD, "Download completed, path: $downloadedPath")
                            downloadedPath
                        } catch (e: Exception) {
                            Log.e(TAG_DOWNLOAD, "Download exception", e)
                            throw e
                        }
                    }
                }

                Log.i(TAG_INIT, "Model path resolved to: $localPath")
                statusMessage = "Initializing Axiom AI engine..."

                val config = LLMConfig(
                    modelPath = localPath,
                    contextSize = 1024,
                    temperature = 0.7f,
                    topK = 40,
                    topP = 0.95f,
                    repeatPenalty = 1.0f,
                    maxTokens = 512
                )

                Log.i(TAG_INIT, "Initializing engine with config: contextSize=${config.contextSize}, temperature=${config.temperature}")
                val success = withContext(Dispatchers.IO) {
                    engine.init(config)
                }
                
                Log.i(TAG_INIT, "Engine init result: $success")
                
                if (success) {
                    isInitialized = true
                    statusMessage = "Axiom AI Ready - Model loaded successfully"
                    Log.i(TAG_INIT, "Engine initialized successfully")
                } else {
                    statusMessage = "Initialization failed"
                    errorMessage = "Failed to initialize Axiom AI engine"
                    Log.e(TAG_INIT, "Engine initialization failed")
                }
            } catch (e: Exception) {
                statusMessage = "Error during initialization"
                errorMessage = "Error: ${e.message}"
                Log.e(TAG_INIT, "Error during initialization", e)
            } finally {
                isLoading = false
            }
        }
    }

    private fun generateText() {
        if (!isInitialized || currentPrompt.isBlank()) {
            Log.w(TAG_GENERATION, "Cannot generate: isInitialized=$isInitialized, promptBlank=${currentPrompt.isBlank()}")
            return
        }
        
        isLoading = true
        errorMessage = ""
        statusMessage = "Generating..."
        generatedText = ""
        streamingText = ""
        
        Log.i(TAG_GENERATION, "Starting text generation (streaming=$useStreaming)")
        Log.d(TAG_GENERATION, "Prompt length: ${currentPrompt.length} chars")
        
        lifecycleScope.launch {
            try {
                if (useStreaming) {
                    Log.i(TAG_GENERATION, "Using streaming generation")
                    var tokenCount = 0
                    engine.stream(currentPrompt) { token ->
                        streamingText += token
                        tokenCount++
                        if (tokenCount % 10 == 0) {
                            Log.d(TAG_GENERATION, "Streamed $tokenCount tokens")
                        }
                    }
                    generatedText = streamingText
                    statusMessage = "Streaming complete"
                    Log.i(TAG_GENERATION, "Streaming complete: $tokenCount tokens generated")
                } else {
                    Log.i(TAG_GENERATION, "Using non-streaming generation")
                    val result = withContext(Dispatchers.IO) {
                        engine.generate(currentPrompt)
                    }
                    generatedText = result
                    statusMessage = "Generation complete"
                    Log.i(TAG_GENERATION, "Generation complete: ${result.length} chars generated")
                }
            } catch (e: Exception) {
                statusMessage = "Error during generation"
                errorMessage = "Error: ${e.message}"
                Log.e(TAG_GENERATION, "Error during generation", e)
            } finally {
                isLoading = false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "MainActivity onDestroy called")
        if (isInitialized) {
            Log.i(TAG_INIT, "Cleaning up engine resources")
            engine.cleanup()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleAppUI(
    isInitialized: Boolean,
    currentPrompt: String,
    onPromptChange: (String) -> Unit,
    generatedText: String,
    streamingText: String,
    isLoading: Boolean,
    errorMessage: String,
    statusMessage: String,
    useStreaming: Boolean,
    onStreamingToggle: (Boolean) -> Unit,
    availableModels: List<Model>,
    onInitialize: (Model) -> Unit,
    onGenerate: () -> Unit,
    onClear: () -> Unit,
    snackbarMessage: String?,
    snackbarColor: androidx.compose.ui.graphics.Color
) {
    var selectedModel by remember { mutableStateOf<Model?>(null) }
    var showModelDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Axiom AI SDK Sample") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = snackbarColor,
                    contentColor = if (snackbarColor == MaterialTheme.colorScheme.error) 
                        MaterialTheme.colorScheme.onErrorContainer 
                    else 
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        errorMessage.isNotEmpty() -> MaterialTheme.colorScheme.errorContainer
                        isInitialized -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Text(
                    text = statusMessage,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Error Message
            if (errorMessage.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Model Selection
            if (!isInitialized) {
                Button(
                    onClick = { showModelDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = availableModels.isNotEmpty()
                ) {
                    Text("Select Model")
                }
                
                if (selectedModel != null) {
                    Text(
                        text = "Selected: ${selectedModel!!.name}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = { onInitialize(selectedModel!!) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Initialize Engine")
                        }
                    }
                }
            }

            // Streaming Toggle
            if (isInitialized) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Use Streaming")
                    Switch(
                        checked = useStreaming,
                        onCheckedChange = onStreamingToggle
                    )
                }
            }

            // Prompt Input
            if (isInitialized) {
                OutlinedTextField(
                    value = currentPrompt,
                    onValueChange = onPromptChange,
                    label = { Text("Enter your prompt") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    enabled = !isLoading
                )

                // Generate Button
                Button(
                    onClick = onGenerate,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = currentPrompt.isNotBlank() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(if (useStreaming) "Stream Text" else "Generate Text")
                    }
                }

                // Generated Output
                if (generatedText.isNotEmpty() || streamingText.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (useStreaming) "Streaming Output" else "Generated Output",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                TextButton(onClick = onClear) {
                                    Text("Clear")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (useStreaming) streamingText else generatedText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Axiom AI SDK Features:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "• Modular architecture (core, llama-cpp, models)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "• Streaming text generation",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "• Model download manager",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "• Configurable generation parameters",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
    
    if (showModelDialog) {
        AlertDialog(
            onDismissRequest = { showModelDialog = false },
            title = { Text("Select Model") },
            text = {
                Column {
                    availableModels.forEach { model ->
                        Button(
                            onClick = {
                                selectedModel = model
                                showModelDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${model.name} (${model.size / (1024 * 1024)}MB)")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModelDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun formatSize(bytes: Long): String {
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024

    return when {
        bytes >= gb -> String.format("%.2f GB", bytes / gb)
        bytes >= mb -> String.format("%.2f MB", bytes / mb)
        bytes >= kb -> String.format("%.2f KB", bytes / kb)
        else -> "$bytes bytes"
    }
}