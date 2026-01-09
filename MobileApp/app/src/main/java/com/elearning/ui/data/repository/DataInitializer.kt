package com.elearning.ui.data.repository

import com.elearning.ui.data.model.*

/**
 * Mock data initializer for testing UI without backend
 * Replace with actual API calls when backend is ready
 */
object DataInitializer {
    private val currentUser = User(
        id = 1,
        email = "student@example.com",
        name = "John Doe",
        role = UserRole.STUDENT,
        createdAt = "2024-01-15T10:00:00Z"
    )

    val mockCourses = listOf(
        Course(
            id = 1,
            title = "Introduction to Machine Learning",
            description = "Learn the fundamentals of machine learning, including supervised and unsupervised learning algorithms, neural networks, and practical applications.",
            teacherId = 1,
            teacherName = "Dr. Sarah Johnson",
            imageUrl = null,
            category = "Computer Science",
            enrolledStudents = 156,
            materialsCount = 12,
            createdAt = "2024-01-10T08:00:00Z",
            isEnrolled = true
        ),
        Course(
            id = 2,
            title = "Web Development with FastAPI",
            description = "Master modern web development using FastAPI, PostgreSQL, and build production-ready REST APIs with authentication and database integration.",
            teacherId = 2,
            teacherName = "Prof. Michael Chen",
            imageUrl = null,
            category = "Web Development",
            enrolledStudents = 89,
            materialsCount = 8,
            createdAt = "2024-01-12T09:30:00Z",
            isEnrolled = true
        ),
        Course(
            id = 3,
            title = "Data Structures and Algorithms",
            description = "Deep dive into essential data structures and algorithms. Learn to solve complex problems efficiently and ace technical interviews.",
            teacherId = 1,
            teacherName = "Dr. Sarah Johnson",
            imageUrl = null,
            category = "Computer Science",
            enrolledStudents = 234,
            materialsCount = 15,
            createdAt = "2024-01-05T07:00:00Z",
            isEnrolled = false
        ),
        Course(
            id = 4,
            title = "Android Development with Jetpack Compose",
            description = "Build modern Android applications using Jetpack Compose, MVVM architecture, and Material Design 3.",
            teacherId = 3,
            teacherName = "Jane Smith",
            imageUrl = null,
            category = "Mobile Development",
            enrolledStudents = 112,
            materialsCount = 10,
            createdAt = "2024-01-08T11:00:00Z",
            isEnrolled = true
        ),
        Course(
            id = 5,
            title = "Natural Language Processing",
            description = "Explore NLP techniques, from text preprocessing to advanced transformer models like BERT and GPT.",
            teacherId = 2,
            teacherName = "Prof. Michael Chen",
            imageUrl = null,
            category = "Artificial Intelligence",
            enrolledStudents = 67,
            materialsCount = 9,
            createdAt = "2024-01-14T13:00:00Z",
            isEnrolled = false
        )
    )

    val mockMaterials = mapOf(
        "1" to listOf(
            CourseMaterial(
                id = 1,
                courseId = 1,
                fileName = "Introduction_to_ML.pdf",
                mimeType = "application/pdf",
                fileSize = 2048576,
                uploadedAt = "2024-01-10T10:00:00Z"
            ),
            CourseMaterial(
                id = 2,
                courseId = 1,
                fileName = "Linear_Regression_Notes.txt",
                mimeType = "text/plain",
                fileSize = 15360,
                uploadedAt = "2024-01-11T14:30:00Z"
            ),
            CourseMaterial(
                id = 3,
                courseId = 1,
                fileName = "Neural_Networks_Guide.pdf",
                mimeType = "application/pdf",
                fileSize = 3145728,
                uploadedAt = "2024-01-12T09:00:00Z"
            )
        ),
        "2" to listOf(
            CourseMaterial(
                id = 4,
                courseId = 2,
                fileName = "FastAPI_Basics.pdf",
                mimeType = "application/pdf",
                fileSize = 1572864,
                uploadedAt = "2024-01-12T10:00:00Z"
            ),
            CourseMaterial(
                id = 5,
                courseId = 2,
                fileName = "Database_Design.txt",
                mimeType = "text/plain",
                fileSize = 8192,
                uploadedAt = "2024-01-13T11:00:00Z"
            )
        )
    )

    fun getMockChatMessages(courseId: String): List<ChatMessage> {
        return listOf(
            ChatMessage(
                id = "msg1",
                content = "Hello! I'm your AI tutor for this course. Ask me anything about the course materials!",
                isUser = false,
                timestamp = System.currentTimeMillis() - 3600000
            )
        )
    }

    fun getMockChatResponse(question: String): String {
        return when {
            question.contains("machine learning", ignoreCase = true) ->
                "Machine Learning is a subset of artificial intelligence that enables systems to learn and improve from experience without being explicitly programmed. It focuses on developing algorithms that can access data and use it to learn for themselves."

            question.contains("neural network", ignoreCase = true) ->
                "Neural networks are computing systems inspired by biological neural networks. They consist of interconnected nodes (neurons) organized in layers. Each connection has a weight that adjusts as learning proceeds, allowing the network to recognize patterns."

            question.contains("fastapi", ignoreCase = true) ->
                "FastAPI is a modern, fast web framework for building APIs with Python 3.7+. It's based on standard Python type hints and provides automatic API documentation, data validation, and high performance."

            else ->
                "That's a great question! Based on the course materials, I can help you understand this topic better. The key concepts include understanding the fundamentals and applying them to practical scenarios. Would you like me to elaborate on any specific aspect?"
        }
    }

    fun getCurrentUser(): User = currentUser
}
