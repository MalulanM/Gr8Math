package com.example.gr8math.dataObject

data class AssessmentResponse(
    val success: Boolean,
    val assessment: AssessmentData
)

data class AssessmentData(
    val id: Int,
    val title: String,
    val start_time: String,
    val end_time: String,
    val assessment_items: Int,
    val assessment_number: Int,
    val questions: List<QuestionData>
)

data class QuestionData(
    val id: Int,
    val question_text: String,
    val choices: List<ChoiceData>
)

data class ChoiceData(
    val id: Int,
    val choice_text: String,
    val is_correct: Boolean
)

