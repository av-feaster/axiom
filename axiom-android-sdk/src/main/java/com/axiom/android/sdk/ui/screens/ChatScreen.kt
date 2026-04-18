package com.axiom.android.sdk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.axiom.android.sdk.AxiomSDK
import com.axiom.android.sdk.engine.AxiomEngine
import kotlinx.coroutines.launch

private const val TAG = "ChatScreen"

/** Set false to silence [ChatLayoutLog] lines (filter Logcat: `ChatLayout`). */
private const val CHAT_LAYOUT_DEBUG = true

private const val SYSTEM_PROMPT = "You are a helpful AI assistant. Always respond in English. Be concise, accurate, and friendly. Provide clear and helpful responses.\n\n"

/**
 * Chat Screen - streaming text generation interface
 * Uses AxiomSDK.getEngine() for text generation
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    // System instruction (hidden from UI, included in prompt)
    val systemInstruction = "You are a helpful AI assistant. Always respond in English. Be concise, accurate, and friendly. Provide clear and helpful responses."
    
    // Auto-scroll when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Without a bounded max height, Column's weight(1f) on LazyColumn is ignored → list collapses
        // and the composer sits under the top bar with a huge blank gap above the keyboard.
        val columnHeightDp = with(density) {
            if (constraints.hasBoundedHeight) {
                constraints.maxHeight.toDp()
            } else {
                configuration.screenHeightDp.dp
            }
        }

        if (CHAT_LAYOUT_DEBUG) {
            val imeBottomPx = WindowInsets.ime.getBottom(density)
            val navBottomPx = WindowInsets.navigationBars.getBottom(density)
            LaunchedEffect(
                constraints.hasBoundedHeight,
                constraints.maxHeight,
                constraints.maxWidth,
                columnHeightDp,
                imeBottomPx,
                navBottomPx,
            ) {
                android.util.Log.i(
                    ChatLayoutLog.TAG,
                    "constraints hasBoundedH=${constraints.hasBoundedHeight} " +
                        "maxW=${constraints.maxWidth}px maxH=${constraints.maxHeight}px " +
                        "columnHeightDp=${columnHeightDp.value} " +
                        "imeBottomPx=$imeBottomPx navBottomPx=$navBottomPx " +
                        "screenHeightDp=${configuration.screenHeightDp}"
                )
            }
        }

        // Root: nav bars only. IME on composer only — avoids a tall empty band when the window does not
        // resize (VRI handleResized abandoned) while imeBottomPx still grows to full keyboard height.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (constraints.hasBoundedHeight) {
                        Modifier.fillMaxHeight()
                    } else {
                        Modifier.height(columnHeightDp)
                    },
                )
                .then(
                    if (CHAT_LAYOUT_DEBUG) {
                        Modifier.onGloballyPositioned { coords ->
                            ChatLayoutLog.layoutLine(
                                "ChatColumn",
                                coords,
                                "hasBoundedH=${constraints.hasBoundedHeight} maxH=${constraints.maxHeight}",
                            )
                        }
                    } else {
                        Modifier
                    },
                )
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
        // TopAppBar with model name
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = modelId,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        Spacer(modifier = Modifier.height(8.dp))
        // Messages list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(
                    if (CHAT_LAYOUT_DEBUG) {
                        Modifier.onGloballyPositioned { coords ->
                            ChatLayoutLog.layoutLine("LazyColumn", coords)
                        }
                    } else {
                        Modifier
                    },
                ),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(messages) { message ->
                ChatMessageItem(
                    message = message,
                    onRegenerate = {
                        // TODO: Implement regenerate functionality
                    },
                    onCopy = {
                        // Copy message to clipboard
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clipData = android.content.ClipData.newPlainText("Message", message.content)
                        clipboard.setPrimaryClip(clipData)
                    }
                )
            }

            // Synthesizing indicator
            if (isGenerating) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Synthesizing...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.3f)
                                        .height(4.dp)
                                        .background(MaterialTheme.colorScheme.tertiary)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Composer: IME padding here lifts the field above the keyboard without hollowing the list area.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(16.dp)
                .then(
                    if (CHAT_LAYOUT_DEBUG) {
                        Modifier.onGloballyPositioned { coords ->
                            ChatLayoutLog.layoutLine("InputColumn", coords)
                        }
                    } else {
                        Modifier
                    },
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Quick action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { messages = emptyList() },
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "Reset Context",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Input field with glassmorphism
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f)
                ),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = currentInput,
                        onValueChange = { currentInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter your message...") },
                        enabled = !isGenerating
                    )

                    if (isGenerating) {
                        Button(
                            onClick = { engine.cancel() },
                            modifier = Modifier.size(48.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
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
                                            val generationStartTime = System.currentTimeMillis()

                                            // Build conversation history prompt with system instruction
                                            val conversationHistory = StringBuilder()
                                            conversationHistory.append("System: $systemInstruction\n\n")

                                            // Add previous messages to context (last 5 for context window)
                                            messages.takeLast(5).forEach { message ->
                                                when (message.role) {
                                                    "user" -> conversationHistory.append("User: ${message.content}\n")
                                                    "assistant" -> conversationHistory.append("Assistant: ${message.content}\n")
                                                }
                                            }

                                            // Add current user message
                                            conversationHistory.append("User: $input\n")
                                            conversationHistory.append("Assistant:")

                                            val fullPrompt = conversationHistory.toString()
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
                                                        content = assistantResponse,
                                                        tokenCount = tokenCount,
                                                        generationTimeMs = currentTime - generationStartTime
                                                    )
                                                } else {
                                                    messages + ChatMessage(
                                                        role = "assistant",
                                                        content = assistantResponse,
                                                        tokenCount = tokenCount,
                                                        generationTimeMs = currentTime - generationStartTime
                                                    )
                                                }
                                            }
                                            val generationTimeMs = System.currentTimeMillis() - generationStartTime
                                            android.util.Log.d("ChatScreen", "Generation complete. Total tokens: $tokenCount, response length: ${assistantResponse.length}, time: ${generationTimeMs}ms")

                                            // Update final message with complete metadata
                                            messages = if (messages.lastOrNull()?.role == "assistant") {
                                                messages.dropLast(1) + ChatMessage(
                                                    role = "assistant",
                                                    content = assistantResponse,
                                                    tokenCount = tokenCount,
                                                    generationTimeMs = generationTimeMs
                                                )
                                            } else {
                                                messages
                                            }
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
                            enabled = currentInput.isNotBlank(),
                            modifier = Modifier.size(48.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send"
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

/**
 * Chat message item component
 */
@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onRegenerate: () -> Unit = {},
    onCopy: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Label row
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (message.role == "assistant") {
                Text(
                    text = "System Response",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "User Input",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Message card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (message.role == "user") {
                        Modifier.padding(start = 32.dp)
                    } else {
                        Modifier
                    }
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.role == "user") {
                    MaterialTheme.colorScheme.surfaceContainerLow
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Metadata for assistant messages
                if (message.role == "assistant" && (message.tokenCount > 0 || message.generationTimeMs > 0)) {
                    Divider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (message.tokenCount > 0) {
                            Column {
                                Text(
                                    text = "Tokens",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${message.tokenCount}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (message.generationTimeMs > 0) {
                            Column {
                                Text(
                                    text = "Time",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${message.generationTimeMs / 1000}s",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action buttons for assistant messages
        if (message.role == "assistant") {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextButton(onClick = onCopy) {
                    Text(
                        text = "Copy",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                TextButton(onClick = onRegenerate) {
                    Text(
                        text = "Regenerate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Data class for chat messages
 */
data class ChatMessage(
    val role: String, // "user" or "assistant"
    val content: String,
    val tokenCount: Int = 0,
    val generationTimeMs: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)
