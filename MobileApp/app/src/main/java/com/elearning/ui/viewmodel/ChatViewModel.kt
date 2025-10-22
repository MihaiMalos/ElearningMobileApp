package com.elearning.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.elearning.ui.data.model.ChatMessage

class ChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun initializeChat(courseId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            // Simulate loading initial messages
            _messages.value = listOf(
                ChatMessage("1", "Welcome to the chat!", isUser = false, timestamp = System.currentTimeMillis())
            )
            _isLoading.value = false
        }
    }

    fun sendMessage(message: String) {
        viewModelScope.launch {
            _isLoading.value = true
            // Simulate sending a message
            val newMessage = ChatMessage(
                id = System.currentTimeMillis().toString(),
                content = message,
                isUser = true,
                timestamp = System.currentTimeMillis()
            )
            _messages.value = _messages.value + newMessage
            _isLoading.value = false
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            _messages.value = emptyList()
        }
    }
}
