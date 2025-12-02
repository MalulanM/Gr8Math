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
import com.example.gr8math.DLLEditActivity.Companion.KEY_DATE
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.UpdateProcedureRequest
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

class DLLStep3Activity : AppCompatActivity() {

    private lateinit var procedureDaysContainer: LinearLayout
    private lateinit var btnAddDay: Button
    private lateinit var btnNext: Button

    private var availableFrom: String? = null
    private var availableUntil: String? = null

    // 🌟 NEW: Properties to store the bounds as Calendar milliseconds
    private var minDateMillis: Long = 0
    private var maxDateMillis: Long = Long.MAX_VALUE
    private var isEditMode = false
    private var dllMainId: Int = -1
    private var sectionTitle: String? = null
    private var originalProcedureId: Int = -1
    // List to track dynamic cards
    private val dayManagers = mutableListOf<ProcedureDayManager>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dll_step3)
        // 🌟 RETRIEVE EDIT INFO
        isEditMode = intent.getBooleanExtra(DLLEditActivity.EXTRA_MODE_EDIT, false)
        dllMainId = intent.getIntExtra(DLLEditActivity.EXTRA_DLL_MAIN_ID, -1)
        sectionTitle = intent.getStringExtra(DLLEditActivity.EXTRA_SECTION_TITLE)

        // Retrieve the specific record ID passed from the fragment (DLLProceduresFragment)
        originalProcedureId = intent.getIntExtra(DLLEditActivity.KEY_RECORD_ID, -1)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        procedureDaysContainer = findViewById(R.id.procedureDaysContainer)
        btnAddDay = findViewById(R.id.btnAddDay)
        btnNext = findViewById(R.id.btnNext)

        // 1. Setup Back Navigation (Toolbar)
        toolbar.setNavigationOnClickListener { handleBackPress() }

        // 2. Setup System Back Navigation
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })

        availableFrom = intent.getStringExtra("EXTRA_FROM")
        availableUntil = intent.getStringExtra("EXTRA_UNTIL")

        val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.US) // Assuming this is the format used in TeacherClassPageActivity

        try {
            // Convert 'From' date string to milliseconds (for min date)
            availableFrom?.let {
                val cal = Calendar.getInstance().apply { time = sdf.parse(it)!! }
                minDateMillis = cal.timeInMillis
            }

            // Convert 'Until' date string to milliseconds (for max date)
            availableUntil?.let {
                val cal = Calendar.getInstance().apply { time = sdf.parse(it)!! }
                // Set to end of day to include the entire day
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                maxDateMillis = cal.timeInMillis
            }
        } catch (e: Exception) {
            // Log or handle parsing error if the format is wrong
            minDateMillis = 0
            maxDateMillis = Long.MAX_VALUE
            Toast.makeText(this, "Date format error in DLL bounds.", Toast.LENGTH_SHORT).show()
        }

        if (isEditMode) {
            toolbar.title = sectionTitle ?: "Edit Daily Lesson Log"
            btnNext.text = "SAVE"
            btnAddDay.visibility = View.GONE
            addDay()
            prefillSingleCardIfEditing()
        } else {
            addDay()
        }



        // 4. Listeners
        btnAddDay.setOnClickListener {
            addDay()
        }

        btnNext.setOnClickListener {
            if (validateFormForSave()) {
                if (isEditMode) {
                    callProceduresUpdateApi()
                } else {
                    validateAndProceed()
                }
            }
        }


        val quarter = intent.getStringExtra("EXTRA_QUARTER")
        val week = intent.getStringExtra("EXTRA_WEEK")
        val from = intent.getStringExtra("EXTRA_FROM")
        val until = intent.getStringExtra("EXTRA_UNTIL")

        val contentStd = intent.getStringExtra("EXTRA_CONTENT_STD")
        val perfStd = intent.getStringExtra("EXTRA_PERF_STD")
        val competencies = intent.getStringExtra("EXTRA_COMPETENCIES")

        val daysCount = intent.getIntExtra("EXTRA_DAYS_COUNT", 0)

        val dayData = mutableListOf<Pair<String, List<String>>>()

        for (i in 1..daysCount) {
            val date = intent.getStringExtra("EXTRA_DAY_${i}_DATE") ?: ""
            val resources =
                intent.getStringArrayExtra("EXTRA_DAY_${i}_RESOURCES")?.toList() ?: emptyList()
            dayData.add(Pair(date, resources))
        }

    }

    private fun prefillSingleCardIfEditing() {
        // This relies on the fragment (DLLProceduresFragment) passing all fields as direct extras.
        val manager = dayManagers.firstOrNull() ?: return

        // ⚠️ NOTE: The keys must exactly match the labels used in DLLProceduresFragment.kt's editMap
        manager.etDate.setText(intent.getStringExtra(KEY_DATE))
        manager.etReview.setText(intent.getStringExtra("Review"))
        manager.etPurpose.setText(intent.getStringExtra("Purpose"))
        manager.etExample.setText(intent.getStringExtra("Example"))
        manager.etDiscussion1.setText(intent.getStringExtra("Discussion Proper"))
        manager.etMastery.setText(intent.getStringExtra("Developing Mastery"))
        manager.etApplication.setText(intent.getStringExtra("Application"))
        manager.etGeneralization.setText(intent.getStringExtra("Generalization"))
        manager.etEvaluation.setText(intent.getStringExtra("Evaluation"))
        manager.etAdditional.setText(intent.getStringExtra("Additional Activities"))
    }

    private fun validateFormForSave(): Boolean {
        // Reuse existing validation logic, check only the mandatory single card
        return dayManagers.firstOrNull()?.isValid() ?: false
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
            try {
                typeface = ResourcesCompat.getFont(context, R.font.lexend)
            } catch (_: Exception) {
            }
        }

        // Message: Normal, Left Aligned
        val messageView = TextView(this).apply {
            text = "You have unsaved content. If you go\nback, your changes will be lost."
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.colorSubtleText))
            setPadding(70, 10, 50, 20)
            try {
                typeface = ResourcesCompat.getFont(context, R.font.lexend)
            } catch (_: Exception) {
            }
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
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            ?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)
            ?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
    }

    private fun addDay() {
        val manager = ProcedureDayManager(this, procedureDaysContainer, minDateMillis, maxDateMillis) { managerToRemove ->
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
            val intent = Intent(this, DLLStep4Activity::class.java)

            intent.putExtras(getIntent())

            intent.putExtra("EXTRA_PROC_COUNT", dayManagers.size)

            dayManagers.forEachIndexed { index, manager ->
                val procIndex = index + 1

                // ⬇️ Pass ALL individual fields needed by the PHP database
                intent.putExtra("EXTRA_PROC_${procIndex}_DATE", manager.getProcedureDate())
                intent.putExtra("EXTRA_PROC_${procIndex}_REVIEW", manager.etReview.text.toString())
                intent.putExtra("EXTRA_PROC_${procIndex}_PURPOSE", manager.etPurpose.text.toString())
                intent.putExtra("EXTRA_PROC_${procIndex}_EXAMPLE", manager.etExample.text.toString())
                intent.putExtra("EXTRA_PROC_${procIndex}_DISCUSSION", manager.etDiscussion1.text.toString())
                intent.putExtra("EXTRA_PROC_${procIndex}_MASTERY", manager.etMastery.text.toString())
                intent.putExtra("EXTRA_PROC_${procIndex}_APPLICATION", manager.etApplication.text.toString())
                intent.putExtra("EXTRA_PROC_${procIndex}_GENERALIZATION", manager.etGeneralization.text.toString())
                intent.putExtra("EXTRA_PROC_${procIndex}_EVALUATION", manager.etEvaluation.text.toString())
                intent.putExtra("EXTRA_PROC_${procIndex}_ADDITIONAL", manager.etAdditional.text.toString())
            }

            startActivity(intent)
        }
    }


    private fun callProceduresUpdateApi() {
        if (originalProcedureId == -1) {
            Toast.makeText(this, "Error: Cannot save, Procedure ID missing.", Toast.LENGTH_LONG).show()
            return
        }

        // Since we are in single-step mode, we only deal with the first card.
        val manager = dayManagers.firstOrNull()
        if (manager == null) {
            Toast.makeText(this, "Validation failed: No data card.", Toast.LENGTH_SHORT).show()
            return
        }

        val request = UpdateProcedureRequest(
            id = originalProcedureId,
            date = manager.getProcedureDate(),
            review = manager.etReview.text.toString(),
            purpose = manager.etPurpose.text.toString(),
            example = manager.etExample.text.toString(),
            discussion_proper = manager.etDiscussion1.text.toString(),
            developing_mastery = manager.etMastery.text.toString(),
            application = manager.etApplication.text.toString(),
            generalization = manager.etGeneralization.text.toString(),
            evaluation = manager.etEvaluation.text.toString(),
            additional_act = manager.etAdditional.text.toString()
        )

        // 2. Call the API
        ConnectURL.api.updateProcedure(request).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: retrofit2.Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    ShowToast.showMessage(this@DLLStep3Activity, "Updated Successfully!")
                    setResult(Activity.RESULT_OK) // Notify fragment to refresh
                    finish()
                } else {
                    ShowToast.showMessage(this@DLLStep3Activity, "Update Failed")
                }
            }
            override fun onFailure(call: retrofit2.Call<ResponseBody>, t: Throwable) {
                ShowToast.showMessage(this@DLLStep3Activity, "Failed to connect to server. Check your internet connection.")
            }
        })
    }

    inner class ProcedureDayManager(
        private val context: Context,
        container: LinearLayout,

        private val minDateMillis: Long,
        private val maxDateMillis: Long,
        private val onRemove: (ProcedureDayManager)-> Unit
    ) {

        val view: View = LayoutInflater.from(context)
            .inflate(R.layout.item_dll_procedure_card, container, false)

        // Date UI
         val etDate: EditText = view.findViewById(R.id.etDate)
         val tilDate: TextInputLayout = view.findViewById(R.id.tilDate)
         val btnRemove: ImageButton = view.findViewById(R.id.btnRemoveDay)

        // Fields (Review, Purpose, Example, etc.)
         val etReview: EditText = view.findViewById(R.id.etReview)
         val tilReview: TextInputLayout = view.findViewById(R.id.tilReview)

         val etPurpose: EditText = view.findViewById(R.id.etPurpose)
         val tilPurpose: TextInputLayout = view.findViewById(R.id.tilPurpose)

         val etExample: EditText = view.findViewById(R.id.etExample)
         val tilExample: TextInputLayout = view.findViewById(R.id.tilExample)

         val etDiscussion1: EditText = view.findViewById(R.id.etDiscussion1)
         val tilDiscussion1: TextInputLayout = view.findViewById(R.id.tilDiscussion1)

         val etMastery: EditText = view.findViewById(R.id.etMastery)
         val tilMastery: TextInputLayout = view.findViewById(R.id.tilMastery)

         val etApplication: EditText = view.findViewById(R.id.etApplication)
         val tilApplication: TextInputLayout = view.findViewById(R.id.tilApplication)

         val etGeneralization: EditText = view.findViewById(R.id.etGeneralization)
         val tilGeneralization: TextInputLayout = view.findViewById(R.id.tilGeneralization)

         val etEvaluation: EditText = view.findViewById(R.id.etEvaluation)
         val tilEvaluation: TextInputLayout = view.findViewById(R.id.tilEvaluation)

         val etAdditional: EditText = view.findViewById(R.id.etAdditional)
         val tilAdditional: TextInputLayout = view.findViewById(R.id.tilAdditional)

        fun getProcedureDate(): String = etDate.text.toString()

        init {
            container.addView(view)
            etDate.setText("")

            // Date Picker
            etDate.setOnClickListener {
                val calendar = Calendar.getInstance()

                val datePicker = DatePickerDialog(context, { _, year, month, day ->
                    val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.US)
                    val selectedCalendar = Calendar.getInstance()
                    selectedCalendar.set(year, month, day)

                    // 🌟 Validation: Check if selected date is within bounds
                    if (selectedCalendar.timeInMillis < minDateMillis || selectedCalendar.timeInMillis > maxDateMillis) {
                        // Show error message
                        Toast.makeText(context, "Date must be between the available period.", Toast.LENGTH_LONG).show()
                        etDate.setText("") // Clear invalid selection
                        tilDate.error = "Out of range"
                    } else {
                        etDate.setText(sdf.format(selectedCalendar.time))
                        tilDate.error = null
                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

                // 🌟 Constraint the Date Picker
                datePicker.datePicker.minDate = minDateMillis
                datePicker.datePicker.maxDate = maxDateMillis

                datePicker.show()
            }

            // Remove Logic
            btnRemove.setOnClickListener { onRemove(this) }
        }

        // Returns TRUE if user typed anything
        fun hasData(): Boolean {
            return etDate.text.isNotEmpty()
                    || etReview.text.isNotEmpty()
                    || etPurpose.text.isNotEmpty()
                    || etExample.text.isNotEmpty()
                    || etDiscussion1.text.isNotEmpty()
                    || etMastery.text.isNotEmpty()
                    || etApplication.text.isNotEmpty()
                    || etGeneralization.text.isNotEmpty()
                    || etEvaluation.text.isNotEmpty()
                    || etAdditional.text.isNotEmpty()
        }

        // Validate required fields
        fun isValid(): Boolean {
            var valid = true
            val errorMsg = "Please enter the needed details"

            UIUtils.errorDisplay(context, tilDate, etDate, true, "Required")
            if (etDate.text.toString().trim().isEmpty()) valid = false

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
                if (et.text.toString().trim().isEmpty()) valid = false
            }

            return valid
        }

        // Build complete procedure text
        fun getProcedureText(): String {
            return listOf(
                "Review: ${etReview.text}",
                "Purpose: ${etPurpose.text}",
                "Example: ${etExample.text}",
                "Discussion: ${etDiscussion1.text}",
                "Mastery: ${etMastery.text}",
                "Application: ${etApplication.text}",
                "Generalization: ${etGeneralization.text}",
                "Evaluation: ${etEvaluation.text}",
                "Additional: ${etAdditional.text}"
            ).joinToString("\n")
        }
    }

}