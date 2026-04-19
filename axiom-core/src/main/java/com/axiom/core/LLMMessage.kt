package com.axiom.core

/**
 * Represents a message in a conversation with the LLM
 */
data class LLMMessage(
    val role: MessageRole,
    val content: String
) {
    enum class MessageRole {
        SYSTEM,
        USER,
        ASSISTANT
    }
}
