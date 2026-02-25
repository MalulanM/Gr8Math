package com.example.gr8math.Activity.TeacherModule.Notification

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
import com.example.gr8math.Activity.TeacherModule.StudentScoresActivity
import com.example.gr8math.Activity.TeacherModule.ClassManager.TeacherClassPageActivity
import com.example.gr8math.Activity.TeacherModule.DLL.DLLViewActivityMain
import com.example.gr8math.Activity.TeacherModule.Participants.TeacherParticipantsActivity
import com.example.gr8math.Adapter.TeacherNotificationAdapter
import com.example.gr8math.Model.CurrentCourse
import com.example.gr8math.R
import com.example.gr8math.Utils.NotificationHelper
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.ViewModel.NotifState
import com.example.gr8math.ViewModel.NotificationsViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TeacherNotificationsActivity : AppCompatActivity() {

    private val viewModel: NotificationsViewModel by viewModels()
    private lateinit var adapter: TeacherNotificationAdapter
    private lateinit var bottomNav: BottomNavigationView

    private lateinit var btnMarkAllRead: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_notifications)

        // 🌟 FOCUS ON THE CLASS SENT BY THE CLASS MANAGER
        val focusedCourseId = intent.getIntExtra("courseId", CurrentCourse.courseId)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.btnSettings).setOnClickListener { showSettingsDialog() }

        btnMarkAllRead = findViewById(R.id.btnMarkAllRead)
        btnMarkAllRead.visibility = View.GONE

        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_notifications
        setupBottomNav()

        val rvNotifications = findViewById<RecyclerView>(R.id.rvNotifications)
        rvNotifications.layoutManager = LinearLayoutManager(this)

        adapter = TeacherNotificationAdapter(mutableListOf()) { item, position ->
            if (!item.isRead) {
                item.isRead = true
                adapter.notifyItemChanged(position)

                updateMarkAllButtonVisibility()

                viewModel.markRead(item.id)
                NotificationHelper.fetchUnreadCount(bottomNav)
            }

            val intent = Intent(this, StudentScoresActivity::class.java).apply {
                putExtra("EXTRA_STUDENT_ID", item.studentId)
                putExtra("EXTRA_STUDENT_NAME", item.studentName)
                putExtra("AUTO_ASSESSMENT_ID", item.assessmentId)
            }
            startActivity(intent)
        }
        rvNotifications.adapter = adapter

        btnMarkAllRead.setOnClickListener {
            val list = adapter.getList()
            val unreadIds = list.filter { !it.isRead }.map { it.id }

            if (unreadIds.isEmpty()) return@setOnClickListener

            list.forEach { it.isRead = true }
            adapter.notifyDataSetChanged()

            updateMarkAllButtonVisibility()

            viewModel.markAllRead(unreadIds)
            NotificationHelper.fetchUnreadCount(bottomNav)

            ShowToast.showMessage(this, "All notifications marked as read")
        }

        // 🌟 TRIGGER TARGETED LOAD
        viewModel.loadTeacherNotifications(CurrentCourse.userId, focusedCourseId)

        viewModel.teacherState.observe(this) { state ->
            when(state) {
                is NotifState.Loading -> {
                    btnMarkAllRead.visibility = View.GONE
                }
                is NotifState.Success -> {
                    adapter.updateList(state.data)
                    updateMarkAllButtonVisibility()
                }
                is NotifState.Error -> {
                    btnMarkAllRead.visibility = View.GONE
                    ShowToast.showMessage(this, state.message)
                }
            }
        }
    }

    // 🌟 ADDED ONDESUME TO FIX BOTTOM NAV GLITCHES
    override fun onResume() {
        super.onResume()
        if (::bottomNav.isInitialized) {
            bottomNav.setOnItemSelectedListener(null)
            bottomNav.selectedItemId = R.id.nav_notifications
            setupBottomNav()
        }
    }

    private fun updateMarkAllButtonVisibility() {
        val hasUnread = adapter.getList().any { !it.isRead }
        btnMarkAllRead.visibility = if (hasUnread) View.VISIBLE else View.GONE
    }

    private fun setupBottomNav() {
        NotificationHelper.fetchUnreadCount(bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_class -> {
                    startActivity(Intent(this, TeacherClassPageActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish(); true
                }
                R.id.nav_participants -> {
                    startActivity(Intent(this, TeacherParticipantsActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish(); true
                }
                R.id.nav_notifications -> true
                R.id.nav_dll -> {
                    startActivity(Intent(this, DLLViewActivityMain::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish(); true
                }
                else -> false
            }
        }
    }

    private fun showSettingsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_teacher_notification_settings, null)
        val dialog = MaterialAlertDialogBuilder(this).setView(dialogView).setCancelable(true).create()

        val swArrival = dialogView.findViewById<androidx.appcompat.widget.AppCompatCheckBox>(R.id.switchClassSchedule)
        val swSubmission = dialogView.findViewById<androidx.appcompat.widget.AppCompatCheckBox>(R.id.switchStudentSubmission)

        val prefs = getSharedPreferences("NotificationPrefs", MODE_PRIVATE)
        swArrival.isChecked = prefs.getBoolean("arrival_enabled", true)
        swSubmission.isChecked = prefs.getBoolean("submission_enabled", true)

        val savePrefs = {
            prefs.edit().apply {
                putBoolean("arrival_enabled", swArrival.isChecked)
                putBoolean("submission_enabled", swSubmission.isChecked)
                apply()
            }
        }

        swArrival.setOnClickListener { savePrefs() }
        swSubmission.setOnClickListener { savePrefs() }

        dialogView.findViewById<ImageView>(R.id.btnBackSettings).setOnClickListener { dialog.dismiss() }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }
}