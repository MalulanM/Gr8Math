package com.example.gr8math.Data.Repository

import android.util.Log
import com.example.gr8math.Data.Model.StudentProfile
import com.example.gr8math.Data.Model.TeacherProfile
import com.example.gr8math.Data.Model.UserProfile
import com.example.gr8math.Services.SupabaseService
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.jan.supabase.auth.OtpType
import kotlinx.serialization.Serializable

@Serializable
data class AuditTrailInsert(
    val user_id: Int?,
    val resource: String,
    val action: String,
    val status: String,
    val details: String
)

class AuthRepository {

    private val auth = SupabaseService.client.auth
    private val db = SupabaseService.client

    // --- NEW: REUSABLE AUDIT TRAIL HELPER ---
    private suspend fun logAuditTrail(
        userId: Int?,
        resource: String,
        action: String,
        status: String,
        details: String
    ) {
        try {
            val auditEntry = AuditTrailInsert(userId, resource, action, status, details)
            db.from("audit_trails").insert(auditEntry)
        } catch (e: Exception) {
            Log.e("AUTH_DEBUG", "Failed to log audit trail: ${e.message}")
        }
    }

    // --- NEW: Helper to safely fetch user ID by email for logging ---
    private suspend fun getUserIdByEmailSafe(email: String): Int? {
        return try {
            val users = db.from("user")
                .select(columns = Columns.list("id")) {
                    filter { eq("email_add", email) }
                }.decodeList<UserProfile>()
            users.firstOrNull()?.id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun login(emailInput: String, passwordInput: String): Result<UserProfile> {
        return withContext(Dispatchers.IO) {
            Log.d("AUTH_DEBUG", "--- LOGIN ATTEMPT STARTED ---")
            Log.d("AUTH_DEBUG", "1. Checking credentials for Email: $emailInput")
            try {

                auth.signInWith(Email) {
                    this.email = emailInput
                    this.password = passwordInput
                }

                val user = db.from("user")
                    .select {
                        filter {
                            eq("email_add", emailInput)
                        }
                    }
                    .decodeSingle<UserProfile>()

                // --- NEW: Log Successful Login ---
                logAuditTrail(user.id, "Authentication", "LOGIN", "SUCCESS", "Successful Login.")

                Result.success(user)

            } catch (e: Exception) {
                // --- NEW: Log Failed Login ---
                val targetUserId = getUserIdByEmailSafe(emailInput)
                logAuditTrail(targetUserId, "Authentication", "LOGIN", "FAILED", "Failed login attempt for $emailInput.")
                Result.failure(Exception("Email or Password not found"))
            }
        }
    }

    suspend fun updateFirstLoginStatus(dbId: Int) {
        withContext(Dispatchers.IO) {
            try {
                db.from("user").update(
                    { set("first_login", false) }
                ) {
                    filter { eq("id", dbId) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun registerStudent(
        emailInput: String, passInput: String, firstName: String, lastName: String,
        gender: String, birthDate: String, lrn: String
    ): Result<UserProfile> {
        return registerUserGeneric(
            emailInput, passInput, firstName, lastName, gender, birthDate, "Student"
        ) { userId ->
            val student = StudentProfile(userId = userId, lrn = lrn, gradeLevel = 8)
            db.from("student").insert(student)
        }
    }

    suspend fun registerTeacher(
        emailInput: String, passInput: String, firstName: String, lastName: String,
        gender: String, birthDate: String, position: String
    ): Result<UserProfile> {
        return registerUserGeneric(
            emailInput, passInput, firstName, lastName, gender, birthDate, "Teacher"
        ) { userId ->
            val teacher = TeacherProfile(userId = userId, teachingPosition = position)
            db.from("teacher").insert(teacher)
        }
    }

    private suspend fun registerUserGeneric(
        emailInput: String, passInput: String, firstName: String, lastName: String,
        gender: String, birthDate: String, role: String,
        insertRoleData: suspend (Int) -> Unit
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val authResult = auth.signUpWith(Email) {
                email = emailInput
                password = passInput
            }
            val authUuid = auth.currentUserOrNull()?.id
                ?: throw Exception("Auth succeeded but user ID is missing")

            val userEntry = UserProfile(
                authUserId = authUuid,
                firstName = firstName,
                lastName = lastName,
                email = emailInput,
                gender = gender,
                birthdate = birthDate,
                roles = role
            )

            val insertedUser = db.from("user").insert(userEntry) {
                select()
            }.decodeSingle<UserProfile>()

            val newUserId = insertedUser.id ?: throw Exception("Failed to retrieve new User ID")

            insertRoleData(newUserId)

            // --- NEW: Log Successful Registration ---
            logAuditTrail(newUserId, "Authentication", "REGISTER", "SUCCESS", "New $role account created for $emailInput.")

            Result.success(insertedUser)

        } catch (e: Exception) {
            Log.e("AUTH", "Registration Failed: ${e.message}")
            val msg = if (e.message?.contains("User already registered") == true)
                "Email already exists" else e.message
            Result.failure(Exception(msg))
        }
    }

    suspend fun checkEmailExists(email: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val count = db.from("user").select(columns = Columns.list("id")) {
                    filter {
                        eq("email_add", email)
                    }
                    count(Count.EXACT)
                }.countOrNull()

                (count ?: 0) > 0
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun checkLrnExists(lrn: String): Boolean{
        return withContext(Dispatchers.IO){
            try{
                val count = db.from("student").select(columns = Columns.list("id")){
                    filter {
                        eq("learners_ref_number", lrn)
                    }
                    count(Count.EXACT)
                }.countOrNull()
                (count?:0)>0
            }
            catch (e: Exception){
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun sendPasswordResetCode(email: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            // Grab ID for logging
            val targetUserId = getUserIdByEmailSafe(email)

            try {
                auth.resetPasswordForEmail(email)

                logAuditTrail(targetUserId, "Authentication", "REQUEST_RESET", "SUCCESS", "Requested password reset OTP.")

                Result.success(Unit)
            } catch (e: Exception) {
                logAuditTrail(targetUserId, "Authentication", "REQUEST_RESET", "FAILED", "Failed OTP request: ${e.message}")
                Result.failure(e)
            }
        }
    }

    suspend fun verifyRecoveryCode(email: String, code: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val targetUserId = getUserIdByEmailSafe(email)

            try {
                auth.verifyEmailOtp(
                    type = OtpType.Email.RECOVERY,
                    email = email,
                    token = code
                )
                Result.success(Unit)
            } catch (e: Exception) {
                // --- NEW: Log Failed Verification ---
                logAuditTrail(targetUserId, "Authentication", "VERIFY_RESET", "FAILED", "Failed OTP verification.")
                Result.failure(e)
            }
        }
    }

    suspend fun updateUserPassword(newPass: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            // Find current logged in user's email to get their ID for logging
            val currentEmail = auth.currentUserOrNull()?.email
            val targetUserId = if (currentEmail != null) getUserIdByEmailSafe(currentEmail) else null

            try {
                auth.updateUser {
                    password = newPass
                }
                // --- NEW: Log Successful Password Update ---
                logAuditTrail(targetUserId, "Authentication", "UPDATE_PASSWORD", "SUCCESS", "Password updated successfully.")
                Result.success(Unit)
            } catch (e: Exception) {
                // --- NEW: Log Failed Password Update ---
                logAuditTrail(targetUserId, "Authentication", "UPDATE_PASSWORD", "FAILED", "Password update failed: ${e.message}")
                Result.failure(e)
            }
        }
    }
}