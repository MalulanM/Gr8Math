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
import com.example.gr8math.R
import com.example.gr8math.Utils.NotificationHelper
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.ViewModel.GradesState
import com.example.gr8math.ViewModel.StudentGradesViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class StudentGradesActivity : AppCompatActivity() {

    private val viewModel: StudentGradesViewModel by viewModels()

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var rvGrades: RecyclerView
    private lateinit var adapter: StudentGradesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_grades)

        // 1. Setup UI
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // Hide report button (as per your original code)
        findViewById<Button>(R.id.btnQuarterlyReport).visibility = View.GONE

        // 2. Setup Navigation
        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_grades
        NotificationHelper.fetchUnreadCount(bottomNav)
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
        viewModel.loadGrades()
    }

    private fun setupObservers() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is GradesState.Loading -> {
                    // Optional: Show loading bar if you have one in XML
                }
                is GradesState.Success -> {
                    if (state.data.isEmpty()) {
                        ShowToast.showMessage(this, "No assessments found.")
                    }
                    adapter.updateList(state.data)
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

        // Calculate Percentage
        val percentage = if (scoreItem.assessmentItems > 0) {
            (scoreItem.score / scoreItem.assessmentItems) * 100
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
        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == bottomNav.selectedItemId) return@setOnItemSelectedListener true

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

    override fun onResume() {
        super.onResume()
        if (::bottomNav.isInitialized) {
            bottomNav.selectedItemId = R.id.nav_grades
        }
    }
}