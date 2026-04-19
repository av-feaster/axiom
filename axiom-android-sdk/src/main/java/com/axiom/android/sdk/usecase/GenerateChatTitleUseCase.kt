package com.axiom.android.sdk.usecase

import com.axiom.android.sdk.data.repository.ChatSessionRepository
import com.axiom.android.sdk.engine.AxiomEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use case for generating chat titles using the LLM
 * Generates neutral, topic-focused titles at message count milestones
 */
class GenerateChatTitleUseCase(
    private val sessionRepository: ChatSessionRepository,
    private val engine: AxiomEngine
) {
    companion object {
        private fun resolveTitleBucket(totalMessages: Int): Int {
            return when {
                totalMessages in 1..4 -> 1
                totalMessages in 5..10 -> 2
                totalMessages in 15..20 -> 3
                else -> 0
            }
        }
        
        private fun sanitizeTitle(raw: String): String {
            val oneLine = raw.replace("\r?\n".toRegex(), " ").replace("^\"'\\-:\\s+|\"'\\-:\\s+$".toRegex(), "")
            return oneLine.take(48).trim()
        }
    }
    
    suspend fun execute(sessionId: Long, modelId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val session = sessionRepository.getSessionById(sessionId)
                    ?: return@withContext Result.failure(Exception("Session not found"))
                
                if (session.isTitleLocked) {
                    return@withContext Result.success(Unit)
                }
                
                val messages = sessionRepository.getMessagesForSessionSync(sessionId)
                val bucket = resolveTitleBucket(messages.size)
                
                if (bucket == 0 || bucket <= session.lastTitleBucket) {
                    return@withContext Result.success(Unit)
                }
                
                // Build transcript for title generation
                val transcript = messages
                    .map { "${it.role.uppercase()}: ${it.content}" }
                    .joinToString("\n")
                    .take(3000)
                
                val userMessages = messages
                    .filter { it.role == "user" }
                    .map { it.content.trim() }
                    .filter { it.isNotEmpty() }
                
                val firstUserIntent = userMessages.firstOrNull() ?: ""
                val latestUserIntent = userMessages.lastOrNull() ?: ""
                val userIntentOnly = userMessages.joinToString("\n").take(1200)
                
                val titlePrompt = """
                    Generate a neutral, topic-focused chat title for chat history.
                    
                    Rules:
                    - Focus on the user's main intent/topic, not assistant wording style.
                    - Ignore emotional/subjective adjectives from assistant responses.
                    - Prefer stable topic nouns over procedural phrasing.
                    - Keep it 3-6 words, clear and scannable.
                    - Return ONLY the title text.
                    - No quotes, no trailing punctuation.
                    
                    First user intent: $firstUserIntent
                    Latest user intent: $latestUserIntent
                    
                    User messages only:
                    $userIntentOnly
                    
                    Full conversation:
                    $transcript
                """.trimIndent()
                
                // Generate title using engine
                var title = ""
                engine.generate(titlePrompt).collect { token ->
                    title += token
                }
                
                val nextTitle = sanitizeTitle(title)
                if (nextTitle.isNotEmpty()) {
                    sessionRepository.updateSessionTitle(sessionId, nextTitle)
                    sessionRepository.updateTitleLock(sessionId, false, bucket)
                }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
