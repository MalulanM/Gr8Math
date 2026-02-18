package com.example.gr8math.Data.Repository

import android.util.Log
import com.example.gr8math.Data.Model.AssessmentFullDetails
import com.example.gr8math.Services.SupabaseService
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class TakeAssessmentRepository {

    private val db = SupabaseService.client

    suspend fun submitAssessment(
        userId: Int,
        assessment: AssessmentFullDetails,
        selectedAnswers: Map<Int, Int>
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. GET REAL STUDENT ID
                val studentRes = db.from("student")
                    .select(columns = Columns.list("id")) {
                        filter { eq("user_id", userId) }
                    }.decodeSingleOrNull<StudentIdHelper>()

                val realStudentId = studentRes?.id ?: throw Exception("Student profile not found")

                val timestamp = getCurrentIsoTime()

                // 2. SAVE ANSWERS
                // We use standard insert here. (Assuming you don't have constraints on answers either)
                if (selectedAnswers.isNotEmpty()) {
                    val answerList = selectedAnswers.map { (qId, cId) ->
                        StudentAnswerInsert(
                            studentId = realStudentId,
                            assessmentId = assessment.id,
                            questionId = qId,
                            choiceId = cId,
                            timestamp = timestamp
                        )
                    }
                    // Try to delete old answers first to prevent duplicates (since we are doing "insert")
                    try {
                        db.from("student_answers").delete {
                            filter {
                                eq("student_id", realStudentId)
                                eq("assessment_id", assessment.id)
                            }
                        }
                    } catch (e: Exception) { /* Ignore if fails, just proceed to insert */ }

                    db.from("student_answers").insert(answerList)
                }

                // 3. CALCULATE SCORE
                var correctCount = 0
                assessment.questions.forEach { question ->
                    val selectedChoiceId = selectedAnswers[question.id]
                    if (selectedChoiceId != null) {
                        val choice = question.choices.find { it.id == selectedChoiceId }
                        if (choice != null && choice.isCorrect) {
                            correctCount++
                        }
                    }
                }

                // 4. SAVE SCORE RECORD (The Fix: Manual Check + Insert)

                // A. Check if record already exists
                val existingRecord = db.from("assessment_record")
                    .select(columns = Columns.list("id")) {
                        filter {
                            eq("student_id", realStudentId)
                            eq("assessment_id", assessment.id)
                        }
                    }.decodeSingleOrNull<RecordIdHelper>()

                // B. Only Insert if it DOES NOT exist
                if (existingRecord == null) {
                    val record = AssessmentRecordInsert(
                        studentId = realStudentId,
                        assessmentId = assessment.id,
                        score = correctCount.toDouble(),
                        dateAccomplished = timestamp
                    )

                    db.from("assessment_record").insert(record)
                    Log.d("SUBMIT_LOG", "Success: New assessment record inserted.")
                } else {
                    Log.d("SUBMIT_LOG", "Skipped: Record already exists for ID ${existingRecord.id}")
                }

                // 5. NOTIFY TEACHER
                notifyTeacher(userId, realStudentId, assessment.courseId, assessment.id)

                Result.success(Unit)

            } catch (e: Exception) {
                Log.e("SUBMIT_ERR", "Error submitting", e)
                Result.failure(e)
            }
        }
    }

    // --- NOTIFICATION LOGIC ---
    private suspend fun notifyTeacher(userId: Int, studentId: Int, courseId: Int, assessmentId: Int) {
        try {
            // A. Get Section ID from Course
            val sectionRes = db.from("course_content")
                .select(columns = Columns.list("section_id")) {
                    filter { eq("id", courseId) }
                }.decodeSingleOrNull<SectionIdRes>() ?: return

            // B. Get Teacher ID (Adviser) from Class
            val classRes = db.from("class")
                .select(columns = Columns.list("adviser_id")) {
                    filter { eq("id", sectionRes.sectionId) }
                }.decodeSingleOrNull<AdviserIdRes>() ?: return

            // C. Get Student Name
            val userRes = db.from("user")
                .select(columns = Columns.list("first_name", "last_name")) {
                    filter { eq("id", userId) }
                }.decodeSingleOrNull<UserNameRes>() ?: return

            val studentName = "${userRes.firstName} ${userRes.lastName ?: ""}".trim()

            // D. Build Meta Data
            val metaJson = buildJsonObject {
                put("assessment_id", assessmentId)
                put("course_id", courseId)
                put("student_id", studentId)
                put("section_id", sectionRes.sectionId)
                put("name", studentName)
            }

            // E. Send Notification
            val notif = NotificationInsert(
                userId = classRes.adviserId,
                type = "assessment_submission",
                title = "Student Submission",
                message = "$studentName submitted their work.",
                meta = metaJson
            )
            db.from("notifications").insert(notif)
            Log.d("NOTIF", "Notification sent to teacher: ${classRes.adviserId}")

        } catch (e: Exception) {
            Log.e("NOTIF_ERR", "Failed to notify teacher", e)
        }
    }

    private fun getCurrentIsoTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    // --- DATA MODELS ---

    @Serializable
    private data class StudentIdHelper(@SerialName("id") val id: Int)

    @Serializable
    private data class RecordIdHelper(@SerialName("id") val id: Int) // Helper to check existence

    @Serializable
    private data class AssessmentRecordInsert(
        @SerialName("student_id") val studentId: Int,
        @SerialName("assessment_id") val assessmentId: Int,
        @SerialName("score") val score: Double,
        @SerialName("date_accomplished") val dateAccomplished: String
    )

    @Serializable
    private data class StudentAnswerInsert(
        @SerialName("student_id") val studentId: Int,
        @SerialName("assessment_id") val assessmentId: Int,
        @SerialName("question_id") val questionId: Int,
        @SerialName("choice_id") val choiceId: Int,
        @SerialName("timestamp") val timestamp: String
    )

    // Notification Models
    @Serializable
    private data class SectionIdRes(@SerialName("section_id") val sectionId: Int)

    @Serializable
    private data class AdviserIdRes(@SerialName("adviser_id") val adviserId: Int)

    @Serializable
    private data class UserNameRes(
        @SerialName("first_name") val firstName: String,
        @SerialName("last_name") val lastName: String?
    )

    @Serializable
    private data class NotificationInsert(
        @SerialName("user_id") val userId: Int,
        val type: String,
        val title: String,
        val message: String,
        val meta: kotlinx.serialization.json.JsonObject
    )
}