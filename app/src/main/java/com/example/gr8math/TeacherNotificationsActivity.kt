package com.example.gr8math // Make sure this matches your package name

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
import com.example.gr8math.adapter.TeacherNotification
import com.example.gr8math.adapter.TeacherNotificationAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TeacherNotificationsActivity : AppCompatActivity() {

    private lateinit var rvNotifications: RecyclerView
    private lateinit var adapter: TeacherNotificationAdapter
    private lateinit var btnMarkAllRead: Button
    private lateinit var bottomNav: BottomNavigationView

    // Dummy data list
    private val notifications = mutableListOf<TeacherNotification>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_notifications)

        // --- 1. Setup Toolbar ---
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.btnSettings).setOnClickListener { showSettingsDialog() }

        // --- 2. Initialize Bottom Navigation (MOVED UP) ---
        // We must find this view BEFORE calling checkUnreadStatus()
        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_notifications // Highlight "Notifications"

        // --- 3. Setup Dummy Data ---
        notifications.add(TeacherNotification("Time for class!", "You have a class in Section 1.", "07:00 AM", false))
        notifications.add(TeacherNotification("Student Submission", "A student submitted their work", "07:00 AM", false))
        notifications.add(TeacherNotification("Old Notification", "This one is already read.", "Yesterday", true))

        // --- 4. Setup RecyclerView ---
        rvNotifications = findViewById(R.id.rvNotifications)
        rvNotifications.layoutManager = LinearLayoutManager(this)
        adapter = TeacherNotificationAdapter(notifications)
        rvNotifications.adapter = adapter

        // --- 5. Setup Mark All Read Button ---
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead)

        // Now it is safe to check status because bottomNav is initialized
        checkUnreadStatus()

        btnMarkAllRead.setOnClickListener {
            // Set all to read
            notifications.forEach { it.isRead = true }
            adapter.notifyDataSetChanged()
            checkUnreadStatus() // Re-check to hide button and badge
            Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show()
        }

        // --- 6. Setup Bottom Navigation Click Listener ---
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_class -> {
                    finish() // Go back to class page
                    true
                }
                R.id.nav_participants -> {
                    // TODO: Navigate to Participants Activity
                    Toast.makeText(this, "Participants clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_notifications -> {
                    // Already here
                    true
                }
                R.id.nav_dll -> {
                    // TODO: Navigate to DLL Activity
                    Toast.makeText(this, "DLL clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }

    private fun checkUnreadStatus() {
        // If any item is NOT read, show the button
        val hasUnread = notifications.any { !it.isRead }

        if (hasUnread) {
            btnMarkAllRead.visibility = View.VISIBLE
        } else {
            btnMarkAllRead.visibility = View.GONE
        }
        updateBadge()
    }

    private fun updateBadge() {
        val hasUnread = notifications.any { !it.isRead }

        // This line caused the crash before because bottomNav wasn't found yet
        val badge = bottomNav.getOrCreateBadge(R.id.nav_notifications)

        if (hasUnread) {
            badge.isVisible = true
            badge.backgroundColor = Color.RED
        } else {
            badge.isVisible = false
        }
    }

    private fun showSettingsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_teacher_notification_settings, null)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Handle Back button inside dialog
        val btnBackSettings = dialogView.findViewById<ImageView>(R.id.btnBackSettings)
        btnBackSettings.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }
}