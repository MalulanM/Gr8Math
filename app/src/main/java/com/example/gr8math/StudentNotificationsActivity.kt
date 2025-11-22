package com.example.gr8math

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gr8math.adapter.StudentNotification
import com.example.gr8math.adapter.StudentNotificationAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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

        // Dummy Data for Student
        notifications.add(StudentNotification("Time for class!", "You have a class in Section 1.", "07:00 AM", false))
        notifications.add(StudentNotification("New Lesson Posted", "New lesson available.", "07:00 AM", false))
        notifications.add(StudentNotification("New Assessment Posted", "New assessment test available.", "07:00 AM", false))

        rvNotifications = findViewById(R.id.rvNotifications)
        rvNotifications.layoutManager = LinearLayoutManager(this)
        adapter = StudentNotificationAdapter(notifications)
        rvNotifications.adapter = adapter

        btnMarkAllRead = findViewById(R.id.btnMarkAllRead)
        checkUnreadStatus()

        btnMarkAllRead.setOnClickListener {
            notifications.forEach { it.isRead = true }
            adapter.notifyDataSetChanged()
            checkUnreadStatus()
            Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show()
        }

        // Student Bottom Navigation Logic
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_class -> {
                    finish() // Go back to student class page
                    true
                }
                R.id.nav_badges -> {
                    Toast.makeText(this, "Badges clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_notifications -> true // Stay here
                R.id.nav_grades -> {
                    Toast.makeText(this, "Grades clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }

    private fun checkUnreadStatus() {
        val hasUnread = notifications.any { !it.isRead }
        btnMarkAllRead.visibility = if (hasUnread) View.VISIBLE else View.GONE
        updateBadge()
    }

    private fun updateBadge() {
        val hasUnread = notifications.any { !it.isRead }
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