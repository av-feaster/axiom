# Token Queue System Documentation

## Overview

The Token Queue System is a batching mechanism for streaming token generation that improves user experience by reducing UI jitter while maintaining low latency. It intelligently batches tokens for smoother rendering and dynamically adjusts batch size based on remaining context window.

## Purpose

**Problem**: Previous implementation rendered each token immediately, causing:
- UI jitter from frequent recompositions
- Higher CPU usage from excessive UI updates
- Poor stop token detection when tokens split across boundaries

**Solution**: TokenQueue batches tokens while ensuring:
- First token renders immediately (no latency)
- Subsequent tokens batch for smoother UX
- Dynamic batch sizing based on context window
- Improved stop token detection across token boundaries

## Architecture

### TokenQueue Class

Located in `axiom-core/src/main/java/com/axiom/core/StreamingSafeguard.kt`

**Key Features**:
- First token immediate rendering
- Configurable batch size (default: 3)
- Dynamic batch sizing based on remaining context
- Stop token detection in batched content
- Automatic flushing on stop token detection

### Integration with ChatScreen

Located in `axiom-android-sdk/src/main/java/com/axiom/android/sdk/ui/screens/ChatScreen.kt`

**Integration Points**:
- Initialize TokenQueue with context size and max tokens
- Replace token buffering logic with TokenQueue.addToken()
- Flush remaining tokens on completion/cancellation
- Maintain existing safeguard checks (garbage, repetition)

## API Reference

### TokenQueue Constructor

```kotlin
TokenQueue(
    contextSize: Int,      // Total context window size (e.g., 1024)
    maxTokens: Int,        // Maximum tokens to generate (e.g., 512)
    defaultBatchSize: Int = 3  // Default batch size for subsequent tokens
)
```

### addToken()

```kotlin
fun addToken(token: String, stopTokens: List<String> = emptyList()): QueueResult
```

**Returns**: `QueueResult` with:
- `shouldRender: Boolean` - Whether to render the batch
- `content: String?` - Content to render (null if not ready)
- `shouldStop: Boolean` - Whether to stop generation

**Behavior**:
- First token: Always renders immediately
- Subsequent tokens: Batched until threshold reached
- Stop token detection: Checks queued content for stop tokens
- Dynamic sizing: Adjusts batch size based on remaining context

### flush()

```kotlin
fun flush(): String
```

**Purpose**: Force flush all queued tokens

**Use Cases**:
- Generation completion
- User cancellation
- Error handling

### Helper Methods

- `getQueueSize()`: Current number of queued tokens
- `getTokensGenerated()`: Total tokens generated
- `reset()`: Reset queue state for new generation

## Dynamic Batch Sizing

Batch size adjusts based on remaining context window:

| Remaining Context | Batch Size | Rationale |
|-------------------|------------|-----------|
| > 500 tokens | 3 (default) | Plenty of context, smooth batching |
| 200-500 tokens | 2 | Approaching limit, reduce batching |
| < 200 tokens | 1 | Near limit, render immediately |

**Formula**:
```kotlin
val remainingContext = contextSize - tokensGenerated
return when {
    remainingContext > 500 -> defaultBatchSize
    remainingContext > 200 -> 2
    else -> 1
}
```

## Stop Token Detection

### Built-in Stop Markers

TokenQueue automatically detects:
- Configured stop tokens (passed to `addToken()`)
- Common chat echo markers:
  - `### User:`
  - `### System:`
  - `### Assistant:`
  - `User:`
  - `System:`
  - `Assistant:`

### Detection Behavior

1. **Immediate Flush**: Stop token detected in queue → flush immediately
2. **Content Trimming**: Remove content after stop token
3. **Stop Signal**: Return `shouldStop = true` to cancel generation

### Example

```kotlin
val queue = TokenQueue(contextSize = 1024, maxTokens = 512)
val stopTokens = listOf("### User:")

queue.addToken("Hello")  // Renders immediately
queue.addToken(" ###")   // Queued
queue.addToken(" ")      // Queued
queue.addToken("User:") // Queued

val result = queue.addToken(" test")
// result.shouldStop = true
// result.content = "Hello " (trimmed at "### User:")
```

