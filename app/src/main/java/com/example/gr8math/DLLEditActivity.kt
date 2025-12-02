package com.example.gr8math

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.CurrentCourse
import com.example.gr8math.dataObject.UpdateDllMainRequest
import com.example.gr8math.dataObject.UpdateProcedureRequest
import com.example.gr8math.dataObject.UpdateReferenceRequest
import com.example.gr8math.dataObject.UpdateReflectionRequest
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import okhttp3.Call
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.Serializable

class DLLEditActivity : AppCompatActivity() {

    private lateinit var editFieldsContainer: LinearLayout
    private lateinit var btnSave: Button
    private lateinit var toolbar: MaterialToolbar

    private var originalDataMap: Map<String, String> = emptyMap()
    private val generatedInputs = mutableMapOf<String, EditText>()

    private var dllMainId: Int = -1 // To store the primary DLL ID
    private var recordDbId: Int = -1 // To store the ID of the specific sub-record (reference, procedure, reflection)
    companion object {
        const val EXTRA_SECTION_TITLE = "EXTRA_SECTION_TITLE"
        const val EXTRA_DATA_MAP = "EXTRA_DATA_MAP"
        const val EXTRA_DLL_MAIN_ID = "EXTRA_DLL_MAIN_ID" // Passed from DLLViewActivity

        // Keys to identify the specific record being edited
        const val KEY_RECORD_ID = "DB_RECORD_ID"
        const val KEY_DATE = "DATE" // Used for consistency

        // Section Identifiers
        const val SECTION_OBJECTIVES = "Edit Objectives"
        const val SECTION_RESOURCES = "Edit Content & Resources"
        const val SECTION_PROCEDURES = "Edit Procedures"
        const val SECTION_REFLECTION = "Edit Reflection"
        const val EXTRA_MODE_EDIT = "EXTRA_MODE_EDIT" // Use this flag
        const val EXTRA_DLL_DATA_OBJECT = "EXTRA_DLL_DATA_OBJECT" // Full data object

        // 🌟 New: Defines WHICH SECTION the user clicked 'Edit' on (to trigger the correct save API)
        const val EDIT_SECTION_OBJECTIVES = "Objectives"
        const val EDIT_SECTION_RESOURCES = "Resources"
        const val EDIT_SECTION_PROCEDURES = "Procedures"
        const val EDIT_SECTION_REFLECTION = "Reflection"


    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dll_edit)
        dllMainId = intent.getIntExtra(EXTRA_DLL_MAIN_ID, -1)
        toolbar = findViewById(R.id.toolbar)
        editFieldsContainer = findViewById(R.id.editFieldsContainer)
        btnSave = findViewById(R.id.btnSave)

        val sectionTitle = intent.getStringExtra(EXTRA_SECTION_TITLE) ?: "Edit"
        toolbar.title = sectionTitle

        val receivedMap = intent.getSerializableExtra(EXTRA_DATA_MAP) as? HashMap<String, String>
        if (receivedMap != null) {
            // 🌟 EXTRACT RECORD ID IF PRESENT (for nested items)
            recordDbId = receivedMap[KEY_RECORD_ID]?.toIntOrNull() ?: -1
            originalDataMap = receivedMap.toMap()
            populateFields(originalDataMap)
        }

        setupBackNavigation()

