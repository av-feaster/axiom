# Second Person Enforcement - Dry Run Guide

## Feature Overview
Second person enforcement converts first-person pronouns in assistant responses to second-person when the user asks a self-reference question (e.g., "What did I say?" → "You said").

## Implementation Details
- **File**: `axiom-core/src/main/java/com/axiom/core/TextProcessing.kt`
- **Function**: `enforceSecondPerson(userText: String, assistantText: String): String`
- **Detection**: Checks for first-person pronouns + question words
- **Transformations**: "I am" → "You are", "I'm" → "You are", "my" → "your"

## Dry Run Steps

### 1. Test Self-Reference Questions
Send questions that ask about the user:
- "What did I say about Python?"
- "Remember what I mentioned about my project"
- "Who am I working with?"
- "What is my name?"

### 2. Verify Detection
Check that `isSelfMemoryQuestion()` returns true for:
- Questions with "I", "me", "my", "mine"
- Questions with "?"
- Questions with memory intent words (who, what, where, remember, recall)

### 3. Test Enforcement
For each self-reference question:
- Wait for assistant response
- Check if response contains first-person pronouns
- Verify they are converted to second-person:
  - "I am" → "You are"
  - "I'm" → "You are"
  - "my" → "your"

### 4. Test Non-Self-Reference Questions
Send normal questions:
- "What is the capital of France?"
- "How do I implement binary search?"
- "Explain quantum computing"

Verify that second person enforcement is NOT applied (no transformation occurs).

### 5. Test Edge Cases
- Empty user text: should not enforce
- User text without question: should not enforce
- Assistant text without first-person pronouns: should return unchanged
- Mixed case: should handle case-insensitive matching

### 6. Unit Test the Function
```kotlin
// Test self-reference question
val userText = "What did I say about Python?"
val assistantText = "I mentioned that Python is great for data science"
val result = TextProcessing.enforceSecondPerson(userText, assistantText)
assert(result == "You mentioned that Python is great for data science")

// Test non-self-reference question
val userText = "What is the capital of France?"
val assistantText = "I think it's Paris"
val result = TextProcessing.enforceSecondPerson(userText, assistantText)
assert(result == "I think it's Paris")  // Unchanged
```

### 7. Apply in ChatScreen
After generation completes in ChatScreen:
```kotlin
val processedResponse = TextProcessing.enforceSecondPerson(input, assistantResponse)
messages = messages + ChatMessage(role = "assistant", content = processedResponse)
```

## Success Criteria
- ✓ Self-reference questions are detected correctly
- ✓ First-person pronouns are converted to second-person
- ✓ Non-self-reference questions are not affected
- ✓ Case-insensitive matching works
- ✓ Empty/edge cases handled gracefully
- ✓ No false positives on normal questions

## Known Issues
- None identified

## Rollback Plan
If issues occur:
1. Remove TextProcessing.kt file
2. Remove calls to `enforceSecondPerson()` from ChatScreen
3. Restore original response handling without transformation
