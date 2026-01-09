package com.elearning.ui.data.model

import com.google.gson.annotations.SerializedName

data class Enrollment(
    val id: Int,
    @SerializedName("student_id")
    val studentId: Int,
    @SerializedName("course_id")
    val courseId: Int,
    @SerializedName("enrolled_at")
    val enrolledAt: String,
    val progress: Float = 0f,
    
    // Detailed fields from EnrollmentDetailResponse
    @SerializedName("course_title")
    val courseTitle: String? = null,
    @SerializedName("course_description")
    val courseDescription: String? = null,
    @SerializedName("teacher_username")
    val teacherName: String? = null
)

