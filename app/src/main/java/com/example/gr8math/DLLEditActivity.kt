package com.example.gr8math

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.Serializable

class DLLEditActivity : AppCompatActivity() {

    private lateinit var editFieldsContainer: LinearLayout
    private lateinit var btnSave: Button
    private lateinit var toolbar: MaterialToolbar

    private var originalDataMap: Map<String, String> = emptyMap()
    private val generatedInputs = mutableMapOf<String, EditText>()

    companion object {
        const val EXTRA_SECTION_TITLE = "EXTRA_SECTION_TITLE"
        const val EXTRA_DATA_MAP = "EXTRA_DATA_MAP"
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dll_edit)

        toolbar = findViewById(R.id.toolbar)
        editFieldsContainer = findViewById(R.id.editFieldsContainer)
        btnSave = findViewById(R.id.btnSave)

        val sectionTitle = intent.getStringExtra(EXTRA_SECTION_TITLE) ?: "Edit"
        toolbar.title = sectionTitle

        val receivedMap = intent.getSerializableExtra(EXTRA_DATA_MAP) as? HashMap<String, String>
        if (receivedMap != null) {
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

                val resultIntent = Intent()
                resultIntent.putExtra(EXTRA_DATA_MAP, updatedData)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
            .create()

        dialog.show()

        // Red Buttons
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))

        // FIX: Removed the transparent background line so the white box appears
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