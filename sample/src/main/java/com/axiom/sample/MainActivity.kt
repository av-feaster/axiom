package com.axiom.sample

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.axiom.core.LLMConfig
import com.axiom.core.LLMEngine
import com.axiom.core.Model
import com.axiom.core.ModelManager
import com.axiom.llama.cpp.LlamaCppEngine
import com.axiom.models.DefaultModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val engine: LLMEngine = LlamaCppEngine()
    private val modelManager: ModelManager = DefaultModelManager(this)
    
    private var isInitialized = false
    private var currentPrompt by mutableStateOf("")
    private var generatedText by mutableStateOf("")
    private var streamingText by mutableStateOf("")
    private var isLoading by mutableStateOf(false)
    private var errorMessage by mutableStateOf("")
    private var statusMessage by mutableStateOf("Axiom AI SDK Sample")
    private var useStreaming by mutableStateOf(false)
    private var availableModels by mutableStateOf<List<Model>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                        onInitialize = { initializeEngine(it) },
                        onGenerate = { generateText() },
                        onClear = { 
                            generatedText = ""
                            streamingText = ""
                        }
                    )
                }
            }
        }
        
        // Fetch available models
        lifecycleScope.launch {
            availableModels = modelManager.fetchRegistry()
        }
    }

    private fun initializeEngine(modelPath: String) {
        isLoading = true
        errorMessage = ""
        statusMessage = "Initializing Axiom AI engine..."
        
        lifecycleScope.launch {
            try {
                val config = LLMConfig(
                    modelPath = modelPath,
                    contextSize = 1024,
                    temperature = 0.7f,
                    topK = 40,
                    topP = 0.95f,
                    repeatPenalty = 1.0f,
                    maxTokens = 512
                )
                
                val success = withContext(Dispatchers.IO) {
                    engine.init(config)
                }
                
                if (success) {
                    isInitialized = true
                    statusMessage = "Axiom AI Ready - Model loaded successfully"
                } else {
                    statusMessage = "Initialization failed"
                    errorMessage = "Failed to initialize Axiom AI engine"
                }
            } catch (e: Exception) {
                statusMessage = "Error during initialization"
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    private fun generateText() {
        if (!isInitialized || currentPrompt.isBlank()) return
        
        isLoading = true
        errorMessage = ""
        statusMessage = "Generating..."
        generatedText = ""
        streamingText = ""
        
        lifecycleScope.launch {
            try {
                if (useStreaming) {
                    engine.stream(currentPrompt) { token ->
                        streamingText += token
                    }
                    generatedText = streamingText
                    statusMessage = "Streaming complete"
                } else {
                    val result = withContext(Dispatchers.IO) {
                        engine.generate(currentPrompt)
                    }
                    generatedText = result
                    statusMessage = "Generation complete"
                }
            } catch (e: Exception) {
                statusMessage = "Error during generation"
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isInitialized) {
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
    onInitialize: (String) -> Unit,
    onGenerate: () -> Unit,
    onClear: () -> Unit
) {
    var selectedModel by remember { mutableStateOf<String?>(null) }
    var showModelDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Axiom AI SDK Sample") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
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
                                selectedModel = model.downloadUrl // In real app, this would be local path
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
