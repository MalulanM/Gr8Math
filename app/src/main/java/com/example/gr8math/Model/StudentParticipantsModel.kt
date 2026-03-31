package com.example.gr8math.Data.Model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDisplayItem(
    val id: Int,
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
    val type: String
)

@Serializable
data class TeacherInfo(
    @SerialName("user_id") val userId: Int,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    @SerialName("profile_pic") val profilePic: String?,
    val roles: String?,
    val birthdate: String?,
    val achievements: List<ProfileDisplayItem> = emptyList() // 🌟 ADDED
)

@Serializable
data class StudentInfoSide(
    @SerialName("user_id") val userId: Int,
    @SerialName("student_id") val studentId: Int,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    @SerialName("profile_pic") val profilePic: String?,
    @SerialName("grade_level") val gradeLevel: Int?,
    @SerialName("learners_ref_number") val lrn:String?,
    val birthdate: String?,
    val badges: List<ProfileDisplayItem> = emptyList() // 🌟 UPDATED
)

data class ParticipantsStateData(
    val teacher: TeacherInfo?,
    val students: List<StudentInfoSide>
)