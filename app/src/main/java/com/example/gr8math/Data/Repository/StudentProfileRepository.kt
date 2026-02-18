package com.example.gr8math.Data.Repository

import aws.sdk.kotlin.services.s3.model.ObjectCannedAcl
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import com.example.gr8math.R
import com.example.gr8math.Services.SupabaseService
import com.example.gr8math.Services.TigrisService
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StudentProfileRepository {
    private val supabase = SupabaseService.client

    suspend fun getStudentProfile(userId: Int): Result<StudentProfileData> = withContext(Dispatchers.IO) {
        try {
            val user = supabase.from("user").select { filter { eq("id", userId) } }.decodeSingle<UserEntity>()
            val student = supabase.from("student").select { filter { eq("user_id", userId) } }.decodeSingleOrNull<StudentEntity>()

            val allBadges = supabase.from("badges").select().decodeList<BadgeEntity>()
            val earnedBadges = if (student != null) {
                supabase.from("student_badges").select { filter { eq("student_id", student.id) } }.decodeList<StudentBadgeEntity>()
            } else emptyList()

            val uiBadges = allBadges.map { b ->
                val record = earnedBadges.find { it.badgeId == b.id }
                BadgeUiModel(
                    id = b.id,
                    name = b.badgeName,
                    description = b.badgeDesc,
                    imageRes = mapBadgeToDrawable(b.id),
                    isAcquired = earnedBadges.any { it.badgeId == b.id },
                    rank = record?.rank,
                    dateRewarded = record?.dateRewarded
                )
            }
            Result.success(StudentProfileData(user, student, uiBadges))
        } catch (e: Exception) { Result.failure(e) }
    }

    private fun mapBadgeToDrawable(id: Int): Int = when (id) {
        1 -> R.drawable.badge_firstace
        2 -> R.drawable.badge_firsttimer
        3 -> R.drawable.badge_firstescape
        4 -> R.drawable.badge_perfectescape
        5 -> R.drawable.badge_firstexplo
        6 -> R.drawable.badge_fullexplo
        7 -> R.drawable.badge_threequarter
        8 -> R.drawable.badge_tripleace
        else -> R.drawable.ic_cert
    }

    suspend fun updateBadgeRanks(studentId: Int, selectedBadgeIds: List<Int>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.from("student_badges").update(
                { set("rank", null as Int?) }
            ) {
                filter { eq("student_id", studentId) }
            }

            selectedBadgeIds.forEachIndexed { index, badgeId ->
                supabase.from("student_badges").update(
                    { set("rank", index + 1) } // index is 0, 1, 2 -> rank becomes 1, 2, 3
                ) {
                    filter {
                        eq("student_id", studentId)
                        eq("badge_id", badgeId)
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadToTigris(userId: Int, bytes: ByteArray, mime: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fileName = "profile_std_${userId}_${System.currentTimeMillis()}.${if (mime.contains("png")) "png" else "jpg"}"
            TigrisService.s3Client.putObject(PutObjectRequest {
                bucket = TigrisService.BUCKET_NAME
                key = fileName
                body = ByteStream.fromBytes(bytes)
                contentType = mime
                acl = ObjectCannedAcl.PublicRead
            })
            Result.success("https://${TigrisService.BUCKET_NAME}.fly.storage.tigris.dev/$fileName")
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateField(userId: Int, table: String, field: String, value: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.from(table).update({ set(field, value) }) {
                filter { eq(if (table == "user") "id" else "user_id", userId) }
            }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}