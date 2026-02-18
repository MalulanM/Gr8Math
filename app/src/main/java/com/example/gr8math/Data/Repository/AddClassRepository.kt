package com.example.gr8math.Data.Repository

import com.example.gr8math.Data.Model.ClassIdResponse
import com.example.gr8math.Data.Model.ClassInsert
import com.example.gr8math.Data.Model.CourseContentInsert
import com.example.gr8math.Services.SupabaseService
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AddClassRepository {

    private val db = SupabaseService.client

    suspend fun createClass(
        adviserId: Int,
        sectionName: String,
        numStudents: Int,
        startTime: String,
        endTime: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Generate Unique Code
                val uniqueCode = generateUniqueClassCode()

                // 2. Prepare Class Data
                val classData = ClassInsert(
                    adviserId = adviserId,
                    className = sectionName,
                    classSize = numStudents,
                    arrivalTime = startTime,
                    dismissalTime = endTime,
                    classCode = uniqueCode,
                    gradeLevel = 8
                )

                // 3. Insert Class and Get ID back
                val insertedClass = db.from("class")
                    .insert(classData) {
                        select() // Returns the inserted row
                    }
                    .decodeSingle<ClassIdResponse>()

                // 4. Automatically Create Course Content
                val contentData = CourseContentInsert(sectionId = insertedClass.id)
                db.from("course_content").insert(contentData)

                // 5. Success
                Result.success(insertedClass.classCode)

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun generateUniqueClassCode(): String {
        val allowedChars = "abcdefghijklmnopqrstuvwxyz0123456789"
        var code: String
        var exists: Boolean

        do {
            // Random 6 chars
            code = (1..6)
                .map { allowedChars.random() }
                .joinToString("")

            // FIX: Removed "head = true", used columns = Columns.list("id") instead
            val count = db.from("class").select(columns = Columns.list("id")) {
                filter {
                    eq("class_code", code)
                }
                count(Count.EXACT)
            }.countOrNull() ?: 0

            exists = count > 0

        } while (exists)

        return code
    }
}