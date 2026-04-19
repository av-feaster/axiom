# ContextBuilder Integration - Dry Run Guide

## Feature Overview
ContextBuilder replaces simple string concatenation with structured LLMMessage format for better context management in chat conversations.

## Implementation Details
- **File**: `axiom-android-sdk/src/main/java/com/axiom/android/sdk/ui/screens/ChatScreen.kt`
- **Changes**: Replaced string concatenation with `ContextBuilder.buildContextMessages()`
- **Format**: Uses `LLMMessage` with roles (SYSTEM, USER, ASSISTANT)

## Dry Run Steps

### 1. Start the Application
```bash
./gradlew assembleDebug
./gradlew installDebug
```

### 2. Open Chat Screen
- Navigate to the chat interface
- Select a model (e.g., llama-3-8b)
- Start a conversation

### 3. Verify Context Building
- Send a message: "What is the capital of France?"
- Observe logs for: `Context messages count: X`
- Check that the prompt is built using LLMMessage format
- Verify system prompt is included from selected ChatMode

### 4. Test Conversation History
- Send 3-4 messages in conversation
- Verify that last 10 messages are included in context
- Check that context respects token budget (if implemented)

### 5. Test Regenerate with ContextBuilder
- Click regenerate button on an assistant message
- Verify that context is rebuilt using ContextBuilder
- Check logs for "Regenerating message with context messages count"

### 6. Verify Log Output
Expected logs:
```
ChatScreen: Starting generation for model: llama-3-8b
ChatScreen: Context messages count: 3
ChatScreen: Engine class: LlamaCppEngine
```

## Success Criteria
- ✓ Messages are sent successfully
- ✓ Context is built using LLMMessage format
- ✓ System prompt is included from ChatMode
- ✓ Conversation history is properly included
- ✓ Regenerate functionality works with ContextBuilder
- ✓ No errors in logs related to context building

## Known Issues
- None identified

## Rollback Plan
If issues occur:
1. Revert to simple string concatenation in ChatScreen
2. Remove ContextBuilder imports
3. Restore original prompt building logic
