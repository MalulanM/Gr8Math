package com.example.gr8math

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gr8math.adapter.FacultyNotification
import com.example.gr8math.adapter.FacultyNotificationAdapter
import com.example.gr8math.adapter.MarkAllReadRequest
import com.example.gr8math.adapter.StudentNotificationAdapter
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.utils.ShowToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FacultyNotificationsActivity : AppCompatActivity() {

    private lateinit var rvNotifications: RecyclerView
    private lateinit var adapter: FacultyNotificationAdapter
    private lateinit var btnMarkAllRead: Button
    private var id: Int = 0
    private val notifications = mutableListOf<FacultyNotification>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faculty_notifications)

        id = intent.getIntExtra("id", 0) // admin user id

        // Find Views
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnSettings = findViewById<ImageView>(R.id.btnSettings)

        // Toolbar Listeners
        btnBack.setOnClickListener { finish() }
        btnSettings.setOnClickListener { showSettingsDialog() }

        // Setup RecyclerView
        rvNotifications = findViewById(R.id.rvNotifications)
        rvNotifications.layoutManager = LinearLayoutManager(this)
        adapter = FacultyNotificationAdapter(notifications) { item, position ->

            // Mark as read
            item.is_read = true
            adapter.notifyItemChanged(position)
            checkUnreadStatus()

            ConnectURL.api.markNotificationRead(item.id)
                .enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        Log.d("NOTIF", "Marked read: ${item.id}")
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Log.e("NOTIF_ERROR", t.localizedMessage ?: "")
                    }
                })
        }
        rvNotifications.adapter = adapter

        // Fetch notifications from API
        fetchNotifications(id)

        // Mark All Read Logic
        btnMarkAllRead.setOnClickListener {
            if (notifications.isEmpty()) return@setOnClickListener

            val notifIds = notifications.map { it.id }

            // Optimistically mark all read in the UI
            notifications.forEach { it.is_read = true }
            adapter.notifyDataSetChanged()
            checkUnreadStatus()

            val request = MarkAllReadRequest(notifIds)

            ConnectURL.api.markAllNotificationsRead(request)
                .enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            Log.d("NOTIF", "All notifications marked read")
                        } else {
                            Log.e("NOTIF_ERROR", "Failed to mark all read: ${response.code()}")
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Log.e("NOTIF_ERROR", t.localizedMessage ?: "")
                    }
                })

            ShowToast.showMessage(this, "All notifications marked as read")
        }

    }

    private fun fetchNotifications(userId: Int) {
        val apiService = ConnectURL.api
        val call = apiService.getAdminNotifications(userId)

        call.enqueue(object : Callback<List<FacultyNotification>> {
            override fun onResponse(
                call: Call<List<FacultyNotification>>,
                response: Response<List<FacultyNotification>>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    notifications.clear()
                    notifications.addAll(response.body()!!)
                    adapter.notifyDataSetChanged()
                    checkUnreadStatus()
                } else {
                    Toast.makeText(this@FacultyNotificationsActivity, "No notifications found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<FacultyNotification>>, t: Throwable) {
                Toast.makeText(this@FacultyNotificationsActivity, "Failed to fetch notifications", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun checkUnreadStatus() {
        btnMarkAllRead.visibility = if (notifications.any { !it.is_read }) View.VISIBLE else View.GONE
    }

    private fun showSettingsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_faculty_notification_settings, null)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<ImageView>(R.id.btnBackSettings).setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }
}
