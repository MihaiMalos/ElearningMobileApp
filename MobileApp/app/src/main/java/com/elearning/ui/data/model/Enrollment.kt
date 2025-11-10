package com.elearning.ui.data.model

data class Enrollment(
    val id: String,
    val userId: String,
    val courseId: String,
    val enrolledAt: String,
    val progress: Float = 0f
)

