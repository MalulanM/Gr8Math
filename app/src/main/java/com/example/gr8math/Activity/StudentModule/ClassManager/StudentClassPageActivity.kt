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
    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classpage_student)

        initViews()
        handleIntentData()
        setupBottomNav()
        setupObservers()

        viewModel.loadContent()
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

        if (incomingCourseId != -1) {
            CurrentCourse.courseId = incomingCourseId
            CurrentCourse.currentRole = incomingRole ?: "student"


            if (incomingUserId != -1) CurrentCourse.userId = incomingUserId

            if (incomingSectionName.isNullOrEmpty()) {
                viewModel.fetchSectionName(incomingCourseId)
            } else {
                CurrentCourse.sectionName = incomingSectionName
                toolbar.title = incomingSectionName
            }
        }
        val toastMsg = intent.getStringExtra("toast_msg")
        if (!toastMsg.isNullOrEmpty()) ShowToast.showMessage(this, toastMsg)
    }

    private fun initViews() {
        parentLayout = findViewById(R.id.parentLayout)
        toolbar = findViewById(R.id.toolbar)
        toolbar.title = CurrentCourse.sectionName
        toolbar.setNavigationOnClickListener { finish() }

        findViewById<View>(R.id.btnParticipants).setOnClickListener {
            startActivity(Intent(this, StudentParticipantsActivity::class.java))
        }

        val gameCard: View = findViewById(R.id.game_card)
        gameCard.findViewById<TextView>(R.id.tvTitle).text = getString(R.string.play_a_game)
        gameCard.setOnClickListener { /* Intent to Game Activity */ }
    }

    private fun setupObservers() {
        viewModel.sectionName.observe(this) { name ->
            toolbar.title = name
            CurrentCourse.sectionName = name
        }

        viewModel.contentState.observe(this) { state ->
            when (state) {
                is ContentState.Loading -> parentLayout.removeAllViews()
                is ContentState.Success -> populateList(state.data)
                is ContentState.Error -> ShowToast.showMessage(this, state.message)
            }
        }

        viewModel.navEvent.observe(this) { event ->
            event?.let {
                val intent = when (it) {
                    is StudentNavEvent.ToLesson -> {
                        Intent(this, LessonDetailActivity::class.java).apply { putExtra("lesson_id", it.id) }
                    }
                    is StudentNavEvent.ToAssessmentDetail -> {
                        Intent(this, AssessmentDetailActivity::class.java).apply { putExtra("assessment_id", it.id) }
                    }
                    is StudentNavEvent.ToAssessmentResult -> {
                        Intent(this, AssessmentResultActivity::class.java).apply { putExtra("assessment_id", it.id) }
                    }
                }
                startActivity(intent)
                viewModel.clearNavEvent()
            }
        }
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return

        val type = intent.getStringExtra("notif_type")
        val metaString = intent.getStringExtra("notif_meta")

        val directLessonId = intent.getIntExtra("lessonId", -1)
        val directAssessmentId = intent.getIntExtra("assessmentId", -1)

        // --- CASE A: Handle Push Notif ---
        if (type != null && !metaString.isNullOrEmpty()) {
            try {
                val metaJson = org.json.JSONObject(metaString)
                when (type) {
                    "lesson" -> {
                        val id = metaJson.optInt("lesson_id", -1)
                        if (id > 0) openLessonDetail(id)
                    }
                    "assessment" -> {
                        val id = metaJson.optInt("assessment_id", -1)
                        if (id > 0) {
                            // 🌟 RESTORED: Uses your original secure checker!
                            viewModel.onAssessmentClicked(id)
                        }
                    }
                    "arrival" -> ShowToast.showMessage(this, "Welcome to class!")
                }
            } catch (e: Exception) { e.printStackTrace() }

            intent.removeExtra("notif_type")
            intent.removeExtra("notif_meta")
        }
        // --- CASE B: Handle Internal Notification Page Click ---
        else if (directLessonId > 0) {
            openLessonDetail(directLessonId)
            intent.removeExtra("lessonId")
        }
        else if (directAssessmentId > 0) {
            // 🌟 RESTORED: Uses your original secure checker!
            viewModel.onAssessmentClicked(directAssessmentId)
            intent.removeExtra("assessmentId")
        }
    }

    private fun openLessonDetail(lessonId: Int) {
        val nextIntent = Intent(this, LessonDetailActivity::class.java)
        nextIntent.putExtra("lesson_id", lessonId)
        startActivity(nextIntent)
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
                        openLessonDetail(item.id)
                    }
                    parentLayout.addView(view)
                }
                is ClassContentItem.AssessmentItem -> {
                    val view = layoutInflater.inflate(R.layout.item_class_assessment_card, parentLayout, false)
                    view.findViewById<TextView>(R.id.tvTitle).text = "Assessment ${item.assessmentNumber}"
                    view.findViewById<ImageView>(R.id.ivArrow).setOnClickListener {
                        // 🌟 Same secure checker here
                        viewModel.onAssessmentClicked(item.id)
                    }
                    parentLayout.addView(view)
                }
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