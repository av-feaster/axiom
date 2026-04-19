package com.axiom.core

import android.util.Log

/**
 * Streaming safeguard for GGUF/llama.cpp chat generation
 * Prevents model hallucination by detecting and stopping unwanted patterns
 */
object StreamingSafeguard {
    private const val TAG = "StreamingSafeguard"

    /**
     * Check if generated text contains any forbidden patterns
     * Returns true if generation should be stopped immediately
     */
    fun shouldStopGeneration(
        generatedText: String,
        stopTokens: List<String>,
        userMarker: String = "### User:"
    ): Boolean {
        // Check stop tokens first (highest priority)
        for (stopToken in stopTokens) {
            if (generatedText.contains(stopToken, ignoreCase = true)) {
                Log.w(TAG, "Stop token detected: '$stopToken'")
                return true
            }
        }

        // Check for user marker hallucination (model generating as User)
        if (generatedText.contains(userMarker, ignoreCase = true)) {
            Log.w(TAG, "User marker hallucination detected: '$userMarker'")
            return true
        }

        // Additional safety checks
        if (detectSystemHallucination(generatedText)) {
            Log.w(TAG, "System marker hallucination detected")
            return true
        }

        return false
    }

    /**
     * Detect if model is generating as System
     */
    private fun detectSystemHallucination(text: String): Boolean {
        val systemMarkers = listOf(
            "### System:",
            "<|im_start|>system",
            "SYSTEM:",
            "System:"
        )
        return systemMarkers.any { text.contains(it, ignoreCase = true) }
    }

    /**
     * Trim the generated text to remove stop tokens and unwanted patterns
     * Returns clean text that should be displayed to the user
     */
    fun trimGeneratedText(
        text: String,
        stopTokens: List<String>,
        userMarker: String = "### User:"
    ): String {
        var cleanText = text

        // Remove content after first stop token
        for (stopToken in stopTokens) {
            val index = cleanText.indexOf(stopToken, ignoreCase = true)
            if (index != -1) {
                cleanText = cleanText.substring(0, index).trim()
                Log.d(TAG, "Trimmed at stop token: '$stopToken'")
                break
            }
        }

        // Remove content after user marker
        val userIndex = cleanText.indexOf(userMarker, ignoreCase = true)
        if (userIndex != -1) {
            cleanText = cleanText.substring(0, userIndex).trim()
            Log.d(TAG, "Trimmed at user marker: '$userMarker'")
        }

        // Remove trailing whitespace and newlines
        cleanText = cleanText.trimEnd()

        return cleanText
    }

    // Chat echo markers for detecting when model starts generating as user
    private val chatEchoMarkers = listOf(
        "### User:",
        "### System:",
        "### Assistant:",
        "User:",
        "System:",
        "Assistant:",
        "<|im_start|>user",
        "<|im_start|>system",
        "<|im_start|>assistant"
    )

    fun shouldStopOnChatEcho(assistantDelta: String): Boolean =
        chatEchoMarkers.any { marker -> assistantDelta.contains(marker, ignoreCase = true) }

    /**
     * Trims [assistantDelta] at the first chat echo marker (exclusive).
     */
    fun trimOnChatEcho(assistantDelta: String): String {
        var cut = assistantDelta.length
        for (marker in chatEchoMarkers) {
            val i = assistantDelta.indexOf(marker, ignoreCase = true)
            if (i >= 0 && i < cut) cut = i
        }
        return assistantDelta.substring(0, cut).trimEnd()
    }

    /**
     * Check if the model is generating garbage characters (consecutive UTF-8 replacement characters)
     * This happens when the model generates out-of-vocabulary tokens or invalid UTF-8
     */
    fun shouldStopOnGarbage(text: String, maxConsecutiveGarbage: Int = 3): Boolean {
        val garbageChar = '\uFFFD' // UTF-8 replacement character
        var consecutiveCount = 0

        for (char in text.reversed()) {
            if (char == garbageChar) {
                consecutiveCount++
                if (consecutiveCount >= maxConsecutiveGarbage) {
                    Log.w(TAG, "Detected $consecutiveCount consecutive garbage characters")
                    return true
                }
            } else {
                break
            }
        }

        return false
    }

    /**
     * Check if the model is repeating the same character (stuck in a loop)
     */
    fun shouldStopOnRepetition(text: String, maxRepetition: Int = 10): Boolean {
        if (text.length < maxRepetition) return false

        val lastChars = text.takeLast(maxRepetition)
        val allSame = lastChars.all { it == lastChars[0] }

        if (allSame) {
            Log.w(TAG, "Detected repetition of character '${lastChars[0]}' $maxRepetition times")
            return true
        }

        return false
    }

    /**
     * Check if the token matches a stop marker prefix (e.g., "User", "Assistant")
     * This is used for buffering suspicious tokens that might form stop markers
     */
    fun matchesStopMarkerPrefix(text: String): Boolean {
        val prefixes = listOf("user", "assistant", "system")
        val textLower = text.lowercase().trim()
        return prefixes.any { it == textLower }
    }

    fun isSafeToDisplay(text: String): Boolean {
        // Discard if text is too short (likely incomplete)
        if (text.length < 3) {
            Log.d(TAG, "Text too short, discarding")
            return false
        }

        // Discard if text starts with forbidden markers
        val forbiddenStarts = listOf(
            "### User:",
            "### System:",
            "<|im_start|>user",
            "<|im_start|>system",
            "USER:",
            "SYSTEM:"
        )

        for (start in forbiddenStarts) {
            if (text.trim().startsWith(start, ignoreCase = true)) {
                Log.w(TAG, "Text starts with forbidden marker: '$start'")
                return false
            }
        }

        return true
    }

    /**
     * Strip DeepSeek R1 thinking tags from text
     * Removes both complete and incomplete thinking blocks
     */
    fun stripThinkingTags(text: String): String {
        return text
            .replace(Regex("<\\|think\\|>[\\s\\S]*?<\\|/think\\|>"), "")  // Complete blocks
            .replace(Regex("<\\|think\\|>[\\s\\S]*"), "")             // Incomplete blocks (aborted mid-generation)
            .trimStart()
    }
}
