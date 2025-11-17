package com.example.gr8math // Make sure this matches your package name

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class LessonDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson_detail)

        // --- Find Views ---
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        val tvWeek: TextView = findViewById(R.id.tvWeek)
        val tvTitle: TextView = findViewById(R.id.tvTitle)
        val tvDescription: TextView = findViewById(R.id.tvDescription)

        // --- Get Data from Intent ---
        val week = intent.getStringExtra("EXTRA_WEEK")
        val title = intent.getStringExtra("EXTRA_TITLE")
        val description = intent.getStringExtra("EXTRA_DESCRIPTION")

        // --- Populate Views ---
        toolbar.title = title // Set the toolbar title
        tvWeek.text = week
        tvTitle.text = title
        tvDescription.text = description

        // --- Setup Toolbar Back Button ---
        toolbar.setNavigationOnClickListener {
            finish() // Go back
        }
    }
}