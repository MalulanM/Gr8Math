package com.example.gr8math.Data.Repository

import com.example.gr8math.Data.Model.AssessmentRecordRes
import com.example.gr8math.Data.Model.StudentScore
import com.example.gr8math.Services.SupabaseService
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StudentGradesRepository {

    private val db = SupabaseService.client

    //  1. Change return type to Result<Pair<Int, List<StudentScore>>>
    suspend fun getStudentGrades(userId: Int, courseId: Int): Result<Pair<Int, List<StudentScore>>> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Get Student ID
                val studentRes = db.from("student")
                    .select(columns = Columns.list("id")) {
                        filter { eq("user_id", userId) }
                    }.decodeSingleOrNull<Map<String, Int>>()

                val studentId = studentRes?.get("id") ?: throw Exception("Student not found")

                // 2. Fetch Records (Using shared AssessmentRecordRes)
                val rawList = db.from("assessment_record")
                    .select(
                        columns = Columns.raw("""
                            score, date_accomplished,
                            assessment_created!inner (
                                id, title, assessment_number, assessment_items,total_points, course_id
                            )
                        """.trimIndent())
                    ) {
                        filter {
                            eq("student_id", studentId)
                            eq("assessment_created.course_id", courseId)
                        }
                        order("assessment_created(assessment_number)", Order.ASCENDING)
                    }.decodeList<AssessmentRecordRes>()

                // 3. Map to Shared UI Model
                val uiList = rawList.map { record ->
                    StudentScore(
                        id = record.assessment.id,
                        title = record.assessment.title,
                        score = record.score,
                        assessmentNumber = record.assessment.assessmentNumber,
                        dateAccomplished = record.dateAccomplished,
                        totalPoints = record.assessment.totalPoints ?: record.assessment.assessmentItems.toDouble(),
                        assessmentItems = record.assessment.assessmentItems
                    )
                }

                // 2. Return both the studentId and the list
                Result.success(Pair(studentId, uiList))

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}