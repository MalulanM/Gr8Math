package com.example.gr8math

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
import com.example.gr8math.adapter.FacultyNotification
import com.example.gr8math.adapter.FacultyNotificationAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class FacultyNotificationsActivity : AppCompatActivity() {

    private lateinit var rvNotifications: RecyclerView
    private lateinit var adapter: FacultyNotificationAdapter
    private lateinit var btnMarkAllRead: Button

    private val notifications = mutableListOf<FacultyNotification>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // --- UPDATED: Now using its own layout file ---
        setContentView(R.layout.activity_faculty_notifications)

        // Find Views
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnSettings = findViewById<ImageView>(R.id.btnSettings)

        // Toolbar Listeners
        btnBack.setOnClickListener { finish() }
        btnSettings.setOnClickListener { showSettingsDialog() }

        // Dummy Data
        notifications.add(FacultyNotification(
            "New Request!",
            "There is a new account request.",
            "07:00 AM",
            false
        ))

        // Setup RecyclerView
        rvNotifications = findViewById(R.id.rvNotifications)
        rvNotifications.layoutManager = LinearLayoutManager(this)
        adapter = FacultyNotificationAdapter(notifications)
        rvNotifications.adapter = adapter

        // Mark All Read Logic
        checkUnreadStatus()

        btnMarkAllRead.setOnClickListener {
            notifications.forEach { it.isRead = true }
            adapter.notifyDataSetChanged()
            checkUnreadStatus()
            Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkUnreadStatus() {
        val hasUnread = notifications.any { !it.isRead }
        if (hasUnread) {
            btnMarkAllRead.visibility = View.VISIBLE
        } else {
            btnMarkAllRead.visibility = View.GONE
        }
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