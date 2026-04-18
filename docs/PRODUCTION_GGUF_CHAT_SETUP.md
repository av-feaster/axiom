# Production-Ready GGUF (llama.cpp) Chat Setup for Android

This guide provides a complete, production-ready setup for GGUF/llama.cpp chat on Android with proper prompt templates, stop tokens, and streaming safeguards.

## 1. Prompt Template

### Alpaca-style Template (Recommended for tinyllama-1.1b)

```kotlin
val systemInstruction = "You are a helpful AI assistant. Always respond in English. Be concise, accurate, and friendly."

val conversationHistory = StringBuilder()
messages.takeLast(5).forEach { message ->
    when (message.role) {
        "user" -> conversationHistory.append("### User:\n${message.content}\n")
        "assistant" -> conversationHistory.append("### Assistant:\n${message.content}\n")
    }
}

val prompt = buildString {
    append("### System:\n")
    append(systemInstruction.trim())
    append("\n\n")
    append(conversationHistory.toString().trim())
    append("\n")
    append("### User:\n")
    append(userMessage.trim())
    append("\n")
    append("### Assistant:\n")
}
```

### ChatML-style Template (Optional)

```kotlin
val prompt = buildString {
    append("<|im_start|>system\n")
    append(systemInstruction.trim())
    append("<|im_end|>\n")
    
    messages.takeLast(5).forEach { message ->
        val role = if (message.role == "user") "user" else "assistant"
        append("<|im_start|>$role\n")
        append(message.content.trim())
        append("<|im_end|>\n")
    }
    
    append("<|im_start|>user\n")
    append(userMessage.trim())
    append("<|im_end|>\n")
    append("<|im_start|>assistant\n")
}
```

## 2. Stop Tokens Config

### Alpaca Template
```kotlin
val stopTokens = listOf(
    "### User:",
    "### System:"
)
```

### ChatML Template
```kotlin
val stopTokens = listOf(
    "<|im_end|>",
    "<|im_start|>user",
    "<|im_start|>system"
)
```

### Vicuna Template
```kotlin
val stopTokens = listOf(
    "USER:",
    "ASSISTANT:"
)
```

## 3. Kotlin Streaming Safeguard

```kotlin
import com.axiom.core.StreamingSafeguard

val stopTokens = listOf("### User:", "### System:")
val userMarker = "### User:"
var generatedText = ""

engine.stream(prompt) { token ->
    generatedText += token
    
    // Check if generation should stop
    if (StreamingSafeguard.shouldStopGeneration(
            generatedText,
            stopTokens,
            userMarker
        )) {
        engine.cancel()
    }
}

// Clean up the final text
val cleanText = StreamingSafeguard.trimGeneratedText(
    generatedText,
    stopTokens,
    userMarker
)

// Validate before displaying
if (StreamingSafeguard.isSafeToDisplay(cleanText)) {
    messages = messages + ChatMessage(
        role = "assistant",
        content = cleanText,
        isStreaming = false
    )
}
```

## 4. Example Integration with llama.cpp

### LLMConfig Setup (Optimized for tinyllama-1.1b)

```kotlin
val config = LLMConfig(
    modelPath = "${context.filesDir.absolutePath}/models/tinyllama-1.1b.gguf",
    contextSize = 2048,  // Increased for conversation history
    temperature = 0.7f,  // Balanced creativity
    topK = 40,           // Standard for small models
    topP = 0.95f,        // Standard nucleus sampling
    repeatPenalty = 1.1f, // Slight penalty to reduce repetition
    threads = 4,         // Adjust based on device cores
    maxTokens = 512,      // Reasonable limit for responses
    stopTokens = listOf("### User:", "### System:"),
    enableStreaming = true
)

val engine = AxiomSDK.getEngine()
val success = engine.init(config)
```

### Streaming Generation with Safeguards

```kotlin
val stopTokens = listOf("### User:", "### System:")
val userMarker = "### User:"
var assistantResponse = ""
var shouldStop = false

scope.launch {
    try {
        engine.stream(prompt).collect { token ->
            if (shouldStop) return@collect
            
            assistantResponse += token
            
            // Safeguard: Check for hallucination
            if (StreamingSafeguard.shouldStopGeneration(
                    assistantResponse,
                    stopTokens,
                    userMarker
                )) {
                shouldStop = true
                engine.cancel()
                Log.w(TAG, "Generation stopped due to safeguard")
            }
            
            // Update UI (throttled for performance)
            if (tokenCount % 5 == 0) {
                messages = messages + ChatMessage(
                    role = "assistant",
                    content = assistantResponse,
                    isStreaming = true
                )
            }
        }
        
        // Clean up final response
        val cleanResponse = StreamingSafeguard.trimGeneratedText(
            assistantResponse,
            stopTokens,
            userMarker
        )
        
        // Validate and display
        if (StreamingSafeguard.isSafeToDisplay(cleanResponse)) {
            messages = messages + ChatMessage(
                role = "assistant",
                content = cleanResponse,
                isStreaming = false
            )
        } else {
            Log.e(TAG, "Generated text failed safety check")
        }
        
    } catch (e: Exception) {
        Log.e(TAG, "Generation failed", e)
    }
}
```

