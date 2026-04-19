package com.axiom.core

/**
 * Chat modes with mode-specific system prompts
 */
enum class ChatMode(val displayName: String, val systemPrompt: String) {
    GENERAL(
        displayName = "General",
        systemPrompt = "You are a helpful AI assistant. Be concise, accurate, and friendly. Provide clear and helpful responses."
    ),
    CODING(
        displayName = "Coding",
        systemPrompt = "You are an expert programmer. Provide clean, well-commented code. Explain your solutions clearly. Use best practices and follow coding standards."
    ),
    CREATIVE(
        displayName = "Creative",
        systemPrompt = "You are a creative assistant. Be imaginative, expressive, and original. Help with creative writing, brainstorming, and artistic projects."
    ),
    MARKETING(
        displayName = "Marketing",
        systemPrompt = "You are a marketing expert. Provide persuasive, clear, and effective marketing copy. Focus on benefits, value propositions, and customer engagement."
    )
}
