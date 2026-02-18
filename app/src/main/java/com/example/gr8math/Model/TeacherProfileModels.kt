package com.example.gr8math.Data.Repository

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserEntity(
    val id: Int,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val gender: String? = null,
    val birthdate: String? = null,
    @SerialName("profile_pic") val profilePic: String? = null
)

@Serializable
data class TeacherEntity(
    val id: Int,
    @SerialName("user_id") val userId: Int,
    @SerialName("teaching_position") val teachingPosition: String? = null
)

@Serializable
data class TeacherAchievementEntity(
    val id: Int? = null,
    @SerialName("teacher_id") val teacherId: Int,
    @SerialName("achievement_desc") val achievementDesc: String,
    @SerialName("date_acquired") val dateAcquired: String,
    val certificate: String? = null
)


data class TeacherProfileData(
    val user: UserEntity,
    val teacher: TeacherEntity?,
    val achievements: List<TeacherAchievementEntity>
)