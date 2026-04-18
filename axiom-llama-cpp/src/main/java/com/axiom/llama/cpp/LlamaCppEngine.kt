package com.axiom.llama.cpp

import android.content.Context
import android.util.Log
import com.axiom.core.LLMConfig
import com.axiom.core.LLMEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Llama.cpp implementation of LLMEngine interface
 */
class LlamaCppEngine : LLMEngine {
    private var isEngineInitialized = false
    
    companion object {
        private const val TAG = "LlamaCppEngine"
        private const val TAG_NATIVE = "LlamaJNI"
    }
    
    init {
        Log.i(TAG, "Loading native library: llama_jni")
        System.loadLibrary("llama_jni")
        Log.i(TAG, "Native library loaded successfully")
    }
    
    override val isInitialized: Boolean
        get() = isEngineInitialized

    override suspend fun init(config: LLMConfig): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Initializing LlamaCppEngine")
        Log.d(TAG, "Model path: ${config.modelPath}")
        Log.d(TAG, "Context size: ${config.contextSize}")
        Log.d(TAG, "Threads: ${config.threads}")
        Log.d(TAG, "Temperature: ${config.temperature}")
        Log.d(TAG, "Top K: ${config.topK}")
        Log.d(TAG, "Top P: ${config.topP}")
        Log.d(TAG, "Repeat penalty: ${config.repeatPenalty}")
        Log.d(TAG, "Max tokens: ${config.maxTokens}")
        
        try {
            val success = nativeInit(
                config.modelPath,
                config.contextSize,
                config.threads,
                config.temperature,
                config.topK,
                config.topP,
                config.repeatPenalty,
                config.maxTokens
            )
            isEngineInitialized = success
            if (success) {
                Log.i(TAG, "LlamaCppEngine initialized successfully")
            } else {
                Log.e(TAG, "LlamaCppEngine initialization failed (native returned false)")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Exception during initialization", e)
            false
        }
    }

    override suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        if (!isEngineInitialized) {
            Log.e(TAG, "Generate called but engine not initialized")
            throw IllegalStateException("Engine not initialized")
        }
        Log.i(TAG, "Starting non-streaming generation")
        Log.d(TAG, "Prompt length: ${prompt.length} chars")
        val result = try {
            nativeGenerate(prompt)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during generation", e)
            throw e
        }
        Log.i(TAG, "Generation completed: ${result.length} chars")
        result
    }

    override suspend fun stream(prompt: String, onToken: (String) -> Unit) {
        withContext(Dispatchers.IO) {
            if (!isEngineInitialized) {
                Log.e(TAG, "Stream called but engine not initialized")
                throw IllegalStateException("Engine not initialized")
            }
            Log.i(TAG, "Starting streaming generation")
            Log.d(TAG, "Prompt length: ${prompt.length} chars")
            
            var tokenCount = 0
            val callback = TokenCallback { token ->
                tokenCount++
                if (tokenCount % 10 == 0) {
                    Log.d(TAG, "Streamed $tokenCount tokens")
                }
                onToken(token)
            }
            
            try {
                nativeStream(prompt, callback)
                Log.i(TAG, "Streaming completed: $tokenCount tokens")
            } catch (e: Exception) {
                Log.e(TAG, "Exception during streaming", e)
                throw e
            }
        }
    }

    override fun cleanup() {
        if (isEngineInitialized) {
            Log.i(TAG, "Cleaning up LlamaCppEngine resources")
            nativeCleanup()
            isEngineInitialized = false
            Log.i(TAG, "Cleanup completed")
        } else {
            Log.d(TAG, "Cleanup called but engine not initialized")
        }
    }

    // Native methods
    private external fun nativeInit(
        modelPath: String,
        contextSize: Int,
        threads: Int,
        temperature: Float,
        topK: Int,
        topP: Float,
        repeatPenalty: Float,
        maxTokens: Int
    ): Boolean

    private external fun nativeGenerate(prompt: String): String
    
    private external fun nativeStream(prompt: String, callback: TokenCallback)

    private external fun nativeCleanup()
}

/**
 * Callback wrapper for streaming tokens from native code
 */
class TokenCallback(private val onToken: (String) -> Unit) {
    @Suppress("unused")
    fun invoke(token: String) {
        onToken(token)
    }
}
