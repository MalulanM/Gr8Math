package com.example.gr8math.Helper

import android.Manifest
import android.app.Activity // 🌟 Added
import android.content.Context
import android.content.Intent // 🌟 Added
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import com.example.gr8math.Services.SupabaseService
import com.google.firebase.messaging.FirebaseMessaging
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object NotificationMethods {

    /**
     * Checks for Android 13+ permissions and triggers token registration.
     */
    fun setupNotifications(
        context: Context,
        userId: Int,
        launcher: ActivityResultLauncher<String>,
        scope: CoroutineScope
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                registerToken(userId, scope)
            } else {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            registerToken(userId, scope)
        }
    }

    /**
     * Fetches the FCM token and saves/updates it in the Supabase 'user_devices' table.
     */
    fun registerToken(userId: Int, scope: CoroutineScope) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener

            val token = task.result
            scope.launch(Dispatchers.IO) {
                try {
                    val deviceData = buildJsonObject {
                        put("user_id", userId)
                        put("fcm_token", token)
                    }
                    SupabaseService.client.from("user_devices").upsert(deviceData)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Handles redirection when a notification is tapped.
     */
    fun handleNotificationIntent(activity: Activity, intent: Intent?, userId: Int, role: String) {
        val type = intent?.getStringExtra("notif_type")
        val metaString = intent?.getStringExtra("notif_meta")

        if (type != null && !metaString.isNullOrEmpty()) {
            try {
                val metaJson = org.json.JSONObject(metaString)

                val courseId = when {
                    metaJson.has("course_id") -> metaJson.getInt("course_id")
                    metaJson.has("class_id") -> metaJson.getInt("class_id")
                    else -> -1
                }

                if (courseId != -1) {
                    // Determine destination based on role
                    val destination = if (role.lowercase() == "teacher") {
                        "com.example.gr8math.Activity.TeacherModule.ClassManager.TeacherClassPageActivity"
                    } else {
                        "com.example.gr8math.Activity.StudentModule.ClassManager.StudentClassPageActivity"
                    }

                    val nextIntent = Intent().setClassName(activity.packageName, destination).apply {
                        putExtra("id", userId)
                        putExtra("role", role)
                        putExtra("courseId", courseId)
                        putExtra("sectionName", "Class from Notification")
                    }
                    activity.startActivity(nextIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Clean up the intent so it doesn't trigger again
            intent.removeExtra("notif_type")
            intent.removeExtra("notif_meta")
        }
    }

    /**
     * Removes the token from the database to stop notifications for this user on logout.
     */
    fun removeTokenOnLogout(userId: Int, scope: CoroutineScope, onComplete: () -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                SupabaseService.client.from("user_devices")
                    .delete { filter { eq("user_id", userId) } }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // Return to main thread to trigger navigation/UI changes
                launch(Dispatchers.Main) { onComplete() }
            }
        }
    }
}