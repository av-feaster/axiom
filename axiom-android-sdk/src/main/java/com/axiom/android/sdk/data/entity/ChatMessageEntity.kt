package com.axiom.android.sdk.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Chat message entity for Room database
 * Represents a single message within a chat session
 */
@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val role: String, // "user" or "assistant"
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val tokenCount: Int = 0,
    val generationTimeMs: Long = 0,
    val isStreaming: Boolean = false
)
