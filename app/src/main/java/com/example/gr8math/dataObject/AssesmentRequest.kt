package com.example.gr8math.dataObject

data class Choice(
    val choice_text: String,
    val is_correct: Boolean
)

data class Question(
    val question_text: String,
    val choices: List<Choice>
)

data class AssessmentRequest(
    val course_id: Int,
    val title: String,
    val start_time: String,
    val end_time: String,
    val assessment_number: Int,
    val assessment_items: Int,
    val questions: List<Question>
)

data class AnswerItem(
    val question_id: Int,
    val choice_id: Int,
    val timestamp: String
)

data class AnswerPayload(
    val student_id: Int,
    val assessment_id: Int,
    val answers: List<AnswerItem>
)


