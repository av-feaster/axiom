package com.axiom.android.sdk.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.axiom.android.sdk.data.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for ChatMessage operations
 */
@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    suspend fun getMessagesForSessionSync(sessionId: Long): List<ChatMessageEntity>

    @Insert
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: Long)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId AND createdAt >= :timestamp")
    suspend fun deleteMessagesAfter(sessionId: Long, timestamp: Long)

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: Long)

    @Transaction
    suspend fun deleteMessagesAndUpdateTimestamp(sessionId: Long, timestamp: Long) {
        deleteMessagesAfter(sessionId, timestamp)
    }

    @Query("SELECT COUNT(*) FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun getMessageCount(sessionId: Long): Int
}
