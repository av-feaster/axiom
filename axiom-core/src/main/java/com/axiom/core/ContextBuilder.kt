package com.axiom.core

/**
 * Context builder for assembling LLM messages within token budget
 * Handles system prompt, memory injection, and history management
 */
object ContextBuilder {
    private val budget = TokenBudget.BudgetConfig.DEFAULT

    /**
     * Build context messages from components
     * Assembles the final LLMMessage[] array within the token budget
     */
    fun buildContextMessages(params: ContextParams): List<LLMMessage> {
        val systemContent = buildSystemContent(params)
        
        // Build history with budget management
        val historyToInclude = trimHistoryToFitBudget(
            params.history,
            systemContent,
            params.currentMessage
        )
        
        // Final total budget check
        val totalEstimate = TokenBudget.estimateTokens(systemContent) +
            historyToInclude.sumOf { TokenBudget.estimateTokens(it.content) } +
            TokenBudget.estimateTokens(params.currentMessage.content)
        
        if (totalEstimate > budget.total && historyToInclude.size > 4) {
            // Last-resort: cut more history until within budget
            return buildContextMessages(
                params.copy(history = historyToInclude.drop(2))
            )
        }

        return listOf(
            LLMMessage(LLMMessage.MessageRole.SYSTEM, systemContent),
            *historyToInclude.toTypedArray(),
            params.currentMessage
        )
    }

    /**
     * Build system message content from all components
     */
    private fun buildSystemContent(params: ContextParams): String {
        val systemParts = mutableListOf<String>()

        // System prompt (with budget cap)
        val resolvedSystem = TokenBudget.truncateToFit(
            params.systemPrompt,
            budget.system
        )
        systemParts.add(resolvedSystem)

        // Pinned facts
        if (params.pinnedFacts.trim().isNotEmpty()) {
            val facts = if (TokenBudget.estimateTokens(params.pinnedFacts) <= budget.pinnedFacts) {
                params.pinnedFacts
            } else {
                TokenBudget.truncateToFit(params.pinnedFacts, budget.pinnedFacts)
            }
            systemParts.add("\nUser notes: $facts")
        }

        // User profile with explicit labeling to prevent role confusion
        if (params.userProfile.trim().isNotEmpty()) {
            val profile = if (TokenBudget.estimateTokens(params.userProfile) <= budget.userProfile) {
                params.userProfile
            } else {
                TokenBudget.truncateToFit(params.userProfile, budget.userProfile)
            }
            systemParts.add(
                """
                |[Human profile - about the USER, not the assistant]
                |Use these facts only to answer about the user.
                |Never say "I am <user>" or "I am building <user project>".
                |Never say "I am currently working on your project".
                |When referring to these facts, address the user as "you/your".
                |If the user asks about themselves (e.g. "who am I", "what am I building"), reply in second person ("You are...", "You are building...").
                |Do not invent project details, stack choices, or plans not explicitly present in user notes/profile/history.
                |$profile
                """.trimMargin()
            )
        }

        // Cross-session memories
        if (params.crossSessionMemories.isNotEmpty()) {
            val includedMemories = includeMemoriesWithinBudget(params.crossSessionMemories)
            if (includedMemories.isNotEmpty()) {
                systemParts.add("\n[Past conversations]\n${includedMemories.joinToString("\n\n")}")
            }
        }

        // In-session summaries
        if (params.inSessionSummaries.isNotEmpty()) {
            val includedSummaries = includeSummariesWithinBudget(params.inSessionSummaries)
            if (includedSummaries.isNotEmpty()) {
                val block = includedSummaries.joinToString("\n\n") { it.summary }
                systemParts.add("\n[Earlier in this conversation]\n$block")
            }
        }

        return systemParts.joinToString("")
    }

    /**
     * Trim history to fit within budget
     * Keeps minimum 4 turns (8 messages)
     */
    private fun trimHistoryToFitBudget(
        history: List<LLMMessage>,
        systemContent: String,
        currentMessage: LLMMessage
    ): List<LLMMessage> {
        val MIN_HISTORY_TURNS = 4
        var historyToInclude = history.toList()

        while (
            historyToInclude.size > MIN_HISTORY_TURNS &&
            TokenBudget.estimateTokens(
                systemContent +
                historyToInclude.joinToString("") { it.content } +
                currentMessage.content
            ) > budget.total
        ) {
            // Drop oldest pair (user + assistant) from the front
            historyToInclude = historyToInclude.drop(2)
        }

        return historyToInclude
    }

    /**
     * Include memories within budget (highest score first)
     */
    private fun includeMemoriesWithinBudget(memories: List<SessionMemory>): List<String> {
        var budgetLeft = budget.crossSessionMemories
        val included = mutableListOf<String>()
        
        for (mem in memories.sortedByDescending { it.score }) {
            val tokens = TokenBudget.estimateTokens(mem.summary)
            if (budgetLeft >= tokens) {
                included.add(mem.summary)
                budgetLeft -= tokens
            }
        }
        
        return included
    }

    /**
     * Include summaries within budget (oldest first)
     */
    private fun includeSummariesWithinBudget(summaries: List<SessionSummary>): List<SessionSummary> {
        var budgetLeft = budget.inSessionSummaries
        val included = mutableListOf<SessionSummary>()
        
        for (summary in summaries.sortedBy { it.bucketIndex }) {
            val tokens = TokenBudget.estimateTokens(summary.summary)
            if (budgetLeft >= tokens) {
                included.add(summary)
                budgetLeft -= tokens
            }
        }
        
        return included
    }
}

/**
 * Parameters for context building
 */
data class ContextParams(
    val systemPrompt: String = "",
    val pinnedFacts: String = "",
    val userProfile: String = "",
    val crossSessionMemories: List<SessionMemory> = emptyList(),
    val inSessionSummaries: List<SessionSummary> = emptyList(),
    val history: List<LLMMessage> = emptyList(),
    val currentMessage: LLMMessage
)

/**
 * Cross-session memory from past conversations
 */
data class SessionMemory(
    val summary: String,
    val score: Float
)

/**
 * In-session summary for long conversations
 */
data class SessionSummary(
    val summary: String,
    val bucketIndex: Int
)
