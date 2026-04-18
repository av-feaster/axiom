package com.axiom.android.sdk.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.axiom.android.sdk.AxiomSDK
import com.axiom.android.sdk.engine.AxiomEngine
import kotlinx.coroutines.launch

private const val TAG = "ChatScreen"
private const val SYSTEM_PROMPT = "You are a helpful AI assistant. Always respond in English. Be concise, accurate, and friendly. Provide clear and helpful responses.\n\n"

/**
 * Chat Screen - streaming text generation interface
 * Uses AxiomSDK.getEngine() for text generation
 */
@Composable
fun ChatScreen(
    modelId: String
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val engine = AxiomSDK.getEngine()
    val scope = rememberCoroutineScope()
    
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var currentInput by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    
    // Auto-scroll when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val windowInsets = WindowInsets.systemBars
    val imeInsets = WindowInsets.ime

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(windowInsets)
    ) {
        // Messages list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                ChatMessageItem(message)
            }
        }

        // Input field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .windowInsetsPadding(imeInsets),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = currentInput,
                onValueChange = { currentInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Enter your message...") },
                enabled = !isGenerating
            )
            
            if (isGenerating) {
                Button(onClick = { engine.cancel() }) {
                    Text("Stop")
                }
            } else {
                Button(
                    onClick = {
                        if (currentInput.isNotBlank()) {
                            isGenerating = true
                            val userMessage = ChatMessage(
                                role = "user",
                                content = currentInput
                            )
                            messages = messages + userMessage
                            val input = currentInput
                            currentInput = ""
                            
                            scope.launch {
                                try {
                                    var assistantResponse = ""
                                    // Prepend system prompt to user input
                                    val fullPrompt = "$SYSTEM_PROMPT$input"
                                    android.util.Log.d("ChatScreen", "Starting generation for model: $modelId")
                                    android.util.Log.d("ChatScreen", "Full prompt: $fullPrompt")
                                    android.util.Log.d("ChatScreen", "Engine class: ${engine::class.java.simpleName}")

                                    // Check if model file exists
                                    val modelPath = "${context.filesDir.absolutePath}/models/$modelId.gguf"
                                    val modelFile = java.io.File(modelPath)
                                    android.util.Log.d("ChatScreen", "Model path: $modelPath")
                                    android.util.Log.d("ChatScreen", "Model file exists: ${modelFile.exists()}, size: ${modelFile.length()} bytes")

                                    var tokenCount = 0
                                    var lastTokenTime = System.currentTimeMillis()
                                    val timeoutMs = 30000L // 30 second timeout

                                    engine.generate(fullPrompt).collect { token ->
                                        val currentTime = System.currentTimeMillis()
                                        val timeSinceLastToken = currentTime - lastTokenTime
                                        lastTokenTime = currentTime

                                        if (timeSinceLastToken > 5000) {
                                            android.util.Log.w("ChatScreen", "Long gap between tokens: ${timeSinceLastToken}ms")
                                        }

                                        tokenCount++
                                        android.util.Log.d("ChatScreen", "Streamed token #$tokenCount: \"$token\"")
                                        assistantResponse += token
                                        // Update last message or add new one
                                        messages = if (messages.lastOrNull()?.role == "assistant") {
                                            messages.dropLast(1) + ChatMessage(
                                                role = "assistant",
                                                content = assistantResponse
                                            )
                                        } else {
                                            messages + ChatMessage(
                                                role = "assistant",
                                                content = assistantResponse
                                            )
                                        }
                                    }
                                    android.util.Log.d("ChatScreen", "Generation complete. Total tokens: $tokenCount, response length: ${assistantResponse.length}")
                                } catch (e: Exception) {
                                    android.util.Log.e("ChatScreen", "Generation failed", e)
                                    messages = messages + ChatMessage(
                                        role = "assistant",
                                        content = "Error: ${e.message}"
                                    )
                                } finally {
                                    isGenerating = false
                                }
                            }
                        }
                    },
                    enabled = currentInput.isNotBlank()
                ) {
                    Text("Send")
                }
            }
        }
    }
}

/**
 * Chat message item component
 */
@Composable
fun ChatMessageItem(message: ChatMessage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (message.role == "user") {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Text(
            text = message.content,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Data class for chat messages
 */
data class ChatMessage(
    val role: String, // "user" or "assistant"
    val content: String
)
