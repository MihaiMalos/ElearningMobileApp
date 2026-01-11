package com.elearning.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elearning.ui.data.api.ApiConfig
import com.elearning.ui.data.model.ChatMessage
import com.elearning.ui.data.model.ChatRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val apiService = ApiConfig.apiService

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var currentCourseId: Int = -1

    fun initializeChat(courseId: Int) {
        currentCourseId = courseId
        // Backend doesn't persist chat history yet, so we start fresh or from local DB.
        // For now, start fresh.
        _messages.value = listOf(
             ChatMessage(
                 id = "welcome", 
                 content = "Hello! I am your AI assistant for this course. Ask me anything about the course materials.", 
                 isUser = false
             )
        )
    }

    fun sendMessage(message: String) {
        if (message.isBlank()) return
        
        val userMsgId = System.currentTimeMillis().toString()
        val userMessage = ChatMessage(
            id = userMsgId,
            content = message,
            isUser = true
        )
        
        // Add user message immediately
        val currentList = _messages.value
        _messages.value = currentList + userMessage
        
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val response = apiService.sendChatMessage(
                    ChatRequest(
                        courseId = currentCourseId,
                        message = message
                    )
                )
                
                if (response.isSuccessful && response.body() != null) {
                    val botResponse = response.body()!!
                    val botMessage = ChatMessage(
                        id = System.currentTimeMillis().toString(),
                        content = botResponse.response,
                        isUser = false
                    )
                    _messages.value = _messages.value + botMessage
                } else {
                     val errorMessage = ChatMessage(
                        id = System.currentTimeMillis().toString(),
                        content = "Error: ${response.message()}",
                        isUser = false
                    )
                    _messages.value = _messages.value + errorMessage
                }
            } catch (e: Exception) {
                 val errorMessage = ChatMessage(
                    id = System.currentTimeMillis().toString(),
                    content = "Connection error: ${e.message}",
                    isUser = false
                )
                _messages.value = _messages.value + errorMessage
            }
            
            _isLoading.value = false
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            _messages.value = emptyList()
        }
    }
}
