package com.elearning.ui.data.model

data class User(
    val id: String,
    val email: String,
    val name: String,
    val role: UserRole,
    val avatarUrl: String? = null,
    val createdAt: String
)

enum class UserRole {
    TEACHER,
    STUDENT
}

