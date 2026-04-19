package com.axiom.android.sdk.engine

import android.app.Application
import com.axiom.android.sdk.AxiomSDKConfig
import com.axiom.android.sdk.AxiomState
import com.axiom.android.sdk.AxiomStateStore
import com.axiom.core.FinishReason
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
object EngineManager : AxiomEngine {
    
    private val internalEngine: LLMEngine = LlamaCppEngine()
    private var currentGenerationJob: Job? = null
    private var isInitialized = false
    private lateinit var config: AxiomSDKConfig
    override var isGenerating: Boolean = false
    
    /**
     * Initialize the engine manager with application and config
     * @param application Application context
     * @param config SDK configuration
     */
    fun init(application: Application, config: AxiomSDKConfig) {
        this.config = config
        // Engine will be initialized when a model is loaded
    }
    
    /**
     * Get the singleton instance
     * @return EngineManager instance
     */
    fun get(): EngineManager = this
    
    /**
     * Initialize the engine with configuration
     * @param config Engine configuration
     * @return true if initialization succeeded
     */
    suspend fun initialize(config: LLMConfig): Boolean = withContext(Dispatchers.IO) {
        val result = internalEngine.init(config)
        isInitialized = result
        if (result) {
            AxiomStateStore.setState(AxiomState.Ready)
        }
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
        
        isGenerating = true
        AxiomStateStore.setState(AxiomState.Generating)
        
        currentGenerationJob = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                internalEngine.stream(prompt) { token ->
                    trySend(token)
                }
                AxiomStateStore.setState(AxiomState.Ready)
            } catch (e: Exception) {
                AxiomStateStore.setState(AxiomState.Error(e.message ?: "Generation failed"))
                close(e)
            } finally {
                isGenerating = false
            }
        }
        
        awaitClose {
            currentGenerationJob?.cancel()
            isGenerating = false
            AxiomStateStore.setState(AxiomState.Ready)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Generate text once (non-streaming)
     * @param prompt Input text prompt
     * @return Complete generated text response with finish reason
     */
    override suspend fun generateOnce(prompt: String): GenerationResult {
        if (!isInitialized) {
            throw IllegalStateException("Engine not initialized. Call initialize() first.")
        }
        return withContext(Dispatchers.IO) {
            val coreResult = internalEngine.stream(prompt) {}
            GenerationResult(
                text = coreResult.text,
                finishReason = coreResult.finishReason,
                tokensGenerated = coreResult.tokensGenerated
            )
        }
    }
    
    /**
     * Cancel ongoing generation
     */
    override fun cancel() {
        currentGenerationJob?.cancel()
        currentGenerationJob = null
        isGenerating = false
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
