package com.example.gr8math.Data.Repository

import com.example.gr8math.Services.SupabaseService
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class DllRepository {
    private val db = SupabaseService.client

    suspend fun getDllMains(courseId: Int): Result<List<DllMainEntity>> {
        return withContext(Dispatchers.IO) {
            try {
                val result = db.from("dll_main")
                    .select {
                        filter { eq("course_id", courseId) }
                        order("available_from", Order.DESCENDING)
                    }.decodeList<DllMainEntity>()

                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getDailyEntries(mainId: Int): Result<List<DllDailyEntryEntity>> {
        return withContext(Dispatchers.IO) {
            try {
                val result = db.from("dll_daily_entry")
                    .select {
                        filter { eq("main_id", mainId) }
                        order("entry_date", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                    }.decodeList<DllDailyEntryEntity>()
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

// Data Model matching your new DB Schema
@Serializable
data class DllMainEntity(
    val id: Int,
    @SerialName("course_id") val courseId: Int,
    @SerialName("quarter_number") val quarterNumber: Int? = null,
    @SerialName("week_number") val weekNumber: Int? = null,
    @SerialName("available_from") val availableFrom: String? = null,
    @SerialName("available_until") val availableUntil: String? = null
)

// Add this Data Class at the bottom of the file
@Serializable
data class DllDailyEntryEntity(
    val id: Int,
    @SerialName("main_id") val mainId: Int,
    @SerialName("entry_date") val entryDate: String,
    @SerialName("content_standard") val contentStandard: String? = null,
    @SerialName("performance_standard") val performanceStandard: String? = null,
    @SerialName("learning_comp") val learningComp: String? = null,
    val review: String? = null,
    val purpose: String? = null,
    val example: String? = null,
    @SerialName("discussion_proper") val discussionProper: String? = null,
    @SerialName("developing_mastery") val developingMastery: String? = null,
    val application: String? = null,
    val generalization: String? = null,
    val evaluation: String? = null,
    @SerialName("additional_act") val additionalAct: String? = null,
    val remark: String? = null,
    val reflection: String? = null
) : java.io.Serializable // Needed to pass it to Fragments via Bundle