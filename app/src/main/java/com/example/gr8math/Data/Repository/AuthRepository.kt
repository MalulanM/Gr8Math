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

class AuthRepository {

    private val auth = SupabaseService.client.auth
    private val db = SupabaseService.client

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

                Result.success(user)

            } catch (e: Exception) {

                Log.e("AUTH_DEBUG", "Login Failed: ${e.message}")
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
            // Insert specific Student Data
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
                // Check public 'user' table for this email
                val count = db.from("user").select(columns = Columns.list("id")) {
                    filter {
                        eq("email_add", email)
                    }
                    count(Count.EXACT)
                }.countOrNull()

                // If count is greater than 0, email exists
                (count ?: 0) > 0
            } catch (e: Exception) {
                e.printStackTrace()
                false // If error, assume it doesn't exist so flow isn't blocked (or handle error strictly)
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
            try {
                auth.resetPasswordForEmail(email)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun verifyRecoveryCode(email: String, code: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                auth.verifyEmailOtp(
                    type = OtpType.Email.RECOVERY,
                    email = email,
                    token = code
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun updateUserPassword(newPass: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                auth.updateUser {
                    password = newPass
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

}