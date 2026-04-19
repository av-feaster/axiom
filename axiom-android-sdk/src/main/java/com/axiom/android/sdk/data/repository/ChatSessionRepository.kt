package com.axiom.android.sdk.data.repository

import com.axiom.android.sdk.data.dao.ChatMessageDao
import com.axiom.android.sdk.data.dao.ChatSessionDao
import com.axiom.android.sdk.data.entity.ChatMessageEntity
import com.axiom.android.sdk.data.entity.ChatSession
import kotlinx.coroutines.flow.Flow

/**
 * Repository for chat session operations
 * Provides a clean API for session and message management
 */
class ChatSessionRepository(
    private val sessionDao: ChatSessionDao,
    private val messageDao: ChatMessageDao
) {
    /**
     * Get all sessions ordered by update time
     */
    fun getAllSessions(): Flow<List<ChatSession>> {
        return sessionDao.getAllSessions()
    }

    /**
     * Get a specific session by ID
     */
    suspend fun getSessionById(sessionId: Long): ChatSession? {
        return sessionDao.getSessionById(sessionId)
    }

    /**
     * Get messages for a session
     */
    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>> {
        return messageDao.getMessagesForSession(sessionId)
    }

    /**
     * Get messages for a session synchronously
     */
    suspend fun getMessagesForSessionSync(sessionId: Long): List<ChatMessageEntity> {
        return messageDao.getMessagesForSessionSync(sessionId)
    }

    /**
     * Create a new session
     */
    suspend fun createSession(modelId: String, chatMode: String = "GENERAL"): Long {
        val session = ChatSession.fromModelId(modelId).copy(chatMode = chatMode)
        return sessionDao.insertSession(session)
    }

    /**
     * Update session timestamp
     */
    suspend fun updateSessionTimestamp(sessionId: Long) {
        val session = sessionDao.getSessionById(sessionId) ?: return
        sessionDao.updateSession(session.copy(updatedAt = System.currentTimeMillis()))
    }

    /**
     * Update session title
     */
    suspend fun updateSessionTitle(sessionId: Long, title: String) {
        sessionDao.updateSessionTitle(sessionId, title)
    }

    /**
     * Lock/unlock session title
     */
    suspend fun updateTitleLock(sessionId: Long, isLocked: Boolean, bucket: Int) {
        sessionDao.updateTitleLock(sessionId, isLocked, bucket)
    }

    /**
     * Add a message to a session
     */
    suspend fun addMessage(sessionId: Long, role: String, content: String): Long {
        val message = ChatMessageEntity(
            sessionId = sessionId,
            role = role,
            content = content
        )
        return messageDao.insertMessage(message)
    }

    /**
     * Update assistant message with generation stats
     */
    suspend fun updateAssistantMessage(
        messageId: Long,
        content: String,
        tokenCount: Int,
        generationTimeMs: Long,
        isStreaming: Boolean
    ) {
        // Note: This would require an update method in the DAO
        // For now, we'll implement a simpler approach
    }

    /**
     * Delete all messages after a specific timestamp (for edit functionality)
     */
    suspend fun deleteMessagesAfter(sessionId: Long, timestamp: Long) {
        messageDao.deleteMessagesAfter(sessionId, timestamp)
    }

    /**
     * Delete a session and all its messages
     */
    suspend fun deleteSession(sessionId: Long) {
        messageDao.deleteMessagesForSession(sessionId)
        sessionDao.deleteSession(sessionId)
    }

    /**
     * Get message count for a session
     */
    suspend fun getMessageCount(sessionId: Long): Int {
        return messageDao.getMessageCount(sessionId)
    }
}
