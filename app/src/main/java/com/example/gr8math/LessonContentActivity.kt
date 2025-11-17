package com.example.gr8math // Make sure this matches your package name

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LessonContentActivity : AppCompatActivity() {

    private lateinit var etLessonContent: EditText
    private var weekNumber: String? = null
    private var lessonTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson_content)

        // Get data from the intent
        weekNumber = intent.getStringExtra("EXTRA_WEEK_NUMBER")
        lessonTitle = intent.getStringExtra("EXTRA_LESSON_TITLE")
        // --- NEW: Get the existing content (if any) ---
        val existingContent = intent.getStringExtra("EXTRA_LESSON_CONTENT")

        // Find views
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        val btnAddMedia: MaterialButton = findViewById(R.id.btnAddMedia)
        val btnSave: MaterialButton = findViewById(R.id.btnSave)
        etLessonContent = findViewById(R.id.etLessonContent)

        // --- NEW: Pre-fill the content if we are editing ---
        if (existingContent != null) {
            etLessonContent.setText(existingContent)
        }
        // --- END OF NEW CODE ---

        // --- Setup Click Listeners ---
        btnSave.setOnClickListener {
            showSaveConfirmationDialog()
        }
        btnAddMedia.setOnClickListener {
            Toast.makeText(this, "Add Media clicked!", Toast.LENGTH_SHORT).show()
        }
        toolbar.setNavigationOnClickListener {
            checkUnsavedContentAndGoBack()
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                checkUnsavedContentAndGoBack()
            }
        })
    }

    /**
     * Checks if there is text. If yes, shows the "Discard" dialog.
     * If no, just goes back to the previous dialog (Step 1).
     */
    private fun checkUnsavedContentAndGoBack() {
        // We will now pass the content back too, in case the user
        // edited it and wants to go back to the dialog
        val content = etLessonContent.text.toString().trim()

        if (content.isNotEmpty()) {
            showDiscardChangesDialog()
        } else {
            // No content typed, just go back to Step 1
            goBackToStep1()
        }
    }

    /**
     * This is a NEW dialog to warn the user they will lose their lesson content.
     */
    private fun showDiscardChangesDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.discard_title)
            .setMessage(R.string.discard_message)
            .setNegativeButton(R.string.discard_action) { _, _ ->
                // User clicked "Discard", go back to Step 1
                goBackToStep1()
            }
            .setPositiveButton(R.string.cancel_action) { dialog, _ ->
                // User clicked "Cancel", stay on the page
                dialog.dismiss()
            }
            .show()
    }

    /**
     * This function bundles the data and sends it back to the launcher.
     */
    private fun goBackToStep1() {
        val resultIntent = Intent()
        resultIntent.putExtra("EXTRA_WEEK_NUMBER", weekNumber)
        resultIntent.putExtra("EXTRA_LESSON_TITLE", lessonTitle)

        // Set the result to "OK" and attach the intent
        setResult(Activity.RESULT_OK, resultIntent)

        // Now, finish the activity
        finish()
    }

    // --- (showSaveConfirmationDialog is unchanged) ---
    private fun showSaveConfirmationDialog() {
        val customMessage = TextView(this).apply {
            text = getString(R.string.dialog_save_message)
            setTextColor(ContextCompat.getColor(this@LessonContentActivity, R.color.colorText))
            textSize = 18f
            setPadding(60, 50, 60, 30)
            try {
                val typeface = ResourcesCompat.getFont(this@LessonContentActivity, R.font.lexend)
                this.typeface = typeface
            } catch (e: Exception) {
                // Font not found
            }
        }
        MaterialAlertDialogBuilder(this)
            .setCustomTitle(customMessage)
            .setNegativeButton(R.string.yes) { _, _ ->
                saveLesson() // Save and exit
            }
            .setPositiveButton(R.string.no) { dialog, _ ->
                dialog.dismiss() // Stay on the page
            }
            .show()
    }

    // --- (saveLesson is unchanged) ---
    private fun saveLesson() {
        val content = etLessonContent.text.toString().trim()
        if (content.isEmpty()) {
            Toast.makeText(this, "Cannot save an empty lesson", Toast.LENGTH_SHORT).show()
            return
        }
        // TODO: Add your database saving/updating logic here
        Toast.makeText(this, "Lesson Saved!", Toast.LENGTH_SHORT).show()

        // This simple finish() will NOT send RESULT_OK, so the launcher
        // will not run, and the user will go back to the class page.
        finish()
    }
}