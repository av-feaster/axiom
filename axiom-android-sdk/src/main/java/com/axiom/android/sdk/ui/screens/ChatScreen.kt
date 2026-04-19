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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.axiom.android.sdk.AxiomSDK
import com.axiom.android.sdk.engine.AxiomEngine
import com.axiom.android.sdk.data.entity.ChatMessageEntity
import com.axiom.android.sdk.data.repository.ChatSessionRepository
import com.axiom.core.ChatMode
import com.axiom.core.ContextBuilder
import com.axiom.core.ContextParams
import com.axiom.core.LLMMessage
import com.axiom.core.StreamingSafeguard
import com.axiom.android.sdk.ui.components.chat.ChatMessage
import com.axiom.android.sdk.ui.components.chat.ChatMessageItem
import com.axiom.android.sdk.ui.screens.EditMessageDialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween

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
    modelId: String,
    sessionId: Long? = null,
    repository: ChatSessionRepository? = null,
    onNavigateBack: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val engine = AxiomSDK.getEngine()
    val scope = rememberCoroutineScope()
    
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var currentInput by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf(ChatMode.GENERAL) }
    var editingMessageIndex by remember { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()
    
    // Lumina Neural Design System - Clean, minimal interface
    val surfaceColor = MaterialTheme.colorScheme.surface
    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    
    // Load messages from database if sessionId is provided
    LaunchedEffect(sessionId, repository) {
        if (sessionId != null && repository != null) {
            repository.getMessagesForSession(sessionId).collect { messageEntities ->
                messages = messageEntities.map { entity ->
                    ChatMessage(
                        role = entity.role,
                        content = entity.content,
                        tokenCount = entity.tokenCount,
                        generationTimeMs = entity.generationTimeMs,
                        isStreaming = entity.isStreaming
                    )
                }
            }
        }
    }

    // System instruction (hidden from UI, included in prompt)
    val systemInstruction = selectedMode.systemPrompt
    
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
                    Text(
                        text = modelId,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
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
                itemsIndexed(
                    items = messages,
                    key = { index, _ -> index },
                ) { index, message ->
                    ChatMessageItem(
                        message = message,
                        onRegenerate = {
                            if (message.role == "assistant" && !isGenerating) {
                                // Regenerate this assistant message
                                val messagesBefore = messages.take(index)
                                val lastUserMessage = messagesBefore.lastOrNull { it.role == "user" }
                                
                                if (lastUserMessage != null) {
                                    isGenerating = true
                                    scope.launch {
                                        try {
                                            var assistantResponse = ""
                                            val generationStartTime = System.currentTimeMillis()
                                            
                                            // Build conversation history using ContextBuilder
                                            val historyMessages = messagesBefore.map { message ->
                                                when (message.role) {
                                                    "user" -> LLMMessage(LLMMessage.MessageRole.USER, message.content)
                                                    "assistant" -> LLMMessage(LLMMessage.MessageRole.ASSISTANT, message.content)
                                                    else -> LLMMessage(LLMMessage.MessageRole.USER, message.content)
                                                }
                                            }

                                            val contextParams = ContextParams(
                                                systemPrompt = systemInstruction,
                                                pinnedFacts = "",
                                                userProfile = "",
                                                crossSessionMemories = emptyList(),
                                                inSessionSummaries = emptyList(),
                                                history = historyMessages,
                                                currentMessage = LLMMessage(LLMMessage.MessageRole.USER, lastUserMessage.content)
                                            )

                                            val llmMessages = ContextBuilder.buildContextMessages(contextParams)

                                            // Convert LLMMessage[] to string prompt for engine
                                            val fullPrompt = llmMessages.joinToString("\n") { 
                                                "${it.role.name}: ${it.content}" 
                                            }

                                            android.util.Log.d("ChatScreen", "Regenerating message with context messages count: ${llmMessages.size}")
                                            
                                            var tokenCount = 0
                                            engine.generate(fullPrompt).collect { token ->
                                                tokenCount++
                                                assistantResponse += token
                                                
                                                // Update UI with regenerated response
                                                messages = messages.toMutableList().apply {
                                                    this[index] = ChatMessage(
                                                        role = "assistant",
                                                        content = assistantResponse,
                                                        tokenCount = tokenCount,
                                                        generationTimeMs = System.currentTimeMillis() - generationStartTime,
                                                        isStreaming = true,
                                                    )
                                                }
                                            }
                                            
                                            // Final update
                                            messages = messages.toMutableList().apply {
                                                this[index] = ChatMessage(
                                                    role = "assistant",
                                                    content = assistantResponse,
                                                    tokenCount = tokenCount,
                                                    generationTimeMs = System.currentTimeMillis() - generationStartTime,
                                                    isStreaming = false,
                                                )
                                            }
                                            
                                            // Save regenerated message to database if session is active
                                            if (sessionId != null && repository != null) {
                                                repository.addMessage(sessionId, "assistant", assistantResponse)
                                                repository.updateSessionTimestamp(sessionId)
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("ChatScreen", "Regeneration failed", e)
                                        } finally {
                                            isGenerating = false
                                        }
                                    }
                                }
                            }
                        },
                        onEdit = {
                            if (message.role == "user" && !isGenerating) {
                                editingMessageIndex = index
                            }
                        },
                        onCopy = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clipData = android.content.ClipData.newPlainText("Message", message.content)
                            clipboard.setPrimaryClip(clipData)
                        },
                    )
                }
                
                // Pre–first-token: cycling thinking copy + indeterminate linear bar
                item(key = "thinking_panel") {
                    ThinkingGenerationPanel(
                        active = isGenerating && messages.lastOrNull()?.role != "assistant",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // Composer: IME padding here lifts the field above the keyboard without hollowing the list area.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Quick action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { messages = emptyList() },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Reset",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // Input field with glassmorphism
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
                tonalElevation = 2.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = currentInput,
                        onValueChange = { currentInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter your message...") },
                        enabled = !isGenerating,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        singleLine = true
                    )

                    if (isGenerating) {
                        IconButton(
                            onClick = { engine.cancel() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Stop",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                if (currentInput.isNotBlank()) {
                                    isGenerating = true
                                    val userMessage = ChatMessage(
                                        role = "user",
                                        content = currentInput
                                    )
                                    messages = messages + userMessage
                                    
                                    // Save user message to database if session is active
                                    if (sessionId != null && repository != null) {
                                        scope.launch {
                                            repository.addMessage(sessionId, "user", currentInput)
                                            repository.updateSessionTimestamp(sessionId)
                                        }
                                    }
                                    
                                    val input = currentInput
                                    currentInput = ""

                                    scope.launch {
                                        try {
                                            var assistantResponse = ""
                                            val generationStartTime = System.currentTimeMillis()

                                            // Build conversation history using ContextBuilder
                                            val historyMessages = messages.dropLast(1).takeLast(10).map { message ->
                                                when (message.role) {
                                                    "user" -> LLMMessage(LLMMessage.MessageRole.USER, message.content)
                                                    "assistant" -> LLMMessage(LLMMessage.MessageRole.ASSISTANT, message.content)
                                                    else -> LLMMessage(LLMMessage.MessageRole.USER, message.content)
                                                }
                                            }

                                            val currentMessage = LLMMessage(LLMMessage.MessageRole.USER, input)

                                            val contextParams = ContextParams(
                                                systemPrompt = systemInstruction,
                                                pinnedFacts = "",
                                                userProfile = "",
                                                crossSessionMemories = emptyList(),
                                                inSessionSummaries = emptyList(),
                                                history = historyMessages,
                                                currentMessage = currentMessage
                                            )

                                            val llmMessages = ContextBuilder.buildContextMessages(contextParams)

                                            // Convert LLMMessage[] to string prompt for engine
                                            val fullPrompt = llmMessages.joinToString("\n") { 
                                                "${it.role.name}: ${it.content}" 
                                            }

                                            android.util.Log.d("ChatScreen", "Starting generation for model: $modelId")
                                            android.util.Log.d("ChatScreen", "Context messages count: ${llmMessages.size}")
                                            android.util.Log.d("ChatScreen", "Engine class: ${engine::class.java.simpleName}")

                                            // Check if model file exists
                                            val modelPath = "${context.filesDir.absolutePath}/models/$modelId.gguf"
                                            val modelFile = java.io.File(modelPath)
                                            android.util.Log.d("ChatScreen", "Model path: $modelPath")
                                            android.util.Log.d("ChatScreen", "Model file exists: ${modelFile.exists()}, size: ${modelFile.length()} bytes")

                                            var tokenCount = 0
                                            var lastTokenTime = System.currentTimeMillis()
                                            val timeoutMs = 30000L // 30 second timeout
                                            var stoppedOnChatEcho = false
                                            var tokenBuffer = "" // Buffer for suspicious tokens
                                            var bufferIsSuspicious = false // Flag to track if buffer contains suspicious tokens

                                            try {
                                                engine.generate(fullPrompt).collect { token ->
                                                    val currentTime = System.currentTimeMillis()
                                                    val timeSinceLastToken = currentTime - lastTokenTime
                                                    lastTokenTime = currentTime

                                                    if (timeSinceLastToken > 5000) {
                                                        android.util.Log.w("ChatScreen", "Long gap between tokens: ${timeSinceLastToken}ms")
                                                    }

                                                    tokenCount++
                                                    android.util.Log.d("ChatScreen", "Streamed token #$tokenCount: \"$token\"")

                                                    // Clean token
                                                    val tokenClean = token.replace("\uFFFD", "")

                                                    // Check if token matches stop marker prefix (suspicious token)
                                                    val isPrefix = StreamingSafeguard.matchesStopMarkerPrefix(tokenClean.trim())

                                                    if (isPrefix) {
                                                        // Buffer suspicious tokens - don't render yet
                                                        android.util.Log.d(TAG, "Buffering suspicious token: '$tokenClean'")
                                                        tokenBuffer += tokenClean
                                                        bufferIsSuspicious = true
                                                    } else if (bufferIsSuspicious) {
                                                        // We have a buffered token, check if buffer + current token triggers stop condition
                                                        val testResponse = assistantResponse + tokenBuffer + tokenClean

                                                        // Check for chat echo
                                                        if (StreamingSafeguard.shouldStopOnChatEcho(testResponse)) {
                                                            android.util.Log.i(TAG, "Buffered token + current token triggers stop condition, discarding buffer")
                                                            stoppedOnChatEcho = true
                                                            engine.cancel()
                                                            throw CancellationException("Stopped on chat echo")
                                                        }

                                                        // Check for garbage characters
                                                        if (StreamingSafeguard.shouldStopOnGarbage(testResponse)) {
                                                            android.util.Log.i(TAG, "Detected garbage characters with buffered token, cancelling stream")
                                                            stoppedOnChatEcho = true
                                                            engine.cancel()
                                                            throw CancellationException("Stopped on garbage characters")
                                                        }

                                                        // Check for repetition
                                                        if (StreamingSafeguard.shouldStopOnRepetition(testResponse)) {
                                                            android.util.Log.i(TAG, "Detected repetition with buffered token, cancelling stream")
                                                            stoppedOnChatEcho = true
                                                            engine.cancel()
                                                            throw CancellationException("Stopped on character repetition")
                                                        }

                                                        // Safe to render buffer + current token
                                                        android.util.Log.d(TAG, "Flushing buffer: '$tokenBuffer' + '$tokenClean'")
                                                        assistantResponse += tokenBuffer + tokenClean
                                                        tokenBuffer = ""
                                                        bufferIsSuspicious = false

                                                        // Update UI
                                                        messages = if (messages.lastOrNull()?.role == "assistant") {
                                                            messages.dropLast(1) + ChatMessage(
                                                                role = "assistant",
                                                                content = assistantResponse,
                                                                tokenCount = tokenCount,
                                                                generationTimeMs = currentTime - generationStartTime,
                                                                isStreaming = true,
                                                            )
                                                        } else {
                                                            messages + ChatMessage(
                                                                role = "assistant",
                                                                content = assistantResponse,
                                                                tokenCount = tokenCount,
                                                                generationTimeMs = currentTime - generationStartTime,
                                                                isStreaming = true,
                                                            )
                                                        }
                                                    } else {
                                                        // Normal token, check stop conditions and render directly
                                                        val testResponse = assistantResponse + tokenClean

                                                        // Check for chat echo
                                                        if (StreamingSafeguard.shouldStopOnChatEcho(testResponse)) {
                                                            android.util.Log.i(TAG, "Detected chat echo, cancelling stream")
                                                            stoppedOnChatEcho = true
                                                            engine.cancel()
                                                            throw CancellationException("Stopped on chat echo")
                                                        }

                                                        // Check for garbage characters
                                                        if (StreamingSafeguard.shouldStopOnGarbage(testResponse)) {
                                                            android.util.Log.i(TAG, "Detected garbage characters, cancelling stream")
                                                            stoppedOnChatEcho = true
                                                            engine.cancel()
                                                            throw CancellationException("Stopped on garbage characters")
                                                        }

                                                        // Check for repetition
                                                        if (StreamingSafeguard.shouldStopOnRepetition(testResponse)) {
                                                            android.util.Log.i(TAG, "Detected character repetition, cancelling stream")
                                                            stoppedOnChatEcho = true
                                                            engine.cancel()
                                                            throw CancellationException("Stopped on character repetition")
                                                        }

                                                        // Safe to add token directly
                                                        assistantResponse += tokenClean

                                                        // Update UI
                                                        messages = if (messages.lastOrNull()?.role == "assistant") {
                                                            messages.dropLast(1) + ChatMessage(
                                                                role = "assistant",
                                                                content = assistantResponse,
                                                                tokenCount = tokenCount,
                                                                generationTimeMs = currentTime - generationStartTime,
                                                                isStreaming = true,
                                                            )
                                                        } else {
                                                            messages + ChatMessage(
                                                                role = "assistant",
                                                                content = assistantResponse,
                                                                tokenCount = tokenCount,
                                                                generationTimeMs = currentTime - generationStartTime,
                                                                isStreaming = true,
                                                            )
                                                        }
                                                    }
                                                }
                                            } catch (_: CancellationException) {
                                                // User pressed stop or engine cancelled after chat-echo cutoff
                                                if (stoppedOnChatEcho) {
                                                    // Trim response to remove any unwanted content
                                                    val trimmedResponse = StreamingSafeguard.trimOnChatEcho(assistantResponse).trimEnd()
                                                    val genMs = System.currentTimeMillis() - generationStartTime
                                                    android.util.Log.i(TAG, "Updating final message after stop condition")
                                                    messages = if (messages.lastOrNull()?.role == "assistant") {
                                                        messages.dropLast(1) + ChatMessage(
                                                            role = "assistant",
                                                            content = trimmedResponse,
                                                            tokenCount = tokenCount,
                                                            generationTimeMs = genMs,
                                                            isStreaming = false,
                                                        )
                                                    } else {
                                                        messages + ChatMessage(
                                                            role = "assistant",
                                                            content = trimmedResponse,
                                                            tokenCount = tokenCount,
                                                            generationTimeMs = genMs,
                                                            isStreaming = false,
                                                        )
                                                    }
                                                } else {
                                                    // Stream cancelled for other reasons, flush buffer if needed
                                                    if (bufferIsSuspicious && tokenBuffer.isNotEmpty()) {
                                                        android.util.Log.d(TAG, "Stream cancelled with buffered token, flushing buffer: '$tokenBuffer'")
                                                        assistantResponse += tokenBuffer
                                                        tokenBuffer = ""
                                                        bufferIsSuspicious = false
                                                    }
                                                }
                                            }

                                            if (!stoppedOnChatEcho) {
                                                // Flush buffer if stream completed normally with buffered token
                                                if (bufferIsSuspicious && tokenBuffer.isNotEmpty()) {
                                                    android.util.Log.d(TAG, "Stream completed with buffered token, flushing buffer: '$tokenBuffer'")
                                                    assistantResponse += tokenBuffer
                                                    tokenBuffer = ""
                                                    bufferIsSuspicious = false
                                                }

                                                val generationTimeMs = System.currentTimeMillis() - generationStartTime
                                                android.util.Log.d(
                                                    TAG,
                                                    "Generation complete. Total tokens: $tokenCount, response length: ${assistantResponse.length}, time: ${generationTimeMs}ms",
                                                )

                                                messages = if (messages.lastOrNull()?.role == "assistant") {
                                                    messages.dropLast(1) + ChatMessage(
                                                        role = "assistant",
                                                        content = assistantResponse,
                                                        tokenCount = tokenCount,
                                                        generationTimeMs = generationTimeMs,
                                                        isStreaming = false,
                                                    )
                                                } else {
                                                    messages
                                                }
                                                
                                                // Save assistant message to database if session is active
                                                if (sessionId != null && repository != null && assistantResponse.isNotEmpty()) {
                                                    scope.launch {
                                                        repository.addMessage(sessionId, "assistant", assistantResponse)
                                                        repository.updateSessionTimestamp(sessionId)
                                                    }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("ChatScreen", "Generation failed", e)
                                            messages = messages + ChatMessage(
                                                role = "assistant",
                                                content = "Error: ${e.message}"
                                            )
                                            
                                            // Save error message to database if session is active
                                            if (sessionId != null && repository != null) {
                                                scope.launch {
                                                    repository.addMessage(sessionId, "assistant", "Error: ${e.message}")
                                                }
                                            }
                                        } finally {
                                            isGenerating = false
                                        }
                                    }
                                }
                            },
                            enabled = currentInput.isNotBlank(),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Send",
                                tint = if (currentInput.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Edit message dialog
    if (editingMessageIndex != null) {
        val messageToEdit = messages[editingMessageIndex!!]
        EditMessageDialog(
            originalContent = messageToEdit.content,
            onDismiss = { editingMessageIndex = null },
            onConfirm = { newContent ->
                scope.launch {
                    try {
                        // Delete all messages after the edited message
                        val editedMessage = messages[editingMessageIndex!!]
                        val messagesBefore = messages.take(editingMessageIndex!!)
                        
                        // Update the message content
                        messages = messagesBefore + ChatMessage(
                            role = "user",
                            content = newContent
                        )
                        
                        // Delete messages after edit from database if session is active
                        if (sessionId != null && repository != null) {
                            repository.deleteMessagesAfter(sessionId, editedMessage.tokenCount.toLong())
                        }
                        
                        // Regenerate assistant response
                        isGenerating = true
                        var assistantResponse = ""
                        val generationStartTime = System.currentTimeMillis()
                        
                        // Build conversation history using ContextBuilder
                        val historyMessages = messagesBefore.map { message ->
                            when (message.role) {
                                "user" -> LLMMessage(LLMMessage.MessageRole.USER, message.content)
                                "assistant" -> LLMMessage(LLMMessage.MessageRole.ASSISTANT, message.content)
                                else -> LLMMessage(LLMMessage.MessageRole.USER, message.content)
                            }
                        }

                        val contextParams = ContextParams(
                            systemPrompt = systemInstruction,
                            pinnedFacts = "",
                            userProfile = "",
                            crossSessionMemories = emptyList(),
                            inSessionSummaries = emptyList(),
                            history = historyMessages,
                            currentMessage = LLMMessage(LLMMessage.MessageRole.USER, newContent)
                        )

                        val llmMessages = ContextBuilder.buildContextMessages(contextParams)

                        // Convert LLMMessage[] to string prompt for engine
                        val fullPrompt = llmMessages.joinToString("\n") { 
                            "${it.role.name}: ${it.content}" 
                        }

                        android.util.Log.d("ChatScreen", "Regenerating after edit with context messages count: ${llmMessages.size}")
                        
                        var tokenCount = 0
                        engine.generate(fullPrompt).collect { token ->
                            tokenCount++
                            assistantResponse += token
                            
                            // Update UI with regenerated response
                            messages = if (messages.lastOrNull()?.role == "assistant") {
                                messages.dropLast(1) + ChatMessage(
                                    role = "assistant",
                                    content = assistantResponse,
                                    tokenCount = tokenCount,
                                    generationTimeMs = System.currentTimeMillis() - generationStartTime,
                                    isStreaming = true,
                                )
                            } else {
                                messages + ChatMessage(
                                    role = "assistant",
                                    content = assistantResponse,
                                    tokenCount = tokenCount,
                                    generationTimeMs = System.currentTimeMillis() - generationStartTime,
                                    isStreaming = true,
                                )
                            }
                        }
                        
                        // Final update
                        messages = if (messages.lastOrNull()?.role == "assistant") {
                            messages.dropLast(1) + ChatMessage(
                                role = "assistant",
                                content = assistantResponse,
                                tokenCount = tokenCount,
                                generationTimeMs = System.currentTimeMillis() - generationStartTime,
                                isStreaming = false,
                            )
                        } else {
                            messages
                        }
                        
                        // Save messages to database if session is active
                        if (sessionId != null && repository != null) {
                            repository.addMessage(sessionId, "user", newContent)
                            repository.addMessage(sessionId, "assistant", assistantResponse)
                            repository.updateSessionTimestamp(sessionId)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ChatScreen", "Edit and regenerate failed", e)
                    } finally {
                        isGenerating = false
                    }
                }
                editingMessageIndex = null
            }
        )
    }
}

/** Shimmer drawn over assistant text while [ChatMessage.isStreaming] is true. */
private fun Modifier.assistantStreamingShimmer(enabled: Boolean): Modifier =
    composed {
        if (!enabled) return@composed Modifier
        val transition = rememberInfiniteTransition(label = "assistantStreamShimmer")
        val shift by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1_200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "assistantShimmerPhase",
        )
        val primary = MaterialTheme.colorScheme.primary
        val onSurface = MaterialTheme.colorScheme.onSurface
        Modifier.drawWithContent {
            drawContent()
            val w = size.width.coerceAtLeast(1f)
            val h = size.height.coerceAtLeast(1f)
            val brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    primary.copy(alpha = 0.12f),
                    onSurface.copy(alpha = 0.07f),
                    primary.copy(alpha = 0.12f),
                    Color.Transparent,
                ),
                start = Offset(x = shift * (w * 1.5f) - w * 0.35f, y = 0f),
                end = Offset(x = shift * (w * 1.5f) + w * 0.48f, y = h),
            )
            drawRect(brush = brush, alpha = 0.42f)
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
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (message.role == "assistant" &&
                                message.isStreaming &&
                                message.content.isBlank()
                            ) {
                                Modifier.defaultMinSize(minHeight = 48.dp)
                            } else {
                                Modifier
                            },
                        )
                        .assistantStreamingShimmer(
                            enabled = message.role == "assistant" && message.isStreaming,
                        ),
                )

                // Metadata for assistant messages (hidden while streaming)
                if (message.role == "assistant" &&
                    !message.isStreaming &&
                    (message.tokenCount > 0 || message.generationTimeMs > 0)
                ) {
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
        if (message.role == "assistant" && !message.isStreaming) {
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
