package com.axiom.llama.cpp

import android.content.Context
import com.axiom.core.LLMConfig
import com.axiom.core.LLMEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Llama.cpp implementation of LLMEngine interface
 */
class LlamaCppEngine : LLMEngine {
    private var isEngineInitialized = false
    
    override val isInitialized: Boolean
        get() = isEngineInitialized

    override suspend fun init(config: LLMConfig): Boolean = withContext(Dispatchers.IO) {
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
            success
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        if (!isEngineInitialized) {
            throw IllegalStateException("Engine not initialized")
        }
        nativeGenerate(prompt)
    }

    override suspend fun stream(prompt: String, onToken: (String) -> Unit) = withContext(Dispatchers.IO) {
        if (!isEngineInitialized) {
            throw IllegalStateException("Engine not initialized")
        }
        val callback = TokenCallback(onToken)
        nativeStream(prompt, callback)
    }

    override fun cleanup() {
        if (isEngineInitialized) {
            nativeCleanup()
            isEngineInitialized = false
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

    companion object {
        init {
            System.loadLibrary("llama_jni")
        }
    }
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
