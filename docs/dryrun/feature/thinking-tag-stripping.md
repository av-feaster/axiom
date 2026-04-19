# DeepSeek R1 Thinking Tag Stripping - Dry Run Guide

## Feature Overview
DeepSeek R1 thinking tag stripping removes `` blocks from model outputs to prevent reasoning content from appearing in the final response.

## Implementation Details
- **File**: `axiom-core/src/main/java/com/axiom/core/StreamingSafeguard.kt`
- **Function**: `stripThinkingTags(text: String): String`
- **Patterns**: Removes complete and incomplete thinking blocks
- **Usage**: Should be applied after streaming completes

## Dry Run Steps

### 1. Test with DeepSeek Model
- Load a DeepSeek R1 model (e.g., deepseek-r1-distill-llama-8b)
- Start a conversation with the model
- Send a question that requires reasoning

### 2. Verify Thinking Tags Appear
- Check the raw output for `` tags
- Example raw output:
  ```
  <|think|>
  Let me think about this step by step...
  <|/think|>
  Based on my analysis, the answer is...
  ```

### 3. Apply Stripping Function
- In ChatScreen, after generation completes:
  ```kotlin
  val cleanResponse = StreamingSafeguard.stripThinkingTags(assistantResponse)
  ```
- Verify the function is called on the assistant response

### 4. Verify Clean Output
- Check that thinking tags are removed
- Verify only the final answer remains
- Example clean output:
  ```
  Based on my analysis, the answer is...
  ```

### 5. Test Incomplete Blocks
- Cancel generation mid-stream
- Verify incomplete thinking blocks are removed:
  ```
  <|think|>
  Let me think...
  ```
  Should become empty string after stripping

### 6. Test Non-DeepSeek Models
- Test with a non-DeepSeek model (e.g., llama-3)
- Verify the stripping function doesn't break normal output
- Verify no unwanted side effects

### 7. Unit Test the Function
Create a simple test:
```kotlin
val withTags = "<|think|>Reasoning here<|/think|>Final answer"
val withoutTags = StreamingSafeguard.stripThinkingTags(withTags)
assert(withoutTags == "Final answer")
```

## Success Criteria
- ✓ Complete thinking blocks are removed
- ✓ Incomplete thinking blocks are removed
- ✓ Non-DeepSeek model output is unaffected
- ✓ Function handles empty strings
- ✓ Function handles strings without thinking tags
- ✓ No errors in logs related to tag stripping

## Known Issues
- None identified

## Rollback Plan
If issues occur:
1. Remove `stripThinkingTags()` function from StreamingSafeguard
2. Remove calls to the function from ChatScreen
3. Restore original response handling without stripping
