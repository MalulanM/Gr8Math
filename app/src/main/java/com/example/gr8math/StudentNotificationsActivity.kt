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
import com.example.gr8math.adapter.MarkAllReadRequest
import com.example.gr8math.adapter.StudentNotification
import com.example.gr8math.adapter.StudentNotificationAdapter
import com.example.gr8math.adapter.StudentNotificationResponse
import com.example.gr8math.adapter.TeacherNotificationAdapter
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.CurrentCourse
import com.example.gr8math.utils.NotificationHelper
import com.example.gr8math.utils.ShowToast
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StudentNotificationsActivity : AppCompatActivity() {

    private lateinit var rvNotifications: RecyclerView
    private lateinit var adapter: StudentNotificationAdapter
    private lateinit var btnMarkAllRead: Button
    private lateinit var bottomNav: BottomNavigationView

    private val notifications = mutableListOf<StudentNotification>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_notifications)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.btnSettings).setOnClickListener { showSettingsDialog() }

        // Initialize Bottom Nav
        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_notifications

        rvNotifications = findViewById(R.id.rvNotifications)
        rvNotifications.layoutManager = LinearLayoutManager(this)
        adapter = StudentNotificationAdapter(notifications) { item, position ->
            NotificationHelper.fetchUnreadCount(bottomNav)
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

                    }
                })


            val intent = Intent(this, StudentClassPageActivity::class.java).apply {
                putExtra("courseId", item.course_id ?: CurrentCourse.courseId)
                putExtra("lessonId", item.lesson_id)
                putExtra("assessmentId", item.assessment_id)
                putExtra("fromNotification", true)

                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        }

        rvNotifications.adapter = adapter

        btnMarkAllRead = findViewById(R.id.btnMarkAllRead)
        checkUnreadStatus()

        fetchNotifications()
        NotificationHelper.fetchUnreadCount(bottomNav)

        btnMarkAllRead.setOnClickListener {
            if (notifications.isEmpty()) return@setOnClickListener

            val notifIds = notifications.map { it.id }

            // Optimistically mark all read in the UI
            notifications.forEach { it.is_read = true }
            adapter.notifyDataSetChanged()
            checkUnreadStatus()
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

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == bottomNav.selectedItemId) {
                return@setOnItemSelectedListener true
            }

            val intent = when (item.itemId) {
                R.id.nav_class -> Intent(this, StudentClassPageActivity::class.java)
                R.id.nav_badges -> Intent(this, StudentBadgesActivity::class.java)
                R.id.nav_notifications -> null
                R.id.nav_grades -> Intent(this, StudentGradesActivity::class.java)
                else -> null
            }

            intent?.let {
                // Prevent stacking
                it.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(it)
            }

            true
        }

    }

    private fun fetchNotifications() {
        val apiService = ConnectURL.api
        val call = apiService.getStudentNotifications(
            CurrentCourse.userId,
            CurrentCourse.courseId
        )

        call.enqueue(object : Callback<StudentNotificationResponse> { // single object
            override fun onResponse(
                call: Call<StudentNotificationResponse>,
                response: Response<StudentNotificationResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {

                    notifications.clear()
                    notifications.addAll(response.body()!!.notifications) // <-- extract list
                    adapter.notifyDataSetChanged()
                    checkUnreadStatus()
                } else {
                    Toast.makeText(this@StudentNotificationsActivity, "No notifications found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<StudentNotificationResponse>, t: Throwable) {
                Toast.makeText(this@StudentNotificationsActivity, "Failed to fetch notifications", Toast.LENGTH_SHORT).show()
            }
        })
    }




    private fun checkUnreadStatus() {
        val hasUnread = notifications.any { !it.is_read }
        btnMarkAllRead.visibility = if (hasUnread) View.VISIBLE else View.GONE
        updateBadge()
    }

    private fun updateBadge() {
        val hasUnread = notifications.any { !it.is_read }
        val badge = bottomNav.getOrCreateBadge(R.id.nav_notifications)
        if (hasUnread) {
            badge.isVisible = true
            badge.backgroundColor = Color.RED
        } else {
            badge.isVisible = false
        }
    }



    private fun showSettingsDialog() {
        // Inflate the STUDENT version of the dialog (with 3 switches)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_student_notification_settings, null)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val btnBackSettings = dialogView.findViewById<ImageView>(R.id.btnBackSettings)
        btnBackSettings.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }
}