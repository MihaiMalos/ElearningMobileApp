package com.elearning.ui.data.api

import com.elearning.ui.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * API Service interface for backend communication
 * When backend is ready, configure base URL in ApiConfig
 */
interface ApiService {

    // Auth endpoints
    @POST("auth/login")
    suspend fun login(
        @Body credentials: LoginRequest
    ): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(
        @Body userData: RegisterRequest
    ): Response<User>

    // Course endpoints
    @GET("courses")
    suspend fun getCourses(): Response<List<Course>>

    @GET("courses/{id}")
    suspend fun getCourseById(
        @Path("id") courseId: String
    ): Response<Course>

    @POST("courses")
    suspend fun createCourse(
        @Body course: CreateCourseRequest
    ): Response<Course>

    @DELETE("courses/{id}")
    suspend fun deleteCourse(
        @Path("id") courseId: String
    ): Response<Unit>

    // Enrollment endpoints
    @POST("enrollments")
    suspend fun enrollInCourse(
        @Body enrollment: EnrollmentRequest
    ): Response<Enrollment>

    @GET("enrollments/user/{userId}")
    suspend fun getUserEnrollments(
        @Path("userId") userId: String
    ): Response<List<Enrollment>>

    // Course materials endpoints
    @GET("courses/{courseId}/materials")
    suspend fun getCourseMaterials(
        @Path("courseId") courseId: String
    ): Response<List<CourseMaterial>>

    @Multipart
    @POST("courses/{courseId}/materials")
    suspend fun uploadMaterials(
        @Path("courseId") courseId: String,
        @Part files: List<okhttp3.MultipartBody.Part>
    ): Response<List<CourseMaterial>>

    @DELETE("materials/{id}")
    suspend fun deleteMaterial(
        @Path("id") materialId: String
    ): Response<Unit>

    // Chat endpoints
    @POST("chat")
    suspend fun sendChatMessage(
        @Body request: ChatRequest
    ): Response<ChatResponse>

    @POST("chat/reindex-all")
    suspend fun reindexAll(): Response<Unit>
}

// Request/Response data classes
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val token: String, val user: User)
data class RegisterRequest(val email: String, val password: String, val name: String, val role: UserRole)
data class CreateCourseRequest(val title: String, val description: String, val category: String)
data class EnrollmentRequest(val courseId: String, val userId: String)

