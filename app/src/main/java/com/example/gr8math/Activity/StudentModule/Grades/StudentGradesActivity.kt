package com.example.gr8math.Activity.StudentModule.Grades

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gr8math.Activity.StudentModule.Badges.StudentBadgesActivity
import com.example.gr8math.Activity.StudentModule.ClassManager.StudentClassPageActivity
import com.example.gr8math.Activity.StudentModule.Notification.StudentNotificationsActivity
import com.example.gr8math.Adapter.StudentGradesAdapter
import com.example.gr8math.Data.Model.StudentScore
import com.example.gr8math.Model.CurrentCourse // Ensure this is imported
// import com.example.gr8math.Model.CurrentUser // Import wherever your logged-in student session is stored
import com.example.gr8math.R
import com.example.gr8math.Utils.NetworkUtils
import com.example.gr8math.Utils.NotificationHelper
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.ViewModel.GradesState
import com.example.gr8math.ViewModel.StudentGradesViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class StudentGradesActivity : AppCompatActivity() {

    private val viewModel: StudentGradesViewModel by viewModels()

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var rvGrades: RecyclerView
    private lateinit var adapter: StudentGradesAdapter
    private var currentStudentId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_grades)

        // 1. Setup UI
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val btnQuarterlyReport = findViewById<Button>(R.id.btnQuarterlyReport)
        btnQuarterlyReport.visibility = View.VISIBLE
        btnQuarterlyReport.setOnClickListener {
            // Prevent crash if they click before data loads
            if (currentStudentId == 0) {
                ShowToast.showMessage(this, "Please wait for grades to load first.")
                return@setOnClickListener
            }

            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH) + 1
            val currentYear = calendar.get(Calendar.YEAR)

            val intent = Intent(this, MonthlyReportActivity::class.java).apply {
                putExtra("EXTRA_IS_STUDENT", true)
                putExtra("EXTRA_MONTH", currentMonth)
                putExtra("EXTRA_YEAR", currentYear)
                putExtra("EXTRA_COURSE_ID", CurrentCourse.courseId)

                // Pass the newly retrieved ID!
                putExtra("EXTRA_STUDENT_ID", currentStudentId)
                putExtra("EXTRA_STUDENT_NAME", "My Report") // Display name on PDF
            }
            startActivity(intent)
        }

        // 2. Setup Navigation
        bottomNav = findViewById(R.id.bottom_navigation)
        setupBottomNav()

        // 3. Setup RecyclerView
        rvGrades = findViewById(R.id.rvGrades)
        rvGrades.layoutManager = LinearLayoutManager(this)

        // Initialize adapter with empty list
        adapter = StudentGradesAdapter(emptyList()) { scoreItem ->
            showAssessmentDetailsDialog(scoreItem)
        }
        rvGrades.adapter = adapter

        // 4. Observe & Load
        setupObservers()

        val btnRefresh = findViewById<Button>(R.id.btnRefresh)
        btnRefresh?.setOnClickListener {
            loadData()
        }

        loadData()
    }

    private fun loadData() {
        val noInternetView = findViewById<View>(R.id.no_internet_view)
        val rvGrades = findViewById<View>(R.id.rvGrades)
        val emptyStateLayout = findViewById<View>(R.id.emptyStateLayout)

        // 1. Check for Internet
        if (!NetworkUtils.isConnected(this)) {
            // Show No Internet Screen, hide the lists
            noInternetView?.visibility = View.VISIBLE
            rvGrades?.visibility = View.GONE
            emptyStateLayout?.visibility = View.GONE
            return
        }

        // 2. HAS INTERNET: Hide error screen
        noInternetView?.visibility = View.GONE

        // 3. Fetch your actual data
        viewModel.loadGrades()
    }

    override fun onResume() {
        super.onResume()
        if (::bottomNav.isInitialized) {
            bottomNav.setOnItemSelectedListener(null)
            bottomNav.selectedItemId = R.id.nav_grades
            NotificationHelper.fetchUnreadCount(bottomNav)
            setupBottomNavListeners(bottomNav)
        }
    }

    private fun setupObservers() {
        viewModel.state.observe(this) { state ->
            val rv = findViewById<RecyclerView>(R.id.rvGrades)
            val emptyLayout = findViewById<View>(R.id.emptyStateLayout)

            when (state) {
                is GradesState.Loading -> {

                }
                is GradesState.Success -> {
                    // Capture the ID from the view model
                    currentStudentId = state.studentId

                    if (state.data.isEmpty()) {
                        rv.visibility = View.GONE
                        emptyLayout.visibility = View.VISIBLE
                    } else {
                        rv.visibility = View.VISIBLE
                        emptyLayout.visibility = View.GONE
                        adapter.updateList(state.data)
                    }
                }
                is GradesState.Error -> {
                    ShowToast.showMessage(this, state.message)
                }
            }
        }
    }

    private fun showAssessmentDetailsDialog(scoreItem: StudentScore) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_assessment_details, null)
        val dialog = MaterialAlertDialogBuilder(this).setView(dialogView).create()

        dialogView.findViewById<TextView>(R.id.tvDetailNumber).text = "${scoreItem.assessmentNumber}"
        dialogView.findViewById<TextView>(R.id.tvDetailTitle).text = scoreItem.title

        // Format Score (Double to String, removing unnecessary decimals)
        val df = DecimalFormat("#.##")
        dialogView.findViewById<TextView>(R.id.tvDetailScore).text = df.format(scoreItem.score)

        dialogView.findViewById<TextView>(R.id.tvDetailItems).text = "${scoreItem.assessmentItems}"

        val percentage = if (scoreItem.totalPoints > 0) {
            (scoreItem.score / scoreItem.totalPoints) * 100
        } else 0.0
        dialogView.findViewById<TextView>(R.id.tvDetailPercentage).text = "${percentage.toInt()}%"
        // Format Dates
        dialogView.findViewById<TextView>(R.id.tvDetailDate).text = formatDate(scoreItem.dateAccomplished)
        dialogView.findViewById<TextView>(R.id.tvDetailTime).text = formatTime(scoreItem.dateAccomplished)

        dialogView.findViewById<View>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    // --- Date Formatting Helpers (Safe for ISO strings) ---
    private fun formatDate(timestamp: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(timestamp) ?: return timestamp
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
        } catch (e: Exception) { timestamp }
    }

    private fun formatTime(timestamp: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(timestamp) ?: return timestamp
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
        } catch (e: Exception) { timestamp }
    }

    // --- Navigation Logic ---
    private fun setupBottomNav() {
        bottomNav.selectedItemId = R.id.nav_grades
        NotificationHelper.fetchUnreadCount(bottomNav)
        setupBottomNavListeners(bottomNav)
    }

    // Extracted so we can safely reuse it in onResume
    private fun setupBottomNavListeners(navView: BottomNavigationView) {
        navView.setOnItemSelectedListener { item ->
            if (item.itemId == navView.selectedItemId) return@setOnItemSelectedListener true

            val intent = when (item.itemId) {
                R.id.nav_class -> Intent(this, StudentClassPageActivity::class.java)
                R.id.nav_badges -> Intent(this, StudentBadgesActivity::class.java)
                R.id.nav_notifications -> Intent(this, StudentNotificationsActivity::class.java)
                R.id.nav_grades -> null
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