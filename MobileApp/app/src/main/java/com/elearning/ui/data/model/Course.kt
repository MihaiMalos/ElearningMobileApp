package com.elearning.ui.data.model

import com.google.gson.annotations.SerializedName

data class Course(
    val id: Int,
    val title: String,
    val description: String? = null,
    @SerializedName("teacher_id")
    val teacherId: Int,
    @SerializedName("teacher_username")
    val teacherName: String? = null,
    
    val imageUrl: String? = null,
    val category: String? = null,
    
    @SerializedName("enrollments_count")
    val enrolledStudents: Int = 0,
    
    @SerializedName("materials_count")
    val materialsCount: Int = 0,
    
    @SerializedName("created_at")
    val createdAt: String,
    
    // UI specific fields
    val isEnrolled: Boolean = false
)

