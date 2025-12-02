package com.example.gr8math

import android.app.Activity
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
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.UpdateReferenceRequest
import com.example.gr8math.utils.ShowToast
import com.example.gr8math.utils.UIUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import okhttp3.ResponseBody
import retrofit2.Callback
import retrofit2.Response
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
    private var sectionTitle: String? = null
    private var originalReferenceId: Int = -1
    // List to keep track of all active day cards
    private val dayManagers = mutableListOf<DayCardManager>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dll_step2)

        isEditMode = intent.getBooleanExtra(DLLEditActivity.EXTRA_MODE_EDIT, false)
        dllMainId = intent.getIntExtra(DLLEditActivity.EXTRA_DLL_MAIN_ID, -1)
        sectionTitle = intent.getStringExtra(DLLEditActivity.EXTRA_SECTION_TITLE)
        originalReferenceId = intent.getIntExtra(DLLEditActivity.KEY_RECORD_ID, -1)

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

        val availableFrom = intent.getStringExtra("EXTRA_FROM")
        val availableUntil = intent.getStringExtra("EXTRA_UNTIL")

        // ⚠️ NOTE: Assuming date format "MM/dd/yy" from TeacherClassPageActivity
        val sdf = SimpleDateFormat("MM/dd/yy", Locale.US)

        try {
            availableFrom?.let {
                val cal = Calendar.getInstance().apply { time = sdf.parse(it)!! }
                minDateMillis = cal.timeInMillis
            }

            availableUntil?.let {
                val cal = Calendar.getInstance().apply { time = sdf.parse(it)!! }
                // Set to end of day to include the entire day
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                maxDateMillis = cal.timeInMillis
            }
        } catch (e: Exception) {
            // Log or handle parsing error if the format is wrong
            minDateMillis = 0
            maxDateMillis = Long.MAX_VALUE
        }


        if (isEditMode) {
            toolbar.title = sectionTitle ?: "Edit Daily Lesson Log"
            btnNext.text = "SAVE"
            btnAddDay.visibility = View.GONE

            // 1. Prefill main content field (Title of the Resources)
            etContent.setText(intent.getStringExtra("EXTRA_STEP2_CONTENT") ?: "")

            // 2. Add the single card for editing
            addDayCard()
            prefillSingleCardIfEditing() // Populate the single card

        } else {
            toolbar.title = "Daily Lesson Log"
            addDayCard()
        }

        // 5. Listeners
        btnAddDay.setOnClickListener {
            addDayCard()
        }

        btnNext.setOnClickListener {
            if (validateFormForSave()) {
                if (isEditMode) {
                    callResourcesUpdateApi(dllMainId)
                } else {
                    validateAndProceed()
                }
            }
        }
    }

    private fun prefillSingleCardIfEditing() {
        val manager = dayManagers.firstOrNull() ?: return

        // Retrieve data passed by DLLResourcesFragment
        val prefillDate = intent.getStringExtra("EXTRA_PREFILL_DATE") ?: ""
        val prefillResourcesArray = intent.getStringArrayExtra("EXTRA_PREFILL_RESOURCES_ARRAY")

        manager.etDate.setText(prefillDate)

        if (prefillResourcesArray != null && prefillResourcesArray.isNotEmpty()) {
            // Clear the default single line added by addDayCard()
            manager.resourcesContainer.removeAllViews()
            manager.resourceInputs.clear()

            // Add lines for each resource text
            prefillResourcesArray.forEach { text ->
                manager.addResourceLine(text)
            }
        }
    }

    // Helper function for unified validation (validates all current cards/fields)
    private fun validateFormForSave(): Boolean {
        var isValid = true
        val errorMsg = "Please enter the needed details"

        UIUtils.errorDisplay(this, tilContent, etContent, true, errorMsg)
        if (etContent.text.toString().trim().isEmpty()) isValid = false

        // Validate ALL cards/content
        dayManagers.forEach { manager ->
            if (!manager.isValid()) isValid = false
        }
        return isValid
    }


    private fun callResourcesUpdateApi(mainId: Int) {
        if (originalReferenceId == -1) {
            Toast.makeText(this, "Error: Cannot save, Reference ID missing.", Toast.LENGTH_LONG).show()
            return
        }

        // 1. GATHER DATA (We only update the first original reference record)
        val firstDayManager = dayManagers.firstOrNull()
        if (firstDayManager == null) return

        val aggregatedResourceText = firstDayManager.resourceInputs
            .map { it.editText?.text.toString() }
            .joinToString(separator = "\n")

        val request = UpdateReferenceRequest(
            id = originalReferenceId,
            date = firstDayManager.etDate.text.toString(),
            reference_title = etContent.text.toString(), // Use Step 2 Content field as title
            reference_text = aggregatedResourceText
        )

        // 2. Call the API
        ConnectURL.api.updateReference(request).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: retrofit2.Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    ShowToast.showMessage(this@DLLStep2Activity, "Updated Successfully!")
                    setResult(Activity.RESULT_OK) // Notify fragment to refresh
                    finish()
                } else {
                    ShowToast.showMessage(this@DLLStep2Activity, "Update Failed")
                }
            }
            override fun onFailure(call: retrofit2.Call<ResponseBody>, t: Throwable) {
                ShowToast.showMessage(this@DLLStep2Activity, "Failed to connect to server. Check your internet connection.")
            }
        })
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

