package com.example.gr8math // Make sure this matches your package name

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AssessmentResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assessment_result)

        // Find Views
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        val tvAssessmentNumber: TextView = findViewById(R.id.tvAssessmentNumber)
        val tvAssessmentTitle: TextView = findViewById(R.id.tvAssessmentTitle)
        val tvScore: TextView = findViewById(R.id.tvScore)
        val tvNumberOfItems: TextView = findViewById(R.id.tvNumberOfItems)
        val tvDate: TextView = findViewById(R.id.tvDate)

        // Get data from Intent
        val assessmentNumber = intent.getStringExtra("EXTRA_NUMBER") ?: "Assessment"
        val assessmentTitle = intent.getStringExtra("EXTRA_TITLE") ?: "Results"
        val score = intent.getIntExtra("EXTRA_SCORE", 0)
        val items = intent.getIntExtra("EXTRA_ITEMS", 0)

        // Get current date
        val sdf = SimpleDateFormat("MMM. d, yyyy", Locale.getDefault())
        val currentDate = sdf.format(Date())

        // Populate Views
        toolbar.title = assessmentTitle
        tvAssessmentNumber.text = assessmentNumber
        tvAssessmentTitle.text = assessmentTitle
        tvScore.text = getString(R.string.score, score)
        tvNumberOfItems.text = getString(R.string.number_of_items, items)
        tvDate.text = getString(R.string.date_accomplished, currentDate)

        // Toolbar Back Button (goes back to class page)
        toolbar.setNavigationOnClickListener {
            // Finish this activity and go back
            finish()
        }
    }
}