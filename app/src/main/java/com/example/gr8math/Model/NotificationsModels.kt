package com.example.gr8math.Data.Model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement // <--- IMPORT THIS

// 1. Database Entity
@Serializable
data class NotificationEntity(
    val id: Int,
    @SerialName("user_id") val userId: Int,
    val type: String,
    val title: String,
    val message: String,
    @SerialName("is_read") val isRead: Boolean,
    val meta: JsonElement? = null,
    @SerialName("created_at") val createdAt: String
)

// 2. Helper to parse the "meta" JSON string
// UPDATE THIS CLASS TO INCLUDE @SerializedName
@Serializable
data class NotificationMeta(

    @SerializedName("course_id")   // <--- Added for Gson
    @SerialName("course_id")       // <--- Kept for Kotlin Serialization
    val courseId: Int? = 0,

    @SerializedName("section_id")
    @SerialName("section_id")
    val sectionId: Int? = 0,

    @SerializedName("lesson_id")
    @SerialName("lesson_id")
    val lessonId: Int? = 0,

    @SerializedName("assessment_id")
    @SerialName("assessment_id")
    val assessmentId: Int? = 0,

    @SerializedName("student_id")
    @SerialName("student_id")
    val studentId: Int? = 0,

    @SerializedName("name")
    @SerialName("name")
    val name: String? = ""
)
data class StudentNotificationUI(
    val id: Int,
    val title: String,
    val message: String,
    val createdAt: String,
    var isRead: Boolean,
    val type: String,
    val courseId: Int,
    val lessonId: Int,
    val studentId: Int,
    val assessmentId: Int
)

data class TeacherNotificationUI(
    val id: Int,
    val title: String,
    val message: String,
    val createdAt: String,
    var isRead: Boolean,
    val type: String,
    val courseId: Int,
    val studentId: Int,
    val assessmentId: Int,
    val studentName: String
)