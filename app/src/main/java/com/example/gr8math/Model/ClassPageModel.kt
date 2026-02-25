package com.example.gr8math.Data.Model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// ==========================================
// 1. DB ENTITIES (Reading from Supabase)
// ==========================================

// This single class now works for BOTH the list (with date) and details (without date)
@Serializable
data class LessonEntity(
    val id: Int,
    @SerialName("course_id") val courseId: Int,
    @SerialName("week_number") val weekNumber: Int,
    @SerialName("lesson_title") val lessonTitle: String,
    @SerialName("lesson_content") val lessonContent: String,
    // Nullable so it doesn't crash if you fetch a lesson without selecting created_at
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class AssessmentEntity(
    val id: Int,
    @SerialName("course_id") val courseId: Int,
    @SerialName("assessment_number") val assessmentNumber: Int,
    @SerialName("created_at") val createdAt: String
)

// ==========================================
// 2. INSERTS (Writing to Supabase)
// ==========================================

@Serializable
data class LessonInsert(
    @SerialName("course_id") val courseId: Int,
    @SerialName("week_number") val weekNumber: Int,
    @SerialName("lesson_title") val lessonTitle: String,
    @SerialName("lesson_content") val lessonContent: String
)

// ==========================================
// 3. UI MODELS (For RecyclerView / Sorting)
// ==========================================

sealed class ClassContentItem {
    abstract val id: Int
    abstract val createdAt: String

    data class LessonItem(
        override val id: Int,
        override val createdAt: String,
        val weekNumber: Int,
        val title: String,
        val previewContent: String,
        val fullContent: String
    ) : ClassContentItem()

    data class AssessmentItem(
        override val id: Int,
        override val createdAt: String,
        val assessmentNumber: Int,
        val title: String = "",
        val quarter: Int = 0,
        val startTime: String = "",
        val endTime: String = ""
    ) : ClassContentItem()
}

@Serializable
data class NotificationInsert(
    @SerialName("user_id") val userId: Int,
    @SerialName("type") val type: String,
    @SerialName("title") val title: String,
    @SerialName("message") val message: String,
    val meta: JsonObject
)


@Serializable
data class CourseSectionRes(
    @SerialName("section_id") val sectionId: Int
)


@Serializable
data class StudentClassRes(
    @SerialName("student") val student: StudentUserRes
)

@Serializable
data class StudentUserRes(
    @SerialName("user_id") val userId: Int
)
@Serializable
data class AssessmentRecordCheck(
    val id: Int
)

@Serializable
data class AssessmentTimeCheck(
    @SerialName("end_time") val endTime: String // ISO String from Supabase
)


enum class AssessmentStatus {
    HAS_RECORD,      // Already took it -> Result Screen
    DEADLINE_PASSED, // Missed it (Auto 0) -> Result Screen
    AVAILABLE        // Good to go -> Detail/Start Screen
}