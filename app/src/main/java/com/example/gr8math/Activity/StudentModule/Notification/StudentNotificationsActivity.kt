package com.example.gr8math.Activity.StudentModule.Notification

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gr8math.Activity.StudentModule.Badges.StudentBadgesActivity
import com.example.gr8math.Activity.StudentModule.ClassManager.StudentClassPageActivity
import com.example.gr8math.Activity.StudentModule.Grades.StudentGradesActivity
import com.example.gr8math.Adapter.StudentNotificationAdapter
import com.example.gr8math.Model.CurrentCourse
import com.example.gr8math.R
import com.example.gr8math.Utils.NotificationHelper
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.ViewModel.NotificationsViewModel
import com.example.gr8math.ViewModel.StudentNotifState
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class StudentNotificationsActivity : AppCompatActivity() {

    // Using the Shared ViewModel
    private val viewModel: NotificationsViewModel by viewModels()

    private lateinit var rvNotifications: RecyclerView
    private lateinit var adapter: StudentNotificationAdapter
    private lateinit var btnMarkAllRead: Button
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_notifications)

        // Init Views
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.btnSettings).setOnClickListener { showSettingsDialog() }
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead)

        // Bottom Nav
        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_notifications
        setupBottomNav()

        // RecyclerView
        rvNotifications = findViewById(R.id.rvNotifications)
        rvNotifications.layoutManager = LinearLayoutManager(this)

        adapter = StudentNotificationAdapter(mutableListOf()) { item, position ->
            // --- ON CLICK ---
            if (!item.isRead) {
                // 1. Visual Update
                item.isRead = true
                adapter.notifyItemChanged(position)
                updateMarkAllButton()

                // 2. Backend Update
                viewModel.markRead(item.id)

                // 3. Badge Update
                NotificationHelper.fetchUnreadCount(bottomNav)
            }

            // 4. Navigation (FIXED)
            val intent = Intent(this, StudentClassPageActivity::class.java).apply {
                // Pass Content IDs
                putExtra("courseId", item.courseId)
                putExtra("lessonId", item.lessonId)
                putExtra("assessmentId", item.assessmentId)
                putExtra("studentId", item.studentId)

                // FIX: Pass User Context so logic works!
                putExtra("id", CurrentCourse.userId)
                putExtra("role", CurrentCourse.currentRole)
                // Note: We use the current section name as fallback,
                // though it might change if the notif is for a different class.
                putExtra("sectionName", CurrentCourse.sectionName)

                putExtra("fromNotification", true)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        }
        rvNotifications.adapter = adapter

        // --- Mark All Read Logic ---
        btnMarkAllRead.setOnClickListener {
            val list = adapter.getList()
            if (list.isEmpty()) return@setOnClickListener

            val ids = list.map { it.id }

            // Optimistic Update
            list.forEach { it.isRead = true }
            adapter.notifyDataSetChanged()
            updateMarkAllButton()

            // Backend Update
            viewModel.markAllRead(ids)
            NotificationHelper.fetchUnreadCount(bottomNav)
            ShowToast.showMessage(this, "All notifications marked as read")
        }

        // --- Observer ---
        viewModel.loadStudentNotifications(CurrentCourse.userId, CurrentCourse.courseId)

        viewModel.studentState.observe(this) { state ->
            when (state) {
                is StudentNotifState.Loading -> { }
                is StudentNotifState.Success -> {
                    adapter.updateList(state.data)
                    updateMarkAllButton()
                }
                is StudentNotifState.Error -> {
                    ShowToast.showMessage(this, state.message)
                }
            }
        }
    }

    private fun updateMarkAllButton() {
        val hasUnread = adapter.getList().any { !it.isRead }
        btnMarkAllRead.visibility = if (hasUnread) View.VISIBLE else View.GONE
    }

    private fun setupBottomNav() {
        NotificationHelper.fetchUnreadCount(bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == bottomNav.selectedItemId) return@setOnItemSelectedListener true

            val intent = when (item.itemId) {
                R.id.nav_class -> Intent(this, StudentClassPageActivity::class.java)
                R.id.nav_badges -> Intent(this, StudentBadgesActivity::class.java)
                R.id.nav_grades -> Intent(this, StudentGradesActivity::class.java)
                else -> null
            }

            intent?.let {
                it.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(it)
            }
            true
        }
    }

    private fun showSettingsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_student_notification_settings, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialogView.findViewById<ImageView>(R.id.btnBackSettings).setOnClickListener { dialog.dismiss() }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }
}