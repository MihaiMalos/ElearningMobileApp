package com.elearning.ui.data.model

import com.google.gson.annotations.SerializedName

data class CourseMaterial(
    val id: Int,
    @SerializedName("course_id")
    val courseId: Int,
    @SerializedName("original_filename")
    val fileName: String,
    @SerializedName("mime_type")
    val mimeType: String,
    @SerializedName("file_size")
    val fileSize: Long,
    @SerializedName("uploaded_at")
    val uploadedAt: String
) {
    val fileType: FileType
        get() = when {
            mimeType.contains("pdf", ignoreCase = true) -> FileType.PDF
            mimeType.contains("text", ignoreCase = true) -> FileType.TXT
            else -> FileType.UNKNOWN
        }
}

enum class FileType {
    PDF,
    TXT,
    UNKNOWN
}

