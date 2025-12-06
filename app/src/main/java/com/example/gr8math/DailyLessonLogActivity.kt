package com.example.gr8math

import android.app.Activity
import android.content.Intent // Required for navigation
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.CurrentCourse
import com.example.gr8math.dataObject.UpdateDllMainRequest
import com.example.gr8math.utils.UIUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import okhttp3.ResponseBody
import retrofit2.Callback
import retrofit2.Response

class DailyLessonLogActivity : AppCompatActivity() {

    private lateinit var tilContent: TextInputLayout
    private lateinit var etContent: EditText

    private lateinit var tilPerformance: TextInputLayout
    private lateinit var etPerformance: EditText

    private lateinit var tilCompetencies: TextInputLayout
    private lateinit var etCompetencies: EditText
    private var isEditMode = false
    private var dllMainId: Int = -1
    private var sectionTitle: String? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_lesson_log)

        isEditMode = intent.getBooleanExtra(DLLEditActivity.EXTRA_MODE_EDIT, false)
        dllMainId = intent.getIntExtra(DLLEditActivity.EXTRA_DLL_MAIN_ID, -1)
        sectionTitle = intent.getStringExtra(DLLEditActivity.EXTRA_SECTION_TITLE)


        // 1. Initialize Views
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        tilContent = findViewById(R.id.tilContentStandard)
        etContent = findViewById(R.id.etContentStandard)

        tilPerformance = findViewById(R.id.tilPerformanceStandard)
        etPerformance = findViewById(R.id.etPerformanceStandard)

        tilCompetencies = findViewById(R.id.tilLearningCompetencies)
        etCompetencies = findViewById(R.id.etLearningCompetencies)

        val btnNext = findViewById<Button>(R.id.btnNext)

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

        // 4. Handle "Next" Button - Navigate to Step 2
        btnNext.setOnClickListener {
            if (validateForm()) {
                if (isEditMode) {
                    callSingleStepUpdateApi()
                } else {
                    // Create Intent to go to Step 2
                    val intent = Intent(this, DLLStep2Activity::class.java)

                    // Pass Data Forward (from previous dialog)
                    intent.putExtra("EXTRA_QUARTER", getIntent().getStringExtra("EXTRA_QUARTER"))
                    intent.putExtra("EXTRA_WEEK", getIntent().getStringExtra("EXTRA_WEEK"))
                    intent.putExtra("EXTRA_FROM", getIntent().getStringExtra("EXTRA_FROM"))
                    intent.putExtra("EXTRA_UNTIL", getIntent().getStringExtra("EXTRA_UNTIL"))

                    // Pass Data from this screen
                    intent.putExtra("EXTRA_CONTENT_STD", etContent.text.toString())
                    intent.putExtra("EXTRA_PERF_STD", etPerformance.text.toString())
                    intent.putExtra("EXTRA_COMPETENCIES", etCompetencies.text.toString())

                    startActivity(intent)
                }
            }

        }

        if (isEditMode) {
            toolbar.title = sectionTitle ?: "Edit Daily Lesson Log"
            btnNext.text = "SAVE"

            etContent.setText(intent.getStringExtra("EXTRA_CONTENT_STD"))
            etPerformance.setText(intent.getStringExtra("EXTRA_PERF_STD"))
            etCompetencies.setText(intent.getStringExtra("EXTRA_COMPETENCIES"))
        }
    }

    /**
     * Checks if there is unsaved content.
     * If YES -> Shows the "Discard Changes" dialog.
     * If NO -> Closes the activity immediately.
     */
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


    private fun callSingleStepUpdateApi() {
        if (dllMainId == -1) {
            Toast.makeText(this, "Error: DLL ID is missing for update.", Toast.LENGTH_LONG).show()
            return
        }

        // 1. Collect Data from UI and Intent Extras
        val request = UpdateDllMainRequest(
            course_id = CurrentCourse.courseId,

            // These fields must be passed from the initial dialog via the Intent (getIntent())
            quarter_number = getIntent().getStringExtra("EXTRA_QUARTER")?.toIntOrNull() ?: 0,
            week_number = getIntent().getStringExtra("EXTRA_WEEK")?.toIntOrNull() ?: 0,
            available_from = getIntent().getStringExtra("EXTRA_FROM") ?: "",
            available_until = getIntent().getStringExtra("EXTRA_UNTIL") ?: "",

            content_standard = etContent.text.toString(),
            performance_standard = etPerformance.text.toString(),
            learning_comp = etCompetencies.text.toString()
        )

        // 2. Call the API
        ConnectURL.api.updateDllMain(dllMainId, request).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: retrofit2.Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@DailyLessonLogActivity, "Objectives Updated Successfully!", Toast.LENGTH_SHORT).show()

                    // 3. Notify the calling activity (DLLViewActivity) to refresh the display
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown API Error"
                    Toast.makeText(this@DailyLessonLogActivity, "Update Failed: ${response.code()}", Toast.LENGTH_LONG).show()

                }
            }

            override fun onFailure(call: retrofit2.Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@DailyLessonLogActivity, "Network Failure: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
    /**
     * Shows the custom dialog matching your screenshot (Left Aligned, White Background).
     */
    private fun showDiscardDialog() {
        // 1. Title View: Left Aligned, Bold
        val titleView = TextView(this).apply {
            text = "Discard Changes?"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.colorText))
            setPadding(70, 50, 50, 10)
            try { typeface = ResourcesCompat.getFont(context, R.font.lexend) } catch (_: Exception) { }
        }

        // 2. Message View: Left Aligned
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
            // "Yes" (Discard) -> Negative Button (Left side)
            .setNegativeButton("Yes") { _, _ ->
                finish() // Close Activity
            }
            // "No" (Keep Editing) -> Positive Button (Right side)
            .setPositiveButton("No") { dialogInterface, _ ->
                dialogInterface.dismiss() // Stay on page
            }
            .create()

        dialog.show()

        // Style the buttons (Red color)
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))

        // Note: Do NOT set window background to Transparent here,
        // we want the default white card background.
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