// PASS previous data received from Step 1
            intent.putExtra("EXTRA_QUARTER", getIntent().getStringExtra("EXTRA_QUARTER"))
            intent.putExtra("EXTRA_WEEK", getIntent().getStringExtra("EXTRA_WEEK"))
            intent.putExtra("EXTRA_FROM", getIntent().getStringExtra("EXTRA_FROM"))
            intent.putExtra("EXTRA_UNTIL", getIntent().getStringExtra("EXTRA_UNTIL"))

            intent.putExtra("EXTRA_CONTENT_STD", getIntent().getStringExtra("EXTRA_CONTENT_STD"))
            intent.putExtra("EXTRA_PERF_STD", getIntent().getStringExtra("EXTRA_PERF_STD"))
            intent.putExtra("EXTRA_COMPETENCIES", getIntent().getStringExtra("EXTRA_COMPETENCIES"))
            intent.putExtra("EXTRA_STEP2_CONTENT", etContent.text.toString())

// SEND Step 2 INNER DATA (DAYS)
            intent.putExtra("EXTRA_DAYS_COUNT", dayManagers.size)

// Loop every day card
            dayManagers.forEachIndexed { index, manager ->
                val dayIndex = index + 1
                intent.putExtra("EXTRA_DAY_${dayIndex}_DATE", manager.etDate.text.toString())

                // RESOURCES per day
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

            // Clear default XML text to check for "Unsaved Data" accurately
            etDate.setText("")

            addResourceLine()

            etDate.setOnClickListener {
                val calendar = Calendar.getInstance()

                val datePicker = DatePickerDialog(context, { _, year, month, day ->
                    val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.US)
                    val selectedCalendar = Calendar.getInstance()

                    // Reset selected calendar time to 00:00:00 for accurate comparison
                    selectedCalendar.set(year, month, day)
                    selectedCalendar.set(Calendar.HOUR_OF_DAY, 0)
                    selectedCalendar.set(Calendar.MINUTE, 0)
                    selectedCalendar.set(Calendar.SECOND, 0)
                    selectedCalendar.set(Calendar.MILLISECOND, 0)

                    // 🌟 Validation: Check if selected date is within bounds
                    // Note: minDateMillis and maxDateMillis should already be set accurately
                    // but we add a manual check for robustness and error messaging.
                    if (selectedCalendar.timeInMillis < minDateMillis || selectedCalendar.timeInMillis > maxDateMillis) {
                        Toast.makeText(context, "Date must be between the available period.", Toast.LENGTH_LONG).show()
                        etDate.setText("") // Clear invalid selection
                        tilDate.error = "Out of range"
                        tilDate.isErrorEnabled = true
                    } else {
                        etDate.setText(sdf.format(selectedCalendar.time))
                        tilDate.error = null
                        tilDate.isErrorEnabled = false
                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

                // 🌟 Constraint the Date Picker
                datePicker.datePicker.minDate = minDateMillis
                datePicker.datePicker.maxDate = maxDateMillis

                datePicker.show()
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

            // 🌟 Set the text input
            til.editText?.setText(prefillText)

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