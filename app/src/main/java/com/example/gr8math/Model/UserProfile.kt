package com.example.gr8math.Data.Model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- UPDATED USER PROFILE ---
@Serializable
data class UserProfile(
    val id: Int? = null,
    @SerialName("auth_id")
    val authUserId: String? = null,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    @SerialName("email_add") val email: String,
    @SerialName("gender") val gender: String,
    @SerialName("birthdate") val birthdate: String,
    val roles: String,
    @SerialName("is_approved") val isApproved: Boolean = false,
    @SerialName("first_login") val firstLogin: Boolean = true,
    @SerialName("profile_pic") val profilePic: String? = null
)


@Serializable
data class StudentProfile(
    @SerialName("user_id") val userId: Int,
    @SerialName("learners_ref_number") val lrn: String?,
    @SerialName("grade_level") val gradeLevel: Int = 8
)


@Serializable
data class TeacherProfile(
    @SerialName("user_id") val userId: Int,
    @SerialName("teaching_position") val teachingPosition: String?,
)

