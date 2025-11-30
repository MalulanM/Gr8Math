package com.example.gr8math

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent // <--- THIS WAS MISSING
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

class DLLStep4Activity : AppCompatActivity() {

    private lateinit var reflectionDaysContainer: LinearLayout
    private lateinit var btnAddDay: Button
    private lateinit var btnSave: Button

    private val dayManagers = mutableListOf<ReflectionDayManager>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dll_step4)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        reflectionDaysContainer = findViewById(R.id.reflectionDaysContainer)
        btnAddDay = findViewById(R.id.btnAddDay)
        btnSave = findViewById(R.id.btnSave)

        // 1. Setup Back Navigation
        toolbar.setNavigationOnClickListener { handleBackPress() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { handleBackPress() }
        })

        // Add first day by default
        addDay()

        btnAddDay.setOnClickListener { addDay() }

        btnSave.setOnClickListener {
            if (validateForms()) {
                showReviewDialog()
            }
        }
    }

    private fun handleBackPress() {
        var hasUnsavedData = false
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

    // --- "DO YOU WANT TO REVIEW?" DIALOG ---
    private fun showReviewDialog() {
        val messageView = TextView(this).apply {
            text = "Do you want to review the daily lesson\nlog?"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            // REMOVED: gravity = android.view.Gravity.CENTER

            // Left aligned by default.
            // setPadding(Left, Top, Right, Bottom)
            setTextColor(ContextCompat.getColor(context, R.color.colorText))
            setPadding(70, 60, 50, 20)
            try { typeface = ResourcesCompat.getFont(context, R.font.lexend) } catch (_: Exception) { }
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(messageView)
            // YES = Dismiss dialog (Stay on page to review)
            .setNegativeButton("Yes") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            // NO = Proceed (Finish / Save)
            .setPositiveButton("No") { dialogInterface, _ ->
                // TODO: Actual API Submit here
                Toast.makeText(this, "DLL Saved Successfully!", Toast.LENGTH_SHORT).show()

                // Go back to Class Page (Clear Activity Stack)
                val intent = Intent(this, TeacherClassPageActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
            }
            .create()

        dialog.show()

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
    }

    private fun addDay() {
        val manager = ReflectionDayManager(this, reflectionDaysContainer) { managerToRemove ->
            reflectionDaysContainer.removeView(managerToRemove.view)
            dayManagers.remove(managerToRemove)
        }
        dayManagers.add(manager)
    }

    private fun validateForms(): Boolean {
        if (dayManagers.isEmpty()) {
            Toast.makeText(this, "Please add at least one day.", Toast.LENGTH_SHORT).show()
            return false
        }

        var allValid = true
        dayManagers.forEach { manager ->
            if (!manager.isValid()) {
                allValid = false
            }
        }
        return allValid
    }

    // --- Inner Class for Reflection Card ---
    inner class ReflectionDayManager(
        private val context: Context,
        container: LinearLayout,
        private val onRemove: (ReflectionDayManager) -> Unit
    ) {
        val view: View = LayoutInflater.from(context).inflate(R.layout.item_dll_reflection_card, container, false)

        private val etDate: EditText = view.findViewById(R.id.etDate)
        private val tilDate: TextInputLayout = view.findViewById(R.id.tilDate)
        private val btnRemove: ImageButton = view.findViewById(R.id.btnRemoveDay)

        private val etReview: EditText = view.findViewById(R.id.etReview)
        private val tilReview: TextInputLayout = view.findViewById(R.id.tilReview)

        private val etReflection: EditText = view.findViewById(R.id.etReflection)
        private val tilReflection: TextInputLayout = view.findViewById(R.id.tilReflection)

        init {
            container.addView(view)
            etDate.setText("") // Clear default

            // Date Picker
            etDate.setOnClickListener {
                val calendar = Calendar.getInstance()
                DatePickerDialog(context, { _, year, month, day ->
                    val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.US)
                    calendar.set(year, month, day)
                    etDate.setText(sdf.format(calendar.time))
                    tilDate.error = null
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
            }

            // Remove logic
            btnRemove.setOnClickListener { onRemove(this) }
        }

        fun hasData(): Boolean {
            if (etDate.text.isNotEmpty()) return true
            if (etReview.text.isNotEmpty()) return true
            if (etReflection.text.isNotEmpty()) return true
            return false
        }

        fun isValid(): Boolean {
            var valid = true
            val errorMsg = "Required"

            if (etDate.text.toString().trim().isEmpty()) {
                UIUtils.errorDisplay(context, tilDate, etDate, true, errorMsg)
                valid = false
            }
            if (etReview.text.toString().trim().isEmpty()) {
                UIUtils.errorDisplay(context, tilReview, etReview, true, errorMsg)
                valid = false
            }
            if (etReflection.text.toString().trim().isEmpty()) {
                UIUtils.errorDisplay(context, tilReflection, etReflection, true, errorMsg)
                valid = false
            }
            return valid
        }
    }
}