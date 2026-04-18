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
    val enableStreaming: Boolean = true
)
