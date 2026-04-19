package com.axiom.android.sdk.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.axiom.core.ChatMode

/**
 * Chat session entity for Room database
 * Represents a single conversation session with metadata
 */
@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val modelId: String,
    val chatMode: String = ChatMode.GENERAL.name,
    val isTitleLocked: Boolean = false,
    val lastTitleBucket: Int = 0
) {
    companion object {
        fun fromModelId(modelId: String): ChatSession {
            return ChatSession(
                title = "New Chat",
                modelId = modelId,
                chatMode = ChatMode.GENERAL.name
            )
        }
    }
}
