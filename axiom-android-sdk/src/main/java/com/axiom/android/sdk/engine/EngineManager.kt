package com.axiom.android.sdk.engine

import com.axiom.android.sdk.AxiomSDK
import com.axiom.android.sdk.AxiomState
import com.axiom.core.LLMConfig
import com.axiom.core.LLMEngine
import com.axiom.llama.cpp.LlamaCppEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Wrapper implementation of AxiomEngine
 * Manages the internal LLMEngine and provides Flow-based streaming API
 */
class EngineManager : AxiomEngine {
    
    private val internalEngine: LLMEngine = LlamaCppEngine()
    private var currentGenerationJob: Job? = null
    private var isInitialized = false
    
    /**
     * Initialize the engine with configuration
     * @param config Engine configuration
     * @return true if initialization succeeded
     */
    suspend fun initialize(config: LLMConfig): Boolean = withContext(Dispatchers.IO) {
        val result = internalEngine.init(config)
        isInitialized = result
        result
    }
    
    /**
     * Generate text with streaming output
     * @param prompt Input text prompt
     * @return Flow of generated text tokens
     */
    override fun generate(prompt: String): Flow<String> = callbackFlow {
        if (!isInitialized) {
            close(IllegalStateException("Engine not initialized. Call initialize() first."))
            return@callbackFlow
        }
        
        AxiomSDK.updateState(AxiomState.Generating)
        
        currentGenerationJob = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                internalEngine.stream(prompt) { token ->
                    trySend(token)
                }
                AxiomSDK.updateState(AxiomState.Ready)
            } catch (e: Exception) {
                AxiomSDK.updateState(AxiomState.Error(e.message ?: "Generation failed"))
                close(e)
            }
        }
        
        awaitClose {
            currentGenerationJob?.cancel()
            AxiomSDK.updateState(AxiomState.Ready)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Generate text once (non-streaming)
     * @param prompt Input text prompt
     * @return Complete generated text response
     */
    override suspend fun generateOnce(prompt: String): String {
        if (!isInitialized) {
            throw IllegalStateException("Engine not initialized. Call initialize() first.")
        }
        return withContext(Dispatchers.IO) {
            internalEngine.generate(prompt)
        }
    }
    
    /**
     * Cancel ongoing generation
     */
    override fun cancel() {
        currentGenerationJob?.cancel()
        currentGenerationJob = null
    }
    
    /**
     * Cleanup and release resources
     */
    fun cleanup() {
        cancel()
        internalEngine.cleanup()
        isInitialized = false
    }
    
    /**
     * Check if engine is initialized
     */
    fun isEngineInitialized(): Boolean = isInitialized
}
