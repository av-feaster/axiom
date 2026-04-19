package com.axiom.core

/**
 * Model-specific configuration including stop tokens
 */
data class ModelConfig(
    val modelId: String,
    val stopTokens: List<String>,
    val contextSize: Int = 2048,
    val maxTokens: Int = 512
) {
    companion object {
        /**
         * Get default stop tokens for a model ID
         */
        fun getDefaultStopTokens(modelId: String?): List<String> {
            return when {
                modelId?.contains("qwen", ignoreCase = true) == true -> {
                    listOf("<|im_end|>", "</s>")
                }
                modelId?.contains("llama", ignoreCase = true) == true -> {
                    listOf("</s>", "<|end_of_text|>")
                }
                modelId?.contains("mistral", ignoreCase = true) == true -> {
                    listOf("</s>")
                }
                modelId?.contains("deepseek", ignoreCase = true) == true -> {
                    listOf("<|im_end|>", "</s>")
                }
                modelId?.contains("gemma", ignoreCase = true) == true -> {
                    listOf("<eos>", "</s>")
                }
                else -> {
                    // Default fallback for unknown models
                    listOf("</s>", "<|im_end|>")
                }
            }
        }

        /**
         * Get or create a ModelConfig from LLMConfig
         */
        fun fromLLMConfig(config: LLMConfig): ModelConfig {
            val effectiveStopTokens = if (config.stopTokens.isNotEmpty()) {
                config.stopTokens
            } else {
                getDefaultStopTokens(config.modelId)
            }

            return ModelConfig(
                modelId = config.modelId ?: "unknown",
                stopTokens = effectiveStopTokens,
                contextSize = config.contextSize,
                maxTokens = config.maxTokens
            )
        }
    }
}
