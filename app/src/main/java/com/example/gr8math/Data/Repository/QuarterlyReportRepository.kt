package com.example.gr8math.Data.Repository

import com.example.gr8math.Data.Model.QuarterlyReportData
import com.example.gr8math.Data.Model.ReportItem
import com.example.gr8math.Data.Model.ReportRecordRes
import com.example.gr8math.Services.SupabaseService
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QuarterlyReportRepository {

    private val db = SupabaseService.client

    suspend fun getQuarterlyReport(courseId: Int, studentId: Int, quarter: Int): Result<QuarterlyReportData> {
        return withContext(Dispatchers.IO) {
            try {
                // Fetch Data joined with assessment_created
                val rawList = db.from("assessment_record")
                    .select(
                        columns = Columns.raw("""
                            score,
                            assessment_created!inner (
                                assessment_number, assessment_items, assessment_quarter, course_id
                            )
                        """.trimIndent())
                    ) {
                        filter {
                            eq("student_id", studentId)
                            eq("assessment_created.course_id", courseId)
                            eq("assessment_created.assessment_quarter", quarter) // Filter by Quarter here
                        }
                        order("assessment_created(assessment_number)", Order.ASCENDING)
                    }.decodeList<ReportRecordRes>()

                // Calculate Totals and Map to UI Model
                var totalScore = 0.0
                var totalItems = 0

                val uiList = rawList.map { record ->
                    val score = record.score
                    val items = record.assessment.assessmentItems

                    totalScore += score
                    totalItems += items

                    // Calculate Percentage String
                    val percentVal = if (items > 0) (score.toFloat() / items) * 100 else 0f
                    val percentStr = "${percentVal.toInt()}%"

                    ReportItem(
                        assessmentNumber = record.assessment.assessmentNumber,
                        score = score.toInt(),
                        items = items,
                        percentString = percentStr
                    )
                }

                Result.success(
                    QuarterlyReportData(
                        items = uiList,
                        totalScore = totalScore.toInt(),
                        totalItems = totalItems
                    )
                )

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}