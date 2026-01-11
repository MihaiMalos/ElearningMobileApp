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

    suspend fun updateCourse(courseId: Int, title: String, description: String?): Result<Course> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateCourse(courseId, CreateCourseRequest(title, description))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to update course: ${response.code()} ${response.message()}"))
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

    suspend fun getMaterialContent(materialId: Int): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.downloadMaterial(materialId)
            if (response.isSuccessful && response.body() != null) {
                // Assuming text content for now based on the prompt "let me see the text content"
                // For binary files like PDF, we'd need a different handling, but let's try to stringify it.
                // If it's a PDF, this might produce garbage text, but the user asked for "text content".
                // Ideally backend provides text/plain or we handle bytes.
                // Given the `FileType` enum has PDF and TXT, let's treat it as string.
                Result.success(response.body()!!.string())
            } else {
                Result.failure(Exception("Failed to download material: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserById(userId: Int): Result<User> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getUserById(userId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch user: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTeacherById(teacherId: Int): Result<User> = getUserById(teacherId)

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

    suspend fun deleteCourse(courseId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteCourse(courseId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete course: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun enrollStudentInCourse(courseId: Int, studentId: Int): Result<Enrollment> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.enrollInCourse(EnrollmentRequest(courseId, studentId))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to enroll student: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchStudents(query: String?): Result<List<User>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getUsers(role = "student", search = query)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to search students: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
