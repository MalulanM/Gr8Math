package com.example.gr8math.Data.Repository

import com.example.gr8math.Data.Model.AssessmentDetailsEntity
import com.example.gr8math.Data.Model.AssessmentRecordEntity
import com.example.gr8math.Data.Model.AssessmentResultUiModel
import com.example.gr8math.Services.SupabaseService
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AssessmentResultRepository {

    private val db = SupabaseService.client

    suspend fun getAssessmentResult(userId: Int, assessmentId: Int): Result<AssessmentResultUiModel> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Get Student ID from User ID
                val studentRes = db.from("student")
                    .select(columns = Columns.list("id")) {
                        filter { eq("user_id", userId) }
                    }.decodeSingleOrNull<Map<String, Int>>()

                val studentId = studentRes?.get("id") ?: throw Exception("Student not found")

                // 2. Fetch Score Record
                val record = db.from("assessment_record")
                    .select(columns = Columns.list("score, date_accomplished")) {
                        filter {
                            eq("student_id", studentId)
                            eq("assessment_id", assessmentId)
                        }
                    }.decodeSingle<AssessmentRecordEntity>()

                // 3. Fetch Assessment Details
                val details = db.from("assessment_created")
                    .select(columns = Columns.list("title, assessment_number, assessment_items")) {
                        filter { eq("id", assessmentId) }
                    }.decodeSingle<AssessmentDetailsEntity>()

                // 4. Combine into UI Model
                val result = AssessmentResultUiModel(
                    score = record.score,
                    dateAccomplished = record.dateAccomplished,
                    title = details.title,
                    assessmentNumber = details.assessmentNumber,
                    assessmentItems = details.assessmentItems
                )

                Result.success(result)

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}