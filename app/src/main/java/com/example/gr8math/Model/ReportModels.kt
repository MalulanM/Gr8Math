package com.example.gr8math.Data.Model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 1. Database Response
@Serializable
data class ReportRecordRes(
    val score: Double,
    // Nested Relation
    @SerialName("assessment_created") val assessment: ReportAssessmentInfo
)

@Serializable
data class ReportAssessmentInfo(
    @SerialName("assessment_number") val assessmentNumber: Int,
    @SerialName("assessment_items") val assessmentItems: Int,
    @SerialName("assessment_quarter") val assessmentQuarter: Int
)

// 2. UI Model (Processed data for the table)
data class ReportItem(
    val assessmentNumber: Int,
    val score: Int,
    val items: Int,
    val percentString: String
)

// 3. State Data (Holds list + totals)
data class QuarterlyReportData(
    val items: List<ReportItem>,
    val totalScore: Int,
    val totalItems: Int
)