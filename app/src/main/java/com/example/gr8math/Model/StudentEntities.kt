package com.example.gr8math.Data.Repository

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class StudentEntity(
    val id: Int,
    @SerialName("user_id") val userId: Int,
    @SerialName("learners_ref_number") val lrn: String? = null
)

@Serializable
data class BadgeEntity(
    val id: Int,
    @SerialName("badge_name") val badgeName: String,
    @SerialName("badge_desc") val badgeDesc: String
)

@Serializable
data class StudentBadgeEntity(
    val id: Int? = null,
    @SerialName("student_id") val studentId: Int,
    @SerialName("badge_id") val badgeId: Int,
    val rank: Int? = null,
    @SerialName("date_rewarded") val dateRewarded: String? = null
)

data class BadgeUiModel(
    val id: Int,
    val name: String,
    val description: String,
    val imageRes: Int,
    val isAcquired: Boolean,
    val rank: Int? = null,
    val dateRewarded: String? = null
)

data class StudentProfileData(
    val user: UserEntity,
    val student: StudentEntity?,
    val badges: List<BadgeUiModel>
)