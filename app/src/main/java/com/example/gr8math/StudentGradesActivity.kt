package com.example.gr8math

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gr8math.adapter.GradeItem
import com.example.gr8math.adapter.StudentGradesAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class StudentGradesActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_grades)

        // --- Toolbar ---
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            finish() // Go back
        }

        // --- Bottom Navigation ---
        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_grades // Highlight "Grades"

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_class -> {
                    finish() // Go back to Class Page
                    true
                }
                R.id.nav_badges -> {
                    Toast.makeText(this, "Badges clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_notifications -> {
                    startActivity(Intent(this, StudentNotificationsActivity::class.java))
                    false
                }
                R.id.nav_grades -> true // Already here
                else -> false
            }
        }

        // --- Report Button ---
        findViewById<Button>(R.id.btnQuarterlyReport).setOnClickListener {
            // Reuse the existing Report Activity
            startActivity(Intent(this, QuarterlyReportActivity::class.java))
        }

        // --- RecyclerView (List of Grades) ---
        val gradesList = listOf(
            GradeItem("Assessment 1", 10),
            GradeItem("Assessment 2", 8)
        )

        val rvGrades = findViewById<RecyclerView>(R.id.rvGrades)
        rvGrades.layoutManager = LinearLayoutManager(this)
        rvGrades.adapter = StudentGradesAdapter(gradesList) { gradeItem ->
            showAssessmentDetailsDialog(gradeItem)
        }
    }

    private fun showAssessmentDetailsDialog(gradeItem: GradeItem) {
        // Reuse the existing dialog layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_assessment_details, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        // Populate Data
        dialogView.findViewById<TextView>(R.id.tvDetailTitle).text = gradeItem.title
        dialogView.findViewById<TextView>(R.id.tvDetailScore).text = gradeItem.score.toString()
        // Set other fields (Date, Time, Items) here...

        dialogView.findViewById<View>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        if (::bottomNav.isInitialized) {
            bottomNav.selectedItemId = R.id.nav_grades
        }
    }
}