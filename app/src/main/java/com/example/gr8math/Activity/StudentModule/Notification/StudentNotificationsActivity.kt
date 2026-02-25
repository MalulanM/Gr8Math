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

    private val viewModel: NotificationsViewModel by viewModels()

    private lateinit var rvNotifications: RecyclerView
    private lateinit var adapter: StudentNotificationAdapter
    private lateinit var btnMarkAllRead: Button
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_notifications)

        // 🌟 FOCUS ON THE CLASS SENT BY THE CLASS MANAGER
        val focusedCourseId = intent.getIntExtra("courseId", CurrentCourse.courseId)

        // Init Views
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.btnSettings).setOnClickListener { showSettingsDialog() }
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead)

        // Bottom Nav
        bottomNav = findViewById(R.id.bottom_navigation)
        setupBottomNav()

        // RecyclerView
        rvNotifications = findViewById(R.id.rvNotifications)
        rvNotifications.layoutManager = LinearLayoutManager(this)

        adapter = StudentNotificationAdapter(mutableListOf()) { item, position ->
            if (!item.isRead) {
                item.isRead = true
                adapter.notifyItemChanged(position)
                updateMarkAllButton()
                viewModel.markRead(item.id)
                NotificationHelper.fetchUnreadCount(bottomNav)
            }

            val nextIntent = Intent(this, StudentClassPageActivity::class.java).apply {
                putExtra("courseId", item.courseId)
                putExtra("lessonId", item.lessonId)
                putExtra("assessmentId", item.assessmentId)
                putExtra("studentId", item.studentId)
                putExtra("id", CurrentCourse.userId)
                putExtra("role", CurrentCourse.currentRole)
                putExtra("sectionName", CurrentCourse.sectionName)
                putExtra("fromNotification", true)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(nextIntent)
        }
        rvNotifications.adapter = adapter

        btnMarkAllRead.setOnClickListener {
            val list = adapter.getList()
            if (list.isEmpty()) return@setOnClickListener
            val ids = list.map { it.id }
            list.forEach { it.isRead = true }
            adapter.notifyDataSetChanged()
            updateMarkAllButton()
            viewModel.markAllRead(ids)
            NotificationHelper.fetchUnreadCount(bottomNav)
            ShowToast.showMessage(this, "All notifications marked as read")
        }

        // 🌟 TRIGGER TARGETED LOAD
        viewModel.loadStudentNotifications(CurrentCourse.userId, focusedCourseId)

        // --- Observer ---
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

    override fun onResume() {
        super.onResume()
        if (::bottomNav.isInitialized) {
            bottomNav.setOnItemSelectedListener(null)
            bottomNav.selectedItemId = R.id.nav_notifications
            setupBottomNavListeners(bottomNav)
        }
        NotificationHelper.fetchUnreadCount(bottomNav)
    }

    private fun updateMarkAllButton() {
        val hasUnread = adapter.getList().any { !it.isRead }
        btnMarkAllRead.visibility = if (hasUnread) View.VISIBLE else View.GONE
    }

    private fun setupBottomNav() {
        bottomNav.selectedItemId = R.id.nav_notifications
        NotificationHelper.fetchUnreadCount(bottomNav)
        setupBottomNavListeners(bottomNav)
    }

    private fun setupBottomNavListeners(navView: BottomNavigationView) {
        navView.setOnItemSelectedListener { item ->
            if (item.itemId == navView.selectedItemId) return@setOnItemSelectedListener true

            val intent = when (item.itemId) {
                R.id.nav_class -> Intent(this, StudentClassPageActivity::class.java)
                R.id.nav_badges -> Intent(this, StudentBadgesActivity::class.java)
                R.id.nav_grades -> Intent(this, StudentGradesActivity::class.java)
                R.id.nav_notifications -> null
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
        val dialog = MaterialAlertDialogBuilder(this).setView(dialogView).setCancelable(true).create()

        val swArrival = dialogView.findViewById<androidx.appcompat.widget.AppCompatCheckBox>(R.id.switchClassSchedule)
        val swLesson = dialogView.findViewById<androidx.appcompat.widget.AppCompatCheckBox>(R.id.switchPostedLesson)
        val swAssessment = dialogView.findViewById<androidx.appcompat.widget.AppCompatCheckBox>(R.id.switchPostedAssessment)

        val prefs = getSharedPreferences("NotificationPrefs", MODE_PRIVATE)
        swArrival.isChecked = prefs.getBoolean("arrival_enabled", true)
        swLesson.isChecked = prefs.getBoolean("lesson_enabled", true)
        swAssessment.isChecked = prefs.getBoolean("assessment_enabled", true)

        val savePrefs = {
            prefs.edit().apply {
                putBoolean("arrival_enabled", swArrival.isChecked)
                putBoolean("lesson_enabled", swLesson.isChecked)
                putBoolean("assessment_enabled", swAssessment.isChecked)
                apply()
            }
        }

        swArrival.setOnClickListener { savePrefs() }
        swLesson.setOnClickListener { savePrefs() }
        swAssessment.setOnClickListener { savePrefs() }

        dialogView.findViewById<ImageView>(R.id.btnBackSettings).setOnClickListener { dialog.dismiss() }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }
}