# Memory Repository - Dry Run Guide

## Feature Overview
Memory repository provides keyword-based memory storage for user facts (name, preferences, project details) that can be retrieved and injected into chat context.

## Implementation Details
- **File**: `axiom-android-sdk/src/main/java/com/axiom/android/sdk/data/repository/MemoryRepository.kt`
- **Storage**: SharedPreferences
- **Retrieval**: Keyword matching
- **Integration**: Can be injected into ContextBuilder

## Dry Run Steps

### 1. Test Memory Creation
```kotlin
val repository = MemoryRepository(context)
repository.addMemory("User's name is John", "personal")
repository.addMemory("Working on Android app project", "project")
repository.addMemory("Prefers Kotlin over Java", "preference")
```

### 2. Verify Memory Storage
- Check SharedPreferences for "axiom_memory" key
- Verify memories are stored as JSON array
- Verify each memory has: id, fact, category, createdAt

### 3. Test Memory Retrieval
```kotlin
val memories = repository.getRelevantMemories("What is my name?")
assert(memories.isNotEmpty())
assert(memories.any { it.fact.contains("name") })
```

### 4. Test Keyword Matching
- Query: "What project am I working on?"
- Should retrieve: "Working on Android app project"
- Query: "What programming language do I prefer?"
- Should retrieve: "Prefers Kotlin over Java"

### 5. Test Memory Deletion
```kotlin
val memoryId = memories.first().id
repository.removeMemory(memoryId)
val updatedMemories = repository.getRelevantMemories("name")
assert(updatedMemories.isEmpty())
```

### 6. Test Memory Formatting
```kotlin
val formatted = repository.getFormattedMemories()
assert(formatted.contains("- User's name is John"))
assert(formatted.contains("- Working on Android app project"))
```

### 7. Test Memory Persistence
- Add memories
- Kill and restart app
- Verify memories persist in SharedPreferences
- Verify memories are loaded on app start

### 8. Test Empty State
- Clear all memories: `repository.clearMemories()`
- Verify SharedPreferences is empty
- Verify retrieval returns empty list
- Verify formatted string is empty

### 9. Test Context Integration
Integrate with ContextBuilder:
```kotlin
val contextParams = ContextBuilder.ContextParams(
    // ... other params
    userProfile = repository.getFormattedMemories()
)
```

### 10. Test in ChatScreen
- Add memories via UI or code
- Send a message that should trigger memory retrieval
- Verify memory is included in context
- Verify assistant uses memory in response

## Success Criteria
- ✓ Memories can be added
- ✓ Memories persist across app restarts
- ✓ Keyword matching retrieves relevant memories
- ✓ Memories can be deleted
- ✓ Memories can be cleared
- ✓ Formatted output is correct
- ✓ Context integration works
- ✓ No errors in SharedPreferences operations

## Known Issues
- None identified

## Rollback Plan
If issues occur:
1. Remove MemoryRepository.kt file
2. Remove memory integration from ContextBuilder
3. Remove memory-related code from ChatScreen
4. Restore context building without memory
