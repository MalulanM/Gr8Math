package com.example.gr8math.dataObject

import com.google.gson.annotations.SerializedName

// 1. The Main Response Wrapper
data class ProfileResponse(
    val status: String,
    val data: ProfileData
)

// 2. The Data Container
data class ProfileData(
    val profile: UserProfile,
    val achievements: List<TeacherAchievement>,
    @SerializedName("teaching_position")
val teachingPosition: String? = null
)

// 3. User Basic Info
data class UserProfile(
    val id: Int,
    @SerializedName("first_name") val firstName: String?,
    @SerializedName("last_name") val lastName: String?,
    val gender: String?,
    val birthdate: String?,
    @SerializedName("profile_pic") val profilePic: String?
)

// 4. Achievement Item
data class TeacherAchievement(
    val id: Int = 0, // Default to 0 for new items
    @SerializedName("achievement_desc") val description: String,
    @SerializedName("date_acquired") val dateAcquired: String,
    val certificate: String? = null // URL or Base64
)

// 5. Request Body for Updating (Matches Laravel $request structure)
// import com.google.gson.annotations.SerializedName
data class UpdateProfileRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("birthdate") val birthdate: String? = null,
    @SerializedName("position") val teachingPosition: String? = null,
    @SerializedName("achievements") val achievements: List<TeacherAchievement>? = null,
    @SerializedName("profile_pic") val profilePic: String? = null
)

