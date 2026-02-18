package com.example.gr8math.Data.Model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class SectionIdRes(
    @SerialName("section_id") val sectionId: Int
)


@Serializable
data class ParticipantRes(
    @SerialName("student") val student: StudentInfo
)

@Serializable
data class StudentInfo(
    val id: Int,
    @SerialName("user") val user: UserInfo
)

// Nested 'user' table info
@Serializable
data class UserInfo(
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String
)


data class Participant(
    val id: Int,
    val name: String,
    val rank: Int
)