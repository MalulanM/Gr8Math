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

                val monthStr = month.toString().padStart(2, '0')
                val startDate = "$year-$monthStr-01"

                val nextMonth = if (month == 12) 1 else month + 1
                val nextYear = if (month == 12) year + 1 else year
                val nextMonthStr = nextMonth.toString().padStart(2, '0')
                val endDate = "$nextYear-$nextMonthStr-01"


                // 1. Updated Select to include total_points 🌟
                val rawList = db.from("assessment_record")
                    .select(
                        columns = Columns.raw("""
                            score,
                            assessment_created!inner (
                                assessment_number, 
                                assessment_items, 
                                total_points,
                                assessment_quarter, 
                                course_id
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


                var totalScoreSum = 0.0
                var totalPossiblePointsSum = 0.0 //  This will track overall points for the footer
                var totalItemsCount = 0

                val uiList = rawList.map { record ->
                    val score = record.score
                    val items = record.assessment.assessmentItems
                    // Use total_points from DB, fallback to items count if null
                    val pointsForThisTest = record.assessment.totalPoints ?: items.toDouble()

                    totalScoreSum += score
                    totalPossiblePointsSum += pointsForThisTest
                    totalItemsCount += items

                    //  Percentage is now calculated using Points, not Items
                    val percentVal = if (pointsForThisTest > 0) (score / pointsForThisTest) * 100 else 0.0
                    val percentStr = "${percentVal.toInt()}%"

                    // 2. Passing all parameters including totalPoints 🌟
                    ReportItem(
                        assessmentNumber = record.assessment.assessmentNumber,
                        score = score, // Ensure ReportItem expects Double/Number if you have decimals
                        totalPoints = pointsForThisTest,
                        items = items,
                        percentString = percentStr
                    )
                }

                Result.success(
                    QuarterlyReportData(
                        items = uiList,
                        //  Footer: Total Score vs Total Possible Points
                        totalScore = totalScoreSum.toInt(),
                        totalItems = totalItemsCount // Keeping this as the literal item count
                    )
                )

            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
}