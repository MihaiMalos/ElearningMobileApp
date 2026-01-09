package com.elearning.ui.data.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: Int,
    val email: String,
    @SerializedName("username")
    val name: String,
    val role: UserRole,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

enum class UserRole {
    @SerializedName("teacher")
    TEACHER,
    @SerializedName("student")
    STUDENT
}

data class AuthToken(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String
)