        btnSave.setOnClickListener {
            showSaveConfirmationDialog()
        }
    }

    private fun populateFields(data: Map<String, String>) {
        for ((label, content) in data) {
            val fieldView = LayoutInflater.from(this).inflate(R.layout.item_dll_edit_field, editFieldsContainer, false)

            val tvLabel = fieldView.findViewById<TextView>(R.id.tvFieldLabel)
            val etInput = fieldView.findViewById<EditText>(R.id.etFieldInput)

            tvLabel.text = label
            etInput.setText(content)

            generatedInputs[label] = etInput
            editFieldsContainer.addView(fieldView)
        }
    }

    private fun setupBackNavigation() {
        val handleBack = {
            if (hasUnsavedChanges()) {
                showDiscardDialog()
            } else {
                finish()
            }
        }

        toolbar.setNavigationOnClickListener { handleBack() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { handleBack() }
        })
    }

    private fun hasUnsavedChanges(): Boolean {
        for ((label, originalText) in originalDataMap) {
            val currentText = generatedInputs[label]?.text.toString()
            if (currentText != originalText) {
                return true
            }
        }
        return false
    }

    // --- DIALOG 1: SAVE CONFIRMATION ---
    private fun showSaveConfirmationDialog() {
        val messageView = TextView(this).apply {
            text = "Do you want to save the changes?" // Left aligned by default
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.colorText))
            setPadding(70, 60, 50, 10) // Padding for standard dialog feel
            try { typeface = ResourcesCompat.getFont(context, R.font.lexend) } catch (_: Exception) { }
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(messageView)
            // Swapped: No (Dismiss) is Positive, Yes (Save) is Negative to match visual order L->R
            .setPositiveButton("No") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .setNegativeButton("Yes") { _, _ ->
                val updatedData = HashMap<String, String>()
                generatedInputs.forEach { (label, editText) ->
                    updatedData[label] = editText.text.toString()
                }

                callApi(updatedData)
            }
            .create()

        dialog.show()

        // Red Buttons
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))

        // FIX: Removed the transparent background line so the white box appears
    }

    private fun callApi(updatedData: HashMap<String, String>) {
        if (dllMainId == -1) {
            Toast.makeText(this, "Error: Cannot save, DLL ID is missing.", Toast.LENGTH_LONG).show()
            return
        }

        val section = toolbar.title.toString()
        val apiCall: retrofit2.Call<ResponseBody>

        // Utility function to safely get the date string (required for most updates)
        val dateString = updatedData[KEY_DATE] ?: ""

        try {
            when (section) {

                // --- 1. OBJECTIVES (Update dll_main) ---
                SECTION_OBJECTIVES -> {
                    val request = UpdateDllMainRequest(
                        course_id = CurrentCourse.courseId,
                        quarter_number = updatedData["Quarter Number"]?.toIntOrNull() ?: 0,
                        week_number = updatedData["Week Number"]?.toIntOrNull() ?: 0,
                        available_from = updatedData["Available From"] ?: "",
                        available_until = updatedData["Available Until"] ?: "",
                        content_standard = updatedData["Content Standard"] ?: "",
                        performance_standard = updatedData["Performance Standard"] ?: "",
                        learning_comp = updatedData["Learning Competencies"] ?: ""
                    )
                    apiCall = ConnectURL.api.updateDllMain(dllMainId, request)
                }

                // --- 2. PROCEDURES (Update dll_procedure single record) ---
                SECTION_PROCEDURES -> {
                    if (recordDbId == -1) throw Exception("Procedure record ID missing.")
                    val request = UpdateProcedureRequest(
                        id = recordDbId,
                        date = dateString, // Pass date from the map
                        review = updatedData["Review"] ?: "",
                        purpose = updatedData["Purpose"] ?: "",
                        example = updatedData["Example"] ?: "",
                        discussion_proper = updatedData["Discussion Proper"] ?: "",
                        developing_mastery = updatedData["Developing Mastery"] ?: "",
                        application = updatedData["Application"] ?: "",
                        generalization = updatedData["Generalization"] ?: "",
                        evaluation = updatedData["Evaluation"] ?: "",
                        additional_act = updatedData["Additional Activities"] ?: ""
                    )
                    apiCall = ConnectURL.api.updateProcedure(request)
                }

                // --- 3. REFLECTION (Update dll_reflection single record) ---
                SECTION_REFLECTION -> {
                    if (recordDbId == -1) throw Exception("Reflection record ID missing.")
                    val request = UpdateReflectionRequest(
                        id = recordDbId,
                        date = dateString, // Pass date from the map
                        remark = updatedData["Remarks"] ?: "",
                        reflection = updatedData["Reflection"] ?: ""
                    )
                    apiCall = ConnectURL.api.updateReflection(request)
                }

                // --- 4. RESOURCES (Update dll_reference single record) ---
                SECTION_RESOURCES -> {
                    if (recordDbId == -1) throw Exception("Reference record ID missing.")
                    // This updates the FIRST reference record's data using aggregated content.
                    val request = UpdateReferenceRequest(
                        id = recordDbId,
                        date = dateString, // Pass date from the map
                        reference_title = updatedData["Content"] ?: "",
                        reference_text = updatedData["Learning Resources"] ?: ""
                    )
                    apiCall = ConnectURL.api.updateReference(request)
                }

                else -> {
                    Toast.makeText(this, "Unknown section.", Toast.LENGTH_LONG).show()
                    return
                }
            }

            // Execute API Call
            apiCall.enqueue(object : retrofit2.Callback<ResponseBody> {
                override fun onResponse(call: retrofit2.Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@DLLEditActivity, "$section Updated Successfully!", Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_OK) // Trigger refresh in DLLViewActivity
                        finish()
                    } else {
                        Toast.makeText(this@DLLEditActivity, "API Error: ${response.code()} - Failed to update.", Toast.LENGTH_LONG).show()
                    }
                }
                override fun onFailure(call: retrofit2.Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(this@DLLEditActivity, "Network Failure: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })

        } catch (e: Exception) {
            Toast.makeText(this, "Data Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }


    // --- DIALOG 2: DISCARD CHANGES ---
    private fun showDiscardDialog() {
        val titleView = TextView(this).apply {
            text = "Discard Changes?"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
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

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
    }
}