## 5. Complete ChatScreen Integration Example

```kotlin
@Composable
fun ChatScreen(
    modelId: String,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val engine = AxiomSDK.getEngine()
    val scope = rememberCoroutineScope()
    
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var currentInput by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    
    // Configuration optimized for tinyllama-1.1b
    val config = remember {
        LLMConfig(
            modelPath = "${context.filesDir.absolutePath}/models/$modelId.gguf",
            contextSize = 2048,
            temperature = 0.7f,
            topK = 40,
            topP = 0.95f,
            repeatPenalty = 1.1f,
            threads = 4,
            maxTokens = 512,
            stopTokens = listOf("### User:", "### System:"),
            enableStreaming = true
        )
    }
    
    // Initialize engine
    LaunchedEffect(modelId) {
        engine.init(config)
    }
    
    fun sendMessage() {
        if (currentInput.isBlank() || isGenerating) return
        
        isGenerating = true
        
        // Build prompt with Alpaca template
        val systemInstruction = "You are a helpful AI assistant. Always respond in English. Be concise, accurate, and friendly."
        
        val conversationHistory = StringBuilder()
        messages.takeLast(5).forEach { message ->
            when (message.role) {
                "user" -> conversationHistory.append("### User:\n${message.content}\n")
                "assistant" -> conversationHistory.append("### Assistant:\n${message.content}\n")
            }
        }
        
        val prompt = buildString {
            append("### System:\n")
            append(systemInstruction.trim())
            append("\n\n")
            append(conversationHistory.toString().trim())
            append("\n")
            append("### User:\n")
            append(currentInput.trim())
            append("\n")
            append("### Assistant:\n")
        }
        
        // Add user message
        messages = messages + ChatMessage(
            role = "user",
            content = currentInput,
            timestamp = System.currentTimeMillis()
        )
        
        val input = currentInput
        currentInput = ""
        
        // Stream response with safeguards
        scope.launch {
            var assistantResponse = ""
            val stopTokens = listOf("### User:", "### System:")
            val userMarker = "### User:"
            
            try {
                engine.stream(prompt).collect { token ->
                    assistantResponse += token
                    
                    // Safeguard check
                    if (StreamingSafeguard.shouldStopGeneration(
                            assistantResponse,
                            stopTokens,
                            userMarker
                        )) {
                        engine.cancel()
                        return@collect
                    }
                    
                    // Update UI periodically
                    messages = if (messages.lastOrNull()?.role == "assistant") {
                        messages.dropLast(1) + ChatMessage(
                            role = "assistant",
                            content = assistantResponse,
                            isStreaming = true
                        )
                    } else {
                        messages + ChatMessage(
                            role = "assistant",
                            content = assistantResponse,
                            isStreaming = true
                        )
                    }
                }
                
                // Clean up final response
                val cleanResponse = StreamingSafeguard.trimGeneratedText(
                    assistantResponse,
                    stopTokens,
                    userMarker
                )
                
                // Validate and display
                if (StreamingSafeguard.isSafeToDisplay(cleanResponse)) {
                    messages = if (messages.lastOrNull()?.role == "assistant") {
                        messages.dropLast(1) + ChatMessage(
                            role = "assistant",
                            content = cleanResponse,
                            isStreaming = false
                        )
                    } else {
                        messages + ChatMessage(
                            role = "assistant",
                            content = cleanResponse,
                            isStreaming = false
                        )
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Generation failed", e)
            } finally {
                isGenerating = false
            }
        }
    }
    
    // UI implementation...
}
```

## 6. Stopping Conditions

The setup includes multiple layers of stopping conditions:

1. **Stop Tokens**: Native llama.cpp stop tokens (configured via `LLMConfig.stopTokens`)
2. **Kotlin Safeguard**: Runtime detection of unwanted patterns
3. **Max Tokens**: Hard limit on token count
4. **User Marker Detection**: Immediate stop if "User:" appears
5. **System Marker Detection**: Stop if "System:" appears

## 7. Optimization Tips for Mobile

- **Context Size**: Use 2048 for small models, 4096 for larger models
- **Threads**: Set to device CPU cores (default uses available processors)
- **Temperature**: 0.7f balances creativity and coherence
- **Top P**: 0.95f is standard for most models
- **Repeat Penalty**: 1.1f reduces repetition without over-penalizing
- **Max Tokens**: 512 is reasonable for chat responses
- **UI Throttling**: Update UI every 5-10 tokens to reduce jitters

## 8. Troubleshooting

### Model generating full conversations
- Ensure stop tokens are properly configured
- Check that the prompt template matches the model's training format
- Verify the system instruction is clear about the assistant role

### Model hallucinating as User
- The `StreamingSafeguard` should catch this automatically
- Check that `userMarker` matches your prompt template
- Increase repeat penalty if model is stuck in loops

### Slow generation
- Reduce context size if not needed
- Decrease max tokens for shorter responses
- Ensure the model file is optimized for mobile (quantized)
