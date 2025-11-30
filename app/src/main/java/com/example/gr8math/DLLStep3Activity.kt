package com.example.gr8math

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.gr8math.utils.UIUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DLLStep3Activity : AppCompatActivity() {

    private lateinit var procedureDaysContainer: LinearLayout
    private lateinit var btnAddDay: Button
    private lateinit var btnNext: Button

    // List to track dynamic cards
    private val dayManagers = mutableListOf<ProcedureDayManager>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dll_step3)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        procedureDaysContainer = findViewById(R.id.procedureDaysContainer)
        btnAddDay = findViewById(R.id.btnAddDay)
        btnNext = findViewById(R.id.btnNext)

        // 1. Setup Back Navigation (Toolbar)
        toolbar.setNavigationOnClickListener { handleBackPress() }

        // 2. Setup System Back Navigation
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { handleBackPress() }
        })

        // 3. Add first day by default
        addDay()

        // 4. Listeners
        btnAddDay.setOnClickListener {
            addDay()
        }

        btnNext.setOnClickListener {
            validateAndProceed()
        }
    }

    private fun handleBackPress() {
        var hasUnsavedData = false
        // Check if any card has data entered
        for (manager in dayManagers) {
            if (manager.hasData()) {
                hasUnsavedData = true
                break
            }
        }

        if (hasUnsavedData) {
            showDiscardDialog()
        } else {
            finish()
        }
    }

    private fun showDiscardDialog() {
        // Title: Bold, Left Aligned
        val titleView = TextView(this).apply {
            text = "Discard Changes?"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.colorText))
            setPadding(70, 50, 50, 10)
            try { typeface = ResourcesCompat.getFont(context, R.font.lexend) } catch (_: Exception) { }
        }

        // Message: Normal, Left Aligned
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
            .setNegativeButton("Yes") { _, _ -> finish() }
            // "No" (Keep Editing) -> Positive Button (Right side)
            .setPositiveButton("No") { dialogInterface, _ -> dialogInterface.dismiss() }
            .create()

        dialog.show()

        // Red Buttons
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
    }

    private fun addDay() {
        val manager = ProcedureDayManager(this, procedureDaysContainer) { managerToRemove ->
            procedureDaysContainer.removeView(managerToRemove.view)
            dayManagers.remove(managerToRemove)
        }
        dayManagers.add(manager)
    }

    private fun validateAndProceed() {
        if (dayManagers.isEmpty()) {
            Toast.makeText(this, "Please add at least one day.", Toast.LENGTH_SHORT).show()
            return
        }

        var allValid = true
        dayManagers.forEach { manager ->
            if (!manager.isValid()) {
                allValid = false
            }
        }

        if (allValid) {
            // Navigate to Step 4
            val intent = Intent(this, DLLStep4Activity::class.java)
            // Pass previous data...
            intent.putExtra("EXTRA_QUARTER", getIntent().getStringExtra("EXTRA_QUARTER"))
            intent.putExtra("EXTRA_WEEK", getIntent().getStringExtra("EXTRA_WEEK"))
            // Pass current data...

            startActivity(intent)
        }
    }

    // --- Inner Class for the Big Procedure Card ---
    inner class ProcedureDayManager(
        private val context: Context,
        container: LinearLayout,
        private val onRemove: (ProcedureDayManager) -> Unit
    ) {
        val view: View = LayoutInflater.from(context).inflate(R.layout.item_dll_procedure_card, container, false)

        // Date UI
        private val etDate: EditText = view.findViewById(R.id.etDate)
        private val tilDate: TextInputLayout = view.findViewById(R.id.tilDate)
        private val btnRemove: ImageButton = view.findViewById(R.id.btnRemoveDay)

        // Fields (Review, Purpose, Example, etc.)
        private val etReview: EditText = view.findViewById(R.id.etReview)
        private val tilReview: TextInputLayout = view.findViewById(R.id.tilReview)

        private val etPurpose: EditText = view.findViewById(R.id.etPurpose)
        private val tilPurpose: TextInputLayout = view.findViewById(R.id.tilPurpose)

        private val etExample: EditText = view.findViewById(R.id.etExample)
        private val tilExample: TextInputLayout = view.findViewById(R.id.tilExample)

        private val etDiscussion1: EditText = view.findViewById(R.id.etDiscussion1)
        private val tilDiscussion1: TextInputLayout = view.findViewById(R.id.tilDiscussion1)

        private val etMastery: EditText = view.findViewById(R.id.etMastery)
        private val tilMastery: TextInputLayout = view.findViewById(R.id.tilMastery)

        private val etApplication: EditText = view.findViewById(R.id.etApplication)
        private val tilApplication: TextInputLayout = view.findViewById(R.id.tilApplication)

        private val etGeneralization: EditText = view.findViewById(R.id.etGeneralization)
        private val tilGeneralization: TextInputLayout = view.findViewById(R.id.tilGeneralization)

        private val etEvaluation: EditText = view.findViewById(R.id.etEvaluation)
        private val tilEvaluation: TextInputLayout = view.findViewById(R.id.tilEvaluation)

        private val etAdditional: EditText = view.findViewById(R.id.etAdditional)
        private val tilAdditional: TextInputLayout = view.findViewById(R.id.tilAdditional)

        init {
            container.addView(view)
            etDate.setText("") // Clear XML default for accurate validation

            // Date Picker Logic
            etDate.setOnClickListener {
                val calendar = Calendar.getInstance()
                DatePickerDialog(context, { _, year, month, day ->
                    val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.US)
                    calendar.set(year, month, day)
                    etDate.setText(sdf.format(calendar.time))
                    // Clear error
                    tilDate.error = null
                    tilDate.isErrorEnabled = false
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
            }

            // Remove Logic
            btnRemove.setOnClickListener { onRemove(this) }
        }

        // Check if user entered anything (for Discard Dialog)
        fun hasData(): Boolean {
            if (etDate.text.isNotEmpty()) return true
            if (etReview.text.isNotEmpty()) return true
            if (etPurpose.text.isNotEmpty()) return true
            if (etExample.text.isNotEmpty()) return true
            if (etDiscussion1.text.isNotEmpty()) return true
            if (etMastery.text.isNotEmpty()) return true
            if (etApplication.text.isNotEmpty()) return true
            if (etGeneralization.text.isNotEmpty()) return true
            if (etEvaluation.text.isNotEmpty()) return true
            if (etAdditional.text.isNotEmpty()) return true
            return false
        }

        // Validate required fields (using UIUtils)
        fun isValid(): Boolean {
            var valid = true
            val errorMsg = "Please enter the needed details"

            // Validate Date
            UIUtils.errorDisplay(context, tilDate, etDate, true, "Required")
            if (etDate.text.toString().trim().isEmpty()) valid = false

            // Validate all text fields
            val fields = listOf(
                Pair(tilReview, etReview),
                Pair(tilPurpose, etPurpose),
                Pair(tilExample, etExample),
                Pair(tilDiscussion1, etDiscussion1),
                Pair(tilMastery, etMastery),
                Pair(tilApplication, etApplication),
                Pair(tilGeneralization, etGeneralization),
                Pair(tilEvaluation, etEvaluation),
                Pair(tilAdditional, etAdditional)
            )

            for ((til, et) in fields) {
                UIUtils.errorDisplay(context, til, et, true, errorMsg)
                if (et.text.toString().trim().isEmpty()) {
                    valid = false
                }
            }

            return valid
        }
    }
}