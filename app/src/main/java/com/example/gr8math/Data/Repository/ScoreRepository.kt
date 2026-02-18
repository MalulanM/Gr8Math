package com.example.gr8math.Data.Repository

import com.example.gr8math.Data.Model.AssessmentRecordRes
import com.example.gr8math.Data.Model.StudentScore
import com.example.gr8math.Services.SupabaseService
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScoreRepository {

    private val db = SupabaseService.client

    suspend fun getStudentScores(courseId: Int, studentId: Int): Result<List<StudentScore>> {
        return withContext(Dispatchers.IO) {
            try {
                // Query: Fetch records for this student
                // Join: assessment_created table to get title/items
                // Filter: Only assessments belonging to this courseId

                val rawList = db.from("assessment_record")
                    .select(
                        columns = Columns.raw("""
                            score, 
                            date_accomplished, 
                            assessment_created!inner (
                                id, 
                                title, 
                                assessment_number, 
                                assessment_items, 
                                course_id
                            )
                        """.trimIndent())
                    ) {
                        filter {
                            eq("student_id", studentId)
                            // We filter the NESTED table using the dot notation or inner join logic
                            eq("assessment_created.course_id", courseId)
                        }
                        // Sort by assessment number (inside the joined table)
                        order("assessment_created(assessment_number)", Order.ASCENDING)
                    }
                    .decodeList<AssessmentRecordRes>()

                // Map to UI Model
                val uiList = rawList.map { item ->
                    StudentScore(
                        id = item.assessment.id,
                        assessmentNumber = item.assessment.assessmentNumber,
                        title = item.assessment.title,
                        score = item.score,
                        dateAccomplished = item.dateAccomplished,
                        assessmentItems = item.assessment.assessmentItems
                    )
                }

                Result.success(uiList)

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}