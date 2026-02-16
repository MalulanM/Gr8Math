package com.example.gr8math.Activity.TeacherModule.ClassManager

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.gr8math.Activity.TeacherModule.Assessment.AssessmentCreatorActivity
import com.example.gr8math.Activity.TeacherModule.DLL.DLLViewActivity
import com.example.gr8math.Activity.TeacherModule.DLL.DLLViewActivityMain
import com.example.gr8math.Activity.TeacherModule.DLL.DailyLessonLogActivity
import com.example.gr8math.Activity.TeacherModule.Lesson.LessonContentActivity
import com.example.gr8math.Activity.TeacherModule.Lesson.LessonDetailActivity
import com.example.gr8math.Activity.TeacherModule.Notification.TeacherNotificationsActivity
import com.example.gr8math.Activity.TeacherModule.Participants.TeacherParticipantsActivity
import com.example.gr8math.Data.Model.ClassContentItem
import com.example.gr8math.Model.CurrentCourse
import com.example.gr8math.R
import com.example.gr8math.Utils.NotificationHelper
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.Utils.UIUtils
import com.example.gr8math.ViewModel.ContentState
import com.example.gr8math.ViewModel.TeacherClassPageViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TeacherClassPageActivity : AppCompatActivity() {

    private val viewModel: TeacherClassPageViewModel by viewModels()
    private lateinit var parentLayout: LinearLayout

    // --- Launchers to refresh list after creating items ---
    private val lessonContentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        // We added a lesson, so we MUST force a reload from server
        if (result.resultCode == RESULT_OK) viewModel.loadContent(forceReload = true)
    }

    private val editLessonLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // We edited a lesson, force reload
        if (result.resultCode == RESULT_OK) viewModel.loadContent(forceReload = true)
    }

    private val createAssessmentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // We added an assessment, force reload
        if (result.resultCode == RESULT_OK) viewModel.loadContent(forceReload = true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classpage_teacher)

        // 1. Setup Global State
        setupCurrentCourse()

        // 2. Setup UI
        initViews()
        setupBottomNav()
        setupObservers()

        // 3. Load Data
        viewModel.loadContent()
    }

    private fun setupCurrentCourse() {
        val incomingCourseId = intent.getIntExtra("courseId", -1)
        val incomingSectionName = intent.getStringExtra("sectionName")
        val incomingRole = intent.getStringExtra("role")
        val incomingUserId = intent.getIntExtra("id", -1)

        // Only update singleton if we are coming from a NEW class intent
        if (incomingCourseId != -1 && incomingCourseId != CurrentCourse.courseId) {
            CurrentCourse.courseId = incomingCourseId
            CurrentCourse.sectionName = incomingSectionName ?: ""
            CurrentCourse.currentRole = incomingRole ?: ""
            CurrentCourse.userId = incomingUserId
            Log.d("TeacherClassPage", "Switched to: ${CurrentCourse.sectionName}")
        }
    }

    private fun initViews() {
        parentLayout = findViewById(R.id.parentLayout)
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)

        // Use CurrentCourse for title
        toolbar.title = CurrentCourse.sectionName
        toolbar.setNavigationOnClickListener { finish() }

        val btnAdd = findViewById<Button>(R.id.btnAdd)
        btnAdd.setOnClickListener { showAddOptionsDialog() }
    }

    private fun setupObservers() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is ContentState.Loading -> {

                    parentLayout.removeAllViews()

                }
                is ContentState.Success -> {
                    populateList(state.data)
                }
                is ContentState.Error -> {
                    ShowToast.showMessage(this, state.message)
                }
            }
        }
    }

    private fun populateList(data: List<ClassContentItem>) {
        parentLayout.removeAllViews()

        for (item in data) {
            val itemView: View = when (item) {
                is ClassContentItem.LessonItem -> {
                    val view = layoutInflater.inflate(R.layout.item_class_lesson_card, parentLayout, false)

                    val week = view.findViewById<TextView>(R.id.tvWeek)
                    val title = view.findViewById<TextView>(R.id.tvTitle)
                    val previewDesc = view.findViewById<TextView>(R.id.tvDescription)
                    val seeMore = view.findViewById<TextView>(R.id.tvSeeMore)
                    val editLesson = view.findViewById<ImageButton>(R.id.ibEditLesson)

                    week.text = item.weekNumber.toString()
                    title.text = item.title
                    previewDesc.text = item.previewContent

                    seeMore.setOnClickListener {
                        val intent = Intent(this, LessonDetailActivity::class.java)
                        intent.putExtra("lesson_id", item.id)
                        startActivity(intent)
                    }

                    editLesson.setOnClickListener {
                        showEditALessonDialog(
                            item.weekNumber.toString(),
                            item.title,
                            item.id
                        )
                    }
                    view
                }
                is ClassContentItem.AssessmentItem -> {
                    val view = layoutInflater.inflate(R.layout.item_class_assessment_card, parentLayout, false)
                    val assessmentTitle = view.findViewById<TextView>(R.id.tvTitle)
                    assessmentTitle.text = "Assessment ${item.assessmentNumber}"
                    view
                }
            }
            parentLayout.addView(itemView)
        }
    }

    // --- NAVIGATION ---
    private fun setupBottomNav() {
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_class
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_class -> true
                R.id.nav_participants -> {
                    startActivity(Intent(this, TeacherParticipantsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    finish()
                    true
                }
                R.id.nav_notifications -> {
                    startActivity(Intent(this, TeacherNotificationsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    finish()
                    true
                }
                R.id.nav_dll -> {
                    startActivity(Intent(this, DLLViewActivityMain::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    finish()
                    true
                }
                else -> false
            }
        }
        NotificationHelper.fetchUnreadCount(bottomNav)
    }

    // --- DIALOGS (Kept mostly as is, just ensured Context is correct) ---

    private fun showAddOptionsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_teacher_add_options, null)
        val btnCloseDialog = dialogView.findViewById<ImageButton>(R.id.btnCloseDialog)
        val btnProceed = dialogView.findViewById<Button>(R.id.btnProceed)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.rgOptions)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCloseDialog.setOnClickListener { dialog.dismiss() }

        btnProceed.setOnClickListener {
            val selectedOptionId = radioGroup.checkedRadioButtonId
            if (selectedOptionId == -1) {
                Toast.makeText(this, "Please select an option", Toast.LENGTH_SHORT).show()
            } else {
                dialog.dismiss()
                when (selectedOptionId) {
                    R.id.rbWriteLesson -> showWriteALessonDialog()
                    R.id.rbCreateAssessment -> showCreateAssessmentDialog()
                }
            }
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

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

        val dialog = MaterialAlertDialogBuilder(this).setView(dialogView).setCancelable(false).create()
        toolbar.setNavigationOnClickListener { dialog.dismiss(); showAddOptionsDialog() }

        btnNext.setOnClickListener {
            val weekNumber = etWeekNumber.text.toString().trim()
            val lessonTitle = etLessonTitle.text.toString().trim()
            if (weekNumber.isEmpty() || lessonTitle.isEmpty()) {
                ShowToast.showMessage(this, "Please fill all fields")
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

    private fun showEditALessonDialog(weekToPreload: String, titleToPreload: String, lessonId: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_a_lesson, null)
        val toolbar = dialogView.findViewById<MaterialToolbar>(R.id.toolbar)
        val btnNext = dialogView.findViewById<Button>(R.id.btnNext)
        val etWeekNumber = dialogView.findViewById<TextInputEditText>(R.id.etWeekNumber)
        val etLessonTitle = dialogView.findViewById<TextInputEditText>(R.id.etLessonTitle)

        etWeekNumber.setText(weekToPreload)
        etLessonTitle.setText(titleToPreload)

        val dialog = MaterialAlertDialogBuilder(this).setView(dialogView).setCancelable(false).create()
        toolbar.setNavigationOnClickListener { dialog.dismiss() }

        btnNext.setOnClickListener {
            val weekNumber = etWeekNumber.text.toString().trim()
            val lessonTitle = etLessonTitle.text.toString().trim()
            if (weekNumber.isEmpty() || lessonTitle.isEmpty()) {
                ShowToast.showMessage(this, "Please fill all fields")
                return@setOnClickListener
            }

            val intent = Intent(this, LessonContentActivity::class.java).apply {
                putExtra("EXTRA_WEEK_NUMBER", weekNumber)
                putExtra("EXTRA_LESSON_TITLE", lessonTitle) // Careful: Intent expects String here
                putExtra("EXTRA_LESSON_ID", lessonId)
            }
            editLessonLauncher.launch(intent)
            dialog.dismiss()
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun showCreateAssessmentDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_assessment, null)
        val toolbar = dialogView.findViewById<MaterialToolbar>(R.id.toolbar)
        val btnNext = dialogView.findViewById<Button>(R.id.btnNext)
        val etQuarterNumber = dialogView.findViewById<TextInputEditText>(R.id.etQuarterNumber)
        val etAssessmentNumber = dialogView.findViewById<TextInputEditText>(R.id.etAssessmentNumber)
        val etAssessmentTitle = dialogView.findViewById<TextInputEditText>(R.id.etAssessmentTitle)
        val etAvailableFrom = dialogView.findViewById<TextInputEditText>(R.id.etAvailableFrom)
        val etAvailableUntil = dialogView.findViewById<TextInputEditText>(R.id.etAvailableUntil)

        val dialog = MaterialAlertDialogBuilder(this).setView(dialogView).setCancelable(false).create()
        toolbar.setNavigationOnClickListener { dialog.dismiss(); showAddOptionsDialog() }

        val myCalendar = Calendar.getInstance()
        var availableFromTimestamp: Long = System.currentTimeMillis()
        val dateTimeFormat = SimpleDateFormat("MM/dd/yy - hh:mm a", Locale.US)

        val fromTimeSetListener = TimePickerDialog.OnTimeSetListener { _, h, m -> myCalendar.set(Calendar.HOUR_OF_DAY, h); myCalendar.set(Calendar.MINUTE, m); etAvailableFrom.setText(dateTimeFormat.format(myCalendar.time)); availableFromTimestamp = myCalendar.timeInMillis }
        val fromDateSetListener = DatePickerDialog.OnDateSetListener { _, y, m, d -> myCalendar.set(Calendar.YEAR, y); myCalendar.set(Calendar.MONTH, m); myCalendar.set(Calendar.DAY_OF_MONTH, d); TimePickerDialog(this, fromTimeSetListener, myCalendar.get(Calendar.HOUR_OF_DAY), myCalendar.get(Calendar.MINUTE), false).show() }
        etAvailableFrom.setOnClickListener { val d = DatePickerDialog(this, fromDateSetListener, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)); d.datePicker.minDate = System.currentTimeMillis() - 1000; d.show() }

        val untilTimeSetListener = TimePickerDialog.OnTimeSetListener { _, h, m -> val temp = Calendar.getInstance().apply { timeInMillis = myCalendar.timeInMillis; set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m) }; if (temp.timeInMillis < availableFromTimestamp) { ShowToast.showMessage(this, "Cannot be before From"); return@OnTimeSetListener }; myCalendar.set(Calendar.HOUR_OF_DAY, h); myCalendar.set(Calendar.MINUTE, m); etAvailableUntil.setText(dateTimeFormat.format(myCalendar.time)) }
        val untilDateSetListener = DatePickerDialog.OnDateSetListener { _, y, m, d -> myCalendar.set(Calendar.YEAR, y); myCalendar.set(Calendar.MONTH, m); myCalendar.set(Calendar.DAY_OF_MONTH, d); TimePickerDialog(this, untilTimeSetListener, myCalendar.get(Calendar.HOUR_OF_DAY), myCalendar.get(Calendar.MINUTE), false).show() }
        etAvailableUntil.setOnClickListener { val d = DatePickerDialog(this, untilDateSetListener, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)); d.datePicker.minDate = availableFromTimestamp; d.show() }

        btnNext.setOnClickListener {
            if(etAssessmentNumber.text.isNullOrEmpty()) return@setOnClickListener

            val intent = Intent(this, AssessmentCreatorActivity::class.java).apply {
                putExtra("EXTRA_ASSESSMENT_NUMBER", etAssessmentNumber.text.toString())
                putExtra("EXTRA_ASSESSMENT_TITLE", etAssessmentTitle.text.toString())
                putExtra("EXTRA_AVAILABLE_FROM", etAvailableFrom.text.toString())
                putExtra("EXTRA_AVAILABLE_UNTIL", etAvailableUntil.text.toString())
                putExtra("EXTRA_AVAILABLE_QUARTER", etQuarterNumber.text.toString())
            }
            createAssessmentLauncher.launch(intent)
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }
}