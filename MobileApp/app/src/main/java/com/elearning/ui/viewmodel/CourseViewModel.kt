package com.elearning.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elearning.ui.data.api.ApiConfig
import com.elearning.ui.data.local.TokenManager
import com.elearning.ui.data.model.Course
import com.elearning.ui.data.model.User
import com.elearning.ui.data.model.UserRole
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

    // Teacher specific state
    private val _isTeacher = MutableStateFlow(false)
    val isTeacher: StateFlow<Boolean> = _isTeacher

    // Student search state
    private val _studentSearchResults = MutableStateFlow<List<User>>(emptyList())
    val studentSearchResults: StateFlow<List<User>> = _studentSearchResults
    private val _isSearchingStudents = MutableStateFlow(false)
    val isSearchingStudents: StateFlow<Boolean> = _isSearchingStudents

    init {
        checkUserRole()
        loadCourses()
    }

    private fun checkUserRole() {
        val role = TokenManager.getUserRole()
        _isTeacher.value = role == UserRole.TEACHER.name
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
            
            // 2. If logged in, fetch enrollments
            if (TokenManager.getToken() != null) {
                if (_isTeacher.value) {
                    val userId = TokenManager.getUserId()
                    // If teacher, filter courses where they are the owner/teacher
                    fetchedCourses = fetchedCourses.map { course ->
                         // We assume teacherId is consistent with userId for ownership
                         // Some backends might not return teacherId on course list, but let's assume it does
                         course
                    }
                } else {
                    // If student, mark enrolled courses
                    val enrollmentsResult = repository.getUserEnrollments()
                    val enrollments = enrollmentsResult.getOrNull() ?: emptyList()
                    val enrolledCourseIds = enrollments.map { it.courseId }.toSet()

                    fetchedCourses = fetchedCourses.map { course ->
                        course.copy(isEnrolled = enrolledCourseIds.contains(course.id))
                    }
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
        return if (_isTeacher.value) {
            // For teacher, show created courses
            val userId = TokenManager.getUserId()
            _courses.value.filter { it.teacherId == userId }
        } else {
            // For student, show enrolled courses
            _courses.value.filter { it.isEnrolled }
        }
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

    fun createCourse(title: String, description: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.createCourse(title, description)
            if (result.isSuccess) {
                loadCourses() // Refresh list
            } else {
                _error.value = "Failed to create course: ${result.exceptionOrNull()?.message}"
            }
            _isLoading.value = false
        }
    }

    fun deleteCourse(courseId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.deleteCourse(courseId)
            if (result.isSuccess) {
                loadCourses() // Refresh list
            } else {
                _error.value = "Failed to delete course: ${result.exceptionOrNull()?.message}"
            }
            _isLoading.value = false
        }
    }

    fun searchStudentsToEnroll(query: String) {
        viewModelScope.launch {
            _isSearchingStudents.value = true
            val result = repository.searchStudents(query)
            if (result.isSuccess) {
                _studentSearchResults.value = result.getOrNull() ?: emptyList()
            } else {
                // handle error silently or clear list
                _studentSearchResults.value = emptyList()
            }
            _isSearchingStudents.value = false
        }
    }

    fun clearStudentSearch() {
        _studentSearchResults.value = emptyList()
    }

    fun enrollStudentInCourse(courseId: Int, studentId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.enrollStudentInCourse(courseId, studentId)
            if (result.isSuccess) {
                // Success message?
                loadCourses() // Refresh mainly to trigger any updates if needed
            } else {
                _error.value = "Failed to enroll student: ${result.exceptionOrNull()?.message}"
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
