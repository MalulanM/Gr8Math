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

class TeacherClassPageActivity : AppCompatActivity() {

    private var id: Int = 0
    private var courseId: Int = 0
    private lateinit var role: String
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classpage_teacher)

        if (CurrentCourse.userId == 0) CurrentCourse.userId = intent.getIntExtra("id", 0)
        if (CurrentCourse.courseId == 0) CurrentCourse.courseId = intent.getIntExtra("courseId", 0)
        if (CurrentCourse.currentRole.isEmpty()) CurrentCourse.currentRole = intent.getStringExtra("role") ?: ""
        if (CurrentCourse.sectionName.isEmpty()) CurrentCourse.sectionName = intent.getStringExtra("sectionName") ?: ""

        id = CurrentCourse.userId
        courseId = CurrentCourse.courseId
        role = CurrentCourse.currentRole
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
                    Log.e("API_ERROR", "Empty response")
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

                                arrowView.setOnClickListener {
                                    val intent = Intent(this@TeacherClassPageActivity, AssessmentDetailActivity::class.java)
                                    intent.putExtra("AssessmentId", id)
                                    startActivity(intent)
                                }
                            }
                        }

                        parentLayout.addView(itemView)
                    }

                } catch (e: Exception) {
                    Log.e("API_ERROR", "Failed to parse response: ${e.localizedMessage}", e)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("API_ERROR", "Internet: ${t.localizedMessage}", t)
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
        val cleanText = text.replace(Regex("<img[^>]*>"), "")
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