package com.elearning.ui.data.repository

import com.elearning.ui.data.api.ApiService
import com.elearning.ui.data.api.CreateCourseRequest
import com.elearning.ui.data.api.EnrollmentRequest
import com.elearning.ui.data.model.Course
import com.elearning.ui.data.model.CourseMaterial
import com.elearning.ui.data.model.Enrollment
import com.elearning.ui.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CourseRepository(private val apiService: ApiService) {

    suspend fun getCourses(search: String? = null): Result<List<Course>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCourses(search)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch courses: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCourseById(id: Int): Result<Course> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCourseById(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch course: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createCourse(title: String, description: String?): Result<Course> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.createCourse(CreateCourseRequest(title, description))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create course: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun enrollInCourse(courseId: Int): Result<Enrollment> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.enrollInCourse(EnrollmentRequest(courseId))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to enroll: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserEnrollments(): Result<List<Enrollment>> = withContext(Dispatchers.IO) {
         try {
            val response = apiService.getUserEnrollments()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch enrollments: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCourseMaterials(courseId: Int): Result<List<CourseMaterial>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCourseMaterials(courseId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch materials: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
             Result.failure(e)
        }
    }

    suspend fun getTeacherById(teacherId: Int): Result<User> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getUserById(teacherId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch teacher: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCourseEnrollments(courseId: Int): Result<List<Enrollment>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCourseEnrollments(courseId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch course enrollments: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
