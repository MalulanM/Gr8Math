package com.example.gr8math.Data.Repository

import android.util.Log
import com.example.gr8math.Data.Model.AssessmentEntity
import com.example.gr8math.Data.Model.AssessmentRecordCheck
import com.example.gr8math.Data.Model.AssessmentRecordInsert
import com.example.gr8math.Data.Model.AssessmentStatus
import com.example.gr8math.Data.Model.AssessmentTimeCheck
import com.example.gr8math.Data.Model.ClassContentItem
import com.example.gr8math.Data.Model.LessonEntity
import com.example.gr8math.Services.SupabaseService
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ClassPageRepository {

    private val db = SupabaseService.client

    // ... [getClassContent stays exactly the same as your previous code] ...
    suspend fun getClassContent(courseId: Int): Result<List<ClassContentItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val lessonsDeferred = async {
                    db.from("lesson").select {
                        filter { eq("course_id", courseId) }
                        order("created_at", Order.DESCENDING)
                    }.decodeList<LessonEntity>()
                }
                val assessmentsDeferred = async {
                    db.from("assessment_created").select {
                        filter { eq("course_id", courseId) }
                        order("created_at", Order.DESCENDING)
                    }.decodeList<AssessmentEntity>()
                }
                val lessons = lessonsDeferred.await()
                val assessments = assessmentsDeferred.await()

                val lessonItems = lessons.map { l ->
                    val cleanText = getFirstSentence(removeBase64(l.lessonContent))
                    ClassContentItem.LessonItem(
                        id = l.id,
                        createdAt = l.createdAt ?: "",
                        weekNumber = l.weekNumber,
                        title = l.lessonTitle,
                        previewContent = cleanText,
                        fullContent = l.lessonContent
                    )
                }
                val assessmentItems = assessments.map { a ->
                    ClassContentItem.AssessmentItem(
                        id = a.id,
                        createdAt = a.createdAt,
                        assessmentNumber = a.assessmentNumber
                    )
                }
                val combinedList = (lessonItems + assessmentItems).sortedByDescending { it.createdAt }
                Result.success(combinedList)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }


    suspend fun checkAssessmentAvailability(studentId: Int, assessmentId: Int): Result<AssessmentStatus> {
        return withContext(Dispatchers.IO) {
            try {
                val existingRecord = db.from("assessment_record")
                    .select(columns = Columns.list("id")) {
                        filter {
                            eq("student_id", studentId)
                            eq("assessment_id", assessmentId)
                        }
                    }.decodeSingleOrNull<AssessmentRecordCheck>()

                if (existingRecord != null) {
                    return@withContext Result.success(AssessmentStatus.HAS_RECORD)
                }

                val assessmentInfo = db.from("assessment_created")
                    .select(columns = Columns.list("end_time")) {
                        filter { eq("id", assessmentId) }
                    }.decodeSingleOrNull<AssessmentTimeCheck>()

                if (assessmentInfo != null) {
                    val isLate = checkIsLate(assessmentInfo.endTime)
                    if (isLate) {
                        try {
                            val failRecord = AssessmentRecordInsert(
                                studentId = studentId,
                                assessmentId = assessmentId,
                                score = 0.0,
                                dateAccomplished = assessmentInfo.endTime
                            )
                            db.from("assessment_record").insert(failRecord)
                        } catch (e: Exception) { }
                        return@withContext Result.success(AssessmentStatus.DEADLINE_PASSED)
                    }
                }
                Result.success(AssessmentStatus.AVAILABLE)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- NEW FUNCTIONS REQUIRED FOR VIEWMODEL ---

    // 1. Get Student ID from User ID
    suspend fun getStudentIdByUserId(userId: Int): Int? {
        return withContext(Dispatchers.IO) {
            try {
                val response = db.from("student")
                    .select(columns = Columns.list("id")) {
                        filter { eq("user_id", userId) }
                    }
                    .decodeSingleOrNull<StudentIdWrapper>()
                response?.id
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun hasAssessmentRecord(studentId: Int, assessmentId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("DEBUG_REPO", "Querying DB: assessment_record where student_id=$studentId AND assessment_id=$assessmentId")

                val resultList = db.from("assessment_record")
                    .select(columns = Columns.list("id")) {
                        filter {
                            eq("student_id", studentId)
                            eq("assessment_id", assessmentId)
                        }
                    }.decodeList<StudentIdWrapper>()

                Log.d("DEBUG_REPO", "DB Query Result Count: ${resultList.size}")

                resultList.isNotEmpty()
            } catch (e: Exception) {
                Log.e("DEBUG_REPO", "DB Query EXCEPTION", e)
                false
            }
        }
    }
    // --- Helper Logic (Keep as is) ---
    private fun checkIsLate(endTimeIso: String): Boolean {
        val formats = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                val endDate = sdf.parse(endTimeIso)
                if (endDate != null) {
                    val now = Date()
                    return now.after(endDate)
                }
            } catch (e: Exception) { continue }
        }
        return false
    }

    private fun removeBase64(content: String): String {
        return content.replace(Regex("data:image/[^;]+;base64,[A-Za-z0-9+/=]+"), "[image]").trim()
    }

    private fun getFirstSentence(content: String): String {
        val noHtml = content.replace(Regex("<[^>]*>"), "").trim()
        val noBase64 = noHtml.replace(Regex("data:image/[^;]+;base64,[A-Za-z0-9+/=]+"), "").trim()
        val lines = noBase64.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

        if (lines.isEmpty()) {
            return " "
        }

        val firstLine = lines[0]
        val periodIndex = firstLine.indexOf(".")

        // Return only the first sentence or the first line
        return if (periodIndex != -1) {
            firstLine.substring(0, periodIndex + 1)
        } else {
            if (firstLine.length > 50) firstLine.take(50) + "..." else firstLine
        }
    }

    // 2. Fetch Section Name by Course ID
    suspend fun getSectionNameByCourseId(courseId: Int): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Note: Adjust table name ('classes') and column name ('section_name')
                // to match your Supabase schema exactly.
                val response = db.from("classes")
                    .select(columns = Columns.list("section_name")) {
                        filter { eq("id", courseId) }
                    }
                    .decodeSingleOrNull<SectionNameWrapper>()
                response?.sectionName
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // Data Wrapper for serialization
    @Serializable
    data class SectionNameWrapper(@SerialName("section_name") val sectionName: String)

    // --- DATA WRAPPER FOR SIMPLE ID FETCHING ---
    @Serializable
    data class StudentIdWrapper(@SerialName("id") val id: Int)
}