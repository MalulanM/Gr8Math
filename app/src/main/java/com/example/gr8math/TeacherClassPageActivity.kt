package com.example.gr8math // Make sure this matches your package name

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TeacherClassPageActivity : AppCompatActivity() {

    // Launcher for CREATING a new lesson
    private val lessonContentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val week = data?.getStringExtra("EXTRA_WEEK_NUMBER") ?: ""
            val title = data?.getStringExtra("EXTRA_LESSON_TITLE") ?: ""
            showWriteALessonDialog(week, title)
        }
    }

    // Launcher for EDITING an existing lesson
    private val editLessonLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val week = data?.getStringExtra("EXTRA_WEEK_NUMBER") ?: ""
            val title = data?.getStringExtra("EXTRA_LESSON_TITLE") ?: ""
            showEditALessonDialog(week, title)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classpage_teacher)

        // --- Setup Toolbar ---
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // --- Setup Bottom Navigation ---
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_class

        // --- Setup Floating "Add" Button ---
        val btnAdd = findViewById<Button>(R.id.btnAdd)
        btnAdd.setOnClickListener {
            showAddOptionsDialog()
        }

        // --- Customize Cards for Teacher ---

        // 1. "Lesson Title" card
        val lessonCard: View = findViewById(R.id.lesson_card)
        val ibEditLesson = lessonCard.findViewById<ImageButton>(R.id.ibEditLesson)
        ibEditLesson.visibility = View.VISIBLE

        ibEditLesson.setOnClickListener {
            val week = lessonCard.findViewById<TextView>(R.id.tvWeek).text.toString()
            val title = lessonCard.findViewById<TextView>(R.id.tvTitle).text.toString()
            showEditALessonDialog(week, title)
        }

        val tvSeeMore = lessonCard.findViewById<TextView>(R.id.tvSeeMore)
        tvSeeMore.setOnClickListener {
            val tvWeek = lessonCard.findViewById<TextView>(R.id.tvWeek)
            val tvTitle = lessonCard.findViewById<TextView>(R.id.tvTitle)
            val week = tvWeek.text.toString()
            val title = tvTitle.text.toString()
            val fullDescription = getString(R.string.lesson_full_desc_placeholder)
            val intent = Intent(this, LessonDetailActivity::class.java).apply {
                putExtra("EXTRA_WEEK", week)
                putExtra("EXTRA_TITLE", title)
                putExtra("EXTRA_DESCRIPTION", fullDescription)
            }
            startActivity(intent)
        }

        // 2. "Assessment" card
        val assessmentCard: View = findViewById(R.id.assessment_card)
        val iconAssessment = assessmentCard.findViewById<ImageView>(R.id.ivIcon)
        val titleAssessment = assessmentCard.findViewById<TextView>(R.id.tvTitle)
        iconAssessment.setImageResource(R.drawable.ic_assessment_green)
        iconAssessment.contentDescription = getString(R.string.assessment_placeholder)
        titleAssessment.text = getString(R.string.assessment_placeholder)

        // --- NEW CODE: Set click listener for the assessment arrow ---
        val ivArrow = assessmentCard.findViewById<ImageView>(R.id.ivArrow)
        ivArrow.setOnClickListener {
            // --- TODO: Pass real assessment ID/data ---
            val intent = Intent(this, AssessmentDetailActivity::class.java)
            startActivity(intent)
        }
        // --- END OF NEW CODE ---
    }

    /**
     * Shows the "Add Options" dialog.
     */
    private fun showAddOptionsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_teacher_add_options, null)
        val btnCloseDialog = dialogView.findViewById<ImageButton>(R.id.btnCloseDialog)
        val btnProceed = dialogView.findViewById<Button>(R.id.btnProceed)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.rgOptions)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCloseDialog.setOnClickListener {
            dialog.dismiss()
        }

        btnProceed.setOnClickListener {
            val selectedOptionId = radioGroup.checkedRadioButtonId
            if (selectedOptionId == -1) {
                Toast.makeText(this, "Please select an option", Toast.LENGTH_SHORT).show()
            } else {
                dialog.dismiss()
                when (selectedOptionId) {
                    R.id.rbWriteLesson -> {
                        showWriteALessonDialog()
                    }
                    R.id.rbCreateAssessment -> {
                        showCreateAssessmentDialog()
                    }
                    R.id.rbLessonLog -> {
                        Toast.makeText(this, "Opening Lesson Log...", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    /**
     * Shows the "Write a Lesson" (Step 1) dialog.
     */
    private fun showWriteALessonDialog(weekToPreload: String = "", titleToPreload: String = "") {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_write_a_lesson, null)
        val toolbar = dialogView.findViewById<MaterialToolbar>(R.id.toolbar)
        val btnNext = dialogView.findViewById<Button>(R.id.btnNext)
        val etWeekNumber = dialogView.findViewById<TextInputEditText>(R.id.etWeekNumber)
        val tilWeekNumber = dialogView.findViewById<TextInputLayout>(R.id.tilWeekNumber)
        val etLessonTitle = dialogView.findViewById<TextInputEditText>(R.id.etLessonTitle)
        val tilLessonTitle = dialogView.findViewById<TextInputLayout>(R.id.tilLessonTitle)

        etWeekNumber.setText(weekToPreload)
        etLessonTitle.setText(titleToPreload)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        toolbar.setNavigationOnClickListener {
            dialog.dismiss()
            showAddOptionsDialog()
        }

        btnNext.setOnClickListener {
            val weekNumber = etWeekNumber.text.toString().trim()
            val lessonTitle = etLessonTitle.text.toString().trim()

            tilWeekNumber.error = null
            tilLessonTitle.error = null
            var isValid = true
            val errorMsg = getString(R.string.error_blank_field)

            if (weekNumber.isEmpty()) {
                tilWeekNumber.error = errorMsg
                isValid = false
            }
            if (lessonTitle.isEmpty()) {
                tilLessonTitle.error = errorMsg
                isValid = false
            }
            if (!isValid) {
                return@setOnClickListener
            }

            val intent = Intent(this, LessonContentActivity::class.java).apply {
                putExtra("EXTRA_WEEK_NUMBER", weekNumber)
                putExtra("EXTRA_LESSON_TITLE", lessonTitle)
            }
            lessonContentLauncher.launch(intent)
            dialog.dismiss()
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    /**
     * Shows the "Edit a Lesson" dialog.
     */
    private fun showEditALessonDialog(weekToPreload: String, titleToPreload: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_a_lesson, null)
        val toolbar = dialogView.findViewById<MaterialToolbar>(R.id.toolbar)
        val btnNext = dialogView.findViewById<Button>(R.id.btnNext)
        val etWeekNumber = dialogView.findViewById<TextInputEditText>(R.id.etWeekNumber)
        val tilWeekNumber = dialogView.findViewById<TextInputLayout>(R.id.tilWeekNumber)
        val etLessonTitle = dialogView.findViewById<TextInputEditText>(R.id.etLessonTitle)
        val tilLessonTitle = dialogView.findViewById<TextInputLayout>(R.id.tilLessonTitle)

        etWeekNumber.setText(weekToPreload)
        etLessonTitle.setText(titleToPreload)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        toolbar.setNavigationOnClickListener {
            dialog.dismiss()
        }

        btnNext.setOnClickListener {
            val weekNumber = etWeekNumber.text.toString().trim()
            val lessonTitle = etLessonTitle.text.toString().trim()

            tilWeekNumber.error = null
            tilLessonTitle.error = null
            var isValid = true
            val errorMsg = getString(R.string.error_blank_field)

            if (weekNumber.isEmpty()) {
                tilWeekNumber.error = errorMsg
                isValid = false
            }
            if (lessonTitle.isEmpty()) {
                tilLessonTitle.error = errorMsg
                isValid = false
            }
            if (!isValid) {
                return@setOnClickListener
            }

            val lessonContent = getString(R.string.lesson_full_desc_placeholder)

            val intent = Intent(this, LessonContentActivity::class.java).apply {
                putExtra("EXTRA_WEEK_NUMBER", weekNumber)
                putExtra("EXTRA_LESSON_TITLE", lessonTitle)
                putExtra("EXTRA_LESSON_CONTENT", lessonContent)
            }

            editLessonLauncher.launch(intent)
            dialog.dismiss()
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    /**
     * Shows the "Create Assessment" dialog, with 12-hour (AM/PM) time pickers.
     */
    private fun showCreateAssessmentDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_assessment, null)

        // Find views
        val toolbar = dialogView.findViewById<MaterialToolbar>(R.id.toolbar)
        val btnNext = dialogView.findViewById<Button>(R.id.btnNext)
        val etAssessmentNumber = dialogView.findViewById<TextInputEditText>(R.id.etAssessmentNumber)
        val tilAssessmentNumber = dialogView.findViewById<TextInputLayout>(R.id.tilAssessmentNumber)
        val etAssessmentTitle = dialogView.findViewById<TextInputEditText>(R.id.etAssessmentTitle)
        val tilAssessmentTitle = dialogView.findViewById<TextInputLayout>(R.id.tilAssessmentTitle)
        val etAvailableFrom = dialogView.findViewById<TextInputEditText>(R.id.etAvailableFrom)
        val tilAvailableFrom = dialogView.findViewById<TextInputLayout>(R.id.tilAvailableFrom)
        val etAvailableUntil = dialogView.findViewById<TextInputEditText>(R.id.etAvailableUntil)
        val tilAvailableUntil = dialogView.findViewById<TextInputLayout>(R.id.tilAvailableUntil)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        toolbar.setNavigationOnClickListener {
            dialog.dismiss()
            showAddOptionsDialog()
        }

        // --- Date & Time Picker Logic (AM/PM Version) ---

        val myCalendar = Calendar.getInstance()

        // 12-hour format with AM/PM
        val dateTimeFormat = SimpleDateFormat("MM/dd/yy - hh:mm a", Locale.US)

        // --- "Available From" Listeners ---
        val fromTimeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            myCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            myCalendar.set(Calendar.MINUTE, minute)
            etAvailableFrom.setText(dateTimeFormat.format(myCalendar.time))
            tilAvailableFrom.error = null
        }

        val fromDateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, monthOfYear)
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            TimePickerDialog(this,
                fromTimeSetListener,
                myCalendar.get(Calendar.HOUR_OF_DAY),
                myCalendar.get(Calendar.MINUTE),
                false // false = Use 12-hour format (AM/PM)
            ).show()
        }

        etAvailableFrom.setOnClickListener {
            DatePickerDialog(this,
                fromDateSetListener,
                myCalendar.get(Calendar.YEAR),
                myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        // --- "Available Until" Listeners ---
        val untilTimeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            myCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            myCalendar.set(Calendar.MINUTE, minute)
            etAvailableUntil.setText(dateTimeFormat.format(myCalendar.time))
            tilAvailableUntil.error = null
        }

        val untilDateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, monthOfYear)
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            TimePickerDialog(this,
                untilTimeSetListener,
                myCalendar.get(Calendar.HOUR_OF_DAY),
                myCalendar.get(Calendar.MINUTE),
                false // false = Use 12-hour format (AM/PM)
            ).show()
        }

        etAvailableUntil.setOnClickListener {
            DatePickerDialog(this,
                untilDateSetListener,
                myCalendar.get(Calendar.YEAR),
                myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH)).show()
        }
        // --- End of Date & Time Picker Logic ---

        btnNext.setOnClickListener {
            val assessmentNumber = etAssessmentNumber.text.toString().trim()
            val assessmentTitle = etAssessmentTitle.text.toString().trim()
            val availableFrom = etAvailableFrom.text.toString().trim()
            val availableUntil = etAvailableUntil.text.toString().trim()

            // Validation
            tilAssessmentNumber.error = null
            tilAssessmentTitle.error = null
            tilAvailableFrom.error = null
            tilAvailableUntil.error = null

            var isValid = true
            val errorMsg = getString(R.string.error_blank_field)

            if (assessmentNumber.isEmpty()) {
                tilAssessmentNumber.error = errorMsg
                isValid = false
            }
            if (assessmentTitle.isEmpty()) {
                tilAssessmentTitle.error = errorMsg
                isValid = false
            }
            if (availableFrom.isEmpty()) {
                tilAvailableFrom.error = errorMsg
                isValid = false
            }
            if (availableUntil.isEmpty()) {
                tilAvailableUntil.error = errorMsg
                isValid = false
            }

            if (!isValid) {
                return@setOnClickListener
            }

            // If validation passes, launch the AssessmentCreatorActivity
            val intent = Intent(this, AssessmentCreatorActivity::class.java).apply {
                // Pass all the details to the new activity
                putExtra("EXTRA_ASSESSMENT_NUMBER", assessmentNumber)
                putExtra("EXTRA_ASSESSMENT_TITLE", assessmentTitle)
                putExtra("EXTRA_AVAILABLE_FROM", availableFrom)
                putExtra("EXTRA_AVAILABLE_UNTIL", availableUntil)
            }
            startActivity(intent)

            dialog.dismiss() // Close this dialog
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }
}