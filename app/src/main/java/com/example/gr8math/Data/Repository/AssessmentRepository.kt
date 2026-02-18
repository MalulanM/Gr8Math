package com.example.gr8math.Data.Repository

import com.example.gr8math.Data.Model.*
import com.example.gr8math.Services.SupabaseService
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AssessmentRepository {

    private val db = SupabaseService.client

    suspend fun createAssessment(
        metaData: AssessmentInsert,
        questions: List<UiQuestion>
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Insert Assessment & Get ID
                val assessment = db.from("assessment_created")
                    .insert(metaData) { select() }
                    .decodeSingle<AssessmentCreated>()

                val assessmentId = assessment.id

                // 2. Loop through Questions
                // Note: We do this strictly sequentially to ensure data integrity
                for (q in questions) {

                    // A. Insert Question & Get ID
                    val qInsert = QuestionInsert(
                        assessmentId = assessmentId,
                        questionText = q.text
                    )
                    val savedQuestion = db.from("assessment_questions")
                        .insert(qInsert) { select() }
                        .decodeSingle<QuestionEntity>()

                    // B. Insert Choices for this Question (Batch Insert)
                    val choicesToInsert = q.choices.map { c ->
                        ChoiceInsert(
                            questionId = savedQuestion.id,
                            choiceText = c.text,
                            isCorrect = c.isCorrect
                        )
                    }
                    db.from("assessment_choices").insert(choicesToInsert)
                }


                notifyStudents(metaData.courseId, assessmentId)

                Result.success(Unit)

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- Notification Logic ---
    private suspend fun notifyStudents(courseId: Int, assessmentId: Int) {
        try {
            // A. Get Section ID
            val sectionRes = db.from("course_content")
                .select(columns = Columns.list("section_id")) {
                    filter { eq("id", courseId) }
                }.decodeSingleOrNull<SectionIdRes>()

            val sectionId = sectionRes?.sectionId ?: return

            // B. Get Students
            val students = db.from("student_class")
                .select(columns = Columns.raw("student(user_id)")) {
                    filter { eq("class_id", sectionId) }
                }.decodeList<StudentClassRes>()

            if (students.isEmpty()) return

            // C. Build Meta JSON
            val metaJson = buildJsonObject {
                put("course_id", courseId)
                put("section_id", sectionId)
                put("assessment_id", assessmentId)
            }

            // D. Batch Insert Notifications
            val notifs = students.map {
                NotificationInsert(
                    userId = it.student.userId,
                    type = "assessment",
                    title = "New Assessment Posted",
                    message = "New assessment test available.",
                    meta = metaJson
                )
            }
            db.from("notifications").insert(notifs)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}