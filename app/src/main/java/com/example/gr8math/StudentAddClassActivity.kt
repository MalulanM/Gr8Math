package com.example.gr8math // Make sure this matches your package name

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class StudentAddClassActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_add_class)

        // Find views
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        val joinButton: Button = findViewById(R.id.btnJoinClass)

        // 1. Make the toolbar's back button work
        toolbar.setNavigationOnClickListener {
            finish() // Closes this page and goes back
        }

        // 2. Set click listener for the "Join Class" button
        joinButton.setOnClickListener {
            // For now, just show a simple "Joined!" message
            Toast.makeText(this, "Join Class Clicked!", Toast.LENGTH_SHORT).show()
            // Later, you will add your API call here
        }
    }
}