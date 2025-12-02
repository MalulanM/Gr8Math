package com.example.gr8math

import CreateDllRequest
import DllProcedure
import DllReference
import DllReflection
import android.app.Activity
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
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.CurrentCourse
import com.example.gr8math.dataObject.UpdateReflectionRequest
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

class DLLStep4Activity : AppCompatActivity() {

    private lateinit var reflectionDaysContainer: LinearLayout
    private lateinit var btnAddDay: Button
    private lateinit var btnSave: Button

    private var isEditMode = false
    private var dllMainId: Int = -1
    private var sectionTitle: String? = null
    private var originalReflectionId: Int = -1

    lateinit var loadingLayout: View
    lateinit var loadingProgress: View
    lateinit var loadingText: TextView
    private var minDateMillis: Long = 0
    private var maxDateMillis: Long = Long.MAX_VALUE
    private val dayManagers = mutableListOf<ReflectionDayManager>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dll_step4)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        loadingLayout = findViewById(R.id.loadingLayout)
        loadingProgress = findViewById(R.id.loadingProgressBg)
        loadingText = findViewById(R.id.loadingText)
        reflectionDaysContainer = findViewById(R.id.reflectionDaysContainer)
        btnAddDay = findViewById(R.id.btnAddDay)
        btnSave = findViewById(R.id.btnSave)
        // 🌟 RETRIEVE EDIT INFO
        isEditMode = intent.getBooleanExtra(DLLEditActivity.EXTRA_MODE_EDIT, false)
        dllMainId = intent.getIntExtra(DLLEditActivity.EXTRA_DLL_MAIN_ID, -1)
        sectionTitle = intent.getStringExtra(DLLEditActivity.EXTRA_SECTION_TITLE)
        originalReflectionId = intent.getIntExtra(DLLEditActivity.KEY_RECORD_ID, -1)


        // 1. Setup Back Navigation
        toolbar.setNavigationOnClickListener { handleBackPress() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { handleBackPress() }
        })

        // --- Date Retrieval and Conversion (ADDED) ---
        val availableFrom = intent.getStringExtra("EXTRA_FROM")
        val availableUntil = intent.getStringExtra("EXTRA_UNTIL")

        // ⚠️ Assuming format is "MM/dd/yy" from the initial dialog
        val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.US)

        try {
            availableFrom?.let {
                val cal = Calendar.getInstance().apply { time = sdf.parse(it)!! }
                minDateMillis = cal.timeInMillis
            }

            availableUntil?.let {
                val cal = Calendar.getInstance().apply { time = sdf.parse(it)!! }
                // Set to end of day (23:59:59) for inclusive check
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                maxDateMillis = cal.timeInMillis
            }
        } catch (e: Exception) {
            minDateMillis = 0
            maxDateMillis = Long.MAX_VALUE
            Toast.makeText(this, "Date boundary initialization failed.", Toast.LENGTH_SHORT).show()
        }

        // 🌟 EDIT MODE UI SETUP AND PREFILL
        if (isEditMode) {
            toolbar.title = sectionTitle ?: "Edit Daily Lesson Log"
            btnAddDay.visibility = View.GONE
            btnSave.text = "SAVE" // Change the default SAVE button text

            addDay() // Add the one card we intend to edit
            prefillSingleCardIfEditing() // Populate it with Intent data
        } else {
            toolbar.title = "Daily Lesson Log"
            addDay()
        }

        btnAddDay.setOnClickListener { addDay() }

        btnSave.setOnClickListener {
            if (validateForms()) {
                if (isEditMode) {
                    callReflectionUpdateApi()
                } else {
                    showReviewDialog() // Existing creation flow
                }
            }
        }
    }

    private fun prefillSingleCardIfEditing() {
        val manager = dayManagers.firstOrNull() ?: return

        // Retrieve fields passed by DLLReflectionFragment
        manager.etDate.setText(intent.getStringExtra(DLLEditActivity.KEY_DATE) ?: "")
        manager.etReview.setText(intent.getStringExtra("Remarks") ?: "")
        manager.etReflection.setText(intent.getStringExtra("Reflection") ?: "")
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
                submitDLL()
            }
            .create()

        dialog.show()

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
    }

    private fun addDay() {
        val manager = ReflectionDayManager(this, reflectionDaysContainer, minDateMillis, maxDateMillis) { managerToRemove ->
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
         val context: Context,
        container: LinearLayout,
         private val minDateMillis: Long,
         private val maxDateMillis: Long,
         val onRemove: (ReflectionDayManager) -> Unit
    ) {
        val view: View = LayoutInflater.from(context).inflate(R.layout.item_dll_reflection_card, container, false)

         val etDate: EditText = view.findViewById(R.id.etDate)
         val tilDate: TextInputLayout = view.findViewById(R.id.tilDate)
         val btnRemove: ImageButton = view.findViewById(R.id.btnRemoveDay)

         val etReview: EditText = view.findViewById(R.id.etReview)
         val tilReview: TextInputLayout = view.findViewById(R.id.tilReview)

         val etReflection: EditText = view.findViewById(R.id.etReflection)
         val tilReflection: TextInputLayout = view.findViewById(R.id.tilReflection)

        init {
            container.addView(view)
            etDate.setText("") // Clear default

            // Date Picker
            etDate.setOnClickListener {
                val calendar = Calendar.getInstance()

                val datePicker = DatePickerDialog(context, { _, year, month, day ->
                    val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.US)

                    val selectedCalendar = Calendar.getInstance()
                    selectedCalendar.set(year, month, day)
                    // Reset selected calendar time to 00:00:00 for accurate comparison
                    selectedCalendar.set(Calendar.HOUR_OF_DAY, 0)
                    selectedCalendar.set(Calendar.MINUTE, 0)
                    selectedCalendar.set(Calendar.SECOND, 0)
                    selectedCalendar.set(Calendar.MILLISECOND, 0)

                    // Manual Validation for toast message
                    if (selectedCalendar.timeInMillis < minDateMillis || selectedCalendar.timeInMillis > maxDateMillis) {
                        Toast.makeText(context, "Date must be between the available period.", Toast.LENGTH_LONG).show()
                        etDate.setText("")
                        tilDate.error = "Out of range"
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

    private fun callReflectionUpdateApi() {
        if (originalReflectionId == -1) {
            Toast.makeText(this, "Error: Cannot save, Reflection ID missing.", Toast.LENGTH_LONG).show()
            return
        }

        val manager = dayManagers.firstOrNull()
        if (manager == null) return

        val request = UpdateReflectionRequest(
            id = originalReflectionId,
            date = manager.etDate.text.toString(),
            remark = manager.etReview.text.toString(),
            reflection = manager.etReflection.text.toString()
        )

        // 2. Call the API
        ConnectURL.api.updateReflection(request).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: retrofit2.Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    ShowToast.showMessage(this@DLLStep4Activity, "Updated Successfully!")
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    ShowToast.showMessage(this@DLLStep4Activity, "Update Failed")
                }
            }
            override fun onFailure(call: retrofit2.Call<ResponseBody>, t: Throwable) {
                ShowToast.showMessage(this@DLLStep4Activity, "Failed to connect to server. Check your internet connection.")
            }
        })
    }

    private fun submitDLL() {
        val intent = intent

        // 1. Gather Step 1 (Main DLL) Data
        val mainData = mapOf(
            "quarter_number" to intent.getStringExtra("EXTRA_QUARTER").orEmpty(),
            "week_number" to intent.getStringExtra("EXTRA_WEEK").orEmpty(),
            "available_from" to intent.getStringExtra("EXTRA_FROM").orEmpty(),
            "available_until" to intent.getStringExtra("EXTRA_UNTIL").orEmpty(),
            "content_standard" to intent.getStringExtra("EXTRA_CONTENT_STD").orEmpty(),
            "performance_standard" to intent.getStringExtra("EXTRA_PERF_STD").orEmpty(),
            "learning_comp" to intent.getStringExtra("EXTRA_COMPETENCIES").orEmpty()
        )

        // 2. Gather Step 2 (Reference) Data
        val step2ContentTitle = intent.getStringExtra("EXTRA_STEP2_CONTENT").orEmpty()
        val references = mutableListOf<DllReference>()
        val daysCount = intent.getIntExtra("EXTRA_DAYS_COUNT", 0)
        for (i in 1..daysCount) {
            val date = intent.getStringExtra("EXTRA_DAY_${i}_DATE").orEmpty()
            val resources = intent.getStringArrayExtra("EXTRA_DAY_${i}_RESOURCES")?.toList() ?: emptyList()

            // Each resource line becomes a separate DllReference entry
            resources.forEachIndexed { resIndex, resourceText ->
                if (resourceText.trim().isNotEmpty()) {
                    references.add(
                        DllReference(
                            date = date,
                            reference_title = step2ContentTitle, // Simple title
                            reference_text = resourceText
                        )
                    )
                }
            }
        }

        // 3. Gather Step 3 (Procedure) Data
        val procedures = mutableListOf<DllProcedure>()
        val procCount = intent.getIntExtra("EXTRA_PROC_COUNT", 0)
        for (i in 1..procCount) {
            procedures.add(DllProcedure(
                date = intent.getStringExtra("EXTRA_PROC_${i}_DATE").orEmpty(),
                review = intent.getStringExtra("EXTRA_PROC_${i}_REVIEW").orEmpty(),
                purpose = intent.getStringExtra("EXTRA_PROC_${i}_PURPOSE").orEmpty(),
                example = intent.getStringExtra("EXTRA_PROC_${i}_EXAMPLE").orEmpty(),
                discussion_proper = intent.getStringExtra("EXTRA_PROC_${i}_DISCUSSION").orEmpty(),
                developing_mastery = intent.getStringExtra("EXTRA_PROC_${i}_MASTERY").orEmpty(),
                application = intent.getStringExtra("EXTRA_PROC_${i}_APPLICATION").orEmpty(),
                generalization = intent.getStringExtra("EXTRA_PROC_${i}_GENERALIZATION").orEmpty(),
                evaluation = intent.getStringExtra("EXTRA_PROC_${i}_EVALUATION").orEmpty(),
                additional_act = intent.getStringExtra("EXTRA_PROC_${i}_ADDITIONAL").orEmpty()
            ))
        }

        // 4. Gather Step 4 (Reflection) Data
        val reflections = dayManagers.map { manager ->
            DllReflection(
                date = manager.etDate.text.toString(),
                remark = manager.etReview.text.toString(),
                reflection = manager.etReflection.text.toString()
            )
        }.filter { it.date.isNotEmpty() } // Filter out empty cards

        // 5. Construct the final request body object
        val requestBody = CreateDllRequest(
            course_id = CurrentCourse.courseId, // ⚠️ REMEMBER TO REPLACE THIS
            quarter_number = mainData["quarter_number"]!!,
            week_number = mainData["week_number"]!!,
            available_from = mainData["available_from"]!!,
            available_until = mainData["available_until"]!!,
            content_standard = mainData["content_standard"]!!,
            performance_standard = mainData["performance_standard"]!!,
            learning_comp = mainData["learning_comp"]!!,
            dll_reference = references,
            dll_procedure = procedures,
            dll_reflection = reflections
        )

        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)

        // 6. Execute API Call
        // Assuming your ConnectURL.api is the Retrofit instance
        ConnectURL.api.createDll(requestBody).enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: retrofit2.Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                if (response.isSuccessful) {
                    ShowToast.showMessage(this@DLLStep4Activity, "DLL Saved Successfully!")
                    val successIntent = Intent(this@DLLStep4Activity, TeacherClassPageActivity::class.java)
                    successIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    startActivity(successIntent)
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Toast.makeText(this@DLLStep4Activity, "API Error: ${response.code()} - $errorBody", Toast.LENGTH_LONG).show()
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)

                }
            }

            override fun onFailure(call: retrofit2.Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@DLLStep4Activity, "Network Failure: ${t.message}", Toast.LENGTH_LONG).show()
                t.printStackTrace()
                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)

            }
        })
    }
}