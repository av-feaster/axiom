package com.axiom.core

/**
 * Production-ready prompt templates for GGUF/llama.cpp chat
 * Designed to prevent model hallucination and ensure clean Assistant-only responses
 */
object PromptTemplates {

    /**
     * Alpaca-style template (recommended for tinyllama-1.1b and similar small models)
     * Clean format with clear role markers
     */
    fun alpaca(
        systemInstruction: String,
        conversationHistory: String,
        userMessage: String
    ): String {
        return buildString {
            // System instruction at the top
            append("### System:\n")
            append(systemInstruction.trim())
            append("\n\n")

            // Conversation history (already formatted)
            append(conversationHistory.trim())

            // Current user message
            append("### User:\n")
            append(userMessage.trim())
            append("\n")

            // Assistant marker - model completes from here
            append("### Assistant:\n")
        }
    }

    /**
     * ChatML-style template (for models trained on ChatML format)
     * More verbose but clearer structure
     */
    fun chatML(
        systemInstruction: String,
        conversationHistory: String,
        userMessage: String
    ): String {
        return buildString {
            // System instruction
            append("<|im_start|>system\n")
            append(systemInstruction.trim())
            append("<|im_end|>\n")

            // Conversation history
            append(conversationHistory.trim())
            if (conversationHistory.isNotEmpty()) {
                append("\n")
            }

            // Current user message
            append("<|im_start|>user\n")
            append(userMessage.trim())
            append("<|im_end|>\n")

            // Assistant marker
            append("<|im_start|>assistant\n")
        }
    }

    /**
     * Vicuna-style template (for Vicuna and Vicuna-based models)
     */
    fun vicuna(
        systemInstruction: String,
        conversationHistory: String,
        userMessage: String
    ): String {
        return buildString {
            // System instruction
            if (systemInstruction.isNotBlank()) {
                append("A chat between a curious user and an artificial intelligence assistant. ")
                append("The assistant gives helpful, detailed, and polite answers to the user's questions.\n\n")
            }

            // Conversation history
            append(conversationHistory.trim())
            if (conversationHistory.isNotEmpty()) {
                append("\n")
            }

            // Current user message
            append("USER: ")
            append(userMessage.trim())
            append("\n")

            // Assistant marker
            append("ASSISTANT: ")
        }
    }

    /**
     * Format a single message for conversation history
     */
    fun formatMessage(role: String, content: String, template: String = "alpaca"): String {
        return when (template.lowercase()) {
            "chatml" -> {
                val roleTag = if (role == "user") "user" else "assistant"
                "<|im_start|>$roleTag\n${content.trim()}<|im_end|>\n"
            }
            "vicuna" -> {
                val roleTag = if (role == "user") "USER" else "ASSISTANT"
                "$roleTag: ${content.trim()}\n"
            }
            else -> { // alpaca (default)
                val roleTag = if (role == "user") "User" else "Assistant"
                "### $roleTag:\n${content.trim()}\n"
            }
        }
    }

    /**
     * Get stop tokens for the specified template
     */
    fun getStopTokens(template: String = "alpaca"): List<String> {
        return when (template.lowercase()) {
            "chatml" -> listOf("<|im_end|>", "<|im_start|>user", "<|im_start|>system")
            "vicuna" -> listOf("USER:", "ASSISTANT:")
            else -> listOf("### User:", "### System:")
        }
    }

    /**
     * Get the assistant marker for the specified template
     * Used to detect if the model is hallucinating as user
     */
    fun getAssistantMarker(template: String = "alpaca"): String {
        return when (template.lowercase()) {
            "chatml" -> "<|im_start|>user"
            "vicuna" -> "USER:"
            else -> "### User:"
        }
    }
}
