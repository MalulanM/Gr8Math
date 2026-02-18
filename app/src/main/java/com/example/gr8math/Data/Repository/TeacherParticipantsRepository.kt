package com.example.gr8math.Data.Repository

import com.example.gr8math.Data.Model.Participant
import com.example.gr8math.Data.Model.ParticipantRes
import com.example.gr8math.Data.Model.SectionIdRes
import com.example.gr8math.Services.SupabaseService
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TeacherParticipantsRepository {

    private val db = SupabaseService.client

    suspend fun getStudents(courseId: Int): Result<List<Participant>> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Get Section ID from 'course_content'
                val sectionRes = db.from("course_content")
                    .select(columns = Columns.list("section_id")) {
                        filter { eq("id", courseId) }
                    }
                    .decodeSingleOrNull<SectionIdRes>()

                val sectionId = sectionRes?.sectionId
                    ?: return@withContext Result.failure(Exception("Course not found"))

                // 2. Fetch Students
                // Query: Select from 'student_class', join 'student', join 'user'
                // We filter by 'class_id' (which is the sectionId)
                val rawList = db.from("student_class")
                    .select(
                        // Nested syntax: student table -> user table (select names)
                        columns = Columns.raw("student(id, user(first_name, last_name))")
                    ) {
                        filter { eq("class_id", sectionId) }
                    }
                    .decodeList<ParticipantRes>()

                // 3. Map to UI Model
                val participants = rawList.mapIndexed { index, item ->
                    val user = item.student.user
                    val fullName = "${user.lastName}, ${user.firstName}"

                    Participant(
                        id = item.student.id,
                        name = fullName,
                        rank = index + 1 // Assign rank based on order
                    )
                }

                Result.success(participants)

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}