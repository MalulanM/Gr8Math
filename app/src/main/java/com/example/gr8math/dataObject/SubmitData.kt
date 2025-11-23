package com.example.gr8math.dataObject
data class SubmitAnswer(
    val question_id: Int,
    val choice_id: Int,
    val timestamp: String
)

data class SubmitAssessmentRequest(
    val student_id: Int,
    val assessment_id: Int,
    val answers: List<SubmitAnswer>
)

