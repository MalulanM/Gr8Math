package com.example.gr8math.Activity.TeacherModule.ClassManager

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.gr8math.R
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.Utils.UIUtils
import com.example.gr8math.ViewModel.AddClassState
import com.example.gr8math.ViewModel.AddClassViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TeacherAddClassActivity : AppCompatActivity() {

    private val viewModel: AddClassViewModel by viewModels()

    private lateinit var etStartTime: TextInputEditText
    private lateinit var etEndTime: TextInputEditText
    private lateinit var etSection: TextInputEditText
    private lateinit var etNumStudents: TextInputEditText

    private lateinit var tilSection: TextInputLayout
    private lateinit var tilStartTime: TextInputLayout
    private lateinit var tilEndTime: TextInputLayout
    private lateinit var tilNumStudents: TextInputLayout

    private lateinit var btnCreateClass: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_add_class)

        val adviserId = intent.getIntExtra("id", 0)

        initViews()
        setupListeners(adviserId)
        setupObservers()
    }

    private fun initViews() {
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        etStartTime = findViewById(R.id.etStartTime)
        etEndTime = findViewById(R.id.etEndTime)
        etSection = findViewById(R.id.etSection)
        etNumStudents = findViewById(R.id.etNumStudents)
        btnCreateClass = findViewById(R.id.btnCreateClass)

        tilSection = findViewById(R.id.tilSection)
        tilNumStudents = findViewById(R.id.tilNumStudents)
        tilStartTime = findViewById(R.id.tilStartTime)
        tilEndTime = findViewById(R.id.tilEndTime)
    }

    private fun setupListeners(adviserId: Int) {
        etStartTime.setOnClickListener { showTimePicker(etStartTime, "Select Start Time") }
        etEndTime.setOnClickListener { showTimePicker(etEndTime, "Select End Time") }

        btnCreateClass.setOnClickListener {
            validateEmptyFields()

            val section = etSection.text.toString().trim()
            val students = etNumStudents.text.toString().trim()
            val start = etStartTime.tag?.toString()
            val end = etEndTime.tag?.toString()

            viewModel.createClass(adviserId, section, students, start, end)
        }
    }

    private fun setupObservers() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is AddClassState.Loading -> {
                    setInputsEnabled(false)
                }
                is AddClassState.Success -> {
                    setInputsEnabled(true)
                    showClassCodeDialog(state.classCode)
                    viewModel.resetState()
                }
                is AddClassState.Error -> {
                    setInputsEnabled(true)
                    ShowToast.showMessage(this, state.message)
                    if (state.message.contains("needed details")) {
                        validateEmptyFields()
                    }
                }
                is AddClassState.Idle -> { }
            }
        }
    }

    private fun validateEmptyFields() {
        val fields = listOf(etSection, etNumStudents, etStartTime, etEndTime)
        val tils = listOf(tilSection, tilNumStudents, tilStartTime, tilEndTime)

        for (i in fields.indices) {
            UIUtils.errorDisplay(this, tils[i], fields[i], fields[i].text.toString().isEmpty(), "Required")
        }
    }

    private fun showTimePicker(timeEditText: TextInputEditText, title: String) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(12)
            .setMinute(0)
            .setTitleText(title)
            .setTheme(R.style.Theme_Gr8_TimePicker_Blue)
            .build()

        picker.addOnPositiveButtonClickListener {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, picker.hour)
            calendar.set(Calendar.MINUTE, picker.minute)

            val displayFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            timeEditText.setText(displayFormat.format(calendar.time))

            val backendFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            timeEditText.tag = backendFormat.format(calendar.time)
        }
        picker.show(supportFragmentManager, "TIME_PICKER")
    }

    private fun showClassCodeDialog(code: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_class_code, null)
        val btnCloseDialog = dialogView.findViewById<ImageButton>(R.id.btnCloseDialog)
        val btnCopyCode = dialogView.findViewById<Button>(R.id.btnCopyCode)
        val tvClassCode = dialogView.findViewById<TextView>(R.id.tvClassCode)

        tvClassCode.text = code

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCloseDialog.setOnClickListener {
            dialog.dismiss()
            setResult(RESULT_OK)
            finish()
        }

        btnCopyCode.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Class Code", code)
            clipboard.setPrimaryClip(clip)
            ShowToast.showMessage(this, "Copied to clipboard!")
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun setInputsEnabled(enabled: Boolean) {
        etSection.isEnabled = enabled
        etNumStudents.isEnabled = enabled
        etStartTime.isEnabled = enabled
        etEndTime.isEnabled = enabled
        btnCreateClass.isEnabled = enabled
    }
}