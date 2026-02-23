package com.example.gr8math.Services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.gr8math.Activity.LoginAndRegister.AppLoginActivity
import com.example.gr8math.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 1. Extract the 'type' from the Data payload
        val type = remoteMessage.data["type"] ?: "general"

        // 2. Check local SharedPreferences
        val prefs = getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE)

        val isEnabled = when (type) {
            "arrival" -> prefs.getBoolean("arrival_enabled", true)
            "lesson" -> prefs.getBoolean("lesson_enabled", true)
            "assessment" -> prefs.getBoolean("assessment_enabled", true)
            "assessment_submission" -> prefs.getBoolean("submission_enabled", true)
            else -> true
        }

        // 3. Only show if the user hasn't toggled it off
        if (isEnabled) {
            val title = remoteMessage.notification?.title ?: "Gr8Math Update"
            val body = remoteMessage.notification?.body ?: "Check your dashboard for details."
            val meta = remoteMessage.data["meta"] ?: "{}"

            showDeviceNotification(title, body, type, meta)
        } else {
            android.util.Log.d("FCM_FILTER", "Notification type '$type' is disabled in settings. Skipping.")
        }
    }

    private fun showDeviceNotification(title: String, message: String, type: String, meta: String) {
        val channelId = "gr8math_notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Gr8Math Alerts", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        // When the user taps the notification, we open the Login activity (or Dashboard)
        // You can use 'type' and 'meta' to route them to specific pages later
        val intent = Intent(this, AppLoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notif_type", type)
            putExtra("notif_meta", meta)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notifications_green)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}