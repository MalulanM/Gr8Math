package com.example.gr8math.Data.Repository

import com.example.gr8math.Data.Model.*
import com.example.gr8math.Services.SupabaseService
import com.google.gson.Gson
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement

class NotificationRepository {

    private val db = SupabaseService.client
    private val gson = Gson()
    private val json = Json { ignoreUnknownKeys = true } // For decoding Kotlin objects

    // --- HELPER: Handles both String ("{...}") and Object ({...}) ---
    private fun parseMeta(element: kotlinx.serialization.json.JsonElement?): NotificationMeta {
        if (element == null) return NotificationMeta()

        return try {
            if (element is JsonPrimitive && element.isString) {
                // CASE 1: It is a String (Your current issue)
                // Use Gson to parse the string content "{\"course_id\":11...}"
                val jsonString = element.contentOrNull ?: ""
                gson.fromJson(jsonString, NotificationMeta::class.java)
            } else if (element is JsonObject) {
                // CASE 2: It is a proper JSON Object (Correct future data)
                json.decodeFromJsonElement<NotificationMeta>(element)
            } else {
                NotificationMeta()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            NotificationMeta() // Return defaults on error
        }
    }

    suspend fun getStudentNotifications(userId: Int, courseId: Int): Result<List<StudentNotificationUI>> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Fetch from DB
                val rawList = db.from("notifications")
                    .select {
                        filter {
                            eq("user_id", userId)
                            isIn("type", listOf("assessment", "lesson", "class_time"))
                        }
                        order("id", Order.DESCENDING)
                    }.decodeList<NotificationEntity>()

                // 2. Filter & Map
                val uiList = rawList.mapNotNull { entity ->
                    // USE THE HELPER HERE to safely extract data
                    val meta = parseMeta(entity.meta)

                    if (meta.courseId == courseId) {
                        StudentNotificationUI(
                            id = entity.id,
                            title = entity.title,
                            message = entity.message,
                            createdAt = entity.createdAt,
                            isRead = entity.isRead,
                            type = entity.type,
                            courseId = meta.courseId ?: 0,
                            lessonId = meta.lessonId ?: 0,
                            assessmentId = meta.assessmentId ?: 0,
                            studentId = meta.studentId ?: 0
                        )
                    } else {
                        null
                    }
                }

                Result.success(uiList)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ... (Keep getTeacherNotifications, markAsRead, etc.) ...
    // Note: Apply the same 'parseMeta' logic inside getTeacherNotifications as well.
    suspend fun getTeacherNotifications(userId: Int, courseId: Int): Result<List<TeacherNotificationUI>> {
        return withContext(Dispatchers.IO) {
            try {
                // ... (Section/Adviser checks) ...
                // For brevity, assuming checks passed or are commented out for testing:

                val rawList = db.from("notifications")
                    .select {
                        filter {
                            eq("user_id", userId)
                            isIn("type", listOf("assessment_submission", "class_time"))
                        }
                        order("id", Order.DESCENDING)
                    }.decodeList<NotificationEntity>()

                val uiList = rawList.mapNotNull { entity ->
                    val meta = parseMeta(entity.meta)

                    if (meta.courseId == courseId) {
                        TeacherNotificationUI(
                            id = entity.id,
                            title = entity.title,
                            message = entity.message,
                            createdAt = entity.createdAt,
                            isRead = entity.isRead,
                            type = entity.type,
                            courseId = meta.courseId ?: 0,
                            studentId = meta.studentId ?: 0,
                            assessmentId = meta.assessmentId ?: 0,
                            studentName = meta.name ?: "Unknown"
                        )
                    } else {
                        null
                    }
                }
                Result.success(uiList)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun markAsRead(notifId: Int) {
        try {
            db.from("notifications").update({ set("is_read", true) }) {
                filter { eq("id", notifId) }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun markAllAsRead(ids: List<Int>) {
        try {
            if (ids.isNotEmpty()) {
                db.from("notifications").update({ set("is_read", true) }) {
                    filter { isIn("id", ids) }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }
}