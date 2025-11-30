package com.example.gr8math.dataObject

import com.google.gson.annotations.SerializedName

// MAIN RESPONSE
data class StudentProfileResponse(
    val status: String,
    val data: StudentProfileData
)

// DATA CONTAINER
data class StudentProfileData(
    val profile: StudentUserProfile,
    @SerializedName("LRN")
    val lrn: String?,
    val badges: List<StudentBadge>
)

// USER BASIC INFO
data class StudentUserProfile(
    val id: Int,
    @SerializedName("first_name") val firstName: String?,
    @SerializedName("last_name") val lastName: String?,
    val gender: String?,
    val birthdate: String?,
    @SerializedName("profile_pic") val profilePic: String?
)

// BADGES
data class StudentBadge(
    @SerializedName("awarded_id") val awardedId: Int,
    @SerializedName("badge_id") val badgeId: Int,
    @SerializedName("badge_name") val name: String,
    @SerializedName("badge_desc") val description: String,
    @SerializedName("date_rewarded") val dateAwarded: String
)

// UPDATE REQUEST BODY
data class UpdateStudentProfileRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("birthdate") val birthdate: String? = null,
    @SerializedName("LRN") val lrn: String? = null,
    @SerializedName("profile_pic") val profilePic: String? = null
)
