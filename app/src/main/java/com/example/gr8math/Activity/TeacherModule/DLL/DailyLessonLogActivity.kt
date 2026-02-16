package com.example.gr8math.Activity.TeacherModule.DLL

import android.app.AlertDialog
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.example.gr8math.R
import com.example.gr8math.Services.SupabaseService
import com.example.gr8math.Utils.UIUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DailyLessonLogActivity : AppCompatActivity() {

    private lateinit var tilContent: TextInputLayout
    private lateinit var etContent: EditText

    private lateinit var tilPerformance: TextInputLayout
    private lateinit var etPerformance: EditText

    private lateinit var tilCompetencies: TextInputLayout
    private lateinit var etCompetencies: EditText

    private var dllMainId: Int = -1
    private var dailyEntryId: Int = -1 // Tracks the specific day being edited
    private var sectionTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_lesson_log)

        // Extract Intent Data
        dllMainId = intent.getIntExtra(DLLEditActivity.EXTRA_DLL_MAIN_ID, -1)
        dailyEntryId = intent.getIntExtra("EXTRA_DAILY_ENTRY_ID", -1)
        sectionTitle = intent.getStringExtra(DLLEditActivity.EXTRA_SECTION_TITLE)

        // 1. Initialize Views
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        tilContent = findViewById(R.id.tilContentStandard)
        etContent = findViewById(R.id.etContentStandard)

        tilPerformance = findViewById(R.id.tilPerformanceStandard)
        etPerformance = findViewById(R.id.etPerformanceStandard)

        tilCompetencies = findViewById(R.id.tilLearningCompetencies)
        etCompetencies = findViewById(R.id.etLearningCompetencies)

        // Using your existing btnNext ID, but treating it as a Save button
        val btnSave = findViewById<Button>(R.id.btnNext)

        // 2. Setup Back Button Logic (Toolbar)
        toolbar.setNavigationOnClickListener {
            handleBackPress()
        }

        // 3. Setup System Back Button Logic (Gesture/Hardware)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })

        // 4. Pre-fill data for Editing
        toolbar.title = sectionTitle ?: "Edit Objectives"
        btnSave.text = "SAVE"

        etContent.setText(intent.getStringExtra("EXTRA_CONTENT_STD"))
        etPerformance.setText(intent.getStringExtra("EXTRA_PERF_STD"))
        etCompetencies.setText(intent.getStringExtra("EXTRA_COMPETENCIES"))

        // 5. Handle "Save" Button
        btnSave.setOnClickListener {
            if (validateForm()) {
                // Update Database Directly
                updateObjectivesInSupabase(btnSave)
            }
        }
    }

    private fun handleBackPress() {
        val hasContent = etContent.text.toString().isNotEmpty() ||
                etPerformance.text.toString().isNotEmpty() ||
                etCompetencies.text.toString().isNotEmpty()

        if (hasContent) {
            showDiscardDialog()
        } else {
            finish()
        }
    }

    // 🌟 SUPABASE UPDATE LOGIC 🌟
    private fun updateObjectivesInSupabase(btnSave: Button) {
        if (dailyEntryId == -1) {
            Toast.makeText(this, "Error: Daily Entry ID is missing.", Toast.LENGTH_LONG).show()
            return
        }

        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Update the dll_daily_entry table with the new text
                SupabaseService.client.from("dll_daily_entry").update({
                    set("content_standard", etContent.text.toString().trim())
                    set("performance_standard", etPerformance.text.toString().trim())
                    set("learning_comp", etCompetencies.text.toString().trim())
                }) {
                    filter { eq("id", dailyEntryId) }
                }

                // Switch back to Main thread for UI updates
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DailyLessonLogActivity, "Objectives Updated Successfully!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK) // Triggers the fragment to refresh data
                    finish()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnSave.isEnabled = true
                    btnSave.text = "SAVE"
                    Toast.makeText(this@DailyLessonLogActivity, "Update Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showDiscardDialog() {
        val titleView = TextView(this).apply {
            text = "Discard Changes?"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.colorText))
            setPadding(70, 50, 50, 10)
            try { typeface = ResourcesCompat.getFont(context, R.font.lexend) } catch (_: Exception) { }
        }

        val messageView = TextView(this).apply {
            text = "You have unsaved content. If you go\nback, your changes will be lost."
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.colorSubtleText))
            setPadding(70, 10, 50, 20)
            try { typeface = ResourcesCompat.getFont(context, R.font.lexend) } catch (_: Exception) { }
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setCustomTitle(titleView)
            .setView(messageView)
            .setNegativeButton("Yes") { _, _ -> finish() }
            .setPositiveButton("No") { dialogInterface, _ -> dialogInterface.dismiss() }
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
    }

    private fun validateForm(): Boolean {
        val errorMsg = "Please enter the needed details"

        UIUtils.errorDisplay(this, tilContent, etContent, true, errorMsg)
        UIUtils.errorDisplay(this, tilPerformance, etPerformance, true, errorMsg)
        UIUtils.errorDisplay(this, tilCompetencies, etCompetencies, true, errorMsg)

        return etContent.text.toString().trim().isNotEmpty() &&
                etPerformance.text.toString().trim().isNotEmpty() &&
                etCompetencies.text.toString().trim().isNotEmpty()
    }
}