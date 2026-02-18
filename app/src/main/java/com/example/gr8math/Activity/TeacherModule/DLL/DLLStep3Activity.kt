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

class DLLStep3Activity : AppCompatActivity() {

    private lateinit var procedureDaysContainer: LinearLayout
    private lateinit var btnAddDay: Button
    private lateinit var btnNext: Button

    private var availableFrom: String? = null
    private var availableUntil: String? = null

    private var minDateMillis: Long = 0
    private var maxDateMillis: Long = Long.MAX_VALUE

    private var isEditMode = false
    private var dllMainId: Int = -1
    private var dailyEntryId: Int = -1
    private var sectionTitle: String? = null

    private val dayManagers = mutableListOf<ProcedureDayManager>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dll_step3)


        isEditMode = intent.getBooleanExtra(DLLEditActivity.EXTRA_MODE_EDIT, false)
        dllMainId = intent.getIntExtra(DLLEditActivity.EXTRA_DLL_MAIN_ID, -1)
        dailyEntryId = intent.getIntExtra("EXTRA_DAILY_ENTRY_ID", -1)
        sectionTitle = intent.getStringExtra(DLLEditActivity.EXTRA_SECTION_TITLE)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        procedureDaysContainer = findViewById(R.id.procedureDaysContainer)
        btnAddDay = findViewById(R.id.btnAddDay)
        btnNext = findViewById(R.id.btnNext)


        toolbar.setNavigationOnClickListener { handleBackPress() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })


        availableFrom = intent.getStringExtra("EXTRA_FROM")
        availableUntil = intent.getStringExtra("EXTRA_UNTIL")
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
                maxDateMillis = cal.timeInMillis
            }
        } catch (e: Exception) {
            minDateMillis = 0
            maxDateMillis = Long.MAX_VALUE
        }


        if (isEditMode) {
            toolbar.title = sectionTitle ?: "Edit Procedures"
            btnNext.text = "SAVE"
            btnAddDay.visibility = View.GONE
            addDay()
            prefillSingleCardIfEditing()
        } else {
            toolbar.title = "Daily Lesson Log"
            addDay()
        }

        btnAddDay.setOnClickListener { addDay() }

        btnNext.setOnClickListener {
            if (validateFormForSave()) {
                if (isEditMode) {
                    updateProceduresInSupabase()
                } else {
                    validateAndProceed()
                }
            }
        }
    }

    private fun prefillSingleCardIfEditing() {
        val manager = dayManagers.firstOrNull() ?: return

        manager.etDate.setText(intent.getStringExtra("EXTRA_ENTRY_DATE") ?: "")


        manager.etDate.isEnabled = false
        manager.etDate.isFocusable = false
        manager.etDate.isClickable = false
        manager.btnRemove.visibility = View.GONE

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
        return dayManagers.firstOrNull()?.isValid() ?: false
    }

    private fun updateProceduresInSupabase() {
        if (dailyEntryId == -1) {
            Toast.makeText(this, "Error: Cannot save, Daily Entry ID missing.", Toast.LENGTH_LONG).show()
            return
        }

        val manager = dayManagers.firstOrNull()
        if (manager == null) {
            Toast.makeText(this, "Validation failed: No data card.", Toast.LENGTH_SHORT).show()
            return
        }

        btnNext.isEnabled = false
        btnNext.text = "Saving..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Update procedure columns inside dll_daily_entry
                SupabaseService.client.from("dll_daily_entry").update({
                    set("review", manager.etReview.text.toString().trim())
                    set("purpose", manager.etPurpose.text.toString().trim())
                    set("example", manager.etExample.text.toString().trim())
                    set("discussion_proper", manager.etDiscussion1.text.toString().trim())
                    set("developing_mastery", manager.etMastery.text.toString().trim())
                    set("application", manager.etApplication.text.toString().trim())
                    set("generalization", manager.etGeneralization.text.toString().trim())
                    set("evaluation", manager.etEvaluation.text.toString().trim())
                    set("additional_act", manager.etAdditional.text.toString().trim())
                }) {
                    filter { eq("id", dailyEntryId) }
                }

                withContext(Dispatchers.Main) {
                    ShowToast.showMessage(this@DLLStep3Activity, "Updated Successfully!")
                    setResult(RESULT_OK) // Notify fragment to refresh
                    finish()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnNext.isEnabled = true
                    btnNext.text = "SAVE"
                    ShowToast.showMessage(this@DLLStep3Activity, "Update Failed: ${e.message}")
                }
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

            // Pass all preceding intent data forward
            intent.putExtras(getIntent())

            intent.putExtra("EXTRA_PROC_COUNT", dayManagers.size)

            dayManagers.forEachIndexed { index, manager ->
                val procIndex = index + 1
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

    // --- INNER CLASS ---
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
                        } else {
                            etDate.setText(sdf.format(selectedCalendar.time))
                            tilDate.error = null
                        }
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

                    datePicker.datePicker.minDate = minDateMillis
                    datePicker.datePicker.maxDate = maxDateMillis
                    datePicker.show()
                }
            }

            btnRemove.setOnClickListener { onRemove(this) }
        }

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
    }
}