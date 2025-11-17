package com.example.gr8math // Make sure this matches your package name

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class WriteALessonActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_write_a_lesson)

        // --- Setup Toolbar ---
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            finish() // Go back
        }

        // --- Setup "Next" Button ---
        val btnNext = findViewById<Button>(R.id.btnNext)
        btnNext.setOnClickListener {
            // TODO: Add logic to validate fields and open the rich text editor page
            Toast.makeText(this, "Next button clicked!", Toast.LENGTH_SHORT).show()
        }
    }
}