package com.example.gr8math // Make sure this matches your package name

import android.content.Intent // <-- IMPORT ADDED
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar // <-- IMPORT ADDED

class StudentClassManagerActivity : AppCompatActivity() {

    // Declare all the views
    private lateinit var mainToolbar: MaterialToolbar
    private lateinit var addClassesButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This line connects your layout file
        setContentView(R.layout.activity_class_manager_student)

        // Find all the views from the XML
        mainToolbar = findViewById(R.id.toolbar)
        addClassesButton = findViewById(R.id.btnAddClasses)

        // --- Set up Click Listeners ---

        // 1. Main toolbar's profile icon/back button
        mainToolbar.setNavigationOnClickListener {
            finish() // Closes the page
        }

        // 2. "Add Classes" button
        addClassesButton.setOnClickListener {
            // UPDATED: This now opens your new page!
            val intent = Intent(this@StudentClassManagerActivity, StudentAddClassActivity::class.java)
            startActivity(intent)
        }
    }
}