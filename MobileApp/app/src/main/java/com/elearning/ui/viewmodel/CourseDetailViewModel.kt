package com.elearning.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elearning.ui.data.model.Course
import com.elearning.ui.data.model.CourseMaterial
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CourseDetailViewModel : ViewModel() {

    private val _course = MutableStateFlow<Course?>(null)
    val course: StateFlow<Course?> = _course

    private val _materials = MutableStateFlow<List<CourseMaterial>>(emptyList())
    val materials: StateFlow<List<CourseMaterial>> = _materials

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isEnrolled = MutableStateFlow(false)
    val isEnrolled: StateFlow<Boolean> = _isEnrolled

    fun loadCourse(courseId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            // Simulate loading course details
            _course.value = Course(
                id = courseId,
                title = "Sample Course",
                description = "This is a sample course description.",
                teacherId = "teacher1",
                teacherName = "John Doe",
                imageUrl = null,
                category = "Category",
                enrolledStudents = 100,
                materialsCount = 10,
                createdAt = "2025-10-01T10:00:00Z",
                isEnrolled = true
            )
            _materials.value = listOf(
                CourseMaterial(
                    id = "material1",
                    courseId = courseId,
                    fileName = "Material 1.pdf",
                    fileType = com.elearning.ui.data.model.FileType.PDF,
                    fileSize = 1024,
                    uploadedAt = "2025-10-01T10:00:00Z",
                    uploadedBy = "John Doe"
                )
            )
            _isEnrolled.value = true
            _isLoading.value = false
        }
    }

    fun enrollInCourse() {
        viewModelScope.launch {
            _isEnrolled.value = true
            // Simulate enrolling in the course
        }
    }
}
