package com.elearning.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elearning.ui.data.api.ApiConfig
import com.elearning.ui.data.model.Course
import com.elearning.ui.data.model.CourseMaterial
import com.elearning.ui.data.repository.CourseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CourseDetailViewModel : ViewModel() {

    private val repository = CourseRepository(ApiConfig.apiService)

    private val _course = MutableStateFlow<Course?>(null)
    val course: StateFlow<Course?> = _course

    private val _materials = MutableStateFlow<List<CourseMaterial>>(emptyList())
    val materials: StateFlow<List<CourseMaterial>> = _materials

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isEnrolled = MutableStateFlow(false)
    val isEnrolled: StateFlow<Boolean> = _isEnrolled
    
    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadCourse(courseId: String) {
        val id = courseId.toIntOrNull()
        if (id == null) {
            _error.value = "Invalid Course ID"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            // 1. Fetch Course Details
            val courseResult = repository.getCourseById(id)
            if (courseResult.isSuccess) {
                val fetchedCourse = courseResult.getOrNull()
                _course.value = fetchedCourse
                
                // Note: fetchedCourse might not have accurate `isEnrolled` if the endpoint 
                // doesn't return it relative to user (which it doesn't).
                // But for now, we leave it as default false from backend or whatever logic we have.
                // Ideally, check enrollments or rely on a "is_enrolled" field from backend if added.
                // Assuming we can check local enrollments:
                
                checkIfEnrolled(id)
                
                // 2. Fetch Materials (only if teacher or enrolled usually, but let's try)
                // If the user is student and not enrolled, this might fail 403.
                loadMaterials(id)
                
            } else {
                _error.value = "Failed to load course: ${courseResult.exceptionOrNull()?.message}"
            }
            
            _isLoading.value = false
        }
    }
    
    private suspend fun checkIfEnrolled(courseId: Int) {
        // This is expensive to call on every detail view if we don't have a single-source-of-truth cache.
        // For MVP, fetch user enrollments and check.
        val result = repository.getUserEnrollments()
        val enrollments = result.getOrNull() ?: emptyList()
        val isEnrolled = enrollments.any { it.courseId == courseId }
        _isEnrolled.value = isEnrolled
        
        // Update the course object too for consistency
        _course.value = _course.value?.copy(isEnrolled = isEnrolled)
    }
    
    private suspend fun loadMaterials(courseId: Int) {
        val result = repository.getCourseMaterials(courseId)
        if (result.isSuccess) {
            _materials.value = result.getOrNull() ?: emptyList()
        } else {
            // It's okay if it fails (e.g. not enrolled), just empty list
            _materials.value = emptyList()
        }
    }

    fun enrollInCourse() {
        val currentCourse = _course.value ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.enrollInCourse(currentCourse.id)
            if (result.isSuccess) {
                _isEnrolled.value = true
                _course.value = _course.value?.copy(isEnrolled = true)
                // Refresh materials now that we are enrolled
                loadMaterials(currentCourse.id)
            } else {
                _error.value = "Enrollment failed: ${result.exceptionOrNull()?.message}"
            }
            _isLoading.value = false
        }
    }
}
