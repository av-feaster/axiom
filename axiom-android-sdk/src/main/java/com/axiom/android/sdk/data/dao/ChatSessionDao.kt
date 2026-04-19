package com.axiom.android.sdk.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.axiom.android.sdk.data.entity.ChatSession
import kotlinx.coroutines.flow.Flow

/**
 * DAO for ChatSession operations
 */
@Dao
interface ChatSessionDao {
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<ChatSession>>

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): ChatSession?

    @Insert
    suspend fun insertSession(session: ChatSession): Long

    @Update
    suspend fun updateSession(session: ChatSession)

    @Query("UPDATE chat_sessions SET title = :title, updatedAt = :updatedAt WHERE id = :sessionId")
    suspend fun updateSessionTitle(sessionId: Long, title: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE chat_sessions SET isTitleLocked = :isLocked, lastTitleBucket = :bucket WHERE id = :sessionId")
    suspend fun updateTitleLock(sessionId: Long, isLocked: Boolean, bucket: Int)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("DELETE FROM chat_sessions")
    suspend fun deleteAllSessions()
}
