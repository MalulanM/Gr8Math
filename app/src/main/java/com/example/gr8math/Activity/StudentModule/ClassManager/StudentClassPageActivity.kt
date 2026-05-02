package com.example.gr8math.Activity.StudentModule.ClassManager

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.gr8math.Activity.GameActivity
import com.example.gr8math.Activity.StudentModule.Badges.StudentBadgesActivity
import com.example.gr8math.Activity.StudentModule.Grades.StudentGradesActivity
import com.example.gr8math.Activity.StudentModule.Assessment.AssessmentDetailActivity
import com.example.gr8math.Activity.StudentModule.Assessment.AssessmentResultActivity
import com.example.gr8math.Activity.StudentModule.Notification.StudentNotificationsActivity
import com.example.gr8math.Activity.StudentModule.Participants.StudentParticipantsActivity
import com.example.gr8math.Activity.TeacherModule.Lesson.LessonDetailActivity
import com.example.gr8math.Data.Model.ClassContentItem
import com.example.gr8math.Helper.NotificationMethods
import com.example.gr8math.Model.CurrentCourse
import com.example.gr8math.R
import com.example.gr8math.Utils.NetworkUtils
import com.example.gr8math.Utils.NotificationHelper
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.ViewModel.ContentState
import com.example.gr8math.ViewModel.StudentClassPageViewModel
import com.example.gr8math.ViewModel.StudentNavEvent
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nl.dionsegijn.konfetti.xml.KonfettiView

class StudentClassPageActivity : AppCompatActivity() {
    private var id: Int = 0
    private var courseId: Int = 0

