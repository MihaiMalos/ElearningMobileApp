package com.elearning.ui.data.api

import com.elearning.ui.data.model.*
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * API Service interface for backend communication
 * Backend Base URL: http://10.0.2.2:8000/api/v1/
 */
interface ApiService {

    // Auth endpoints
    @FormUrlEncoded
    @POST("auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<AuthToken>

    @POST("auth/register")
    suspend fun register(
        @Body userData: RegisterRequest
    ): Response<User>

    @GET("users/me")
    suspend fun getCurrentUser(): Response<User>

    // Course endpoints
    @GET("courses/")
    suspend fun getCourses(
        @Query("search") search: String? = null
    ): Response<List<Course>>

    @GET("courses/{id}")
    suspend fun getCourseById(
        @Path("id") courseId: Int
    ): Response<Course>

    @POST("courses/")
    suspend fun createCourse(
        @Body course: CreateCourseRequest
    ): Response<Course>

    @DELETE("courses/{id}")
    suspend fun deleteCourse(
        @Path("id") courseId: Int
    ): Response<Unit>

    // Enrollment endpoints
    @POST("enrollments/")
    suspend fun enrollInCourse(
        @Body enrollment: EnrollmentRequest
    ): Response<Enrollment>

    @GET("enrollments/my-enrollments")
    suspend fun getUserEnrollments(
    ): Response<List<Enrollment>> // Note: May need EnrollmentDetailResponse matching if fields differ

    @GET("enrollments/course/{courseId}")
    suspend fun getCourseEnrollments(
        @Path("courseId") courseId: Int
    ): Response<List<Enrollment>>

    // Course materials endpoints
    @GET("files/course/{courseId}")
    suspend fun getCourseMaterials(
        @Path("courseId") courseId: Int
    ): Response<List<CourseMaterial>>

    @Multipart
    @POST("files/upload/{courseId}")
    suspend fun uploadMaterials(
        @Path("courseId") courseId: Int,
        @Part files: List<MultipartBody.Part>
    ): Response<FileUploadResponse>

    @DELETE("files/{id}")
    suspend fun deleteMaterial(
        @Path("id") materialId: Int
    ): Response<Unit>

    // Chat endpoints
    @POST("chat/")
    suspend fun sendChatMessage(
        @Body request: ChatRequest
    ): Response<ChatResponse>

    // User endpoints
    @GET("users/{id}")
    suspend fun getUserById(
        @Path("id") userId: Int
    ): Response<User>
}

// Request/Response data classes
data class RegisterRequest(
    val email: String, 
    val username: String, 
    val password: String, 
    val role: UserRole
)

data class CreateCourseRequest(val title: String, val description: String?)

data class EnrollmentRequest(@SerializedName("course_id") val courseId: Int)

data class FileUploadResponse(
    @SerializedName("uploaded_files")
    val uploadedFiles: List<CourseMaterial>,
    @SerializedName("total_files")
    val totalFiles: Int,
    @SerializedName("failed_files")
    val failedFiles: List<String> = emptyList()
)
