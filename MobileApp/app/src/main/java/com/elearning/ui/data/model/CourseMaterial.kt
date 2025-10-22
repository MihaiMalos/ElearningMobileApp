package com.elearning.ui.data.model

data class CourseMaterial(
    val id: String,
    val courseId: String,
    val fileName: String,
    val fileType: FileType,
    val fileSize: Long,
    val uploadedAt: String,
    val uploadedBy: String
)

enum class FileType {
    PDF,
    TXT,
    UNKNOWN
}

