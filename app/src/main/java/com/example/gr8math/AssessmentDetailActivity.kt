package com.example.gr8math // Make sure this matches your package name

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class AssessmentDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assessment_detail)

        // Find Views
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        val tvAssessmentNumber: TextView = findViewById(R.id.tvAssessmentNumber)
        val tvAssessmentTitle: TextView = findViewById(R.id.tvAssessmentTitle)
        val tvNumberOfItems: TextView = findViewById(R.id.tvNumberOfItems)
        val tvStartsAt: TextView = findViewById(R.id.tvStartsAt)
        val tvEndsAt: TextView = findViewById(R.id.tvEndsAt)
        val btnStartAssessment: Button = findViewById(R.id.btnStartAssessment)

        // --- TODO: Get real data from Intent ---
        // For now, we'll use placeholder data
        val assessmentNumber = "Assessment 2"
        val assessmentTitle = "Polynomial"
        val items = 10
        val starts = "1/1/2026 - 11:00 AM"
        val ends = "1/1/2026 - 12:00 PM"

        // Populate Views
        toolbar.title = assessmentNumber
        tvAssessmentNumber.text = assessmentNumber
        tvAssessmentTitle.text = assessmentTitle
        tvNumberOfItems.text = getString(R.string.number_of_items, items)
        tvStartsAt.text = getString(R.string.starts_at, starts)
        tvEndsAt.text = getString(R.string.ends_at, ends)

        // Toolbar Back Button
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // Start Assessment Button
        btnStartAssessment.setOnClickListener {
            // --- TODO: Pass real assessment data/ID ---
            val intent = Intent(this, TakeAssessmentActivity::class.java)
            startActivity(intent)
        }
    }
}