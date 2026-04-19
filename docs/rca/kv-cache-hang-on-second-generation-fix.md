# KV Cache Hang on Second Generation Fix

## Issue
Second chat generation takes ~4 minutes to start after the first generation completes.

## Symptoms
- First generation completes successfully with 37 tokens
- JobCancellationException occurs after first generation
- Second generation starts and tokenizes prompt successfully (150 tokens)
- No streaming logs appear for 4 minutes
- Native code logs "Tokenized prompt into 150 tokens" but never logs "Prompt decoded successfully"
- `llama_decode` call at line 387 in llama_jni.cpp blocks indefinitely

## Root Cause
The KV cache retains state from the previous generation. When the second generation tries to decode the prompt, the cache is in a bad state, causing `llama_decode` to hang. The commented-out code at line 231-232 claimed `llama_kv_cache_clear` doesn't exist, but the current llama.cpp API provides state management functions to reset the context.

## Investigation
Logs showed:
```
2026-04-20 04:13:52.507 LlamaCppEngine I Streaming completed: 37 tokens, finish reason: STOP
2026-04-20 04:13:52.508 EngineManager E Generation failed
kotlinx.coroutines.JobCancellationException: StandaloneCoroutine was cancelled
2026-04-20 04:13:52.509 LlamaCppEngine I Starting streaming generation
2026-04-20 04:13:52.524 LlamaJNI I Tokenized prompt into 150 tokens
[4-minute gap with no logs]
```

The native code in `llama_jni.cpp`:
- Line 382: Logs "Tokenized prompt into %d tokens" ✓
- Line 387: `llama_decode(g_context, batch)` - blocks indefinitely
- Line 393: Logs "Prompt decoded successfully" - never reached

## Solution
Implement KV cache reset between generations using llama.cpp state management API:

1. **Save initial clean state** after model initialization in `nativeInit`:
   - Use `llama_state_get_size()` to get state size
   - Use `llama_state_get_data()` to save initial state
   - Store in global buffer `g_initial_state`

2. **Restore initial state** before each generation in `nativeGenerate` and `nativeStream`:
   - Use `llama_state_set_data()` to restore clean state
   - This resets KV cache to initial condition
   - Ensures each generation starts with fresh cache

3. **Cleanup** in `nativeCleanup`:
   - Clear state buffer to prevent stale data

## Changes Made
File: `axiom-llama-cpp/src/main/cpp/llama_jni.cpp`

1. Added global state management variables:
```cpp
static std::vector<uint8_t> g_initial_state;
static size_t g_initial_state_size = 0;
```

2. Save initial state after context creation in `nativeInit`:
```cpp
g_initial_state_size = llama_state_get_size(g_context);
g_initial_state.resize(g_initial_state_size);
size_t copied = llama_state_get_data(g_context, g_initial_state.data(), g_initial_state_size);
```

3. Restore state before generation in `nativeGenerate` and `nativeStream`:
```cpp
if (!g_initial_state.empty()) {
    size_t restored = llama_state_set_data(g_context, g_initial_state.data(), g_initial_state_size);
    LOGI("Restored initial state: %zu bytes", restored);
}
```

4. Clear state buffer in `nativeCleanup`:
```cpp
g_initial_state.clear();
g_initial_state_size = 0;
```

## Verification
- Build successful with only minor warnings
- Second generation should now start immediately without hanging
- Each generation starts with fresh KV cache state

## Related Code
- `axiom-llama-cpp/src/main/cpp/llama_jni.cpp` - JNI layer
- `axiom-llama-cpp/src/main/java/com/axiom/llama/cpp/LlamaCppEngine.kt` - Kotlin engine wrapper
- `axiom-android-sdk/src/main/java/com/axiom/android/sdk/engine/EngineManager.kt` - Engine manager

## Date
2026-04-20
