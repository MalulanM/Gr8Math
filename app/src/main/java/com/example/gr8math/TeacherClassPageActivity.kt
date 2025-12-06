package com.example.gr8math // Make sure this matches your package name

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.CurrentCourse
import com.example.gr8math.utils.Notifs
import com.example.gr8math.utils.ShowToast
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.gr8math.utils.NotificationHelper
import com.example.gr8math.utils.UIUtils

class TeacherClassPageActivity : AppCompatActivity() {

    private var id: Int = 0
    private var courseId: Int = 0
    private lateinit var role: String

    private lateinit var sectionName : String
    private lateinit var parentLayout : LinearLayout
    private var isEditMode = false

    // Launcher for CREATING a new lesson
    private val lessonContentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            inflateAssessmentAndLesson(courseId)
        }
    }

    private val editLessonLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            inflateAssessmentAndLesson(courseId)  // refresh the list
        }
    }

    private val createAssessmentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Refresh the lesson/assessment list
            inflateAssessmentAndLesson(courseId)
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classpage_teacher)

        // Get incoming values (if any)
        val incomingCourseId = intent.getIntExtra("courseId", -1)
        val incomingSectionName = intent.getStringExtra("sectionName")
        val incomingRole = intent.getStringExtra("role")
        val incomingUserId = intent.getIntExtra("id", -1)

// Update ONLY if a new class is selected
        if (incomingCourseId != -1 && incomingCourseId != CurrentCourse.courseId) {

            // Update global course data
            CurrentCourse.courseId = incomingCourseId
            CurrentCourse.sectionName = incomingSectionName ?: ""
            CurrentCourse.currentRole = incomingRole ?: ""
            CurrentCourse.userId = incomingUserId

            Log.d("TeacherClassPage", "Switched to NEW CLASS: ${CurrentCourse.sectionName}")
        } else {
            Log.d("TeacherClassPage", "Same class — keeping CurrentCourse data.")
        }

