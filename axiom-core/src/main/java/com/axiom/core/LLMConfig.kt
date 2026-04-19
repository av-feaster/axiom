package com.axiom.core

/**
 * Configuration for LLM engine initialization
 */
data class LLMConfig(
    val modelPath: String,
    val contextSize: Int = 1024,
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.95f,
    val repeatPenalty: Float = 1.0f,
    val threads: Int = Runtime.getRuntime().availableProcessors(),
    val maxTokens: Int = 512,
    /** Stop tokens to terminate generation early (e.g., ["### User:", "### System:"]) */
    val stopTokens: List<String> = emptyList(),
    /** Enable streaming output with safeguards */
    val enableStreaming: Boolean = true,
    /** Simple prompt string (for backward compatibility) */
    val prompt: String? = null,
    /** Message-based context (alternative to simple prompt) */
    val messages: List<LLMMessage> = emptyList(),
    /** System prompt override (takes precedence over messages[0] if provided) */
    val systemPrompt: String? = null,
    /** Model ID for model-specific stop tokens */
    val modelId: String? = null
) {
    /**
     * Get the effective prompt for generation
     * If messages are provided, formats them as a single prompt string
     * Otherwise uses the simple prompt string
     */
    fun getEffectivePrompt(): String {
        return if (messages.isNotEmpty()) {
            messages.joinToString("\n") { "${it.role.name}: ${it.content}" }
        } else {
            prompt ?: ""
        }
    }
}
