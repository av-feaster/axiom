# RCA: "User:" Appearing in Message Box

## Issue Description
When the model generated the text "User:" as part of its response, the "User:" token was appearing in the chat message box even though stop conditions were implemented to prevent this.

## Timeline
- **Date**: 2026-04-19
- **Severity**: High (UX issue - model hallucinating role markers)
- **Status**: Resolved

## Root Cause Analysis

### The Problem
The token processing logic in `ChatScreen.kt` had a race condition where tokens were added to the response BEFORE stop condition checks were performed.

### Original Code Flow (Buggy)
```kotlin
engine.generate(fullPrompt).collect { token ->
    tokenCount++
    assistantResponse += token.replace("\uFFFD", "")  // ❌ Token added FIRST
    
    // Check stop conditions AFTER adding
    if (StreamingSafeguard.shouldStopOnChatEcho(assistantResponse)) {
        assistantResponse = StreamingSafeguard.trimOnChatEcho(assistantResponse)
        // Stop generation
    }
    
    // Update UI with assistantResponse (already contains "User:")
    messages = messages + ChatMessage(content = assistantResponse, ...)
}
```

### Why This Caused the Issue
1. Model generates token "User"
2. Token is immediately added to `assistantResponse`
3. UI is updated with `assistantResponse` containing "User"
4. Model generates token ":"
5. Token is added to `assistantResponse` (now contains "User:")
6. Stop condition check detects "User:"
7. Response is trimmed to remove "User:"
8. BUT UI was already updated with untrimmed response during streaming

### Additional Complications
1. **Stream not stopping**: Even after calling `engine.cancel()`, the native stream continued emitting tokens that were being ignored, causing noisy logs
2. **Garbage characters**: Model was generating UTF-8 replacement characters () after completing responses
3. **Character repetition**: Model would get stuck in loops repeating the same character

## The Fix

### Solution 1: Check Stop Conditions BEFORE Adding Token
```kotlin
engine.generate(fullPrompt).collect { token ->
    tokenCount++
    
    // Check stop conditions BEFORE adding token
    val tokenClean = token.replace("\uFFFD", "")
    val testResponse = assistantResponse + tokenClean
    
    if (StreamingSafeguard.shouldStopOnChatEcho(testResponse)) {
        // Stop WITHOUT adding the token
        engine.cancel()
        throw CancellationException("Stopped on chat echo")
    }
    
    // Only add token if safe
    assistantResponse += tokenClean
    
    // Update UI
    messages = messages + ChatMessage(content = assistantResponse, ...)
}
```

### Solution 2: Immediate Stream Cancellation
Changed from using `return@collect` to throwing `CancellationException` to break out of the entire collect loop immediately, preventing the stream from continuing to emit tokens.

### Solution 3: Trim Response in Catch Block
Added proper handling in the `CancellationException` catch block to ensure the final message is trimmed:
```kotlin
} catch (_: CancellationException) {
    if (stoppedOnChatEcho) {
        val trimmedResponse = StreamingSafeguard.trimOnChatEcho(assistantResponse).trimEnd()
        messages = messages + ChatMessage(
            content = trimmedResponse,
            isStreaming = false
        )
    }
}
```

### Solution 4: Additional Safeguards
Added safeguards for other edge cases:
- `shouldStopOnGarbage()` - Detects consecutive UTF-8 replacement characters
- `shouldStopOnRepetition()` - Detects character repetition loops

## Impact
- **Before**: "User:" and other role markers appeared in assistant responses
- **After**: Stop conditions are detected before tokens are added, preventing unwanted content from appearing in messages

## Lessons Learned
1. **Check before mutate**: Always validate state changes before applying them, not after
2. **Stream cancellation**: `return@collect` only exits the current lambda; use exceptions to break out of entire flow
3. **UI consistency**: Ensure final state is consistent even when streaming is interrupted
4. **Multiple safeguards**: Implement multiple layers of protection for different failure modes

## Related Files
- `axiom-android-sdk/src/main/java/com/axiom/android/sdk/ui/screens/ChatScreen.kt`
- `axiom-core/src/main/java/com/axiom/core/StreamingSafeguard.kt`
- `axiom-android-sdk/src/main/java/com/axiom/android/sdk/engine/EngineManager.kt`

## Verification
The fix ensures that:
1. Tokens are checked for stop conditions BEFORE being added to the response
2. Stream is cancelled immediately when stop condition is detected
3. Final message is trimmed to remove any unwanted content
4. User can ask other questions without waiting for stream to finish
