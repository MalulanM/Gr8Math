package com.example.gr8math.Data.Model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==========================================
// 1. FOR DISPLAYING (READING) - Keep as is
// ==========================================
@Serializable
data class ClassEntity(
    val id: Int,
    @SerialName("class_name") val className: String,
    @SerialName("arrival_time") val arrivalTime: String? = null,
    @SerialName("dismissal_time") val dismissalTime: String? = null,
    @SerialName("class_size") val classSize: Int = 0,
    @SerialName("grade_level") val gradeLevel: Int? = 0,
    @SerialName("course_content") val courseContent: List<CourseContent>? = null,
    @SerialName("adviser") val adviser: AdviserInfo? = null
)

@Serializable
data class AdviserInfo(
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String
)

@Serializable
data class CourseContent(
    val id: Int,
    @SerialName("section_id") val sectionId: Int
)

@Serializable
data class SearchHistory(
    @SerialName("user_id") val userId: Int,
    @SerialName("search_term") val searchTerm: String
)

data class ClassUiModel(
    val id: Int,
    val sectionName: String,
    val schedule: String,
    val studentCount: Int,
    val courseId: Int,
    val teacherName: String
)

// ==========================================
// 2. FOR CREATING (WRITING) - Add these
// ==========================================

@Serializable
data class ClassInsert(
    @SerialName("adviser_id") val adviserId: Int,
    @SerialName("class_name") val className: String,
    @SerialName("grade_level") val gradeLevel: Int = 8,
    @SerialName("class_size") val classSize: Int,
    @SerialName("arrival_time") val arrivalTime: String,
    @SerialName("dismissal_time") val dismissalTime: String,
    @SerialName("class_code") val classCode: String
)

@Serializable
data class ClassIdResponse(
    val id: Int,
    @SerialName("class_code") val classCode: String
)

@Serializable
data class CourseContentInsert(
    @SerialName("section_id") val sectionId: Int
)