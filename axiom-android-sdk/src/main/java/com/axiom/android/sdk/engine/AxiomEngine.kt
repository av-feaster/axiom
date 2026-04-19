package com.axiom.android.sdk.engine

import com.axiom.core.FinishReason
import kotlinx.coroutines.flow.Flow

/**
 * Public interface for LLM text generation
 * Wraps the internal LLMEngine to provide a clean SDK API
 */
interface AxiomEngine {
    /**
     * Generate text with streaming output
     * @param prompt Input text prompt
     * @return Flow of generated text tokens
     */
    fun generate(prompt: String): Flow<String>
    
    /**
     * Generate text once (non-streaming)
     * @param prompt Input text prompt
     * @return Complete generated text response with finish reason
     */
    suspend fun generateOnce(prompt: String): GenerationResult
    
    /**
     * Cancel ongoing generation
     */
    fun cancel()
    
    /**
     * Check if engine is currently generating
     */
    val isGenerating: Boolean
}

/**
 * Result of text generation for SDK
 */
data class GenerationResult(
    val text: String,
    val finishReason: FinishReason,
    val tokensGenerated: Int = 0
)
