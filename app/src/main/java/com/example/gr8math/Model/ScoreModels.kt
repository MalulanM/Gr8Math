package com.example.gr8math.Data.Model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==========================================
// 1. DATABASE RESPONSE (Shared)
// ==========================================
@Serializable
data class AssessmentRecordRes(
    val id: Int? = null,
    val score: Double,
    @SerialName("date_accomplished") val dateAccomplished: String,
    @SerialName("assessment_created") val assessment: AssessmentCreatedInfo
)

@Serializable
data class AssessmentCreatedInfo(
    val id: Int,
    val title: String,
    @SerialName("assessment_number") val assessmentNumber: Int,
    @SerialName("assessment_items") val assessmentItems: Int
)

// ==========================================
// 2. UI MODEL (Shared for Adapter)
// ==========================================
data class StudentScore(
    val id: Int, // Assessment ID
    val assessmentNumber: Int,
    val title: String,
    val score: Double,
    val dateAccomplished: String,
    val assessmentItems: Int
)