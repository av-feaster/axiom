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

    /**
     * Validate that the generated text is safe to display
     * Returns false if the text should be discarded entirely
     */
    /**
     * Markers that indicate the model is hallucinating the next chat turn (common with plain-text
     * prompts like `User:` / `Assistant:`). Stop streaming when any of these appear in the
     * assistant delta only (not the full prompt).
     */
    private val chatEchoMarkers: List<String> = listOf(
        "\n### User:",
        "\r\n### User:",
        "\nUser:",
        "\r\nUser:",
        "\n### Assistant:",
        "\nAssistant:",
        "\r\nAssistant:",
        "### User:",
        "### Assistant:",
        "<|im_start|>user",
        "<|im_start|>assistant",
    )

    /**
     * True if [assistantDelta] contains a role boundary echo and generation should stop.
     */
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
}
