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

class DLLStep2Activity : AppCompatActivity() {

    private lateinit var daysContainer: LinearLayout
    private lateinit var btnAddDay: Button
    private lateinit var btnNext: Button
    private lateinit var etContent: EditText
    private lateinit var tilContent: TextInputLayout

    // List to keep track of all active day cards
    private val dayManagers = mutableListOf<DayCardManager>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dll_step2)

        // 1. Setup Views
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)

        daysContainer = findViewById(R.id.daysContainer)
        btnAddDay = findViewById(R.id.btnAddDay)
        btnNext = findViewById(R.id.btnNext)
        etContent = findViewById(R.id.etContent)
        tilContent = findViewById(R.id.tilContent)

        // 2. Setup Back Navigation (Toolbar)
        toolbar.setNavigationOnClickListener {
            handleBackPress()
        }

        // 3. Setup System Back Navigation
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })

        // 4. Add the first day card by default
        addDayCard()

        // 5. Listeners
        btnAddDay.setOnClickListener {
            addDayCard()
        }

        btnNext.setOnClickListener {
            validateAndProceed()
        }
    }

    private fun handleBackPress() {
        var hasUnsavedData = false

        // Check Content Field
        if (etContent.text.toString().trim().isNotEmpty()) {
            hasUnsavedData = true
        }

        // Check Day Cards
        if (!hasUnsavedData) {
            for (manager in dayManagers) {
                if (manager.hasData()) {
                    hasUnsavedData = true
                    break
                }
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

    private fun addDayCard() {
        val manager = DayCardManager(this, daysContainer) { managerToRemove ->
            daysContainer.removeView(managerToRemove.view)
            dayManagers.remove(managerToRemove)
        }
        dayManagers.add(manager)
    }

    private fun validateAndProceed() {
        var isValid = true
        val errorMsg = "Please enter the needed details"

        UIUtils.errorDisplay(this, tilContent, etContent, true, errorMsg)
        if (etContent.text.toString().trim().isEmpty()) isValid = false

        if (dayManagers.isEmpty()) {
            Toast.makeText(this, "Please add at least one day.", Toast.LENGTH_SHORT).show()
            isValid = false
        } else {
            dayManagers.forEach { manager ->
                if (!manager.isValid()) {
                    isValid = false
                }
            }
        }

        if (isValid) {
            val intent = Intent(this, DLLStep3Activity::class.java)
            // Pass data...
            startActivity(intent)
        }
    }

    // --- INNER CLASS ---
    inner class DayCardManager(
        private val context: Context,
        private val container: LinearLayout,
        private val onRemove: (DayCardManager) -> Unit
    ) {
        val view: View = LayoutInflater.from(context).inflate(R.layout.item_dll_day_card, container, false)

        private val etDate: EditText = view.findViewById(R.id.etDate)
        private val tilDate: TextInputLayout = view.findViewById(R.id.tilDate)
        private val resourcesContainer: LinearLayout = view.findViewById(R.id.resourcesContainer)
        private val btnRemove: ImageButton = view.findViewById(R.id.btnRemoveDay)
        private val btnAddResource: Button = view.findViewById(R.id.btnAddResource)

        private val resourceInputs = mutableListOf<TextInputLayout>()

        init {
            container.addView(view)

            // Clear default XML text to check for "Unsaved Data" accurately
            etDate.setText("")

            addResourceLine()

            etDate.setOnClickListener {
                val calendar = Calendar.getInstance()
                DatePickerDialog(context, { _, year, month, day ->
                    val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.US)
                    calendar.set(year, month, day)
                    etDate.setText(sdf.format(calendar.time))
                    tilDate.error = null
                    tilDate.isErrorEnabled = false
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
            }

            btnRemove.setOnClickListener { onRemove(this) }

            btnAddResource.setOnClickListener {
                if (resourceInputs.isNotEmpty()) {
                    val lastTil = resourceInputs.last()
                    val lastEt = lastTil.editText
                    if (lastEt?.text.toString().trim().isEmpty()) {
                        UIUtils.errorDisplay(context, lastTil, lastEt!!, true, "Please fill this first")
                        return@setOnClickListener
                    }
                }
                addResourceLine()
            }
        }

        private fun addResourceLine() {
            val resourceView = LayoutInflater.from(context).inflate(R.layout.item_dll_resource_input, resourcesContainer, false)
            val til = resourceView.findViewById<TextInputLayout>(R.id.tilResourceItem)
            val btnRemoveRes = resourceView.findViewById<ImageButton>(R.id.btnRemoveResource)

            resourcesContainer.addView(resourceView)
            resourceInputs.add(til)

            btnRemoveRes.setOnClickListener {
                resourcesContainer.removeView(resourceView)
                resourceInputs.remove(til)
            }
        }

        // Helper to check if user typed anything
        fun hasData(): Boolean {
            if (etDate.text.toString().isNotEmpty()) return true
            resourceInputs.forEach { til ->
                if (til.editText?.text.toString().isNotEmpty()) return true
            }
            return false
        }

        fun isValid(): Boolean {
            var valid = true
            val errorMsg = "Required"

            if (etDate.text.toString().trim().isEmpty()) {
                UIUtils.errorDisplay(context, tilDate, etDate, true, errorMsg)
                valid = false
            }

            if (resourceInputs.isEmpty()) {
                valid = false
                Toast.makeText(context, "Please add at least one resource", Toast.LENGTH_SHORT).show()
            } else {
                resourceInputs.forEach { til ->
                    val et = til.editText
                    if (et?.text.toString().trim().isEmpty()) {
                        UIUtils.errorDisplay(context, til, et!!, true, errorMsg)
                        valid = false
                    }
                }
            }
            return valid
        }
    }
}