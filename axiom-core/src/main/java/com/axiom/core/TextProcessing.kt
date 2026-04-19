package com.axiom.core

/**
 * Text processing utilities for post-processing LLM responses
 */
object TextProcessing {
    
    /**
     * Detect if the user text is a self-reference question
     * Checks for first-person pronouns combined with question words
     */
    fun isSelfMemoryQuestion(text: String): Boolean {
        val t = text.trim().lowercase()
        val hasSelfRef = Regex("\\b(i|me|my|mine)\\b").containsMatchIn(t)
        val hasMemoryIntent =
            t.contains("?") ||
            Regex("\\b(who|what|where|which|remember|recall|tell|mentioned|said|told|building|from|using)\\b").containsMatchIn(t)
        return hasSelfRef && hasMemoryIntent
    }
    
    /**
     * Enforce second person for self-reference questions
     * Converts "I am" → "You are", "I'm" → "You are", "my" → "your"
     * Only applies when the user asked a self-reference question
     */
    fun enforceSecondPerson(userText: String, assistantText: String): String {
        if (!isSelfMemoryQuestion(userText)) return assistantText
        return assistantText
            .replace(Regex("(^|[.!?]\\s+)i am\\b", RegexOption.IGNORE_CASE), "$1You are")
            .replace(Regex("(^|[.!?]\\s+)i'm\\b", RegexOption.IGNORE_CASE), "$1You are")
            .replace(Regex("(^|[.!?]\\s+)my\\b", RegexOption.IGNORE_CASE), "$1Your")
    }
}
