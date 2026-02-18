package com.example.gr8math.Data.Repository


import aws.sdk.kotlin.services.s3.model.ObjectCannedAcl
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import com.example.gr8math.Services.SupabaseService
import com.example.gr8math.Services.TigrisService
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.jan.supabase.storage.storage


class TeacherProfileRepository {

    suspend fun getProfileData(userId: Int): Result<TeacherProfileData> {
        return withContext(Dispatchers.IO) {
            try {
                val user = SupabaseService.client.from("user")
                    .select { filter { eq("id", userId) } }.decodeSingle<UserEntity>()

                val teacher = SupabaseService.client.from("teacher")
                    .select { filter { eq("user_id", userId) } }.decodeSingleOrNull<TeacherEntity>()

                val achievements = if (teacher != null) {
                    SupabaseService.client.from("teacher_achievement")
                        .select { filter { eq("teacher_id", teacher.id) } }.decodeList<TeacherAchievementEntity>()
                } else emptyList()

                Result.success(TeacherProfileData(user, teacher, achievements))
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    suspend fun updateUserField(userId: Int, fieldName: String, value: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseService.client.from("user").update({ set(fieldName, value) }) { filter { eq("id", userId) } }
                Result.success(Unit)
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    suspend fun updateTeacherField(teacherId: Int, fieldName: String, value: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseService.client.from("teacher").update({ set(fieldName, value) }) { filter { eq("id", teacherId) } }
                Result.success(Unit)
            } catch (e: Exception) { Result.failure(e) }
        }
    }

//    suspend fun uploadProfilePicture(userId: Int, imageBytes: ByteArray, mimeType: String): Result<String> {
//        return withContext(Dispatchers.IO) {
//            try {
//                val extension = if (mimeType.contains("png")) "png" else "jpg"
//                val fileName = "profile_${userId}_${System.currentTimeMillis()}.$extension"
//                val bucket = SupabaseService.client.storage.from("lesson_images")
//                bucket.upload(fileName, imageBytes) { upsert = true }
//                Result.success(bucket.publicUrl(fileName))
//            } catch (e: Exception) { Result.failure(e) }
//        }
//    }
//
//
//    suspend fun uploadCertificate(userId: Int, imageBytes: ByteArray, mimeType: String): Result<String> {
//        return withContext(Dispatchers.IO) {
//            try {
//                val extension = if (mimeType.contains("png")) "png" else "jpg"
//                val fileName = "cert_${userId}_${System.currentTimeMillis()}.$extension"
//                val bucket = SupabaseService.client.storage.from("lesson_images")
//                bucket.upload(fileName, imageBytes) { upsert = true }
//                Result.success(bucket.publicUrl(fileName))
//            } catch (e: Exception) { Result.failure(e) }
//        }
//    }

    suspend fun deleteAchievement(achievementId: Int, imageUrl: String?): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                imageUrl?.let { url ->
                    val fileName = url.substringAfterLast("/")

                    TigrisService.s3Client.deleteObject(aws.sdk.kotlin.services.s3.model.DeleteObjectRequest {
                        bucket = TigrisService.BUCKET_NAME
                        key = fileName
                    })
                }

                SupabaseService.client.from("teacher_achievement").delete {
                    filter { eq("id", achievementId) }
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    suspend fun uploadProfilePicture(userId: Int, imageBytes: ByteArray, mimeType: String): Result<String> {
        return uploadToTigris(userId, imageBytes, mimeType, isCert = false)
    }

    suspend fun uploadCertificate(userId: Int, imageBytes: ByteArray, mimeType: String): Result<String> {
        return uploadToTigris(userId, imageBytes, mimeType, isCert = true)
    }

    suspend fun addAchievement(achievement: TeacherAchievementEntity): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseService.client.from("teacher_achievement").insert(achievement)
                Result.success(Unit)
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    suspend fun uploadToTigris(userId: Int, imageBytes: ByteArray, mimeType: String, isCert: Boolean = false): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val prefix = if (isCert) "cert" else "profile"
                val extension = if (mimeType.contains("png")) "png" else "jpg"
                val fileName = "${prefix}_${userId}_${System.currentTimeMillis()}.$extension"

                TigrisService.s3Client.putObject(PutObjectRequest {
                    bucket = TigrisService.BUCKET_NAME
                    key = fileName
                    body = ByteStream.fromBytes(imageBytes)
                    contentType = mimeType
                    acl = ObjectCannedAcl.PublicRead
                })

                val publicUrl = "https://${TigrisService.BUCKET_NAME}.fly.storage.tigris.dev/$fileName"

                Result.success(publicUrl)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}