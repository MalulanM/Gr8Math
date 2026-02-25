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
import com.example.gr8math.Activity.TeacherModule.DLL.DLLViewActivityMain
import com.example.gr8math.Activity.TeacherModule.Lesson.LessonContentActivity
import com.example.gr8math.Activity.TeacherModule.Lesson.LessonDetailActivity
import com.example.gr8math.Activity.TeacherModule.Notification.TeacherNotificationsActivity
import com.example.gr8math.Activity.TeacherModule.Participants.TeacherParticipantsActivity
import com.example.gr8math.Data.Model.ClassContentItem
import com.example.gr8math.Model.CurrentCourse
import com.example.gr8math.R
import com.example.gr8math.Utils.NotificationHelper
import com.example.gr8math.Utils.ShowToast
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
    private lateinit var toolbar: MaterialToolbar

    // Launchers for refreshing
    private val lessonContentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) viewModel.loadContent(forceReload = true)
    }
    private val editLessonLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) viewModel.loadContent(forceReload = true)
    }
    private val createAssessmentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) viewModel.loadContent(forceReload = true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classpage_teacher)

        initViews()
        setupCurrentCourse()
        setupBottomNav()
        setupObservers()

        viewModel.loadContent()
        handleNotificationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun initViews() {
        parentLayout = findViewById(R.id.parentLayout)
        toolbar = findViewById(R.id.toolbar)

        toolbar.setNavigationOnClickListener { finish() }

        findViewById<Button>(R.id.btnAdd).setOnClickListener { showAddOptionsDialog() }
    }

    private fun setupCurrentCourse() {
        val incomingCourseId = intent.getIntExtra("courseId", -1)
        val incomingSectionName = intent.getStringExtra("sectionName")
        val incomingRole = intent.getStringExtra("role")
        val incomingUserId = intent.getIntExtra("id", -1)

        if (incomingCourseId != -1) {
            CurrentCourse.courseId = incomingCourseId
            CurrentCourse.currentRole = incomingRole ?: "teacher"
            if (incomingUserId != -1) CurrentCourse.userId = incomingUserId

            if (incomingSectionName.isNullOrEmpty()) {
                toolbar.title = "Loading..."
                viewModel.fetchSectionName(incomingCourseId)
            } else {
                CurrentCourse.sectionName = incomingSectionName
                toolbar.title = incomingSectionName
            }
        }
    }

    private fun setupObservers() {
        viewModel.sectionName.observe(this) { name ->
            if (!name.isNullOrEmpty()) {
                toolbar.title = name
                CurrentCourse.sectionName = name
            }
        }
        viewModel.state.observe(this) { state ->
            when (state) {
                is ContentState.Loading -> parentLayout.removeAllViews()
                is ContentState.Success -> populateList(state.data)
                is ContentState.Error -> ShowToast.showMessage(this, state.message)
            }
        }
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return

        val type = intent.getStringExtra("notif_type")
        val metaString = intent.getStringExtra("notif_meta")

        val directLessonId = intent.getIntExtra("lessonId", -1)
        val directAssessmentId = intent.getIntExtra("assessmentId", -1)
        val directStudentId = intent.getIntExtra("studentId", -1)

        // --- CASE A: Push Notifications (JSON Meta) ---
        if (type != null && !metaString.isNullOrEmpty()) {
            try {
                val metaJson = org.json.JSONObject(metaString)
                when (type) {
                    "lesson" -> {
                        val id = metaJson.optInt("lesson_id", -1)
                        if (id > 0) navigateToLesson(id)
                    }
                    "assessment" -> {
                        val aId = metaJson.optInt("assessment_id", -1)
                        val sId = metaJson.optInt("student_id", -1)
                        if (aId > 0) navigateToScores(aId, sId)
                    }
                    "arrival" -> ShowToast.showMessage(this, "Class is starting!")
                }
            } catch (e: Exception) { e.printStackTrace() }

            intent.removeExtra("notif_type")
            intent.removeExtra("notif_meta")
        }
        // --- CASE B: Internal Notification clicks ---
        else if (directLessonId > 0) {
            navigateToLesson(directLessonId)
            intent.removeExtra("lessonId")
        }
        else if (directAssessmentId > 0) {
            navigateToScores(directAssessmentId, directStudentId)
            intent.removeExtra("assessmentId")
        }
    }

    private fun navigateToScores(assessmentId: Int, studentId: Int) {
        val i = Intent(this, com.example.gr8math.Activity.TeacherModule.StudentScoresActivity::class.java).apply {
            putExtra("AUTO_ASSESSMENT_ID", assessmentId)
            if (studentId > 0) putExtra("EXTRA_STUDENT_ID", studentId)
        }
        startActivity(i)
    }

    private fun navigateToLesson(lessonId: Int) {
        val i = Intent(this, LessonDetailActivity::class.java)
        i.putExtra("lesson_id", lessonId)
        startActivity(i)
    }

    private fun populateList(data: List<ClassContentItem>) {
        parentLayout.removeAllViews()
        for (item in data) {
            val itemView: View = when (item) {
                is ClassContentItem.LessonItem -> {
                    val view = layoutInflater.inflate(R.layout.item_class_lesson_card, parentLayout, false)
                    view.findViewById<TextView>(R.id.tvWeek).text = item.weekNumber.toString()
                    view.findViewById<TextView>(R.id.tvTitle).text = item.title
                    view.findViewById<TextView>(R.id.tvDescription).text = item.previewContent

                    view.findViewById<TextView>(R.id.tvSeeMore).setOnClickListener {
                        val i = Intent(this, LessonDetailActivity::class.java)
                        i.putExtra("lesson_id", item.id)
                        startActivity(i)
                    }
                    view.findViewById<ImageButton>(R.id.ibEditLesson).setOnClickListener {
                        showEditALessonDialog(item.weekNumber.toString(), item.title, item.id)
                    }
                    view
                }
                is ClassContentItem.AssessmentItem -> {
                    val view = layoutInflater.inflate(R.layout.item_class_assessment_card, parentLayout, false)
                    view.findViewById<TextView>(R.id.tvTitle).text = "Assessment ${item.assessmentNumber}"

                    val btnEditAssessment = view.findViewById<ImageView>(R.id.ivArrow)
                    if (btnEditAssessment != null) {
                        btnEditAssessment.setImageResource(R.drawable.ic_edit)
                        btnEditAssessment.setOnClickListener {
                            showEditAssessmentWarning(item)
                        }
                    }
                    view
                }
            }
            parentLayout.addView(itemView)
        }
    }

    private fun setupBottomNav() {
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_class
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_class -> true
                R.id.nav_participants -> {
                    startActivity(Intent(this, TeacherParticipantsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    finish(); true
                }
                R.id.nav_notifications -> {
                    startActivity(Intent(this, TeacherNotificationsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    finish(); true
                }
                R.id.nav_dll -> {
                    startActivity(Intent(this, DLLViewActivityMain::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    finish(); true
                }
                else -> false
            }
        }
        NotificationHelper.fetchUnreadCount(bottomNav)
    }

    private fun showAddOptionsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_teacher_add_options, null)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.rgOptions)
        val dialog = MaterialAlertDialogBuilder(this).setView(dialogView).setCancelable(false).create()

        dialogView.findViewById<ImageButton>(R.id.btnCloseDialog).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<Button>(R.id.btnProceed).setOnClickListener {
            val selected = radioGroup.checkedRadioButtonId
            if (selected == -1) return@setOnClickListener
            dialog.dismiss()
            if (selected == R.id.rbWriteLesson) showWriteALessonDialog() else showCreateAssessmentDialog()
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun showWriteALessonDialog(weekToPreload: String = "", titleToPreload: String = "") {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_write_a_lesson, null)
        val etWeek = dialogView.findViewById<TextInputEditText>(R.id.etWeekNumber)
        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.etLessonTitle)
        val dialog = MaterialAlertDialogBuilder(this).setView(dialogView).setCancelable(false).create()

        etWeek.setText(weekToPreload)
        etTitle.setText(titleToPreload)

        dialogView.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { dialog.dismiss(); showAddOptionsDialog() }
        dialogView.findViewById<Button>(R.id.btnNext).setOnClickListener {
            val intent = Intent(this, LessonContentActivity::class.java).apply {
                putExtra("EXTRA_WEEK_NUMBER", etWeek.text.toString())
                putExtra("EXTRA_LESSON_TITLE", etTitle.text.toString())
            }
            lessonContentLauncher.launch(intent)
            dialog.dismiss()
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun showEditALessonDialog(weekToPreload: String, titleToPreload: String, lessonId: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_a_lesson, null)
        val etWeek = dialogView.findViewById<TextInputEditText>(R.id.etWeekNumber)
        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.etLessonTitle)
        val dialog = MaterialAlertDialogBuilder(this).setView(dialogView).setCancelable(false).create()

        etWeek.setText(weekToPreload)
        etTitle.setText(titleToPreload)

        dialogView.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { dialog.dismiss() }
        dialogView.findViewById<Button>(R.id.btnNext).setOnClickListener {
            val intent = Intent(this, LessonContentActivity::class.java).apply {
                putExtra("EXTRA_WEEK_NUMBER", etWeek.text.toString())
                putExtra("EXTRA_LESSON_TITLE", etTitle.text.toString())
                putExtra("EXTRA_LESSON_ID", lessonId)
            }
            editLessonLauncher.launch(intent)
            dialog.dismiss()
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun showEditAssessmentWarning(item: ClassContentItem.AssessmentItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Edit Assessment?")
            .setMessage("Editing deletes all current student scores. Proceed?")
            .setPositiveButton("Proceed") { _, _ -> showCreateAssessmentDialog(item) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCreateAssessmentDialog(existingItem: ClassContentItem.AssessmentItem? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_assessment, null)
        val etNum = dialogView.findViewById<TextInputEditText>(R.id.etAssessmentNumber)
        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.etAssessmentTitle)
        val etFrom = dialogView.findViewById<TextInputEditText>(R.id.etAvailableFrom)
        val etUntil = dialogView.findViewById<TextInputEditText>(R.id.etAvailableUntil)
        val etQuarter = dialogView.findViewById<TextInputEditText>(R.id.etQuarterNumber)

        val dialog = MaterialAlertDialogBuilder(this).setView(dialogView).setCancelable(false).create()

        dialogView.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            dialog.dismiss()
            if (existingItem == null) showAddOptionsDialog()
        }

        // PRE-FILL LOGIC
        if (existingItem != null) {
            etNum.setText(existingItem.assessmentNumber.toString())
            etTitle.setText(existingItem.title)
            etQuarter.setText(existingItem.quarter.toString())
            etFrom.setText(existingItem.startTime)
            etUntil.setText(existingItem.endTime)
        }

        val dateTimeFormat = SimpleDateFormat("MM/dd/yy - hh:mm a", Locale.US)

        val fromCalendar = Calendar.getInstance()
        val untilCalendar = Calendar.getInstance()

        // If we have an existing start time, let the user pick an end time immediately
        var isFromSet = existingItem?.startTime?.isNotEmpty() == true

        etFrom.isFocusable = false
        etFrom.isClickable = true
        etUntil.isFocusable = false
        etUntil.isClickable = true

        etFrom.setOnClickListener {
            val datePicker = DatePickerDialog(this, { _, year, month, day ->
                fromCalendar.set(Calendar.YEAR, year)
                fromCalendar.set(Calendar.MONTH, month)
                fromCalendar.set(Calendar.DAY_OF_MONTH, day)

                TimePickerDialog(this, { _, hour, min ->
                    fromCalendar.set(Calendar.HOUR_OF_DAY, hour)
                    fromCalendar.set(Calendar.MINUTE, min)
                    fromCalendar.set(Calendar.SECOND, 0)

                    etFrom.setText(dateTimeFormat.format(fromCalendar.time))
                    isFromSet = true

                    if (etUntil.text.toString().isNotEmpty() && untilCalendar.timeInMillis <= fromCalendar.timeInMillis) {
                        etUntil.text?.clear()
                        ShowToast.showMessage(this, "'Available Until' cleared because it was earlier than new start time.")
                    }
                }, fromCalendar.get(Calendar.HOUR_OF_DAY), fromCalendar.get(Calendar.MINUTE), false).show()
            }, fromCalendar.get(Calendar.YEAR), fromCalendar.get(Calendar.MONTH), fromCalendar.get(Calendar.DAY_OF_MONTH))

            datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
            datePicker.show()
        }

        etUntil.setOnClickListener {
            if (!isFromSet) {
                ShowToast.showMessage(this, "Please select 'Available From' first!")
                return@setOnClickListener
            }

            val datePicker = DatePickerDialog(this, { _, year, month, day ->
                untilCalendar.set(Calendar.YEAR, year)
                untilCalendar.set(Calendar.MONTH, month)
                untilCalendar.set(Calendar.DAY_OF_MONTH, day)

                TimePickerDialog(this, { _, hour, min ->
                    val tempCal = Calendar.getInstance()
                    tempCal.timeInMillis = untilCalendar.timeInMillis
                    tempCal.set(Calendar.HOUR_OF_DAY, hour)
                    tempCal.set(Calendar.MINUTE, min)
                    tempCal.set(Calendar.SECOND, 0)

                    if (tempCal.timeInMillis <= fromCalendar.timeInMillis) {
                        ShowToast.showMessage(this, "End time must be later than Start time!")
                    } else {
                        untilCalendar.timeInMillis = tempCal.timeInMillis
                        etUntil.setText(dateTimeFormat.format(untilCalendar.time))
                    }
                }, untilCalendar.get(Calendar.HOUR_OF_DAY), untilCalendar.get(Calendar.MINUTE), false).show()
            }, untilCalendar.get(Calendar.YEAR), untilCalendar.get(Calendar.MONTH), untilCalendar.get(Calendar.DAY_OF_MONTH))

            datePicker.datePicker.minDate = fromCalendar.timeInMillis
            datePicker.show()
        }

        dialogView.findViewById<Button>(R.id.btnNext).setOnClickListener {
            val assessmentNumber = etNum.text.toString().trim()
            val assessmentTitle = etTitle.text.toString().trim()
            val availableFrom = etFrom.text.toString().trim()
            val availableUntil = etUntil.text.toString().trim()
            val assessmentQuarter = etQuarter.text.toString().trim()

            if (assessmentNumber.isEmpty() || assessmentTitle.isEmpty() || availableFrom.isEmpty() || availableUntil.isEmpty() || assessmentQuarter.isEmpty()) {
                ShowToast.showMessage(this, "Please fill in all fields.")
                return@setOnClickListener
            }

            val intent = Intent(this, AssessmentCreatorActivity::class.java).apply {
                putExtra("EXTRA_ASSESSMENT_NUMBER", assessmentNumber)
                putExtra("EXTRA_ASSESSMENT_TITLE", assessmentTitle)
                putExtra("EXTRA_AVAILABLE_FROM", availableFrom)
                putExtra("EXTRA_AVAILABLE_UNTIL", availableUntil)
                putExtra("EXTRA_AVAILABLE_QUARTER", assessmentQuarter)


                if (existingItem != null) {
                    putExtra("EXTRA_EDIT_ASSESSMENT_ID", existingItem.id)
                }
            }
            createAssessmentLauncher.launch(intent)
            dialog.dismiss()
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }
}