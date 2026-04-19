package com.axiom.android.sdk.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axiom.android.sdk.engine.AxiomEngine
import com.axiom.android.sdk.ui.components.chat.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Internal ViewModel for chat functionality
 * Depends on AxiomEngine (not LLMEngine) to maintain clean architecture
 */
internal class ChatViewModel(
    private val engine: AxiomEngine
) : ViewModel() {
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * Send a message and generate AI response
     * @param message User message to send
     */
    fun sendMessage(message: String) {
        if (message.isBlank()) return
        
        viewModelScope.launch {
            try {
                // Add user message
                val userMessage = ChatMessage(
                    role = "user",
                    content = message
                )
                _messages.value = _messages.value + userMessage
                
                // Create empty AI message
                val aiMessage = ChatMessage(
                    role = "assistant",
                    content = ""
                )
                _messages.value = _messages.value + aiMessage
                
                _isGenerating.value = true
                
                // Stream AI response
                var fullResponse = ""
                engine.generate(message).collect { token ->
                    fullResponse += token
                    val lastIndex = _messages.value.lastIndex
                    _messages.value = _messages.value.toMutableList().apply {
                        this[lastIndex] = this[lastIndex].copy(content = fullResponse)
                    }
                }
                
                _isGenerating.value = false
            } catch (e: Exception) {
                _isGenerating.value = false
                _error.value = e.message ?: "Generation failed"
            }
        }
    }
    
    /**
     * Stop ongoing generation
     */
    fun stopGeneration() {
        engine.cancel()
        _isGenerating.value = false
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }
}
