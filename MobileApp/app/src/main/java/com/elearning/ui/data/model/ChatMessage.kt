package com.elearning.ui.data.model

data class ChatMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false
)

data class ChatRequest(
    val courseId: String,
    val message: String
)

data class ChatResponse(
    val response: String,
    val sources: List<String> = emptyList()
)

