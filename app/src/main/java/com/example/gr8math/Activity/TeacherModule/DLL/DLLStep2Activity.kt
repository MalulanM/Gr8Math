package com.example.gr8math.Activity.TeacherModule.DLL

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
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
import androidx.lifecycle.lifecycleScope
import com.example.gr8math.R
import com.example.gr8math.Services.SupabaseService
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.Utils.UIUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DLLStep2Activity : AppCompatActivity() {

    private lateinit var daysContainer: LinearLayout
    private lateinit var btnAddDay: Button
    private lateinit var btnNext: Button
    private lateinit var etContent: EditText
    private lateinit var tilContent: TextInputLayout
    private var minDateMillis: Long = 0
    private var maxDateMillis: Long = Long.MAX_VALUE

    private var isEditMode = false
    private var dllMainId: Int = -1
    private var dailyEntryId: Int = -1 // Target specific daily entry
    private var originalReferenceId: Int = -1 // Target specific reference record
    private var sectionTitle: String? = null

    // List to keep track of all active day cards
    private val dayManagers = mutableListOf<DayCardManager>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dll_step2)

        isEditMode = intent.getBooleanExtra(DLLEditActivity.EXTRA_MODE_EDIT, false)
        dllMainId = intent.getIntExtra(DLLEditActivity.EXTRA_DLL_MAIN_ID, -1)
        dailyEntryId = intent.getIntExtra("EXTRA_DAILY_ENTRY_ID", -1)
        originalReferenceId = intent.getIntExtra(DLLEditActivity.KEY_RECORD_ID, -1)
        sectionTitle = intent.getStringExtra(DLLEditActivity.EXTRA_SECTION_TITLE)

        // 1. Setup Views
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)

        daysContainer = findViewById(R.id.daysContainer)
        btnAddDay = findViewById(R.id.btnAddDay)
        btnNext = findViewById(R.id.btnNext)
        etContent = findViewById(R.id.etContent)
        tilContent = findViewById(R.id.tilContent)

        // 2. Setup Back Navigation
        toolbar.setNavigationOnClickListener { handleBackPress() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { handleBackPress() }
        })

        // Date Constraints
        val availableFrom = intent.getStringExtra("EXTRA_FROM")
        val availableUntil = intent.getStringExtra("EXTRA_UNTIL")
        val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.US)

        try {
            availableFrom?.let {
                val cal = Calendar.getInstance().apply { time = sdf.parse(it)!! }
                minDateMillis = cal.timeInMillis
            }
            availableUntil?.let {
                val cal = Calendar.getInstance().apply { time = sdf.parse(it)!! }
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                maxDateMillis = cal.timeInMillis
            }
        } catch (e: Exception) {
            minDateMillis = 0
            maxDateMillis = Long.MAX_VALUE
        }

        if (isEditMode) {
            toolbar.title = sectionTitle ?: "Edit Resources"
            btnNext.text = "SAVE"
            btnAddDay.visibility = View.GONE

            etContent.setText(intent.getStringExtra("EXTRA_STEP2_CONTENT") ?: "")
            addDayCard()
            prefillSingleCardIfEditing()

        } else {
            toolbar.title = "Daily Lesson Log"
            addDayCard()
        }

        // 5. Listeners
        btnAddDay.setOnClickListener { addDayCard() }

        btnNext.setOnClickListener {
            if (validateFormForSave()) {
                if (isEditMode) {
                    updateResourcesInSupabase()
                } else {
                    validateAndProceed()
                }
            }
        }
    }

    private fun prefillSingleCardIfEditing() {
        val manager = dayManagers.firstOrNull() ?: return
        val prefillDate = intent.getStringExtra("EXTRA_ENTRY_DATE") ?: ""
        val prefillResourcesArray = intent.getStringArrayExtra("EXTRA_PREFILL_RESOURCES_ARRAY")

        manager.etDate.setText(prefillDate)

        // strictly disable date editing because references belong to a fixed daily entry
        manager.etDate.isEnabled = false
        manager.etDate.isFocusable = false
        manager.etDate.isClickable = false
        manager.btnRemove.visibility = View.GONE

        if (prefillResourcesArray != null && prefillResourcesArray.isNotEmpty()) {
            manager.resourcesContainer.removeAllViews()
            manager.resourceInputs.clear()

            // Re-split lines if they were aggregated
            val lines = prefillResourcesArray.flatMap { it.split("\n") }
            lines.forEach { text ->
                if (text.isNotBlank()) manager.addResourceLine(text)
            }
        }
    }

    private fun validateFormForSave(): Boolean {
        var isValid = true
        val errorMsg = "Please enter the needed details"

        UIUtils.errorDisplay(this, tilContent, etContent, true, errorMsg)
        if (etContent.text.toString().trim().isEmpty()) isValid = false

        dayManagers.forEach { manager ->
            if (!manager.isValid()) isValid = false
        }
        return isValid
    }

    // 🌟 SUPABASE UPDATE LOGIC (Strictly Updates) 🌟
    private fun updateResourcesInSupabase() {
        if (dailyEntryId == -1 || originalReferenceId == -1) {
            Toast.makeText(this, "Error: Required IDs are missing.", Toast.LENGTH_LONG).show()
            return
        }

        btnNext.isEnabled = false
        btnNext.text = "Saving..."

        val firstDayManager = dayManagers.firstOrNull() ?: return
        val contentTitle = etContent.text.toString().trim()

        // Aggregate all text inputs into one string separated by newline
        val aggregatedResourceText = firstDayManager.resourceInputs
            .mapNotNull { it.editText?.text?.toString()?.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(separator = "\n")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Update Content Standard in dll_daily_entry
                SupabaseService.client.from("dll_daily_entry").update({
                    set("content_standard", contentTitle)
                }) {
                    filter { eq("id", dailyEntryId) }
                }

                // 2. Update the existing record in dll_references
                SupabaseService.client.from("dll_references").update({
                    set("reference_title", contentTitle)
                    set("reference_text", aggregatedResourceText)
                }) {
                    filter { eq("id", originalReferenceId) }
                }

                withContext(Dispatchers.Main) {
                    ShowToast.showMessage(this@DLLStep2Activity, "Updated Successfully!")
                    setResult(RESULT_OK)
                    finish()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnNext.isEnabled = true
                    btnNext.text = "SAVE"
                    ShowToast.showMessage(this@DLLStep2Activity, "Update Failed: ${e.message}")
                }
            }
        }
    }

    private fun handleBackPress() {
        var hasUnsavedData = false

        if (etContent.text.toString().trim().isNotEmpty()) {
            hasUnsavedData = true
        }

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

    private fun addDayCard() {
        val manager = DayCardManager(this, daysContainer, minDateMillis, maxDateMillis) { managerToRemove ->
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

            intent.putExtra("EXTRA_QUARTER", getIntent().getStringExtra("EXTRA_QUARTER"))
            intent.putExtra("EXTRA_WEEK", getIntent().getStringExtra("EXTRA_WEEK"))
            intent.putExtra("EXTRA_FROM", getIntent().getStringExtra("EXTRA_FROM"))
            intent.putExtra("EXTRA_UNTIL", getIntent().getStringExtra("EXTRA_UNTIL"))

            intent.putExtra("EXTRA_CONTENT_STD", getIntent().getStringExtra("EXTRA_CONTENT_STD"))
            intent.putExtra("EXTRA_PERF_STD", getIntent().getStringExtra("EXTRA_PERF_STD"))
            intent.putExtra("EXTRA_COMPETENCIES", getIntent().getStringExtra("EXTRA_COMPETENCIES"))
            intent.putExtra("EXTRA_STEP2_CONTENT", etContent.text.toString())

            intent.putExtra("EXTRA_DAYS_COUNT", dayManagers.size)

            dayManagers.forEachIndexed { index, manager ->
                val dayIndex = index + 1
                intent.putExtra("EXTRA_DAY_${dayIndex}_DATE", manager.etDate.text.toString())

                val resources = manager.resourceInputs.map { it.editText?.text.toString() }
                intent.putExtra("EXTRA_DAY_${dayIndex}_RESOURCES", resources.toTypedArray())
            }

            startActivity(intent)
        }
    }

    // --- INNER CLASS ---
    inner class DayCardManager(
        val context: Context,
        val container: LinearLayout,
        private val minDateMillis: Long,
        private val maxDateMillis: Long,
        val onRemove: (DayCardManager) -> Unit
    ) {
        val view: View = LayoutInflater.from(context).inflate(R.layout.item_dll_day_card, container, false)

        val etDate: EditText = view.findViewById(R.id.etDate)
        val tilDate: TextInputLayout = view.findViewById(R.id.tilDate)
        val resourcesContainer: LinearLayout = view.findViewById(R.id.resourcesContainer)
        val btnRemove: ImageButton = view.findViewById(R.id.btnRemoveDay)
        val btnAddResource: Button = view.findViewById(R.id.btnAddResource)

        val resourceInputs = mutableListOf<TextInputLayout>()

        init {
            container.addView(view)
            etDate.setText("")
            addResourceLine()

            etDate.setOnClickListener {
                if (!isEditMode) {
                    val calendar = Calendar.getInstance()
                    val datePicker = DatePickerDialog(context, { _, year, month, day ->
                        val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.US)
                        val selectedCalendar = Calendar.getInstance()
                        selectedCalendar.set(year, month, day, 0, 0, 0)
                        selectedCalendar.set(Calendar.MILLISECOND, 0)

                        if (selectedCalendar.timeInMillis < minDateMillis || selectedCalendar.timeInMillis > maxDateMillis) {
                            Toast.makeText(context, "Date must be between the available period.", Toast.LENGTH_LONG).show()
                            etDate.setText("")
                            tilDate.error = "Out of range"
                            tilDate.isErrorEnabled = true
                        } else {
                            etDate.setText(sdf.format(selectedCalendar.time))
                            tilDate.error = null
                            tilDate.isErrorEnabled = false
                        }
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

                    datePicker.datePicker.minDate = minDateMillis
                    datePicker.datePicker.maxDate = maxDateMillis
                    datePicker.show()
                }
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

        fun addResourceLine(prefillText: String = "") {
            val resourceView = LayoutInflater.from(context).inflate(R.layout.item_dll_resource_input, resourcesContainer, false)
            val til = resourceView.findViewById<TextInputLayout>(R.id.tilResourceItem)
            val btnRemoveRes = resourceView.findViewById<ImageButton>(R.id.btnRemoveResource)

            til.editText?.setText(prefillText)

            resourcesContainer.addView(resourceView)
            resourceInputs.add(til)

            btnRemoveRes.setOnClickListener {
                resourcesContainer.removeView(resourceView)
                resourceInputs.remove(til)
            }
        }

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