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

    suspend fun getMonthlyReport(courseId: Int, studentId: Int, month: Int, year: Int): Result<QuarterlyReportData> {
        return withContext(Dispatchers.IO) {
            try {
                val rawList = db.from("assessment_record")
                    .select(
                        columns = Columns.raw("""
                            score,
                            date_accomplished, 
                            assessment_created!inner (
                                assessment_number, 
                                assessment_items, 
                                total_points,
                                assessment_quarter, 
                                course_id
                            )
                        """.trimIndent()) // NOTE: Removed date_accomplished from here
                    ) {
                        filter {
                            eq("student_id", studentId)
                            eq("assessment_created.course_id", courseId)
                        }
                        order("assessment_created(assessment_number)", Order.ASCENDING)
                    }.decodeList<ReportRecordRes>()

                var totalScoreSum = 0.0
                var totalPossiblePointsSum = 0.0
                var totalItemsCount = 0

                val uiList = rawList.map { record ->
                    val score = record.score
                    val items = record.assessment.assessmentItems
                    val pointsForThisTest = record.assessment.totalPoints ?: items.toDouble()

                    totalScoreSum += score
                    totalPossiblePointsSum += pointsForThisTest
                    totalItemsCount += items

                    val percentVal = if (pointsForThisTest > 0) (score / pointsForThisTest) * 100 else 0.0
                    val percentStr = "${percentVal.toInt()}%"

                    ReportItem(
                        assessmentNumber = record.assessment.assessmentNumber,
                        score = score,
                        totalPoints = pointsForThisTest,
                        items = items,
                        percentString = percentStr,
                        dateAccomplished = record.dateAccomplished // 🌟 3. Pass the date
                    )
                }

                Result.success(
                    QuarterlyReportData(
                        items = uiList,
                        totalScore = totalScoreSum.toInt(),
                        totalItems = totalItemsCount
                    )
                )

            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
}