package com.example.gr8math.Data.Repository

import android.util.Log
import com.example.gr8math.Services.SupabaseService
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class BadgeRes(val id: Int, val badge_name: String)
@Serializable
data class StudentBadgeInsert(val student_id: Int, val badge_id: Int)
@Serializable
data class AssessmentRecordRes(val id: Int, val score: Double, val assessment_id: Int)
@Serializable
data class ExistingBadgeRes(val badge_id: Int)
@Serializable
data class StudentIdRes(val id: Int)

class BadgeRepository {
    private val db = SupabaseService.client

    suspend fun evaluateAndAwardBadges(userId: Int, currentScore: Double, totalItems: Int): List<String> {
        return withContext(Dispatchers.IO) {
            val newlyEarnedBadges = mutableListOf<String>()

            if (userId == 0) {
                return@withContext emptyList()
            }

            try {

                val studentRecord = db.from("student")
                    .select(columns = Columns.list("id")) {
                        filter { eq("user_id", userId) }
                    }.decodeSingleOrNull<StudentIdRes>()

                if (studentRecord == null) {
                    Log.e("BadgeRepo", "Could not find a student profile for User ID: $userId")
                    return@withContext emptyList()
                }

                val actualStudentId = studentRecord.id


                val allBadges = db.from("badges")
                    .select(columns = Columns.list("id", "badge_name"))
                    .decodeList<BadgeRes>()


                val existingStudentBadges = db.from("student_badges")
                    .select(columns = Columns.list("badge_id")) {
                        filter { eq("student_id", actualStudentId) }
                    }
                    .decodeList<ExistingBadgeRes>()
                val existingBadgeIds = existingStudentBadges.map { it.badge_id }

                val pastRecords = db.from("assessment_record")
                    .select(columns = Columns.list("id", "score", "assessment_id")) {
                        filter { eq("student_id", actualStudentId) }
                    }
                    .decodeList<AssessmentRecordRes>()

                val totalAssessmentsTaken = pastRecords.size

                val isPerfectScore = currentScore.toInt() == totalItems
                val perfectScoreCount = if (isPerfectScore) {
                    pastRecords.count { it.score.toInt() >= totalItems }
                } else 0

                val percentage = if (totalItems > 0) (currentScore / totalItems) * 100 else 0.0

                // --- EVALUATE BADGES ---
                suspend fun awardBadge(badgeName: String) {
                    val badge = allBadges.find { it.badge_name == badgeName }
                    if (badge != null && !existingBadgeIds.contains(badge.id)) {

                        db.from("student_badges").insert(
                            StudentBadgeInsert(student_id = actualStudentId, badge_id = badge.id)
                        )
                        newlyEarnedBadges.add(badgeName)

                    } else if (badge == null) {
                        Log.e("BadgeRepo", "CRITICAL ERROR: Badge '$badgeName' was NOT FOUND in your Supabase 'badges' table! Did you run the SQL insert?")
                    } else {
                        Log.d("BadgeRepo", "Student already has the '$badgeName' badge. Skipping.")
                    }
                }

                if (totalAssessmentsTaken == 1) {
                    awardBadge("First-Timer")
                }
                if (percentage >= 75.0) {
                    awardBadge("Three-Quarter Score!")
                }
                if (isPerfectScore) {
                    if (perfectScoreCount == 1) awardBadge("First Ace!")
                    if (perfectScoreCount == 3) awardBadge("Triple Ace")
                }

                return@withContext newlyEarnedBadges

            } catch (e: Exception) {
                return@withContext emptyList()
            }
        }
    }

    suspend fun awardGameBadgeFromUnity(userId: Int, badgeId: Int): String? {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Convert the app's user_id into the database's student_id
                val studentRecord = db.from("student")
                    .select(columns = Columns.list("id")) {
                        filter { eq("user_id", userId) }
                    }.decodeSingleOrNull<StudentIdRes>()

                if (studentRecord == null) {
                    Log.e("BadgeRepo", "Could not find student profile for User ID: $userId")
                    return@withContext null
                }
                val actualStudentId = studentRecord.id

                // 2. Check if the student already earned this specific game badge
                val existingBadge = db.from("student_badges")
                    .select(columns = Columns.list("badge_id")) {
                        filter {
                            eq("student_id", actualStudentId)
                            eq("badge_id", badgeId)
                        }
                    }.decodeSingleOrNull<ExistingBadgeRes>()

                if (existingBadge != null) {
                    Log.d("BadgeRepo", "Student already has game badge $badgeId. Skipping.")
                    return@withContext null // Return null so we don't show a Toast duplicate
                }

                // 3. Insert the new badge into the database!
                db.from("student_badges").insert(
                    StudentBadgeInsert(student_id = actualStudentId, badge_id = badgeId)
                )

                // 4. Fetch the actual badge name so we can show it in the popup Toast
                val badgeDef = db.from("badges")
                    .select(columns = Columns.list("id", "badge_name")) {
                        filter { eq("id", badgeId) }
                    }.decodeSingleOrNull<BadgeRes>()

                return@withContext badgeDef?.badge_name ?: "Secret Game Badge"

            } catch (e: Exception) {
                Log.e("BadgeRepo", "Error saving game badge: ${e.message}")
                return@withContext null
            }
        }
    }
}