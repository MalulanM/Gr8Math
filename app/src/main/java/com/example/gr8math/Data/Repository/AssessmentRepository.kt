package com.example.gr8math.Data.Repository

import com.example.gr8math.Data.Model.*
import com.example.gr8math.Services.SupabaseService
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AssessmentRepository {

    private val db = SupabaseService.client

    suspend fun createAssessment(
        userId: Int,
        metaData: AssessmentInsert,
        questions: List<UiQuestion>
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. MODERATION CHECK (Matches Web)
                val allTextToCheck = metaData.title + " " + questions.joinToString(" ") { q ->
                    q.text + " " + q.choices.joinToString(" ") { it.text }
                }

                val modCheck = checkContentModeration(allTextToCheck)
                metaData.status = if (modCheck.isSafe) "approved" else "pending"


                val assessment = db.from("assessment_created")
                    .insert(metaData) { select() }
                    .decodeSingle<AssessmentCreated>()

                val assessmentId = assessment.id

                // 3. Insert Questions and Choices
                insertQuestionsAndChoices(assessmentId, questions)

                // 4. Handle Moderation / Notifications
                if (!modCheck.isSafe) {
                    val fullContext = "[FLAGGED ITEM: ${modCheck.offendingWord}]\n\nTITLE: ${metaData.title}\n\nCONTENT:\n$allTextToCheck"

                    val modAction = ModerationActionInsert(
                        targetUserId = userId,
                        contentType = "assessment",
                        contentId = assessmentId,
                        violationDetails = fullContext,
                        reasonCode = modCheck.reasonCode ?: "Banned Word",
                        status = "pending"
                    )
                    db.from("moderation_actions").insert(modAction)

                    return@withContext Result.success(true) // Flagged! Stop here.
                }

                notifyStudents(metaData.courseId, assessmentId)

                val auditTrail = AuditTrailInsert(
                    userId = userId,
                    resource = "Assessment",
                    action = "CREATE",
                    status = "SUCCESS",
                    details = "Published new assessment: ${metaData.title}"
                )
                db.from("audit_trails").insert(auditTrail)

                Result.success(false) // Safe!

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun updateAssessment(
        userId: Int, // 🌟 Added UserId
        assessmentId: Int,
        updateData: AssessmentUpdate,
        questions: List<UiQuestion>
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. MODERATION CHECK
                val allTextToCheck = updateData.title + " " + questions.joinToString(" ") { q ->
                    q.text + " " + q.choices.joinToString(" ") { it.text }
                }

                val modCheck = checkContentModeration(allTextToCheck)
                updateData.status = if (modCheck.isSafe) "approved" else "pending"


                val existing = db.from("assessment_created")
                    .select(columns = Columns.list("end_time")) {
                        filter { eq("id", assessmentId) }
                    }.decodeSingleOrNull<AssessmentEndTime>()

                db.from("assessment_created").update(updateData) { filter { eq("id", assessmentId) } }

                if (existing?.endTime != null) {
                    val oldEnd = parseIsoDate(existing.endTime)
                    val newEnd = parseIsoDate(updateData.endTime)

                    if (newEnd > oldEnd) {
                        val recordsToWipe = db.from("assessment_record").select(columns = Columns.list("student_id")) {
                            filter { eq("assessment_id", assessmentId); eq("score", 0.0) }
                        }.decodeList<StudentRecordId>()

                        if (recordsToWipe.isNotEmpty()) {
                            val studentIds = recordsToWipe.map { it.studentId }
                            db.from("assessment_record").delete { filter { eq("assessment_id", assessmentId); eq("score", 0.0) } }
                            db.from("student_answers").delete { filter { eq("assessment_id", assessmentId); isIn("student_id", studentIds) } }
                        }
                    }
                }

                // 3. Wipe and Replace Questions
                db.from("assessment_questions").delete { filter { eq("assessment_id", assessmentId) } }
                insertQuestionsAndChoices(assessmentId, questions)

                // 4. Handle Moderation / Logging
                if (!modCheck.isSafe) {
                    val fullContext = "[FLAGGED ITEM: ${modCheck.offendingWord}]\n\nTITLE: ${updateData.title}\n\nCONTENT:\n$allTextToCheck"

                    val modAction = ModerationActionInsert(
                        targetUserId = userId,
                        contentType = "assessment",
                        contentId = assessmentId,
                        violationDetails = fullContext,
                        reasonCode = modCheck.reasonCode ?: "Banned Word",
                        status = "pending"
                    )
                    db.from("moderation_actions").insert(modAction)

                    return@withContext Result.success(true) // Flagged
                }

                val auditTrail = AuditTrailInsert(
                    userId = userId,
                    resource = "Assessment",
                    action = "UPDATE",
                    status = "SUCCESS",
                    details = "Updated assessment: ${updateData.title}"
                )
                db.from("audit_trails").insert(auditTrail)

                Result.success(false) // Safe
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    suspend fun getAssessmentQuestions(assessmentId: Int): Result<List<UiQuestion>> {
        return withContext(Dispatchers.IO) {
            try {
                val columns = Columns.raw("id, question_text, assessment_choices(id, choice_text, is_correct)")
                val questions = db.from("assessment_questions").select(columns = columns) {
                    filter { eq("assessment_id", assessmentId) }
                }.decodeList<QuestionFullDetails>()

                val uiQuestions = questions.map { q ->
                    UiQuestion(
                        text = q.questionText,
                        choices = q.choices.map { c -> UiChoice(c.choiceText, c.isCorrect) }
                    )
                }
                Result.success(uiQuestions)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    // 🌟 EXACT WEB MODERATION LOGIC
    private suspend fun checkContentModeration(text: String): ModerationResult {
        return try {
            val bannedWords = db.from("banned_words").select(columns = Columns.list("word")).decodeList<BannedWord>()
            val wordList = bannedWords.map { it.word.lowercase() }
            val lowercaseText = text.lowercase()

            val offendingWord = wordList.find { lowercaseText.contains(it) }

            val linkRegex = Regex("(?:https?://|www\\.)[^\\s\"<>]+")
            val matches = linkRegex.findAll(text).map { it.value }.toList()

            val suspiciousLinks = matches.filter { link ->
                !link.contains("fly.storage.tigris.dev") && !link.contains("math.now.sh")
            }

            if (offendingWord != null || suspiciousLinks.isNotEmpty()) {
                ModerationResult(
                    isSafe = false,
                    offendingWord = offendingWord ?: suspiciousLinks.firstOrNull() ?: "Suspicious Link",
                    reasonCode = if (suspiciousLinks.isNotEmpty()) "Suspicious Link" else "Banned Word"
                )
            } else {
                ModerationResult(isSafe = true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ModerationResult(isSafe = true)
        }
    }


    private fun parseIsoDate(dateStr: String?): Long {
        if (dateStr.isNullOrEmpty()) return 0L
        val formats = arrayOf("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ss")
        for (pattern in formats) {
            try {
                val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.US)
                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                return sdf.parse(dateStr)?.time ?: 0L
            } catch (e: Exception) {}
        }
        return 0L
    }

    private suspend fun insertQuestionsAndChoices(assessmentId: Int, questions: List<UiQuestion>) {
        for (q in questions) {
            val qInsert = QuestionInsert(assessmentId = assessmentId, questionText = q.text)
            val savedQuestion = db.from("assessment_questions").insert(qInsert) { select() }.decodeSingle<QuestionEntity>()

            val choicesToInsert = q.choices.map { c ->
                ChoiceInsert(questionId = savedQuestion.id, choiceText = c.text, isCorrect = c.isCorrect)
            }
            db.from("assessment_choices").insert(choicesToInsert)
        }
    }

    private suspend fun notifyStudents(courseId: Int, assessmentId: Int) {
        try {
            val sectionRes = db.from("course_content")
                .select(columns = Columns.list("section_id")) { filter { eq("id", courseId) } }
                .decodeSingleOrNull<SectionIdRes>() ?: return

            val students = db.from("student_class")
                .select(columns = Columns.raw("student(user_id)")) { filter { eq("class_id", sectionRes.sectionId) } }
                .decodeList<StudentClassRes>()

            if (students.isEmpty()) return

            val metaJson = buildJsonObject {
                put("course_id", courseId)
                put("section_id", sectionRes.sectionId)
                put("assessment_id", assessmentId)
            }

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

    // --- HELPER DATA CLASSES ---
    @Serializable private data class BannedWord(val word: String)
    @Serializable private data class AssessmentEndTime(@SerialName("end_time") val endTime: String?)
    @Serializable private data class StudentRecordId(@SerialName("student_id") val studentId: Int)

    private data class ModerationResult(val isSafe: Boolean, val offendingWord: String? = null, val reasonCode: String? = null)

    @Serializable
    private data class ModerationActionInsert(
        @SerialName("target_user_id") val targetUserId: Int,
        @SerialName("content_type") val contentType: String,
        @SerialName("content_id") val contentId: Int,
        @SerialName("violation_details") val violationDetails: String,
        @SerialName("reason_code") val reasonCode: String,
        val status: String
    )

    @Serializable
    private data class AuditTrailInsert(
        @SerialName("user_id") val userId: Int,
        val resource: String,
        val action: String,
        val status: String,
        val details: String
    )
}