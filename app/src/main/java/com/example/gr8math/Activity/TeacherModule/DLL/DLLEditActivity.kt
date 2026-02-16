package com.example.gr8math.Activity.TeacherModule.DLL

import android.app.AlertDialog
import android.graphics.Typeface
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
import androidx.lifecycle.lifecycleScope
import com.example.gr8math.R
import com.example.gr8math.Services.SupabaseService
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DLLEditActivity : AppCompatActivity() {

    private lateinit var editFieldsContainer: LinearLayout
    private lateinit var btnSave: Button
    private lateinit var toolbar: MaterialToolbar

    private var originalDataMap: Map<String, String> = emptyMap()
    private val generatedInputs = mutableMapOf<String, EditText>()

    private var dllMainId: Int = -1
    private var dailyEntryId: Int = -1 // ID for dll_daily_entry (New Schema)
    private var recordDbId: Int = -1 // ID for specific sub-records like dll_references

    companion object {
        const val EXTRA_SECTION_TITLE = "EXTRA_SECTION_TITLE"
        const val EXTRA_DATA_MAP = "EXTRA_DATA_MAP"
        const val EXTRA_DLL_MAIN_ID = "EXTRA_DLL_MAIN_ID"

        const val KEY_RECORD_ID = "DB_RECORD_ID"
        const val KEY_DATE = "DATE"

        const val SECTION_OBJECTIVES = "Edit Objectives"
        const val SECTION_RESOURCES = "Edit Content & Resources"
        const val SECTION_PROCEDURES = "Edit Procedures"
        const val SECTION_REFLECTION = "Edit Reflection"

        const val EXTRA_MODE_EDIT = "EXTRA_MODE_EDIT"

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
        dailyEntryId = intent.getIntExtra("EXTRA_DAILY_ENTRY_ID", -1) // Get the daily entry ID

        toolbar = findViewById(R.id.toolbar)
        editFieldsContainer = findViewById(R.id.editFieldsContainer)
        btnSave = findViewById(R.id.btnSave)

        val sectionTitle = intent.getStringExtra(EXTRA_SECTION_TITLE) ?: "Edit"
        toolbar.title = sectionTitle

        val receivedMap = intent.getSerializableExtra(EXTRA_DATA_MAP) as? java.util.HashMap<String, String>
        if (receivedMap != null) {
            recordDbId = receivedMap[KEY_RECORD_ID]?.toIntOrNull() ?: -1

            // Remove meta-data keys so they don't render as EditText fields on the screen
            receivedMap.remove(KEY_RECORD_ID)
            receivedMap.remove(KEY_DATE)

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

    private fun showSaveConfirmationDialog() {
        val messageView = TextView(this).apply {
            text = "Do you want to save the changes?"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.colorText))
            setPadding(70, 60, 50, 10)
            try { typeface = ResourcesCompat.getFont(context, R.font.lexend) } catch (_: Exception) { }
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(messageView)
            .setPositiveButton("No") { dialogInterface, _ -> dialogInterface.dismiss() }
            .setNegativeButton("Yes") { _, _ ->
                val updatedData = HashMap<String, String>()
                generatedInputs.forEach { (label, editText) ->
                    updatedData[label] = editText.text.toString()
                }
                updateDatabase(updatedData)
            }
            .create()

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
    }

    // 🌟 SUPABASE UPDATE LOGIC 🌟
    private fun updateDatabase(updatedData: HashMap<String, String>) {
        val section = toolbar.title.toString()

        // Disable button to prevent double clicks
        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                when (section) {
                    // --- 1. OBJECTIVES (Updates dll_daily_entry) ---
                    SECTION_OBJECTIVES -> {
                        if (dailyEntryId == -1) throw Exception("Daily Entry ID missing.")
                        SupabaseService.client.from("dll_daily_entry").update({
                            set("content_standard", updatedData["Content Standard"] ?: "")
                            set("performance_standard", updatedData["Performance Standard"] ?: "")
                            set("learning_comp", updatedData["Learning Competencies"] ?: "")
                        }) {
                            filter { eq("id", dailyEntryId) }
                        }
                    }

                    // --- 2. PROCEDURES (Updates dll_daily_entry) ---
                    SECTION_PROCEDURES -> {
                        if (dailyEntryId == -1) throw Exception("Daily Entry ID missing.")
                        SupabaseService.client.from("dll_daily_entry").update({
                            set("review", updatedData["Review"] ?: "")
                            set("purpose", updatedData["Purpose"] ?: "")
                            set("example", updatedData["Example"] ?: "")
                            set("discussion_proper", updatedData["Discussion Proper"] ?: "")
                            set("developing_mastery", updatedData["Developing Mastery"] ?: "")
                            set("application", updatedData["Application"] ?: "")
                            set("generalization", updatedData["Generalization"] ?: "")
                            set("evaluation", updatedData["Evaluation"] ?: "")
                            set("additional_act", updatedData["Additional Activities"] ?: "")
                        }) {
                            filter { eq("id", dailyEntryId) }
                        }
                    }

                    // --- 3. REFLECTION (Updates dll_daily_entry) ---
                    SECTION_REFLECTION -> {
                        if (dailyEntryId == -1) throw Exception("Daily Entry ID missing.")
                        SupabaseService.client.from("dll_daily_entry").update({
                            set("remark", updatedData["Remarks"] ?: "")
                            set("reflection", updatedData["Reflection"] ?: "")
                        }) {
                            filter { eq("id", dailyEntryId) }
                        }
                    }

                    // --- 4. RESOURCES (Updates dll_references) ---
                    SECTION_RESOURCES -> {
                        if (recordDbId == -1) throw Exception("Reference record ID missing.")
                        SupabaseService.client.from("dll_references").update({
                            set("reference_title", updatedData["Content"] ?: "")
                            set("reference_text", updatedData["Learning Resources"] ?: "")
                        }) {
                            filter { eq("id", recordDbId) }
                        }
                    }

                    else -> throw Exception("Unknown section: $section")
                }

                // Success
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DLLEditActivity, "$section Updated Successfully!", Toast.LENGTH_SHORT).show()

                    // Return the updated data to the calling Fragment so it can refresh immediately
                    val resultIntent = android.content.Intent()
                    resultIntent.putExtra(EXTRA_DATA_MAP, updatedData)
                    setResult(RESULT_OK, resultIntent)

                    finish()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnSave.isEnabled = true
                    btnSave.text = "Save"
                    Toast.makeText(this@DLLEditActivity, "Update Error: ${e.message}", Toast.LENGTH_LONG).show()
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
}