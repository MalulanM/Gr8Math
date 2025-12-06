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
import com.example.gr8math.adapter.MarkAllReadRequest
import com.example.gr8math.adapter.TeacherNotification
import com.example.gr8math.adapter.TeacherNotificationAdapter
import com.example.gr8math.adapter.TeacherNotificationResponse
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.CurrentCourse
import com.example.gr8math.utils.ShowToast
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.gr8math.utils.NotificationHelper
class TeacherNotificationsActivity : AppCompatActivity() {

    private lateinit var rvNotifications: RecyclerView
    private lateinit var adapter: TeacherNotificationAdapter
    private lateinit var btnMarkAllRead: Button
    private lateinit var bottomNav: BottomNavigationView

    private val notifications = mutableListOf<TeacherNotification>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_notifications)

        // --- Toolbar ---
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.btnSettings).setOnClickListener { showSettingsDialog() }

        // --- Bottom Navigation ---
        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_notifications

        // --- RecyclerView ---
        rvNotifications = findViewById(R.id.rvNotifications)
        rvNotifications.layoutManager = LinearLayoutManager(this)

        adapter = TeacherNotificationAdapter(notifications) { item, position ->

            // Mark as read
            item.is_read = true
            adapter.notifyItemChanged(position)


            NotificationHelper.fetchUnreadCount(bottomNav)

            ConnectURL.api.markNotificationRead(item.id)
                .enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        Log.d("NOTIF", "Marked read: ${item.id}")
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {

                    }
                })


            val intent = Intent(this, StudentScoresActivity::class.java).apply {
                putExtra("EXTRA_STUDENT_ID", item.student_id)
                putExtra("EXTRA_STUDENT_NAME", item.name)
                putExtra("AUTO_ASSESSMENT_ID", item.assessment_id)   // <-- IMPORTANT
            }
            startActivity(intent)

        }

        rvNotifications.adapter = adapter

        // --- Button: Mark All Read ---
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead)

        fetchNotifications()


        btnMarkAllRead.setOnClickListener {
            if (notifications.isEmpty()) return@setOnClickListener

            val notifIds = notifications.map { it.id }

            // Optimistically mark all read in the UI
            notifications.forEach { it.is_read = true }
            adapter.notifyDataSetChanged()

            NotificationHelper.fetchUnreadCount(bottomNav)

            val request = MarkAllReadRequest(notifIds)


            ConnectURL.api.markAllNotificationsRead(request)
                .enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {

                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {

                    }
                })

            ShowToast.showMessage(this, "All notifications marked as read")
        }




        // --- Bottom Nav Clicks ---
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_class -> {
                    startActivity(Intent(this, TeacherClassPageActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish()
                    true
                }
                R.id.nav_participants -> {
                    startActivity(Intent(this, TeacherParticipantsActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish()
                    true
                }
                R.id.nav_notifications -> {
                    true // Already here
                }
                R.id.nav_dll -> {
                    startActivity(Intent(this, DLLViewActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish()
                    true
                }
                else -> false
            }
        }
        NotificationHelper.fetchUnreadCount(bottomNav)
    }

    // ----------------------------------------------------------
    // Fetch Notifications from API
    // ----------------------------------------------------------
    private fun fetchNotifications() {
        val apiService = ConnectURL.api
        val call = apiService.getTeacherNotifications(
            CurrentCourse.userId,
            CurrentCourse.courseId
        )

        call.enqueue(object : Callback<TeacherNotificationResponse> {
            override fun onResponse(
                call: Call<TeacherNotificationResponse>,
                response: Response<TeacherNotificationResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {

                    notifications.clear()
                    notifications.addAll(response.body()!!.notifications)
                    adapter.notifyDataSetChanged()


                } else {
                    Toast.makeText(this@TeacherNotificationsActivity, "No notifications found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<TeacherNotificationResponse>, t: Throwable) {
                ShowToast.showMessage(this@TeacherNotificationsActivity, "Failed to connect to server.")
            }
        })
    }


    // ----------------------------------------------------------
    // Settings Dialog
    // ----------------------------------------------------------
    private fun showSettingsDialog() {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_teacher_notification_settings, null)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<ImageView>(R.id.btnBackSettings)
            .setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }
}
