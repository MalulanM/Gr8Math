package com.example.gr8math.Data.Repository

import com.example.gr8math.Data.Model.*
import com.example.gr8math.Data.Repository.ContentModerationService
import com.example.gr8math.Data.Repository.AuditTrailService // <-- Imported new service
import com.example.gr8math.Services.SupabaseService
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import io.github.jan.supabase.auth.auth

class LessonRepository {

    private val db = SupabaseService.client

    private suspend fun getCurrentUserId(): Int? {
        return try {
            val currentUser = db.auth.currentUserOrNull() ?: return null
            val email = currentUser.email ?: return null
            val dbUser = db.from("user")
                .select(columns = Columns.list("id")) {
                    filter { eq("email_add", email) }
                }
                .decodeSingleOrNull<UserIdRow>()
            dbUser?.id
        } catch (e: Exception) {
            null
        }
    }

    @kotlinx.serialization.Serializable
    private data class UserIdRow(val id: Int)

    suspend fun getLesson(lessonId: Int): Result<LessonEntity> {
        return withContext(Dispatchers.IO) {
            try {
                val lesson = db.from("lesson")
                    .select {
                        filter { eq("id", lessonId) }
                    }
                    .decodeSingle<LessonEntity>()
                Result.success(lesson)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun createLesson(lesson: LessonInsert): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Combine title and content for the check
                val fullText = "${lesson.lessonTitle} ${lesson.lessonContent}"

                // 2. Run the moderation service (the one we built)
                val modResult = ContentModerationService.checkContentModeration(db, fullText)

                // 3. Determine final status
                val finalStatus = if (modResult.isSafe) "approved" else "pending"

                // 4. Insert lesson with the correct status
                val newLesson = db.from("lesson")
                    .insert(lesson.copy(status = finalStatus)) {
                        select()
                    }
                    .decodeSingle<LessonEntity>()

                val userId = getCurrentUserId()

                // 5. If flagged, insert a moderation_actions record
                if (!modResult.isSafe) {
                    if (userId != null) {
                        val context = "[FLAGGED ITEM: ${modResult.offendingWord}]\n\n" +
                                "TITLE: ${lesson.lessonTitle}\n\nCONTENT:\n${lesson.lessonContent}"

                        db.from("moderation_actions").insert(
                            buildJsonObject {
                                put("target_user_id", userId)
                                put("content_type", "lesson")
                                put("content_id", newLesson.id)
                                put("violation_details", context)
                                put("reason_code", modResult.reasonCode ?: "Banned Word")
                                put("status", "pending")
                            }
                        )
                    }
                }

                // 6. Notify students ONLY if the lesson passed moderation
                if (finalStatus == "approved") {
                    notifyStudentsOfNewLesson(newLesson.id, lesson.courseId)

                    // --- CHANGED: Use the separated AuditTrailService ---
                    if (userId != null) {
                        AuditTrailService.logAuditTrail(
                            userId = userId,
                            resource = "Lesson",
                            action = "CREATE",
                            status = "SUCCESS",
                            details = "Created new lesson: ${lesson.lessonTitle}"
                        )
                    }
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun updateLesson(id: Int, lesson: LessonInsert): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Moderation check
                val fullText = "${lesson.lessonTitle} ${lesson.lessonContent}"
                val modResult = ContentModerationService.checkContentModeration(db, fullText)
                val finalStatus = if (modResult.isSafe) "approved" else "pending"

                // 2. Update the lesson with the new status
                db.from("lesson")
                    .update(lesson.copy(status = finalStatus)) {
                        filter { eq("id", id) }
                    }

                val userId = getCurrentUserId()

                // 3. If flagged, add a moderation_actions record
                if (!modResult.isSafe) {
                    if (userId != null) {
                        val context = "[FLAGGED ITEM: ${modResult.offendingWord}]\n\n" +
                                "TITLE: ${lesson.lessonTitle}\n\nCONTENT:\n${lesson.lessonContent}"

                        db.from("moderation_actions").insert(
                            buildJsonObject {
                                put("target_user_id", userId)
                                put("content_type", "lesson")
                                put("content_id", id)
                                put("violation_details", context)
                                put("reason_code", modResult.reasonCode ?: "Banned Word")
                                put("status", "pending")
                            }
                        )
                    }
                }
                // 4. Log Audit Trail if safe
                else if (finalStatus == "approved") {
                    // --- CHANGED: Use the separated AuditTrailService ---
                    if (userId != null) {
                        AuditTrailService.logAuditTrail(
                            userId = userId,
                            resource = "Lesson",
                            action = "UPDATE",
                            status = "SUCCESS",
                            details = "Updated lesson: ${lesson.lessonTitle}"
                        )
                    }
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- NOTIFICATION LOGIC (unchanged) ---
    private suspend fun notifyStudentsOfNewLesson(lessonId: Int, courseId: Int) {
        try {
            val sectionRes = db.from("course_content")
                .select(columns = Columns.list("section_id")) {
                    filter { eq("id", courseId) }
                }
                .decodeSingleOrNull<CourseSectionRes>()

            val sectionId = sectionRes?.sectionId ?: return

            val students = db.from("student_class")
                .select(columns = Columns.raw("student(user_id)")) {
                    filter { eq("class_id", sectionId) }
                }
                .decodeList<StudentClassRes>()

            if (students.isEmpty()) return

            val metaJson = buildJsonObject {
                put("course_id", courseId)
                put("lesson_id", lessonId)
                put("section_id", sectionId)
            }

            val notifications = students.map {
                NotificationInsert(
                    userId = it.student.userId,
                    type = "lesson",
                    title = "New Lesson Posted",
                    message = "New lesson available.",
                    meta = metaJson
                )
            }

            db.from("notifications").insert(notifications)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}