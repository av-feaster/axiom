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

        // Check for word/phrase repetition (same word appearing 5+ times)
        val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.size >= 5) {
            val lastWords = words.takeLast(5)
            val uniqueWords = lastWords.toSet()
            if (uniqueWords.size <= 2) {
                Log.w(TAG, "Detected word repetition: ${lastWords.joinToString(" ")}")
                return true
            }
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

/**
 * Result of adding a token to the queue
 */
data class QueueResult(
    val shouldRender: Boolean,
    val content: String?,
    val shouldStop: Boolean
)

/**
 * Token queue for batching tokens during streaming
 * 
 * Features:
 * - Renders first token immediately (eliminates latency)
 * - Batches subsequent tokens for smoother UX
 * - Dynamically adjusts batch size based on remaining context window
 * - Improved stop token detection with batched content
 */
class TokenQueue(
    private val contextSize: Int,
    private val maxTokens: Int,
    private val defaultBatchSize: Int = 3
) {
    private val tokenQueue = mutableListOf<String>()
    private var isFirstToken = true
    private var tokensGenerated = 0
    private val TAG = "TokenQueue"

    /**
     * Add a token to the queue
     * Returns QueueResult indicating whether to render, what content to render, and whether to stop
     */
    fun addToken(token: String, stopTokens: List<String> = emptyList()): QueueResult {
        tokensGenerated++
        
        // First token always renders immediately
        if (isFirstToken) {
            isFirstToken = false
            Log.d(TAG, "Rendering first token immediately")
            return QueueResult(
                shouldRender = true,
                content = token,
                shouldStop = false
            )
        }
        
        // Add token to queue
        tokenQueue.add(token)
        
        // Check for stop tokens in queued content
        val queuedContent = tokenQueue.joinToString("")
        if (checkStopTokens(queuedContent, stopTokens)) {
            Log.w(TAG, "Stop token detected in queue, flushing immediately")
            val trimmedContent = trimStopTokens(queuedContent, stopTokens)
            tokenQueue.clear()
            return QueueResult(
                shouldRender = true,
                content = trimmedContent,
                shouldStop = true
            )
        }
        
        // If queue contains partial stop marker, never flush (wait for more tokens)
        if (queueContainsPartialStopMarker()) {
            Log.d(TAG, "Queue contains partial stop marker, waiting for more tokens")
            return QueueResult(
                shouldRender = false,
                content = null,
                shouldStop = false
            )
        }
        
        // Check if batch is ready to render
        if (shouldFlush()) {
            val content = tokenQueue.joinToString("")
            tokenQueue.clear()
            return QueueResult(
                shouldRender = true,
                content = content,
                shouldStop = false
            )
        }
        
        // Keep queueing
        return QueueResult(
            shouldRender = false,
            content = null,
            shouldStop = false
        )
    }
    
    /**
     * Force flush all queued tokens
     * Called when generation completes or is cancelled
     */
    fun flush(): String {
        val content = tokenQueue.joinToString("")
        tokenQueue.clear()
        return content
    }
    
    /**
     * Determine if batch is ready to render based on dynamic batch size
     */
    private fun shouldFlush(): Boolean {
        val batchSize = calculateDynamicBatchSize()
        return tokenQueue.size >= batchSize
    }
    
    /**
     * Calculate dynamic batch size based on remaining context window
     * Decreases batch size as context fills up
     */
    private fun calculateDynamicBatchSize(): Int {
        val remainingContext = contextSize - tokensGenerated
        return when {
            remainingContext > 500 -> defaultBatchSize
            remainingContext > 200 -> 2
            else -> 1
        }
    }
    
    /**
     * Check if content contains stop tokens (###, User, etc.)
     */
    private fun checkStopTokens(content: String, stopTokens: List<String>): Boolean {
        // Check configured stop tokens
        for (stopToken in stopTokens) {
            if (content.contains(stopToken, ignoreCase = true)) {
                Log.w(TAG, "Stop token detected: '$stopToken'")
                return true
            }
        }
        
        // Check for common chat echo markers
        val commonStopMarkers = listOf(
            "### User:",
            "### System:",
            "### Assistant:",
            "User:",
            "System:",
            "Assistant:"
        )
        
        for (marker in commonStopMarkers) {
            if (content.contains(marker, ignoreCase = true)) {
                Log.w(TAG, "Chat echo marker detected: '$marker'")
                return true
            }
        }
        
        return false
    }
    
    /**
     * Check if content might be a partial stop marker prefix
     * This prevents rendering partial stop markers like "###" before " User:" is received
     */
    private fun mightBePartialStopMarker(content: String): Boolean {
        val prefixes = listOf("#", "###", "User", "System", "Assistant")
        val trimmed = content.trim()
        return prefixes.any { trimmed == it || trimmed.startsWith(it) }
    }
    
    /**
     * Check if any token in the queue is a partial stop marker
     * This prevents flushing a batch that contains a partial stop marker
     */
    private fun queueContainsPartialStopMarker(): Boolean {
        val partialMarkers = listOf("#", "###", "User", "System", "Assistant")
        return tokenQueue.any { token ->
            val trimmed = token.trim()
            partialMarkers.any { marker -> trimmed == marker || trimmed.startsWith(marker) }
        }
    }
    
    /**
     * Remove content after first stop token
     * Also removes partial stop marker prefixes like "###" before the detected stop marker
     */
    private fun trimStopTokens(content: String, stopTokens: List<String>): String {
        Log.d(TAG, "trimStopTokens called with content: '$content'")
        var cleanContent = content
        
        // Check configured stop tokens
        for (stopToken in stopTokens) {
            val index = cleanContent.indexOf(stopToken, ignoreCase = true)
            if (index != -1) {
                cleanContent = cleanContent.substring(0, index)
                Log.d(TAG, "Trimmed at stop token: '$stopToken'")
                break
            }
        }
        
        // Check for common chat echo markers
        val commonStopMarkers = listOf(
            "### User:",
            "### System:",
            "### Assistant:",
            "User:",
            "System:",
            "Assistant:"
        )
        
        for (marker in commonStopMarkers) {
            val index = cleanContent.indexOf(marker, ignoreCase = true)
            if (index != -1) {
                cleanContent = cleanContent.substring(0, index)
                Log.d(TAG, "Trimmed at chat echo marker: '$marker'")
                break
            }
        }
        
        // Remove partial stop marker prefixes (e.g., "###", "User") that might appear before stop marker
        val partialPrefixes = listOf("###", "User", "System", "Assistant")
        for (prefix in partialPrefixes) {
            // Check if content ends with the partial marker (with or without whitespace)
            val trimmedContent = cleanContent.trim()
            Log.d(TAG, "Checking partial prefix '$prefix' against trimmed content: '$trimmedContent'")
            if (trimmedContent.endsWith(prefix)) {
                cleanContent = cleanContent.substring(0, cleanContent.lastIndexOf(prefix))
                Log.d(TAG, "Removed partial stop marker prefix (ends with): '$prefix'")
                break
            }
            // Also check if content starts with partial marker followed by whitespace
            if (trimmedContent.startsWith(prefix)) {
                cleanContent = cleanContent.substring(prefix.length)
                Log.d(TAG, "Removed partial stop marker prefix (starts with): '$prefix'")
                break
            }
        }
        
        Log.d(TAG, "Final trimmed content: '$cleanContent'")
        return cleanContent
    }
    
    /**
     * Get current queue size
     */
    fun getQueueSize(): Int = tokenQueue.size
    
    /**
     * Get number of tokens generated so far
     */
    fun getTokensGenerated(): Int = tokensGenerated
    
    /**
     * Reset the queue state
     */
    fun reset() {
        tokenQueue.clear()
        isFirstToken = true
        tokensGenerated = 0
    }
}
