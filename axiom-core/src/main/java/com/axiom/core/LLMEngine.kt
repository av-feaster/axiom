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
     * @return Generated text response with finish reason
     */
    suspend fun generate(prompt: String): GenerationResult

    /**
     * Generate text with streaming callback
     * @param prompt Input text prompt
     * @param onToken Callback for each generated token
     * @return Generation result with finish reason
     */
    suspend fun stream(prompt: String, onToken: (String) -> Unit): GenerationResult

    /**
     * Cancel ongoing generation
     */
    fun cancelGeneration()

    /**
     * Cleanup and release resources
     */
    fun cleanup()

    /**
     * Check if engine is initialized
     */
    val isInitialized: Boolean

    /**
     * Check if engine is currently generating
     */
    val isGenerating: Boolean
}

/**
 * Result of text generation
 */
data class GenerationResult(
    val text: String,
    val finishReason: FinishReason,
    val tokensGenerated: Int = 0
)