## Integration Guide

### Basic Usage

```kotlin
// Initialize TokenQueue
val tokenQueue = TokenQueue(
    contextSize = 1024,
    maxTokens = 512,
    defaultBatchSize = 3
)

// In token streaming loop
engine.generate(prompt).collect { token ->
    val tokenClean = token.replace("\uFFFD", "")
    val result = tokenQueue.addToken(tokenClean, stopTokens)
    
    if (result.shouldRender && result.content != null) {
        // Update UI with batched content
        assistantResponse += result.content
        updateMessages(assistantResponse)
    }
    
    if (result.shouldStop) {
        engine.cancel()
        throw CancellationException("Stopped by token queue")
    }
}

// Flush remaining tokens on completion
val remaining = tokenQueue.flush()
if (remaining.isNotEmpty()) {
    assistantResponse += remaining
}
```

### Configuration

**Context Size**: From model configuration (typically 1024 or 2048)
**Max Tokens**: From generation config (typically 512)
**Batch Size**: 3 recommended for balance of smoothness vs latency

## Performance Characteristics

### Benefits

- **Smoother Rendering**: 3-4x fewer UI updates
- **Lower CPU Usage**: Reduced recomposition frequency
- **Better Stop Detection**: Catches tokens split across boundaries
- **No Latency**: First token renders immediately

### Trade-offs

- **Slight Delay**: 2-3 tokens delay for subsequent batches (minimal impact)
- **Memory**: Small queue buffer (negligible)
- **Complexity**: Single class, simple integration

### Benchmarks

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| UI Updates | 1 per token | 1 per 3 tokens | 3x reduction |
| Stop Token Detection | Single token | Batched content | Better accuracy |
| Latency | 0ms | 0ms (first token) | No change |

## Troubleshooting

### Issue: Tokens not rendering

**Cause**: Batch size too large or generation completes before threshold

**Solution**: 
- Reduce `defaultBatchSize`
- Call `flush()` on generation completion
- Check logs for queue size

### Issue: Stop tokens not detected

**Cause**: Stop token not in configured list or common markers

**Solution**:
- Add stop token to `addToken()` call
- Verify marker matches exactly (case-sensitive)
- Check logs for "Stop token detected" messages

### Issue: High latency

**Cause**: First token not rendering immediately

**Solution**:
- Verify `isFirstToken` flag logic
- Check for exceptions in token processing
- Ensure `shouldRender` is true for first token

## Testing

Unit tests located in `axiom-core/src/test/java/com/axiom/core/TokenQueueTest.kt`

Run tests:
```bash
./gradlew :axiom-core:test
```

### Test Coverage

- First token immediate rendering
- Subsequent token batching
- Stop token detection
- Dynamic batch sizing
- Flush functionality
- Reset functionality
- Multiple stop tokens
- Chat echo marker detection

## Migration Guide

### From Old Buffering System

**Before**:
```kotlin
var tokenBuffer = ""
var bufferIsSuspicious = false

if (isPrefix) {
    tokenBuffer += tokenClean
    bufferIsSuspicious = true
}
```

**After**:
```kotlin
val tokenQueue = TokenQueue(contextSize, maxTokens)
val result = tokenQueue.addToken(tokenClean, stopTokens)

if (result.shouldRender) {
    assistantResponse += result.content
}
```

### Backward Compatibility

- Existing safeguards (garbage, repetition) still work
- No changes to external API
- Drop-in replacement for buffering logic

## Future Enhancements

Potential improvements:
- Adaptive batch size based on token generation speed
- User-configurable batch size
- Performance metrics collection
- Integration with context window estimation from engine

## References

- **Implementation**: `axiom-core/src/main/java/com/axiom/core/StreamingSafeguard.kt`
- **Integration**: `axiom-android-sdk/src/main/java/com/axiom/android/sdk/ui/screens/ChatScreen.kt`
- **Tests**: `axiom-core/src/test/java/com/axiom/core/TokenQueueTest.kt`
- **Plan**: `.windsurf/plans/token-queue-system-e97210.md`
