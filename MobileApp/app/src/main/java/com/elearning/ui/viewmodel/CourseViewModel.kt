package com.elearning.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elearning.ui.data.api.ApiConfig
import com.elearning.ui.data.local.TokenManager
import com.elearning.ui.data.model.Course
import com.elearning.ui.data.repository.CourseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CourseViewModel : ViewModel() {

    private val repository = CourseRepository(ApiConfig.apiService)

    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _enrollmentCounts = mutableMapOf<Int, MutableStateFlow<Int>>()

    init {
        loadCourses()
    }

    fun loadCourses() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            // 1. Fetch Courses
            val coursesResult = repository.getCourses()
            var fetchedCourses = coursesResult.getOrElse {
                _error.value = "Failed to load courses: ${it.message}"
                emptyList()
            }
            
            // 2. If logged in, fetch enrollments to mark courses as enrolled
            if (TokenManager.getToken() != null && fetchedCourses.isNotEmpty()) {
                val enrollmentsResult = repository.getUserEnrollments()
                val enrollments = enrollmentsResult.getOrNull() ?: emptyList()
                val enrolledCourseIds = enrollments.map { it.courseId }.toSet()
                
                fetchedCourses = fetchedCourses.map { course ->
                    course.copy(isEnrolled = enrolledCourseIds.contains(course.id))
                }
            }
            
            _courses.value = fetchedCourses
            _isLoading.value = false
        }
    }

    fun searchCourses(query: String) {
        _searchQuery.value = query
    }

    fun getFilteredCourses(): List<Course> {
        var filtered = _courses.value

        // Filter by search query
        if (_searchQuery.value.isNotBlank()) {
            filtered = filtered.filter { course ->
                course.title.contains(_searchQuery.value, ignoreCase = true) ||
                (course.description?.contains(_searchQuery.value, ignoreCase = true) == true) ||
                (course.teacherName?.contains(_searchQuery.value, ignoreCase = true) == true)
            }
        }

        return filtered
    }

    fun getMyEnrolledCourses(): List<Course> {
        return _courses.value.filter { it.isEnrolled }
    }
    
    fun enrollInCourse(courseId: Int) {
        viewModelScope.launch {
             _isLoading.value = true
             val result = repository.enrollInCourse(courseId)
             if (result.isSuccess) {
                 // Refresh list to update UI
                 loadCourses()
             } else {
                 _error.value = "Enrollment failed: ${result.exceptionOrNull()?.message}"
             }
             _isLoading.value = false
        }
    }

    fun fetchTeacherName(teacherId: Int) {
        viewModelScope.launch {
            val result = repository.getTeacherById(teacherId)
            if (result.isSuccess) {
                val teacher = result.getOrNull()
                // Update the teacherName for all courses with this teacherId
                _courses.value = _courses.value.map { course ->
                    if (course.teacherId == teacherId) {
                        course.copy(teacherName = teacher?.name ?: "Unknown Teacher")
                    } else course
                }
            } else {
                _error.value = "Failed to fetch teacher name: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun getCourseEnrollmentCount(courseId: Int): StateFlow<Int> {
        val flow = _enrollmentCounts.getOrPut(courseId) { MutableStateFlow(0) }
        viewModelScope.launch {
            val result = repository.getCourseEnrollments(courseId)
            if (result.isSuccess) {
                flow.value = result.getOrNull()?.size ?: 0
            } else {
                flow.value = 0
            }
        }
        return flow.asStateFlow()
    }
}