// Now use CurrentCourse for the UI
        courseId = CurrentCourse.courseId
        sectionName = CurrentCourse.sectionName
        role = CurrentCourse.currentRole
        id = CurrentCourse.userId

        parentLayout = findViewById(R.id.parentLayout)

        // --- Setup Toolbar ---
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.title = CurrentCourse.sectionName
        toolbar.setNavigationOnClickListener {

            finish()
        }
        inflateAssessmentAndLesson(courseId)

        // --- Setup Bottom Navigation ---
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_class
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_class -> {
                    startActivity(Intent(this, TeacherClassPageActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish()
                    true
                }
                R.id.nav_participants -> {
                    startActivity(Intent(this, TeacherParticipantsActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish()
                    true
                }
                R.id.nav_notifications -> {
                    startActivity(Intent(this, TeacherNotificationsActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish()
                    true
                }
                R.id.nav_dll -> {
                    startActivity(Intent(this, DLLViewActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish()
                    true
                }
                else -> false
            }
        }
        NotificationHelper.fetchUnreadCount(bottomNav)
        // --- Setup Floating "Add" Button ---
        val btnAdd = findViewById<Button>(R.id.btnAdd)
        btnAdd.setOnClickListener {
            showAddOptionsDialog()
        }

    }


    fun inflateAssessmentAndLesson(courseId: Int) {
        val apiService = ConnectURL.api
        val call = apiService.getClassContent(courseId)

        call.enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val responseString = response.body()?.string() ?: response.errorBody()?.string()
                if (responseString.isNullOrEmpty()) {

                    return
                }

                try {
                    val jsonObj = org.json.JSONObject(responseString)
                    val dataArray = jsonObj.optJSONArray("data") ?: org.json.JSONArray()

                    parentLayout.removeAllViews()

                    for (i in 0 until dataArray.length()) {
                        val item = dataArray.getJSONObject(i)
                        val type = item.optString("type", "lesson")

                        val itemView: View = when (type) {
                            "lesson" -> layoutInflater.inflate(R.layout.item_class_lesson_card, parentLayout, false)
                            "assessment" -> layoutInflater.inflate(R.layout.item_class_assessment_card, parentLayout, false)
                            else -> continue
                        }

                        val id = item.optInt("id")
                        val cleanContent = removeBase64(item.optString("lesson_content", ""))

                        when (type) {
                            "lesson" -> {
                                val week = itemView.findViewById<TextView>(R.id.tvWeek)
                                val title = itemView.findViewById<TextView>(R.id.tvTitle)
                                val previewDesc = itemView.findViewById<TextView>(R.id.tvDescription)
                                val seeMore = itemView.findViewById<TextView>(R.id.tvSeeMore)
                                val editLesson = itemView.findViewById<ImageButton>(R.id.ibEditLesson)

                                week.text = item.optInt("week_number").toString()
                                title.text = item.optString("lesson_title")
                                previewDesc.text = getFirstSentence(cleanContent)

                                seeMore.setOnClickListener {
                                    val intent = Intent(this@TeacherClassPageActivity, LessonDetailActivity::class.java)
                                    intent.putExtra("lesson_id", id)
                                    startActivity(intent)
                                }

                                editLesson.setOnClickListener {
                                    showEditALessonDialog(
                                        item.optInt("week_number").toString(),
                                        item.optString("lesson_title"),
                                        id
                                    )
                                }
                            }
                            "assessment" -> {
                                val assessmentTitle = itemView.findViewById<TextView>(R.id.tvTitle)
                                val arrowView = itemView.findViewById<ImageView>(R.id.ivArrow)
                                assessmentTitle.text = "Assessment ${item.optInt("assessment_number")}"
                            }
                        }

                        parentLayout.addView(itemView)
                    }

                } catch (e: Exception) {

                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {

                ShowToast.showMessage(this@TeacherClassPageActivity, "Failed to connect to server. Check your internet connection.")
            }
        })
    }

    /** Remove Base64 image data from lesson content */
    private fun removeBase64(content: String): String {
        return content.replace(Regex("data:image/[^;]+;base64,[A-Za-z0-9+/=]+"), "[image]")
            .trim()
    }

    /** Extract first sentence for preview */
    private fun getFirstSentence(text: String): String {
        val cleanText = text.replace(Regex("<ic_read_admin[^>]*>"), "")
        val lines = cleanText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return ""
        val firstLine = lines[0]
        val periodIndex = firstLine.indexOf(".")
        return if (periodIndex != -1) firstLine.substring(0, periodIndex + 1) else firstLine
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
                        showCreateDLLDialog()
                    }
                }
            }
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }


    // In TeacherClassPageActivity.kt

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

        // --- Date & Time Picker Logic (COPIED FROM ASSESSMENT) ---

        val myCalendar = Calendar.getInstance()

        // 🌟 NEW: This variable stores the chosen "Available From" date *and* time.
        var availableFromTimestamp: Long = System.currentTimeMillis()

        // 🌟 NEW: 12-hour format with AM/PM
        val dateTimeFormat = SimpleDateFormat("MM/dd/yy - hh:mm a", Locale.US)

        // --- "Available From" Listeners ---
        val fromTimeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            myCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            myCalendar.set(Calendar.MINUTE, minute)

            // Update the EditText field and save the timestamp
            etFrom.setText(dateTimeFormat.format(myCalendar.time))
            tilFrom.error = null
            availableFromTimestamp = myCalendar.timeInMillis
        }

        val fromDateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, monthOfYear)
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            // Show TimePicker immediately after DatePicker
            TimePickerDialog(this,
                fromTimeSetListener,
                myCalendar.get(Calendar.HOUR_OF_DAY),
                myCalendar.get(Calendar.MINUTE),
                false // 12-hour format
            ).show()
        }

        etFrom.setOnClickListener {
            val datePicker = DatePickerDialog(
                this,
                fromDateSetListener,
                myCalendar.get(Calendar.YEAR),
                myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH)
            )
            // 🌟 CONSTRAINT: Cannot select date before the current time
            datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
            datePicker.show()
        }

        // --- "Available Until" Listeners ---
        val untilTimeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            // Create a temporary Calendar instance for validation only
            val tempCalendar = Calendar.getInstance().apply {
                // Use the date set in the DatePicker (stored in myCalendar)
                timeInMillis = myCalendar.timeInMillis
                set(Calendar.HOUR_OF_DAY, hourOfDay)
                set(Calendar.MINUTE, minute)
            }

            // 🌟 CONSTRAINT VALIDATION: Check if time is before 'Available From' timestamp
            if (tempCalendar.timeInMillis < availableFromTimestamp) {
                ShowToast.showMessage(this@TeacherClassPageActivity,"Cannot select time before 'Available From'")
                // Do not update myCalendar or EditText if invalid
                return@OnTimeSetListener
            }

            // If valid, set the time and update the EditText
            myCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            myCalendar.set(Calendar.MINUTE, minute)
            etUntil.setText(dateTimeFormat.format(myCalendar.time))
            tilUntil.error = null
        }

        val untilDateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, monthOfYear)
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            // Show TimePicker immediately after DatePicker
            TimePickerDialog(this,
                untilTimeSetListener,
                myCalendar.get(Calendar.HOUR_OF_DAY),
                myCalendar.get(Calendar.MINUTE),
                false // 12-hour format
            ).show()
        }

        etUntil.setOnClickListener {
            // 🌟 INITIAL CHECK: Ensure "Available From" has been set
            if (etFrom.text.toString().isEmpty()) {
                ShowToast.showMessage(this, "Please select 'Available From' date and time first.")
                return@setOnClickListener
            }

            val datePicker = DatePickerDialog(
                this,
                untilDateSetListener,
                myCalendar.get(Calendar.YEAR),
                myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH)
            )
            // 🌟 CONSTRAINT: Prevent selecting a date before "Available From" date
            datePicker.datePicker.minDate = availableFromTimestamp
            datePicker.show()
        }

        // --- End of Date & Time Picker Logic ---

        btnNext.setOnClickListener {
            val errorMsg = getString(R.string.error_blank_field)

            // Basic Text Validation
            UIUtils.errorDisplay(this, tilQuarter, etQuarter, true, errorMsg)
            UIUtils.errorDisplay(this, tilWeek, etWeek, true, errorMsg)
            UIUtils.errorDisplay(this, tilFrom, etFrom, true, errorMsg)
            UIUtils.errorDisplay(this, tilUntil, etUntil, true, errorMsg)

            val isValid = etQuarter.text.toString().trim().isNotEmpty() &&
                    etWeek.text.toString().trim().isNotEmpty() &&
                    etFrom.text.toString().trim().isNotEmpty() &&
                    etUntil.text.toString().trim().isNotEmpty()

            if (isValid) {

                // 🌟 FINAL DATE/TIME VALIDATION (in case of manual manipulation)
                try {
                    val fromDate = dateTimeFormat.parse(etFrom.text.toString())
                    val untilDate = dateTimeFormat.parse(etUntil.text.toString())

                    if (fromDate != null && untilDate != null && untilDate.time < fromDate.time) {
                        UIUtils.errorDisplay(this, tilUntil, etUntil, true, "Must be on or after 'Available From' time.")
                        return@setOnClickListener
                    }
                } catch (e: Exception) {
                    // Should not happen if user uses the pickers, but handles edge case
                    UIUtils.errorDisplay(this, tilUntil, etUntil, true, "Invalid date format.")
                    return@setOnClickListener
                }

                // If validation passes, proceed to next activity
                val intent = Intent(this, DailyLessonLogActivity::class.java).apply {
                    putExtra("EXTRA_QUARTER", etQuarter.text.toString().trim())
                    putExtra("EXTRA_WEEK", etWeek.text.toString().trim())
                    // 🌟 PASSING DATE TIME STRING
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

            var isValid = true
            val errorMsg = getString(R.string.error_blank_field)

            // Clear previous errors first
            UIUtils.errorDisplay(this, tilWeekNumber, etWeekNumber, false, "")
            UIUtils.errorDisplay(this, tilLessonTitle, etLessonTitle, false, "")

            // Validate Week Number
            if (weekNumber.isEmpty()) {
                UIUtils.errorDisplay(this, tilWeekNumber, etWeekNumber, true, errorMsg)
                isValid = false
            }

            // Validate Lesson Title
            if (lessonTitle.isEmpty()) {
                UIUtils.errorDisplay(this, tilLessonTitle, etLessonTitle, true, errorMsg)
                isValid = false
            }

            if (!isValid) return@setOnClickListener

            // If valid → proceed
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
    private fun showEditALessonDialog(weekToPreload: String, titleToPreload: String, lessonId : Int) {
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


            val intent = Intent(this, LessonContentActivity::class.java).apply {
                putExtra("EXTRA_WEEK_NUMBER", weekNumber)
                putExtra("EXTRA_LESSON_TITLE", lessonId)
                putExtra("EXTRA_LESSON_ID", lessonId)
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
        val etQuarterNumber = dialogView.findViewById<TextInputEditText>(R.id.etQuarterNumber)
        val tilQuarterNumber = dialogView.findViewById<TextInputLayout>(R.id.tilQuarterNumber)
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

        var availableFromTimestamp: Long = System.currentTimeMillis()

        // 12-hour format with AM/PM
        val dateTimeFormat = SimpleDateFormat("MM/dd/yy - hh:mm a", Locale.US)

        // --- "Available From" Listeners ---
        // --- "Available From" Listeners ---
        val fromTimeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            myCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            myCalendar.set(Calendar.MINUTE, minute)
            etAvailableFrom.setText(dateTimeFormat.format(myCalendar.time))
            tilAvailableFrom.error = null

            availableFromTimestamp = myCalendar.timeInMillis
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
            val datePicker = DatePickerDialog(
                this,
                fromDateSetListener,
                myCalendar.get(Calendar.YEAR),
                myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
            datePicker.show()
        }

        // --- "Available Until" Listeners ---
        val untilTimeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            // If the same day as "Available From", ensure the time is not earlier
            val tempCalendar = Calendar.getInstance().apply {
                timeInMillis = myCalendar.timeInMillis
                set(Calendar.HOUR_OF_DAY, hourOfDay)
                set(Calendar.MINUTE, minute)
            }
            if (tempCalendar.timeInMillis < availableFromTimestamp) {
                ShowToast.showMessage(this@TeacherClassPageActivity,"Cannot select time before 'Available From'" )
                return@OnTimeSetListener
            }

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
                false
            ).show()
        }

        etAvailableUntil.setOnClickListener {
            val datePicker = DatePickerDialog(
                this,
                untilDateSetListener,
                myCalendar.get(Calendar.YEAR),
                myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH)
            )
            // Prevent selecting a date before "Available From"
            datePicker.datePicker.minDate = availableFromTimestamp
            datePicker.show()
        }



        // --- End of Date & Time Picker Logic ---

        btnNext.setOnClickListener {
            val assessmentNumber = etAssessmentNumber.text.toString().trim()
            val assessmentTitle = etAssessmentTitle.text.toString().trim()
            val availableFrom = etAvailableFrom.text.toString().trim()
            val availableUntil = etAvailableUntil.text.toString().trim()
            val assessmentQuarter = etQuarterNumber.text.toString().trim()

            var isValid = true
            val errorMsg = getString(R.string.error_blank_field)

            // Clear previous errors
            UIUtils.errorDisplay(this, tilAssessmentNumber, etAssessmentNumber, false, "")
            UIUtils.errorDisplay(this, tilAssessmentTitle, etAssessmentTitle, false, "")
            UIUtils.errorDisplay(this, tilAvailableFrom, etAvailableFrom, false, "")
            UIUtils.errorDisplay(this, tilAvailableUntil, etAvailableUntil, false, "")
            UIUtils.errorDisplay(this, tilQuarterNumber, etQuarterNumber, false, "")

            // Validate each field
            if (assessmentNumber.isEmpty()) {
                UIUtils.errorDisplay(this, tilAssessmentNumber, etAssessmentNumber, true, errorMsg)
                isValid = false
            }

            if (assessmentTitle.isEmpty()) {
                UIUtils.errorDisplay(this, tilAssessmentTitle, etAssessmentTitle, true, errorMsg)
                isValid = false
            }

            if (availableFrom.isEmpty()) {
                UIUtils.errorDisplay(this, tilAvailableFrom, etAvailableFrom, true, errorMsg)
                isValid = false
            }

            if (availableUntil.isEmpty()) {
                UIUtils.errorDisplay(this, tilAvailableUntil, etAvailableUntil, true, errorMsg)
                isValid = false
            }

            if (assessmentQuarter.isEmpty()) {
                UIUtils.errorDisplay(this, tilQuarterNumber, etQuarterNumber, true, errorMsg)
                isValid = false
            }

            if (!isValid) return@setOnClickListener


            val intent = Intent(this, AssessmentCreatorActivity::class.java).apply {
                putExtra("EXTRA_ASSESSMENT_NUMBER", assessmentNumber)
                putExtra("EXTRA_ASSESSMENT_TITLE", assessmentTitle)
                putExtra("EXTRA_AVAILABLE_FROM", availableFrom)
                putExtra("EXTRA_AVAILABLE_UNTIL", availableUntil)
                putExtra("EXTRA_AVAILABLE_QUARTER", assessmentQuarter)
            }
            createAssessmentLauncher.launch(intent)
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

    }




}