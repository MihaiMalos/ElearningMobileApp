package com.elearning.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elearning.ui.data.api.ApiConfig
import com.elearning.ui.data.model.Course
import com.elearning.ui.data.model.CourseMaterial
import com.elearning.ui.data.model.User
import com.elearning.ui.data.repository.CourseRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

    private val _enrollmentCount = MutableStateFlow(0)
    val enrollmentCount: StateFlow<Int> = _enrollmentCount

    // Material Content State
    private val _selectedMaterialContent = MutableStateFlow<String?>(null)
    val selectedMaterialContent: StateFlow<String?> = _selectedMaterialContent

    private val _viewingMaterial = MutableStateFlow<CourseMaterial?>(null)
    val viewingMaterial: StateFlow<CourseMaterial?> = _viewingMaterial

    private val _participants = MutableStateFlow<List<User>>(emptyList())
    val participants: StateFlow<List<User>> = _participants
    private val _isLoadingParticipants = MutableStateFlow(false)
    val isLoadingParticipants: StateFlow<Boolean> = _isLoadingParticipants

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
                
                // 3. Load Enrollment Count
                loadEnrollmentCount(id)

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
                // Refresh enrollment status
                _isEnrolled.value = true
                _course.value = _course.value?.copy(isEnrolled = true)
                loadEnrollmentCount(currentCourse.id)
            } else {
                _error.value = "Enrollment failed: ${result.exceptionOrNull()?.message}"
            }
            _isLoading.value = false
        }
    }

    private suspend fun loadEnrollmentCount(courseId: Int) {
         val result = repository.getCourseEnrollments(courseId)
         if (result.isSuccess) {
             _enrollmentCount.value = result.getOrNull()?.size ?: 0
         }
    }

    fun viewMaterial(material: CourseMaterial) {
        viewModelScope.launch {
            _viewingMaterial.value = material
            _selectedMaterialContent.value = null // Reset while loading

            // Only fetch content for text-based or potentially readable files
            if (material.fileType == com.elearning.ui.data.model.FileType.TXT ||
                material.fileName.endsWith(".txt", true) ||
                material.fileName.endsWith(".md", true)) {

                val result = repository.getMaterialContent(material.id)
                if (result.isSuccess) {
                    _selectedMaterialContent.value = result.getOrNull()
                } else {
                    _selectedMaterialContent.value = "Failed to load content: ${result.exceptionOrNull()?.message}"
                }
            } else {
                _selectedMaterialContent.value = "Preview not available for this file type (${material.fileType}).\nThis is where we would implement a PDF viewer or download logic."
            }
        }
    }

    fun closeMaterialViewer() {
        _viewingMaterial.value = null
        _selectedMaterialContent.value = null
    }

    fun fetchTeacherName(teacherId: Int) {
        viewModelScope.launch {
            val result = repository.getTeacherById(teacherId)
            if (result.isSuccess) {
                val teacher = result.getOrNull()
                _course.value = _course.value?.copy(teacherName = teacher?.name ?: "Unknown Teacher")
            } else {
                _error.value = "Failed to fetch teacher name: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun loadParticipants() {
        val currentCourse = _course.value ?: return
        if (_participants.value.isNotEmpty()) return // Already loaded

        viewModelScope.launch {
            _isLoadingParticipants.value = true
            val participantsList = mutableListOf<User>()

            // 1. Add Teacher
            if (currentCourse.teacherId != null) {
                // Try to use cached teacher name if we just fetched it, but we need full User object
                // We likely fetched teacher name but not full object stored in course.
                // Fetch teacher details
                repository.getTeacherById(currentCourse.teacherId).getOrNull()?.let { teacher ->
                    participantsList.add(teacher)
                }
            }

            // 2. Fetch Students
            val enrollmentsResult = repository.getCourseEnrollments(currentCourse.id)
            val enrollments = enrollmentsResult.getOrNull() ?: emptyList()

            val studentIds = enrollments.map { it.studentId }.distinct()

            // Parallel fetch for students
            val students = studentIds.map { studentId ->
                async { repository.getUserById(studentId).getOrNull() }
            }.awaitAll().filterNotNull()

            participantsList.addAll(students)

            _participants.value = participantsList
            _isLoadingParticipants.value = false
        }
    }
}
