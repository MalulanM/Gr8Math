package com.example.gr8math.Activity.StudentModule.ClassManager

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.gr8math.Activity.StudentModule.Badges.StudentBadgesActivity
import com.example.gr8math.Activity.StudentModule.Grades.StudentGradesActivity
import com.example.gr8math.Activity.StudentModule.Assessment.AssessmentDetailActivity
import com.example.gr8math.Activity.StudentModule.Assessment.AssessmentResultActivity
import com.example.gr8math.Activity.StudentModule.Notification.StudentNotificationsActivity
import com.example.gr8math.Activity.StudentModule.Participants.StudentParticipantsActivity
import com.example.gr8math.Activity.TeacherModule.Lesson.LessonDetailActivity
import com.example.gr8math.Data.Model.ClassContentItem
import com.example.gr8math.Model.CurrentCourse
import com.example.gr8math.R
import com.example.gr8math.Utils.NotificationHelper
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.ViewModel.ContentState
import com.example.gr8math.ViewModel.StudentClassPageViewModel
import com.example.gr8math.ViewModel.StudentNavEvent
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class StudentClassPageActivity : AppCompatActivity() {
    private var id: Int = 0
    private var courseId: Int = 0

    private val viewModel: StudentClassPageViewModel by viewModels()
    private lateinit var parentLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classpage_student)

        handleIntentData()
        initViews()
        setupBottomNav()
        setupObservers()

        // Load Data
        viewModel.loadContent()

        // Handle Deep Link
        handleNotificationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleIntentData() {
        val incomingCourseId = intent.getIntExtra("courseId", -1)
        val incomingSectionName = intent.getStringExtra("sectionName")
        val incomingRole = intent.getStringExtra("role")
        val incomingUserId = intent.getIntExtra("id", -1)

        if (incomingCourseId != -1 && incomingCourseId != CurrentCourse.courseId) {
            CurrentCourse.courseId = incomingCourseId
            CurrentCourse.sectionName = incomingSectionName ?: ""
            CurrentCourse.currentRole = incomingRole ?: ""
            CurrentCourse.userId = incomingUserId
        }

        val fromNotification = intent.getBooleanExtra("fromNotification", false)

        if (fromNotification || (incomingCourseId != -1 && incomingCourseId != CurrentCourse.courseId)) {
            CurrentCourse.courseId = incomingCourseId
            CurrentCourse.sectionName = incomingSectionName ?: ""
            CurrentCourse.currentRole = incomingRole ?: ""

            // CRITICAL: Ensure ID is updated
            if (incomingUserId != -1) {
                CurrentCourse.userId = incomingUserId
            }
        }

        val toastMsg = intent.getStringExtra("toast_msg")
        if (!toastMsg.isNullOrEmpty()) ShowToast.showMessage(this, toastMsg)
    }

    private fun initViews() {
        parentLayout = findViewById(R.id.parentLayout)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.title = CurrentCourse.sectionName
        toolbar.setNavigationOnClickListener { finish() }

        findViewById<View>(R.id.btnParticipants).setOnClickListener {
            startActivity(Intent(this, StudentParticipantsActivity::class.java))
        }

        // Game Card Setup
        val gameCard: View = findViewById(R.id.game_card)
        gameCard.findViewById<TextView>(R.id.tvTitle).text = getString(R.string.play_a_game)
        gameCard.setOnClickListener {
            // Intent to Game Activity
        }
    }

    private fun setupObservers() {
        // 1. Content Data Observer
        viewModel.contentState.observe(this) { state ->
            when (state) {
                is ContentState.Loading -> {
                    parentLayout.removeAllViews()
                    // Show Loading Spinner if you have one
                }
                is ContentState.Success -> {
                    populateList(state.data)
                }
                is ContentState.Error -> {
                    ShowToast.showMessage(this, state.message)
                }
            }
        }

        // 2. Navigation Observer (Assessment Clicks)
        viewModel.navEvent.observe(this) { event ->
            event?.let {
                val intent = when (it) {
                    is StudentNavEvent.ToLesson -> {
                        Intent(this, LessonDetailActivity::class.java).apply {
                            putExtra("lesson_id", it.id)
                        }
                    }
                    is StudentNavEvent.ToAssessmentDetail -> {
                        Intent(this, AssessmentDetailActivity::class.java).apply {
                            putExtra("assessment_id", it.id)
                        }
                    }
                    is StudentNavEvent.ToAssessmentResult -> {
                        Intent(this, AssessmentResultActivity::class.java).apply {
                            putExtra("assessment_id", it.id)
                        }
                    }
                }
                startActivity(intent)
                viewModel.clearNavEvent()
            }
        }
    }

    private fun populateList(data: List<ClassContentItem>) {
        parentLayout.removeAllViews()

        for (item in data) {
            when (item) {
                is ClassContentItem.LessonItem -> {
                    val view = layoutInflater.inflate(R.layout.item_class_lesson_card, parentLayout, false)

                    view.findViewById<TextView>(R.id.tvWeek).text = item.weekNumber.toString()
                    view.findViewById<TextView>(R.id.tvTitle).text = item.title
                    view.findViewById<TextView>(R.id.tvDescription).text = item.previewContent
                    view.findViewById<ImageButton>(R.id.ibEditLesson).visibility = View.GONE

                    view.findViewById<TextView>(R.id.tvSeeMore).setOnClickListener {
                        val intent = Intent(this, LessonDetailActivity::class.java)
                        intent.putExtra("lesson_id", item.id)
                        startActivity(intent)
                    }
                    parentLayout.addView(view)
                }
                is ClassContentItem.AssessmentItem -> {
                    val view = layoutInflater.inflate(R.layout.item_class_assessment_card, parentLayout, false)

                    view.findViewById<TextView>(R.id.tvTitle).text = "Assessment ${item.assessmentNumber}"

                    view.findViewById<ImageView>(R.id.ivArrow).setOnClickListener {
                        // Check Status via ViewModel
                        viewModel.onAssessmentClicked(item.id)
                    }
                    parentLayout.addView(view)
                }
            }
        }
    }
    private fun handleNotificationIntent(intent: Intent?) {
        intent?.let {
            val lessonId = it.getIntExtra("lessonId", -1)
            val assessmentId = it.getIntExtra("assessmentId", -1)
            val studentId = it.getIntExtra("studentId", -1)

            // DEBUG LOGS (Place these at the very top to verify data)
            android.util.Log.d("DEBUG_NOTIF", "Processing Intent - Lesson: $lessonId, Assessment: $assessmentId, Student: $studentId")

            // FIX: Check if ID is greater than 0 (Valid DB ID)
            if (lessonId > 0) {
                android.util.Log.d("DEBUG_NOTIF", "Navigating to Lesson Detail")
                val nextIntent = Intent(this, LessonDetailActivity::class.java)
                nextIntent.putExtra("lesson_id", lessonId)
                startActivity(nextIntent)
            }
            else if (assessmentId > 0) {
                android.util.Log.d("DEBUG_NOTIF", "Navigating to Assessment Logic")

                // Pass the studentId to the ViewModel
                viewModel.onAssessmentClicked(assessmentId, studentId)
            }
        }
    }
    private fun setupBottomNav() {
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_class
        NotificationHelper.fetchUnreadCount(bottomNav)

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == bottomNav.selectedItemId) return@setOnItemSelectedListener true

            val intent = when (item.itemId) {
                R.id.nav_class -> null
                R.id.nav_badges -> Intent(this, StudentBadgesActivity::class.java)
                R.id.nav_notifications -> Intent(this, StudentNotificationsActivity::class.java)
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
}