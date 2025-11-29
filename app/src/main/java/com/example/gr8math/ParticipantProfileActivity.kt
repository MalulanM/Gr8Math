package com.example.gr8math

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class ParticipantProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_participant_profile)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // 1. Get Data from Intent
        val name = intent.getStringExtra("EXTRA_NAME") ?: "Dela Cruz, Juan"
        val role = intent.getStringExtra("EXTRA_ROLE") ?: "Student" // Default to Student

        // 2. Find Views
        val tvName = findViewById<TextView>(R.id.tvName)
        val tvRole = findViewById<TextView>(R.id.tvRole)
        val tvLRN = findViewById<TextView>(R.id.tvLRN) // Reused for Teaching Position
        val tvBadgesHeader = findViewById<TextView>(R.id.tvBadgesHeader) // Reused for Achievements

        // 3. Set Data
        tvName.text = name
        tvRole.text = "Role: $role"

        // 4. Role-Specific Logic
        if (role == "Teacher") {
            // --- TEACHER VIEW ---
            tvLRN.text = "Teaching Position: Teacher I"
            tvBadgesHeader.text = "Teaching Achievements"
        } else {
            // --- STUDENT VIEW ---
            tvLRN.text = "LRN: 123456123456"
            tvBadgesHeader.text = "Badges"
        }
    }
}