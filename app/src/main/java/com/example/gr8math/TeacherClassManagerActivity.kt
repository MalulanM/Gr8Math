package com.example.gr8math // Make sure this matches your package name

import android.content.Intent // <-- NEW IMPORT
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputLayout
import com.example.gr8math.TeacherAddClassActivity // <-- NEW IMPORT

class TeacherClassManagerActivity : AppCompatActivity() {

    // Declare all the views
    private lateinit var defaultView: ConstraintLayout
    private lateinit var searchView: ConstraintLayout
    private lateinit var mainToolbar: MaterialToolbar
    private lateinit var tilSearch: TextInputLayout
    private lateinit var searchIcon: ImageView
    private lateinit var addClassesButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make sure this file name matches your layout XML file
        // It should be "activity_teacher_class_manager"
        setContentView(R.layout.activity_class_manager_teacher)

        // Find all the views from the XML
        defaultView = findViewById(R.id.default_view)
        searchView = findViewById(R.id.search_view)
        mainToolbar = findViewById(R.id.toolbar_main)
        tilSearch = findViewById(R.id.tilSearch)
        searchIcon = findViewById(R.id.iv_search)
        addClassesButton = findViewById(R.id.btnAddClasses)

        // --- Set up Click Listeners ---

        // 1. Main toolbar's profile icon
        mainToolbar.setNavigationOnClickListener {
            finish()
        }

        // 2. "Add Classes" button
        addClassesButton.setOnClickListener {
            // UPDATED: This now opens your new TeacherAddClassActivity page!
            val intent = Intent(this@TeacherClassManagerActivity, TeacherAddClassActivity::class.java)
            startActivity(intent)
        }

        // 3. Search Icon in the main toolbar (This is correct)
        searchIcon.setOnClickListener {
            // Hide the default view and show the search view
            defaultView.visibility = View.GONE
            searchView.visibility = View.VISIBLE
        }

        // 4. Back Arrow in the NEW search bar
        tilSearch.setStartIconOnClickListener {
            // Hide the search view and show the default view
            searchView.visibility = View.GONE
            defaultView.visibility = View.VISIBLE
        }
    }

    // This handles the Android system back button (at the bottom of the phone)
    override fun onBackPressed() {
        // If the search view is visible, close it.
        if (searchView.visibility == View.VISIBLE) {
            searchView.visibility = View.GONE
            defaultView.visibility = View.VISIBLE
        } else {
            // Otherwise, close the page as normal.
            super.onBackPressed()
        }
    }
}