package com.example.gr8math.Data.Repository

import com.example.gr8math.Data.Model.ClassEntity
import com.example.gr8math.Data.Model.SearchHistory
import com.example.gr8math.Services.SupabaseService
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClassRepository {

    private val db = SupabaseService.client

    suspend fun getClasses(userId: Int, role: String, searchQuery: String? = null): Result<List<ClassEntity>> {
        return withContext(Dispatchers.IO) {
            try {
                // Determine columns based on role (Student needs Adviser Name)
                val columns = if (role.equals("student", ignoreCase = true)) {
                    // Fetch Adviser details via Foreign Key 'adviser_id' -> 'user' table
                    Columns.raw("id, class_name, arrival_time, dismissal_time, class_size, grade_level, course_content(id, section_id), adviser:user(first_name, last_name)")
                } else {
                    Columns.raw("id, class_name, arrival_time, dismissal_time, class_size, grade_level, course_content(id, section_id)")
                }

                val query = db.from("class").select(columns = columns) {

                    // 1. FILTER BY ROLE
                    if (role.equals("teacher", ignoreCase = true)) {
                        filter { eq("adviser_id", userId) }
                    } else if (role.equals("student", ignoreCase = true)) {
                        // Get Student -> StudentClasses -> Filter Class IDs
                        val studentRes = db.from("student").select(columns = Columns.raw("id")) {
                            filter { eq("user_id", userId) }
                        }.decodeSingleOrNull<Map<String, Int>>()

                        val studentId = studentRes?.get("id") ?: throw Exception("Student record not found")

                        val classIds = db.from("student_class").select(columns = Columns.raw("class_id")) {
                            filter { eq("student_id", studentId) }
                        }.decodeList<Map<String, Int>>().map { it["class_id"] ?: 0 }

                        filter { isIn("id", classIds) }
                    }

                    // 2. SEARCH FILTER
                    if (!searchQuery.isNullOrEmpty()) {
                        filter { ilike("class_name", "%$searchQuery%") }
                    }

                    order("class_name", Order.ASCENDING)
                }

                val classes = query.decodeList<ClassEntity>()
                Result.success(classes)

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    suspend fun getSearchHistory(userId: Int): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                db.from("search_history").select(columns = Columns.raw("search_term")) {
                    filter { eq("user_id", userId) }
                    order("searched_at", Order.DESCENDING)
                    limit(5)
                }.decodeList<Map<String, String>>().map { it["search_term"] ?: "" }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun saveSearchHistory(userId: Int, term: String) {
        withContext(Dispatchers.IO) {
            try {
                val entry = SearchHistory(userId, term)
                db.from("search_history").insert(entry)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getSectionNameByCourseId(courseId: Int): String? {
        return withContext(Dispatchers.IO) {
            try {
                val result = db.from("course_content").select(columns = Columns.raw("class(class_name)")) {
                    filter { eq("id", courseId) }
                }.decodeSingleOrNull<Map<String, Map<String, String>>>()

                result?.get("class")?.get("class_name")
            } catch (e: Exception) {
                null
            }
        }
    }
}