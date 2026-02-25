package com.example.gr8math.Data.Model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==========================================
// 1. FOR CREATING (Teacher)
// ==========================================
@Serializable
data class AssessmentInsert(
    @SerialName("course_id") val courseId: Int,
    val title: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("assessment_items") val assessmentItems: Int,
    @SerialName("assessment_number") val assessmentNumber: Int,
    @SerialName("assessment_quarter") val assessmentQuarter: Int
)

@Serializable
data class AssessmentCreated(
    val id: Int
)

@Serializable
data class QuestionInsert(
    @SerialName("assessment_id") val assessmentId: Int,
    @SerialName("question_text") val questionText: String
)

@Serializable
data class QuestionEntity(
    val id: Int
)

@Serializable
data class ChoiceInsert(
    @SerialName("question_id") val questionId: Int,
    @SerialName("choice_text") val choiceText: String,
    @SerialName("is_correct") val isCorrect: Boolean
)

// UI Helpers
data class UiQuestion(
    val text: String,
    val choices: List<UiChoice>
)

data class UiChoice(
    val text: String,
    val isCorrect: Boolean
)

// ==========================================
// 2. FOR READING DETAILS (Student/Shared)
// ==========================================
@Serializable
data class AssessmentFullDetails(
    val id: Int,
    @SerialName("course_id") val courseId: Int,
    val title: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("assessment_items") val assessmentItems: Int,
    @SerialName("assessment_number") val assessmentNumber: Int,
    @SerialName("assessment_quarter") val assessmentQuarter: Int,
    @SerialName("assessment_questions") val questions: List<QuestionFullDetails> = emptyList()
)

@Serializable
data class QuestionFullDetails(
    val id: Int,
    @SerialName("question_text") val questionText: String,
    @SerialName("assessment_choices") val choices: List<ChoiceFullDetails> = emptyList()
)

@Serializable
data class ChoiceFullDetails(
    val id: Int,
    @SerialName("choice_text") val choiceText: String,
    @SerialName("is_correct") val isCorrect: Boolean
)

// ==========================================
// 3. FOR SUBMITTING (Student)
// ==========================================
@Serializable
data class AssessmentRecordInsert(
    @SerialName("student_id") val studentId: Int,
    @SerialName("assessment_id") val assessmentId: Int,
    @SerialName("score") val score: Double,
    @SerialName("date_accomplished") val dateAccomplished: String
)

@Serializable
data class StudentAnswerInsert(
    @SerialName("student_id") val studentId: Int,
    @SerialName("assessment_id") val assessmentId: Int,
    @SerialName("question_id") val questionId: Int,
    @SerialName("choice_id") val choiceId: Int,
    val timestamp: String
)

// ==========================================
// 3. FOR VIEWING RESULTS (Student)
// ==========================================


// 1. To read from 'assessment_record'
@Serializable
data class AssessmentRecordEntity(
    val score: Double,
    @SerialName("date_accomplished") val dateAccomplished: String // ISO String
)

// 2. To read from 'assessment_created'
@Serializable
data class AssessmentDetailsEntity(
    val title: String,
    @SerialName("assessment_number") val assessmentNumber: Int,
    @SerialName("assessment_items") val assessmentItems: Int
)

// 3. Combined UI Model
data class AssessmentResultUiModel(
    val score: Double,
    val dateAccomplished: String,
    val title: String,
    val assessmentNumber: Int,
    val assessmentItems: Int
)


@Serializable
data class AssessmentUpdate(
    val title: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("assessment_items") val assessmentItems: Int,
    @SerialName("assessment_number") val assessmentNumber: Int,
    @SerialName("assessment_quarter") val assessmentQuarter: Int
)