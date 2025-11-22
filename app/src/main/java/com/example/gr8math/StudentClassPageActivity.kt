package com.example.gr8math // Make sure this matches your package name

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class StudentClassPageActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classpage_student)

        // --- Setup Toolbar ---
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            finish() // Go back
        }

        // --- Setup Bottom Navigation ---
        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_class

        // --- Handle Bottom Navigation Item Clicks ---
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_class -> {
                    // Already on Class Page
                    true
                }
                R.id.nav_badges -> {
                    // TODO: Navigate to Badges Activity
                    Toast.makeText(this, "Badges clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_notifications -> {
                    // UPDATED: Navigate to Student Notifications Activity
                    startActivity(Intent(this, StudentNotificationsActivity::class.java))
                    false // Don't highlight this tab, we are moving away
                }
                R.id.nav_grades -> {
                    // TODO: Navigate to Grades Activity
                    Toast.makeText(this, "Grades clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }

        // --- Customize the Cards ---

        // 1. "Game" card
        val gameCard: View = findViewById(R.id.game_card)
        gameCard.findViewById<ImageView>(R.id.ivIcon).contentDescription = getString(R.string.play_a_game)
        gameCard.findViewById<TextView>(R.id.tvTitle).text = getString(R.string.play_a_game)

        // 2. "Assessment" card
        val assessmentCard: View = findViewById(R.id.assessment_card)
        val iconAssessment = assessmentCard.findViewById<ImageView>(R.id.ivIcon)
        val titleAssessment = assessmentCard.findViewById<TextView>(R.id.tvTitle)

        // Set the icon and text to Assessment
        iconAssessment.setImageResource(R.drawable.ic_assessment_green)
        iconAssessment.contentDescription = getString(R.string.assessment_placeholder)
        titleAssessment.text = getString(R.string.assessment_placeholder)

        // --- Set click listener for the assessment arrow ---
        val ivArrow = assessmentCard.findViewById<ImageView>(R.id.ivArrow)
        ivArrow.setOnClickListener {
            val intent = Intent(this, AssessmentDetailActivity::class.java)
            startActivity(intent)
        }

        // 3. "Lesson" card
        val lessonCard: View = findViewById(R.id.lesson_card)
        val tvSeeMore = lessonCard.findViewById<TextView>(R.id.tvSeeMore)

        // Set click listener for "See More"
        tvSeeMore.setOnClickListener {
            val tvWeek = lessonCard.findViewById<TextView>(R.id.tvWeek)
            val tvTitle = lessonCard.findViewById<TextView>(R.id.tvTitle)

            val week = tvWeek.text.toString()
            val title = tvTitle.text.toString()
            val fullDescription = getString(R.string.lesson_full_desc_placeholder)

            val intent = Intent(this, LessonDetailActivity::class.java).apply {
                putExtra("EXTRA_WEEK", week)
                putExtra("EXTRA_TITLE", title)
                putExtra("EXTRA_DESCRIPTION", fullDescription)
            }
            startActivity(intent)
        }
    }

    // --- Resets the navigation item to "Class" when returning to this page ---
    override fun onResume() {
        super.onResume()
        if (::bottomNav.isInitialized) {
            bottomNav.selectedItemId = R.id.nav_class
        }
    }
}