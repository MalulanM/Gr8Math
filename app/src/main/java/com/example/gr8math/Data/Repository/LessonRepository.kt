package com.example.gr8math.Data.Repository

import com.example.gr8math.Data.Model.*
import com.example.gr8math.Services.SupabaseService
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class LessonRepository {

    private val db = SupabaseService.client

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

    // UPDATED: Now performs Notification Logic
    suspend fun createLesson(lesson: LessonInsert): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Insert Lesson and RETURN the new data (so we get the ID)
                val newLesson = db.from("lesson")
                    .insert(lesson) {
                        select() // Important: Ask Supabase to return the created row
                    }
                    .decodeSingle<LessonEntity>()

                // 2. Trigger Notification Logic (Fire and Forget)
                notifyStudentsOfNewLesson(newLesson.id, lesson.courseId)

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun updateLesson(id: Int, lesson: LessonInsert): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                db.from("lesson").update(lesson) {
                    filter { eq("id", id) }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- NOTIFICATION LOGIC (Replicating Laravel Service) ---

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

            // D. Prepare Notification Objects for Batch Insert
            val notifications = students.map {
                NotificationInsert(
                    userId = it.student.userId,
                    type = "lesson",
                    title = "New Lesson Posted",
                    message = "New lesson available.",
                    meta = metaJson
                )
            }

            // E. Batch Insert into 'notifications' table
            db.from("notifications").insert(notifications)


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}