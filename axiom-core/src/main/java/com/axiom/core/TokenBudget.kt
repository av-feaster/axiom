package com.axiom.core

/**
 * Token budget manager for context building
 * Provides script-aware token estimation and budget management
 */
object TokenBudget {
    /**
     * Estimate tokens from text based on script detection
     * - Latin/ASCII: ~4 chars per token
     * - Indic (Devanagari, Bengali, Tamil, etc.): ~1 char per token (worst-case)
     * - CJK: ~1.5 chars per token
     * - Arabic: ~2 chars per token
     */
    fun estimateTokens(text: String): Int {
        if (text.isEmpty()) return 0

        var indicChars = 0
        var cjkChars = 0
        var arabicChars = 0
        val total = text.length

        for (i in text.indices) {
            val cp = text.codePointAt(i)
            when {
                // Indic scripts
                cp in 0x0900..0x097F -> indicChars++ // Devanagari (Hindi)
                cp in 0x0980..0x09FF -> indicChars++ // Bengali
                cp in 0x0B80..0x0BFF -> indicChars++ // Tamil
                cp in 0x0C00..0x0C7F -> indicChars++ // Telugu
                cp in 0x0C80..0x0CFF -> indicChars++ // Kannada
                cp in 0x0D00..0x0D7F -> indicChars++ // Malayalam
                // CJK scripts
                cp in 0x4E00..0x9FFF -> cjkChars++ // CJK Unified
                cp in 0x3040..0x30FF -> cjkChars++ // Hiragana + Katakana
                cp in 0xAC00..0xD7AF -> cjkChars++ // Korean Hangul
                // Arabic
                cp in 0x0600..0x06FF -> arabicChars++
            }
        }

        val dominantNonLatin = maxOf(indicChars, cjkChars, arabicChars)
        val nonLatinFraction = if (total > 0) dominantNonLatin.toDouble() / total else 0.0

        return when {
            nonLatinFraction > 0.3 -> total // Indic worst-case: 1 char/token
            nonLatinFraction > 0.1 -> (total / 2.0).toInt() // Mixed
            else -> (total / 4.0).toInt() // Latin standard
        }
    }

    /**
     * Budget buckets for context building
     */
    data class BudgetConfig(
        val system: Int = 300,
        val pinnedFacts: Int = 100,
        val userProfile: Int = 200,
        val crossSessionMemories: Int = 200,
        val inSessionSummaries: Int = 150,
        val history: Int = 800,
        val total: Int = 3800
    ) {
        companion object {
            val DEFAULT = BudgetConfig()
        }
    }

    /**
     * Truncate text to fit within token budget
     * Uses binary search to find optimal truncation point
     */
    fun truncateToFit(text: String, maxTokens: Int): String {
        if (estimateTokens(text) <= maxTokens) return text

        var lo = 0
        var hi = text.length
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (estimateTokens(text.substring(0, mid)) <= maxTokens) {
                lo = mid
            } else {
                hi = mid - 1
            }
        }

        return text.substring(0, lo) + " [truncated]"
    }

    /**
     * Truncate message with head+tail preservation
     * Keeps beginning and end of message for context
     */
    fun truncateMessage(text: String, maxChars: Int = 2000): String {
        if (text.length <= maxChars) return text

        val headSize = 900
        val tailSize = 900
        return "${text.substring(0, headSize)}\n\n[…truncated…]\n\n${text.substring(text.length - tailSize)}"
    }
}
