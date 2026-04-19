package com.axiom.android.sdk.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Simple keyword-based memory repository
 * Stores user facts (name, preferences, project details) for context building
 */
class MemoryRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("axiom_memory", Context.MODE_PRIVATE)
    
    private val _memories = MutableStateFlow<List<UserMemory>>(emptyList())
    val memories: Flow<List<UserMemory>> = _memories.asStateFlow()
    
    init {
        loadMemories()
    }
    
    /**
     * User memory fact
     */
    data class UserMemory(
        val id: String,
        val fact: String,
        val category: String = "general",
        val createdAt: Long = System.currentTimeMillis()
    )
    
    /**
     * Load memories from SharedPreferences
     */
    private fun loadMemories() {
        val memoriesJson = prefs.getString("memories", "[]") ?: "[]"
        val memoriesArray = JSONArray(memoriesJson)
        val memoryList = mutableListOf<UserMemory>()
        
        for (i in 0 until memoriesArray.length()) {
            val memoryObj = memoriesArray.getJSONObject(i)
            memoryList.add(
                UserMemory(
                    id = memoryObj.getString("id"),
                    fact = memoryObj.getString("fact"),
                    category = memoryObj.optString("category", "general"),
                    createdAt = memoryObj.getLong("createdAt")
                )
            )
        }
        
        _memories.value = memoryList
    }
    
    /**
     * Save memories to SharedPreferences
     */
    private fun saveMemories() {
        val memoriesArray = JSONArray()
        _memories.value.forEach { memory ->
            val memoryObj = JSONObject()
            memoryObj.put("id", memory.id)
            memoryObj.put("fact", memory.fact)
            memoryObj.put("category", memory.category)
            memoryObj.put("createdAt", memory.createdAt)
            memoriesArray.put(memoryObj)
        }
        
        prefs.edit().putString("memories", memoriesArray.toString()).apply()
    }
    
    /**
     * Add a memory
     */
    suspend fun addMemory(fact: String, category: String = "general") {
        val memory = UserMemory(
            id = System.currentTimeMillis().toString(),
            fact = fact,
            category = category
        )
        _memories.value = _memories.value + memory
        saveMemories()
    }
    
    /**
     * Remove a memory
     */
    suspend fun removeMemory(id: String) {
        _memories.value = _memories.value.filter { it.id != id }
        saveMemories()
    }
    
    /**
     * Retrieve relevant memories based on keyword matching
     */
    suspend fun getRelevantMemories(query: String, maxResults: Int = 5): List<UserMemory> {
        val queryLower = query.lowercase()
        val queryWords = queryLower.split("\\s+".toRegex())
        
        return _memories.value
            .map { memory ->
                val factLower = memory.fact.lowercase()
                val matchScore = queryWords.count { word -> factLower.contains(word) }
                Pair(memory, matchScore)
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(maxResults)
            .map { it.first }
    }
    
    /**
     * Get all memories as formatted string for context
     */
    fun getFormattedMemories(): String {
        return _memories.value.joinToString("\n") { "- ${it.fact}" }
    }
    
    /**
     * Clear all memories
     */
    suspend fun clearMemories() {
        _memories.value = emptyList()
        saveMemories()
    }
}
