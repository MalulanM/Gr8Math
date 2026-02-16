package com.example.gr8math.Model
data class TeacherModel(
    val user_id: Int,
    val first_name: String,
    val last_name: String,
    val roles: String?,
    val profile_pic: String?,
    val birthdate: String?
)

data class BadgeModel(
    val badge_id: Int,
    val badge_name: String,
    val badge_desc: String,
    val date_rewarded: String
)

data class StudentModel(
    val user_id: Int,
    val first_name: String,
    val last_name: String,
    val grade_level: String?,
    val profile_pic: String?,
    val birthdate: String?,
    val badges: List<BadgeModel>
)

data class ParticipantResponse(
    val status: String,
    val course_id: Int,
    val teacher: TeacherModel?,
    val students: List<StudentModel>
)
