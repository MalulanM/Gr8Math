package com.example.gr8math.Data.Model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 1. To find class by code
@Serializable
data class ClassCodeRes(
    val id: Int,
    @SerialName("class_name") val className: String
)

// 2. To find student by user_id
@Serializable
data class StudentIdRes(
    val id: Int
)

// 3. To find course_id linked to the class
@Serializable
data class CourseContentRes(
    val id: Int
)

// 4. To insert into student_class
@Serializable
data class StudentClassInsert(
    @SerialName("student_id") val studentId: Int,
    @SerialName("class_id") val classId: Int
)

// 5. Success Data for UI
data class JoinClassSuccess(
    val courseId: Int,
    val className: String,
    val message: String
)