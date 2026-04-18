package com.axiom.android.sdk.engine

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
     * @return Complete generated text response
     */
    suspend fun generateOnce(prompt: String): String
    
    /**
     * Cancel ongoing generation
     */
    fun cancel()
}
