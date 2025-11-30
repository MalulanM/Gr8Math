package com.example.gr8math

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
import com.example.gr8math.utils.UIUtils // Ensure this import exists
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TeacherClassPageActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

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
        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_class

        // --- Handle Bottom Navigation Item Clicks ---
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_class -> true
                R.id.nav_participants -> {
                    startActivity(Intent(this, TeacherParticipantsActivity::class.java))
                    false
                }
                R.id.nav_notifications -> {
                    startActivity(Intent(this, TeacherNotificationsActivity::class.java))
                    false
                }
                R.id.nav_dll -> {
                    // UPDATED: Navigate to the DLL Viewer Activity
                    startActivity(Intent(this, DLLViewActivity::class.java))
                    false
                }
                else -> false
            }
        }

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

        // --- Set click listener for the assessment arrow ---
        val ivArrow = assessmentCard.findViewById<ImageView>(R.id.ivArrow)
        ivArrow.setOnClickListener {
            val intent = Intent(this, AssessmentDetailActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::bottomNav.isInitialized) {
            bottomNav.selectedItemId = R.id.nav_class
        }
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
                    R.id.rbWriteLesson -> showWriteALessonDialog()
                    R.id.rbCreateAssessment -> showCreateAssessmentDialog()
                    R.id.rbLessonLog -> showCreateDLLDialog()
                }
            }
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    /**
     * Shows the "Create Daily Lesson Log" dialog.
     */
    private fun showCreateDLLDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_dll, null)

        val btnBack = dialogView.findViewById<ImageButton>(R.id.btnBack)
        val btnNext = dialogView.findViewById<Button>(R.id.btnNext)

        val etQuarter = dialogView.findViewById<TextInputEditText>(R.id.etQuarterNumber)
        val tilQuarter = dialogView.findViewById<TextInputLayout>(R.id.tilQuarterNumber)
        val etWeek = dialogView.findViewById<TextInputEditText>(R.id.etWeekNumber)
        val tilWeek = dialogView.findViewById<TextInputLayout>(R.id.tilWeekNumber)
        val etFrom = dialogView.findViewById<TextInputEditText>(R.id.etAvailableFrom)
        val tilFrom = dialogView.findViewById<TextInputLayout>(R.id.tilAvailableFrom)
        val etUntil = dialogView.findViewById<TextInputEditText>(R.id.etAvailableUntil)
        val tilUntil = dialogView.findViewById<TextInputLayout>(R.id.tilAvailableUntil)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnBack.setOnClickListener {
            dialog.dismiss()
            showAddOptionsDialog()
        }

        // Date Pickers
        val myCalendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MM/dd/yy", Locale.US)

        val fromDateListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, month)
            myCalendar.set(Calendar.DAY_OF_MONTH, day)
            etFrom.setText(dateFormat.format(myCalendar.time))
        }

        val untilDateListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, month)
            myCalendar.set(Calendar.DAY_OF_MONTH, day)
            etUntil.setText(dateFormat.format(myCalendar.time))
        }

        etFrom.setOnClickListener {
            DatePickerDialog(this, fromDateListener,
                myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        etUntil.setOnClickListener {
            DatePickerDialog(this, untilDateListener,
                myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnNext.setOnClickListener {
            val errorMsg = getString(R.string.error_blank_field)

            UIUtils.errorDisplay(this, tilQuarter, etQuarter, true, errorMsg)
            UIUtils.errorDisplay(this, tilWeek, etWeek, true, errorMsg)
            UIUtils.errorDisplay(this, tilFrom, etFrom, true, errorMsg)
            UIUtils.errorDisplay(this, tilUntil, etUntil, true, errorMsg)

            val isValid = etQuarter.text.toString().trim().isNotEmpty() &&
                    etWeek.text.toString().trim().isNotEmpty() &&
                    etFrom.text.toString().trim().isNotEmpty() &&
                    etUntil.text.toString().trim().isNotEmpty()

            if (isValid) {
                val intent = Intent(this, DailyLessonLogActivity::class.java).apply {
                    putExtra("EXTRA_QUARTER", etQuarter.text.toString().trim())
                    putExtra("EXTRA_WEEK", etWeek.text.toString().trim())
                    putExtra("EXTRA_FROM", etFrom.text.toString().trim())
                    putExtra("EXTRA_UNTIL", etUntil.text.toString().trim())
                }
                startActivity(intent)
                dialog.dismiss()
            }
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    /**
     * Shows the "Write a Lesson" dialog.
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
            val errorMsg = getString(R.string.error_blank_field)

            UIUtils.errorDisplay(this, tilWeekNumber, etWeekNumber, true, errorMsg)
            UIUtils.errorDisplay(this, tilLessonTitle, etLessonTitle, true, errorMsg)

            val isValid = etWeekNumber.text.toString().trim().isNotEmpty() &&
                    etLessonTitle.text.toString().trim().isNotEmpty()

            if (isValid) {
                val intent = Intent(this, LessonContentActivity::class.java).apply {
                    putExtra("EXTRA_WEEK_NUMBER", etWeekNumber.text.toString().trim())
                    putExtra("EXTRA_LESSON_TITLE", etLessonTitle.text.toString().trim())
                }
                lessonContentLauncher.launch(intent)
                dialog.dismiss()
            }
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
            val errorMsg = getString(R.string.error_blank_field)

            UIUtils.errorDisplay(this, tilWeekNumber, etWeekNumber, true, errorMsg)
            UIUtils.errorDisplay(this, tilLessonTitle, etLessonTitle, true, errorMsg)

            val isValid = etWeekNumber.text.toString().trim().isNotEmpty() &&
                    etLessonTitle.text.toString().trim().isNotEmpty()

            if (isValid) {
                val lessonContent = getString(R.string.lesson_full_desc_placeholder)
                val intent = Intent(this, LessonContentActivity::class.java).apply {
                    putExtra("EXTRA_WEEK_NUMBER", etWeekNumber.text.toString().trim())
                    putExtra("EXTRA_LESSON_TITLE", etLessonTitle.text.toString().trim())
                    putExtra("EXTRA_LESSON_CONTENT", lessonContent)
                }
                editLessonLauncher.launch(intent)
                dialog.dismiss()
            }
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    /**
     * Shows the "Create Assessment" dialog.
     */
    private fun showCreateAssessmentDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_assessment, null)

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

        // --- Date & Time Picker Logic ---
        val myCalendar = Calendar.getInstance()
        val dateTimeFormat = SimpleDateFormat("MM/dd/yy - hh:mm a", Locale.US)

        val fromTimeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            myCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            myCalendar.set(Calendar.MINUTE, minute)
            etAvailableFrom.setText(dateTimeFormat.format(myCalendar.time))
        }

        val fromDateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, monthOfYear)
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            TimePickerDialog(this,
                fromTimeSetListener,
                myCalendar.get(Calendar.HOUR_OF_DAY),
                myCalendar.get(Calendar.MINUTE),
                false
            ).show()
        }

        etAvailableFrom.setOnClickListener {
            DatePickerDialog(this,
                fromDateSetListener,
                myCalendar.get(Calendar.YEAR),
                myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        val untilTimeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            myCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            myCalendar.set(Calendar.MINUTE, minute)
            etAvailableUntil.setText(dateTimeFormat.format(myCalendar.time))
        }

        val untilDateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, monthOfYear)
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            TimePickerDialog(this,
                untilTimeSetListener,
                myCalendar.get(Calendar.HOUR_OF_DAY),
                myCalendar.get(Calendar.MINUTE),
                false
            ).show()
        }

        etAvailableUntil.setOnClickListener {
            DatePickerDialog(this,
                untilDateSetListener,
                myCalendar.get(Calendar.YEAR),
                myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnNext.setOnClickListener {
            val errorMsg = getString(R.string.error_blank_field)

            UIUtils.errorDisplay(this, tilAssessmentNumber, etAssessmentNumber, true, errorMsg)
            UIUtils.errorDisplay(this, tilAssessmentTitle, etAssessmentTitle, true, errorMsg)
            UIUtils.errorDisplay(this, tilAvailableFrom, etAvailableFrom, true, errorMsg)
            UIUtils.errorDisplay(this, tilAvailableUntil, etAvailableUntil, true, errorMsg)

            val isValid = etAssessmentNumber.text.toString().trim().isNotEmpty() &&
                    etAssessmentTitle.text.toString().trim().isNotEmpty() &&
                    etAvailableFrom.text.toString().trim().isNotEmpty() &&
                    etAvailableUntil.text.toString().trim().isNotEmpty()

            if (isValid) {
                val intent = Intent(this, AssessmentCreatorActivity::class.java).apply {
                    putExtra("EXTRA_ASSESSMENT_NUMBER", etAssessmentNumber.text.toString().trim())
                    putExtra("EXTRA_ASSESSMENT_TITLE", etAssessmentTitle.text.toString().trim())
                    putExtra("EXTRA_AVAILABLE_FROM", etAvailableFrom.text.toString().trim())
                    putExtra("EXTRA_AVAILABLE_UNTIL", etAvailableUntil.text.toString().trim())
                }
                startActivity(intent)
                dialog.dismiss()
            }
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }
}