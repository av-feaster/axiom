# Chat Title Generation - Dry Run Guide

## Feature Overview
Chat title generation automatically creates neutral, topic-focused titles for chat sessions using the LLM at message count milestones.

## Implementation Details
- **File**: `axiom-android-sdk/src/main/java/com/axiom/android/sdk/usecase/GenerateChatTitleUseCase.kt`
- **Milestones**: 1-4, 5-10, 15-20 messages
- **Sanitization**: 3-6 words, no punctuation
- **Locking**: Titles can be locked to prevent regeneration

## Dry Run Steps

### 1. Create a New Session
- Open SessionListScreen
- Create a new session
- Verify initial title is "New Chat"

### 2. Send First Message
- Navigate to the session
- Send a message: "How do I implement a binary search in Python?"
- Wait for assistant response
- Check that title is still "New Chat" (bucket 1, not triggered yet)

### 3. Send More Messages (5 total)
- Send 4 more messages in the conversation
- After 5th message, title generation should trigger
- Check logs for: "GenerateChatTitleUseCase: Generating title"
- Verify title is updated to something like: "Binary Search Python"

### 4. Verify Title Sanitization
- Check that title is 3-6 words
- Verify no punctuation marks
- Verify title is topic-focused, not procedural

### 5. Test Title Locking
- Manually update session title to "Custom Title"
- Lock the title using repository
- Send more messages (to reach next bucket)
- Verify title is not regenerated (it's locked)

### 6. Test Different Topics
- Create a new session about cooking
- Send messages about recipes
- Verify title reflects cooking topic
- Create a new session about coding
- Send messages about debugging
- Verify title reflects coding topic

### 7. Test Title Bucket Logic
- Verify title generates at message counts: 5, 10, 15, 20
- Check that `lastTitleBucket` is updated
- Verify title doesn't regenerate within same bucket

### 8. Test Use Case Directly
```kotlin
val useCase = GenerateChatTitleUseCase(repository, engine)
val result = useCase.execute(sessionId, modelId)
assert(result.isSuccess)
```

## Success Criteria
- ✓ Title generates at correct message milestones
- ✓ Title is 3-6 words
- ✓ Title has no punctuation
- ✓ Title is topic-focused
- ✓ Locked titles don't regenerate
- ✓ Bucket logic works correctly
- ✓ No errors in logs during generation

## Known Issues
- None identified

## Rollback Plan
If issues occur:
1. Remove GenerateChatTitleUseCase file
2. Remove calls to use case from ChatScreen
3. Remove title generation logic from session management
4. Restore static "New Chat" titles
