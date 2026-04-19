package com.axiom.android.sdk.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.axiom.android.sdk.data.dao.ChatMessageDao
import com.axiom.android.sdk.data.dao.ChatSessionDao
import com.axiom.android.sdk.data.entity.ChatMessageEntity
import com.axiom.android.sdk.data.entity.ChatSession

/**
 * Room database for Axiom SDK
 * Manages chat sessions and messages with persistence
 */
@Database(
    entities = [ChatSession::class, ChatMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AxiomDatabase : RoomDatabase() {
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AxiomDatabase? = null

        fun getDatabase(context: Context): AxiomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AxiomDatabase::class.java,
                    "axiom_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
