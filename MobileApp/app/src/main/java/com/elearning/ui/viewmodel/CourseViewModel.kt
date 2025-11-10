package com.elearning.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elearning.ui.data.model.Course
import com.elearning.ui.data.repository.DataInitializer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CourseViewModel : ViewModel() {

    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    init {
        loadCourses()
    }

    fun loadCourses() {
        viewModelScope.launch {
            _isLoading.value = true
            // Simulate loading courses from data source
            _courses.value = DataInitializer.mockCourses
            _isLoading.value = false
        }
    }

    fun searchCourses(query: String) {
        _searchQuery.value = query
    }

    fun filterByCategory(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun getFilteredCourses(): List<Course> {
        var filtered = _courses.value

        // Filter by search query
        if (_searchQuery.value.isNotBlank()) {
            filtered = filtered.filter { course ->
                course.title.contains(_searchQuery.value, ignoreCase = true) ||
                course.description.contains(_searchQuery.value, ignoreCase = true) ||
                course.teacherName.contains(_searchQuery.value, ignoreCase = true)
            }
        }

        // Filter by category
        if (_selectedCategory.value != null) {
            filtered = filtered.filter { it.category == _selectedCategory.value }
        }

        return filtered
    }

    fun getCategories(): List<String> {
        return listOf("All") + _courses.value.map { it.category }.distinct().sorted()
    }

    fun getMyEnrolledCourses(): List<Course> {
        return _courses.value.filter { it.isEnrolled }
    }
}