    private val viewModel: StudentClassPageViewModel by viewModels()
    private lateinit var parentLayout: LinearLayout
    private lateinit var toolbar: MaterialToolbar

    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var konfettiView: KonfettiView
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classpage_student)

        initViews()
        handleIntentData()
        setupBottomNav()
        setupObservers()

        handleNotificationIntent(intent)

        val btnRefresh = findViewById<Button>(R.id.btnRefresh)
        btnRefresh.setOnClickListener {
            loadData()
        }

        loadData()
    }

    private fun loadData() {
        val noInternetView = findViewById<View>(R.id.no_internet_view)
        val scrollView = findViewById<View>(R.id.scrollView) // 🌟 Target the ScrollView instead

        // 1. Check for Internet
        if (!NetworkUtils.isConnected(this)) {
            // Show No Internet Screen, hide everything else
            noInternetView?.visibility = View.VISIBLE
            scrollView?.visibility = View.GONE
            emptyStateLayout.visibility = View.GONE
            return
        }

        // 2. HAS INTERNET: Hide error screen, show main view
        noInternetView?.visibility = View.GONE
        scrollView?.visibility = View.VISIBLE

        // 3. Fetch data
        viewModel.loadContent()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener(null)
        bottomNav.selectedItemId = R.id.nav_class
        NotificationHelper.fetchUnreadCount(bottomNav)
        setupBottomNavListeners(bottomNav)

        window.decorView.postDelayed({
            checkPendingGameBadges()
        }, 2000)
    }


    private fun setupBottomNav() {
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_class
        NotificationHelper.fetchUnreadCount(bottomNav)
        setupBottomNavListeners(bottomNav)
    }

    private fun setupBottomNavListeners(bottomNav: BottomNavigationView) {
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

    private fun initViews() {
        parentLayout = findViewById(R.id.parentLayout)
        toolbar = findViewById(R.id.toolbar)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        toolbar.setNavigationOnClickListener { finish() }

        findViewById<View>(R.id.btnParticipants).setOnClickListener {
            startActivity(Intent(this, StudentParticipantsActivity::class.java))
        }
        konfettiView = findViewById(R.id.konfettiView)
        val gameCard: View = findViewById(R.id.game_card)
        gameCard.findViewById<TextView>(R.id.tvTitle).text = getString(R.string.play_a_game)
        gameCard.setOnClickListener {
            // 1. Create the intent to open our Unity wrapper
            val intent = Intent(this, GameActivity::class.java)

            // 2. Pass the student's ID so the game knows where to save badges in Supabase
            intent.putExtra("student_id", CurrentCourse.userId)

            // 3. Launch the game!
            startActivity(intent)
        }

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
                // CASE: Push Notification (We only have the ID)
                toolbar.title = "..." // Temporary loading state
                viewModel.fetchSectionName(incomingCourseId)
            } else {
                // CASE: Normal navigation (Name passed from list)
                CurrentCourse.sectionName = incomingSectionName
                toolbar.title = incomingSectionName
            }
        }
        val toastMsg = intent.getStringExtra("toast_msg")
        if (!toastMsg.isNullOrEmpty()) ShowToast.showMessage(this, toastMsg)
    }

    private fun setupObservers() {
        viewModel.sectionName.observe(this) { name ->
            if (!name.isNullOrEmpty()) {
                toolbar.title = name
                CurrentCourse.sectionName = name
            }
        }

        viewModel.contentState.observe(this) { state ->
            when (state) {
                is ContentState.Loading -> {
                    emptyStateLayout.visibility = View.GONE
                }
                is ContentState.Success -> {
                    if (state.data.isEmpty()) {
                        emptyStateLayout.visibility = View.VISIBLE

                        if (parentLayout.childCount > 1) {
                            parentLayout.removeViews(1, parentLayout.childCount - 1)
                        }
                    } else {
                        emptyStateLayout.visibility = View.GONE
                        populateList(state.data)
                    }
                }
                is ContentState.Error -> {
                    ShowToast.showMessage(this, state.message)
                    emptyStateLayout.visibility = View.VISIBLE
                }
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

    private fun checkPendingGameBadges() {
        val file = java.io.File("/data/data/com.example.gr8math/files/pending_badges.txt")
        Log.d("StudentClassPageActivity", "Checking file: ${file.absolutePath}, exists: ${file.exists()}")

        if (file.exists()) {
            val badges = file.readText().trim().split("\n").filter { it.isNotEmpty() }
            Log.d("StudentClassPageActivity", "badges: $badges")
            file.delete()
            for (badgeName in badges) {
                showGameBadgeDialog(badgeName)
            }
        }
    }

    private fun showGameBadgeDialog(badgeName: String) {
        // Map the game badges to their images!
        // (Make sure these match the actual names of your drawables in res/drawable)
        val imageResource = when (badgeName) {
            "First Escape!" -> R.drawable.badge_firstescape
            "Perfect Escape!" -> R.drawable.badge_perfectescape
            "First Exploration..." -> R.drawable.badge_firstescape
            "Full Exploration..." -> R.drawable.badge_fullexplo
            else -> R.drawable.badge_firsttimer
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_badge_acquired, null)

        val ivBadge = dialogView.findViewById<ImageView>(R.id.ivDialogBadge)
        val tvBadgeTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnClose)

        ivBadge.setImageResource(imageResource)
        tvBadgeTitle.text = "$badgeName Badge!"

        mediaPlayer = MediaPlayer.create(this, R.raw.game_win)
        mediaPlayer?.start()

        val party = nl.dionsegijn.konfetti.core.Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def, 0x1E4B95),
            position = nl.dionsegijn.konfetti.core.Position.Relative(0.5, 0.3),
            emitter = nl.dionsegijn.konfetti.core.emitter.Emitter(duration = 100, java.util.concurrent.TimeUnit.MILLISECONDS).max(100)
        )
        konfettiView.start(party)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setOnDismissListener {
                mediaPlayer?.release()
                mediaPlayer = null
            }
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return

        val type = intent.getStringExtra("notif_type")
        val metaString = intent.getStringExtra("notif_meta")

        val directLessonId = intent.getIntExtra("lessonId", -1)
        val directAssessmentId = intent.getIntExtra("assessmentId", -1)

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
                        if (id > 0) viewModel.onAssessmentClicked(id)
                    }
                    "arrival" -> ShowToast.showMessage(this, "Welcome to class!")
                }
            } catch (e: Exception) { e.printStackTrace() }

            intent.removeExtra("notif_type")
            intent.removeExtra("notif_meta")
        } else if (directLessonId > 0) {
            openLessonDetail(directLessonId)
            intent.removeExtra("lessonId")
        } else if (directAssessmentId > 0) {
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
        if (parentLayout.childCount > 1) {
            parentLayout.removeViews(1, parentLayout.childCount - 1)
        }

        for (item in data) {
            when (item) {
                is ClassContentItem.LessonItem -> {
                    val view = layoutInflater.inflate(R.layout.item_class_lesson_card, parentLayout, false)
                    view.findViewById<TextView>(R.id.tvWeek).text = item.weekNumber.toString()
                    view.findViewById<TextView>(R.id.tvTitle).text = item.title
                    view.findViewById<TextView>(R.id.tvDescription).text = item.previewContent
                    view.findViewById<ImageButton>(R.id.ibEditLesson).visibility = View.GONE
                    view.findViewById<ImageButton>(R.id.ibDeleteLesson).visibility = View.GONE
                    view.findViewById<TextView>(R.id.tvSeeMore).setOnClickListener {
                        openLessonDetail(item.id)
                    }
                    parentLayout.addView(view)
                }
                is ClassContentItem.AssessmentItem -> {
                    val view = layoutInflater.inflate(R.layout.item_class_assessment_card, parentLayout, false)
                    view.findViewById<TextView>(R.id.tvTitle).text = "Assessment ${item.assessmentNumber}"

                    view.findViewById<ImageButton>(R.id.ibEditAssessment)?.visibility = View.GONE
                    view.findViewById<ImageButton>(R.id.ibDeleteAssessment)?.visibility = View.GONE

                    val ivArrow = view.findViewById<ImageView>(R.id.ivArrow)
                    ivArrow?.visibility = View.VISIBLE
                    ivArrow?.setOnClickListener {
                        viewModel.onAssessmentClicked(item.id)
                    }

                    view.setOnClickListener {
                        viewModel.onAssessmentClicked(item.id)
                    }
                    parentLayout.addView(view)
                }
            }
        }
    }
}