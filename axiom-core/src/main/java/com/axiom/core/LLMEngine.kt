package com.axiom.core

/**
 * Interface for LLM inference engines
 */
interface LLMEngine {
    /**
     * Initialize the engine with configuration
     * @param config Engine configuration
     * @return true if initialization succeeded
     */
    suspend fun init(config: LLMConfig): Boolean
    
    /**
     * Generate text from a prompt
     * @param prompt Input text prompt
     * @return Generated text response
     */
    suspend fun generate(prompt: String): String
    
    /**
     * Generate text with streaming callback
     * @param prompt Input text prompt
     * @param onToken Callback for each generated token
     */
    suspend fun stream(prompt: String, onToken: (String) -> Unit)
    
    /**
     * Cleanup and release resources
     */
    fun cleanup()
    
    /**
     * Check if engine is initialized
     */
    val isInitialized: Boolean
}
