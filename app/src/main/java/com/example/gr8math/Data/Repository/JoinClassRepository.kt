package com.example.gr8math.Data.Repository

import com.example.gr8math.Data.Model.ClassCodeRes
import com.example.gr8math.Data.Model.CourseContentRes
import com.example.gr8math.Data.Model.JoinClassSuccess
import com.example.gr8math.Data.Model.StudentClassInsert
import com.example.gr8math.Data.Model.StudentIdRes
import com.example.gr8math.Services.SupabaseService
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JoinClassRepository {

    private val db = SupabaseService.client

    suspend fun joinClass(userId: Int, classCode: String): Result<JoinClassSuccess> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Find Class by Code
                val classObj = db.from("class")
                    .select(columns = Columns.list("id, class_name")) {
                        filter { eq("class_code", classCode) }
                    }.decodeSingleOrNull<ClassCodeRes>()
                    ?: return@withContext Result.failure(Exception("Invalid class code."))

                // 2. Find Student Record
                val studentObj = db.from("student")
                    .select(columns = Columns.list("id")) {
                        filter { eq("user_id", userId) }
                    }.decodeSingleOrNull<StudentIdRes>()
                    ?: return@withContext Result.failure(Exception("Student record not found."))

                // 3. Check if Already Joined (FIXED: Just fetch list and check size)
                val existingJoinList = db.from("student_class").select {
                    filter {
                        eq("student_id", studentObj.id)
                        eq("class_id", classObj.id)
                    }
                }.decodeList<StudentClassInsert>()

                if (existingJoinList.isNotEmpty()) {
                    return@withContext Result.failure(Exception("You are already in this class."))
                }

                // 4. Insert into student_class
                db.from("student_class").insert(
                    StudentClassInsert(
                        studentId = studentObj.id,
                        classId = classObj.id
                    )
                )

                // 5. Get Course ID
                val courseObj = db.from("course_content")
                    .select(columns = Columns.list("id")) {
                        filter { eq("section_id", classObj.id) }
                    }.decodeSingleOrNull<CourseContentRes>()
                    ?: return@withContext Result.failure(Exception("Class content not ready yet."))

                // Success
                Result.success(
                    JoinClassSuccess(
                        courseId = courseObj.id,
                        className = classObj.className,
                        message = "Welcome to ${classObj.className}!"
                    )
                )

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}