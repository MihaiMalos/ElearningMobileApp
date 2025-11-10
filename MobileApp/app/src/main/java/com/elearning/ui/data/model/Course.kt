package com.elearning.ui.data.model

data class Course(
    val id: String,
    val title: String,
    val description: String,
    val teacherId: String,
    val teacherName: String,
    val imageUrl: String? = null,
    val category: String,
    val enrolledStudents: Int = 0,
    val materialsCount: Int = 0,
    val createdAt: String,
    val isEnrolled: Boolean = false
)

