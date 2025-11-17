package com.example.gr8math // Make sure this matches your package name

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class StudentClassPageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classpage_student)

        // --- Setup Toolbar ---
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            finish() // Go back
        }

        // --- Setup Bottom Navigation ---
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)
        // Set "Class" as the selected item
        bottomNav.selectedItemId = R.id.nav_class

        // --- Customize the Cards ---

        // 1. "Game" card
        val gameCard: View = findViewById(R.id.game_card)
        gameCard.findViewById<ImageView>(R.id.ivIcon).contentDescription = getString(R.string.play_a_game)
        gameCard.findViewById<TextView>(R.id.tvTitle).text = getString(R.string.play_a_game)

        // 2. "Assessment" card
        val assessmentCard: View = findViewById(R.id.assessment_card) // Find by ID
        val iconAssessment = assessmentCard.findViewById<ImageView>(R.id.ivIcon)
        val titleAssessment = assessmentCard.findViewById<TextView>(R.id.tvTitle)

        // Set the icon and text to Assessment
        iconAssessment.setImageResource(R.drawable.ic_assessment_green)
        iconAssessment.contentDescription = getString(R.string.assessment_placeholder)
        titleAssessment.text = getString(R.string.assessment_placeholder)

        // --- NEW CODE: Set click listener for the assessment arrow ---
        val ivArrow = assessmentCard.findViewById<ImageView>(R.id.ivArrow)
        ivArrow.setOnClickListener {
            // --- TODO: Pass real assessment ID/data ---
            val intent = Intent(this, AssessmentDetailActivity::class.java)
            startActivity(intent)
        }
        // --- END OF NEW CODE ---

        // 3. "Lesson" card
        val lessonCard: View = findViewById(R.id.lesson_card)
        val tvSeeMore = lessonCard.findViewById<TextView>(R.id.tvSeeMore)

        // Set click listener for "See More"
        tvSeeMore.setOnClickListener {
            // Find the other views in the card to get their text
            val tvWeek = lessonCard.findViewById<TextView>(R.id.tvWeek)
            val tvTitle = lessonCard.findViewById<TextView>(R.id.tvTitle)

            // Get the text values
            val week = tvWeek.text.toString()
            val title = tvTitle.text.toString()
            // Get the FULL description from strings.xml
            val fullDescription = getString(R.string.lesson_full_desc_placeholder)

            // Create the intent to open the detail page
            val intent = Intent(this, LessonDetailActivity::class.java).apply {
                putExtra("EXTRA_WEEK", week)
                putExtra("EXTRA_TITLE", title)
                putExtra("EXTRA_DESCRIPTION", fullDescription)
            }
            startActivity(intent)
        }
    }
}