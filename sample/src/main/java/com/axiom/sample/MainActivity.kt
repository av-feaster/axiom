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
import com.toonandtools.axiom.llama.LlamaEngine
import com.toonandtools.axiom.llama.ModelLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var isInitialized = false
    private var currentPrompt by mutableStateOf("")
    private var generatedText by mutableStateOf("")
    private var isLoading by mutableStateOf(false)
    private var errorMessage by mutableStateOf("")
    private var statusMessage by mutableStateOf("Not initialized")

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
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        statusMessage = statusMessage,
                        onInitialize = { initializeEngine() },
                        onGenerate = { generateText() },
                        onClear = { generatedText = "" }
                    )
                }
            }
        }
    }

    private fun initializeEngine() {
        isLoading = true
        errorMessage = ""
        statusMessage = "Initializing..."
        
        lifecycleScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    // Copy model from assets if it exists
                    val modelPath = try {
                        ModelLoader.copyModelToFilesDir(this@MainActivity, "tinyllama.gguf")
                    } catch (e: Exception) {
                        statusMessage = "Model file not found in assets. Please add a GGUF model to sample/src/main/assets/models/"
                        errorMessage = "Model file not found: ${e.message}"
                        isLoading = false
                        return@withContext false
                    }
                    
                    LlamaEngine.init(this@MainActivity, modelPath.absolutePath)
                }
                
                if (success) {
                    isInitialized = true
                    statusMessage = "Ready - Model loaded successfully"
                } else {
                    statusMessage = "Initialization failed"
                    errorMessage = "Failed to initialize llama.cpp engine"
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
        
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    LlamaEngine.generate(currentPrompt)
                }
                generatedText = result
                statusMessage = "Generation complete"
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
            LlamaEngine.cleanup()
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
    isLoading: Boolean,
    errorMessage: String,
    statusMessage: String,
    onInitialize: () -> Unit,
    onGenerate: () -> Unit,
    onClear: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Llama.cpp SDK Sample") },
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

            // Initialize Button
            Button(
                onClick = onInitialize,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isInitialized && !isLoading
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
                        Text("Generate Text")
                    }
                }

                // Generated Output
                if (generatedText.isNotEmpty()) {
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
                                    "Generated Output",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                TextButton(onClick = onClear) {
                                    Text("Clear")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = generatedText,
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
                        "Instructions:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "1. Add a GGUF model file to sample/src/main/assets/models/",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "2. Rename it to 'tinyllama.gguf' or update the code",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "3. Click 'Initialize Engine' to load the model",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "4. Enter a prompt and click 'Generate Text'",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
