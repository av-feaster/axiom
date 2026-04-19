package com.axiom.llama.cpp

import android.content.Context
import android.util.Log
import com.axiom.core.FinishReason
import com.axiom.core.GenerationResult
import com.axiom.core.LLMConfig
import com.axiom.core.LLMEngine
import com.axiom.core.ModelConfig
import com.axiom.core.StreamingSafeguard
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Llama.cpp implementation of LLMEngine interface
 */
class LlamaCppEngine : LLMEngine {
    private var isEngineInitialized = false
    private var isGeneratingFlag = false
    private var completionEnd = CompletableDeferred<Unit>()
    private var activeCancelCallback: (() -> Unit)? = null
    private var currentConfig: LLMConfig? = null
    private var currentModelConfig: ModelConfig? = null
    
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

    override val isGenerating: Boolean
        get() = isGeneratingFlag

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
        Log.d(TAG, "Stop tokens: ${config.stopTokens}")
        Log.d(TAG, "Enable streaming: ${config.enableStreaming}")

        try {
            currentConfig = config
            currentModelConfig = ModelConfig.fromLLMConfig(config)
            
            val success = nativeInit(
                config.modelPath,
                config.contextSize,
                config.threads,
                config.temperature,
                config.topK,
                config.topP,
                config.repeatPenalty,
                config.maxTokens,
                currentModelConfig!!.stopTokens.toTypedArray()
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

    override suspend fun generate(prompt: String): GenerationResult = withContext(Dispatchers.IO) {
        if (!isEngineInitialized) {
            Log.e(TAG, "Generate called but engine not initialized")
            throw IllegalStateException("Engine not initialized")
        }
        
        // Wait for any ongoing completion to finish
        completionEnd.await()
        
        // Set up new completion
        val newCompletionEnd = CompletableDeferred<Unit>()
        completionEnd = newCompletionEnd
        isGeneratingFlag = true
        
        Log.i(TAG, "Starting non-streaming generation")
        Log.d(TAG, "Prompt length: ${prompt.length} chars")
        
        try {
            val result = nativeGenerate(prompt)
            val finishReason = determineFinishReason(result, currentModelConfig?.stopTokens ?: emptyList())
            
            Log.i(TAG, "Generation completed: ${result.length} chars, finish reason: $finishReason")
            GenerationResult(result, finishReason)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during generation", e)
            GenerationResult("", FinishReason.ERROR)
        } finally {
            isGeneratingFlag = false
            newCompletionEnd.complete(Unit)
        }
    }

    override suspend fun stream(prompt: String, onToken: (String) -> Unit): GenerationResult {
        withContext(Dispatchers.IO) {
            if (!isEngineInitialized) {
                Log.e(TAG, "Stream called but engine not initialized")
                throw IllegalStateException("Engine not initialized")
            }
            
            // Wait for any ongoing completion to finish
            completionEnd.await()
            
            // Set up new completion
            val newCompletionEnd = CompletableDeferred<Unit>()
            completionEnd = newCompletionEnd
            isGeneratingFlag = true
            
            Log.i(TAG, "Starting streaming generation")
            Log.d(TAG, "Prompt length: ${prompt.length} chars")
            
            var accumulatedText = ""
            var tokenCount = 0
            var finishReason = FinishReason.STOP
            val stopTokens = currentModelConfig?.stopTokens ?: emptyList()
            
            val callback = TokenCallback { token ->
                tokenCount++
                accumulatedText += token
                
                // Log every 10 tokens
                if (tokenCount % 10 == 0) {
                    Log.d(TAG, "Streamed $tokenCount tokens")
                }
                
                // Check for stop conditions using StreamingSafeguard
                if (StreamingSafeguard.shouldStopOnChatEcho(token)) {
                    Log.w(TAG, "Stop condition detected: chat echo marker")
                    finishReason = FinishReason.SAFETY
                    return@TokenCallback
                }
                
                if (StreamingSafeguard.shouldStopOnGarbage(accumulatedText)) {
                    Log.w(TAG, "Stop condition detected: garbage characters")
                    finishReason = FinishReason.SAFETY
                    return@TokenCallback
                }
                
                if (StreamingSafeguard.shouldStopOnRepetition(accumulatedText)) {
                    Log.w(TAG, "Stop condition detected: character repetition")
                    finishReason = FinishReason.SAFETY
                    return@TokenCallback
                }
                
                // Trim token before passing to callback
                val trimmedToken = StreamingSafeguard.trimOnChatEcho(token)
                if (trimmedToken.isNotEmpty()) {
                    onToken(trimmedToken)
                }
            }
            
            try {
                nativeStream(prompt, callback)
                
                // Trim final accumulated text
                val trimmedText = StreamingSafeguard.trimGeneratedText(accumulatedText, stopTokens)
                finishReason = determineFinishReason(trimmedText, stopTokens)
                
                Log.i(TAG, "Streaming completed: $tokenCount tokens, finish reason: $finishReason")
                GenerationResult(trimmedText, finishReason, tokenCount)
            } catch (e: Exception) {
                Log.e(TAG, "Exception during streaming", e)
                GenerationResult(accumulatedText, FinishReason.ERROR, tokenCount)
            } finally {
                isGeneratingFlag = false
                newCompletionEnd.complete(Unit)
            }
        }
    }

    override fun cancelGeneration() {
        if (isGeneratingFlag) {
            Log.i(TAG, "Cancelling generation")
            activeCancelCallback?.invoke()
            nativeCancel()
        }
    }

    override fun cleanup() {
        if (isEngineInitialized) {
            Log.i(TAG, "Cleaning up LlamaCppEngine resources")
            nativeCleanup()
            isEngineInitialized = false
            currentConfig = null
            currentModelConfig = null
            Log.i(TAG, "Cleanup completed")
        } else {
            Log.d(TAG, "Cleanup called but engine not initialized")
        }
    }

    private fun determineFinishReason(text: String, stopTokens: List<String>): FinishReason {
        // Check if stopped by stop token
        for (stopToken in stopTokens) {
            if (text.contains(stopToken, ignoreCase = true)) {
                return FinishReason.STOP
            }
        }
        
        // Check if stopped by safety safeguards
        if (StreamingSafeguard.shouldStopOnChatEcho(text) ||
            StreamingSafeguard.shouldStopOnGarbage(text) ||
            StreamingSafeguard.shouldStopOnRepetition(text)) {
            return FinishReason.SAFETY
        }
        
        // Default to STOP (EOS token)
        return FinishReason.STOP
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
        maxTokens: Int,
        stopTokens: Array<String>
    ): Boolean

    private external fun nativeGenerate(prompt: String): String
    
    private external fun nativeStream(prompt: String, callback: TokenCallback)
    
    private external fun nativeCancel()

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
