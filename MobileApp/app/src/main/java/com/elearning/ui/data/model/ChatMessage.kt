package com.elearning.ui.data.model

import com.google.gson.annotations.SerializedName

data class ChatMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false
)

data class ChatRequest(
    @SerializedName("course_id")
    val courseId: Int,
    @SerializedName("question")
    val message: String
)

data class ChatResponse(
    @SerializedName("answer")
    val response: String,
    @SerializedName("retrieved_chunks")
    val retrievedChunks: Int = 0
)

