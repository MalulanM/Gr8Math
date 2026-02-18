package com.example.gr8math.Data.Repository

import com.example.gr8math.Data.Model.AssessmentFullDetails
import com.example.gr8math.Services.SupabaseService
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AssessmentDetailRepository {

    private val db = SupabaseService.client

    suspend fun getAssessmentDetails(assessmentId: Int): Result<AssessmentFullDetails> {
        return withContext(Dispatchers.IO) {
            try {
                // Fetch the Single Assessment with nested Questions and Choices
                val assessment = db.from("assessment_created")
                    .select(
                        columns = Columns.raw("""
                            id, course_id, title, start_time, end_time, 
                            assessment_items, assessment_number, assessment_quarter,
                            assessment_questions (
                                id, question_text,
                                assessment_choices (
                                    id, choice_text, is_correct
                                )
                            )
                        """.trimIndent())
                    ) {
                        filter { eq("id", assessmentId) }
                    }.decodeSingleOrNull<AssessmentFullDetails>()

                if (assessment != null) {
                    Result.success(assessment)
                } else {
                    Result.failure(Exception("Assessment not found"))
                }
            } catch (e: Exception) {
                // Log the error to see if it's a parsing issue
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
}