package com.example.gr8math.Services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.gr8math.NotificationActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.gr8math.Data.Api.ConnectURL
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyFirebaseService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        val title = remoteMessage.notification?.title ?: "New Notification"
        val body = remoteMessage.notification?.body ?: ""

        // Show the notification
        showNotification(title, body)
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "class_time_channel"

        // Open NotificationActivity when tapped
        val intent = Intent(this, NotificationActivity::class.java).apply {
            putExtra("title", title)
            putExtra("message", message)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1)

        if (userId != -1) {
            // Send the new token to the backend
            ConnectURL.api.storeDeviceToken(userId, token)
                .enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        // Optional: token successfully sent
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        // Optional: log failure
                        t.printStackTrace()
                    }
                })
        }
    }
}
