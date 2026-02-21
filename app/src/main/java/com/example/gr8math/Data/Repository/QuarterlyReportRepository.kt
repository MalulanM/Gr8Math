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

    // 🌟 NEW: The Monthly Report Function!
    suspend fun getMonthlyReport(courseId: Int, studentId: Int, month: Int, year: Int): Result<QuarterlyReportData> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Figure out the exact start and end dates for the database filter
                val monthStr = month.toString().padStart(2, '0')
                val startDate = "$year-$monthStr-01" // e.g., "2026-02-01"

                val nextMonth = if (month == 12) 1 else month + 1
                val nextYear = if (month == 12) year + 1 else year
                val nextMonthStr = nextMonth.toString().padStart(2, '0')
                val endDate = "$nextYear-$nextMonthStr-01" // e.g., "2026-03-01"

                // 2. Fetch Data joined with assessment_created
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
                            gte("date_accomplished", startDate)
                            lt("date_accomplished", endDate)
                        }
                        order("assessment_created(assessment_number)", Order.ASCENDING)
                    }.decodeList<ReportRecordRes>()


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