package com.example.gr8math.Data.Repository

import com.example.gr8math.Services.SupabaseService
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * A service that mimics the web's logAuditTrail logic.
 */
object AuditTrailService {

    /**
     * Logs an action to the audit_trails table.
     *
     * @param userId The ID of the user performing the action (can be null).
     * @param resource The module being accessed (e.g., "Lesson", "Assessment").
     * @param action The specific action taken (e.g., "CREATE", "UPDATE", "DELETE").
     * @param status The outcome of the action (e.g., "SUCCESS", "FAILED").
     * @param details A descriptive string of what happened.
     */
    suspend fun logAuditTrail(
        userId: Int?,
        resource: String,
        action: String,
        status: String,
        details: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val db = SupabaseService.client
                db.from("audit_trails").insert(
                    buildJsonObject {
                        // Using put for nullable Int requires safe handling or omitting if null.
                        // Supabase Kotlin SDK allows inserting nulls safely this way:
                        if (userId != null) {
                            put("user_id", userId)
                        }
                        put("resource", resource)
                        put("action", action)
                        put("status", status)
                        put("details", details)
                    }
                )
                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                // Fail silently for audit trails so it doesn't crash the main operation
                Result.failure(e)
            }
        }
    }
}