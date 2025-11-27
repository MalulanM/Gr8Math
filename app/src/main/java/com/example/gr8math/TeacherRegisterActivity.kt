package com.example.gr8math

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.gr8math.utils.UIUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TeacherRegisterActivity : AppCompatActivity() {

    // Fields specifically for Teacher
    lateinit var teachingPosField: MaterialAutoCompleteTextView
    lateinit var tilTeachingPos: TextInputLayout

    // Common fields
    lateinit var email: EditText
    lateinit var firstName: EditText
    lateinit var lastName: EditText
    lateinit var date: EditText
    lateinit var genderField: MaterialAutoCompleteTextView
    lateinit var nextButton: Button

    // TILs for validation
    lateinit var tilEmail: TextInputLayout
    lateinit var tilFirstName: TextInputLayout
    lateinit var tilLastName: TextInputLayout
    lateinit var tilGender: TextInputLayout
    lateinit var tilBirthDate: TextInputLayout

    var selectedGender: String = ""
    var selectedTeachingPos: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // --- USE YOUR NEW LAYOUT ---
        setContentView(R.layout.activity_teacher_register)

        initViews()
        setupDropdowns()
        setupDatePicker()

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        nextButton.setOnClickListener {
            if (validateForm()) {
                // Proceed to Password Creation
                val intent = Intent(this, PasswordCreationActivity::class.java)

                // Pass Teacher Data
                intent.putExtra("EXTRA_EMAIL", email.text.toString().trim())
                intent.putExtra("EXTRA_FIRST_NAME", firstName.text.toString().trim())
                intent.putExtra("EXTRA_LAST_NAME", lastName.text.toString().trim())
                intent.putExtra("EXTRA_TEACHING_POS", selectedTeachingPos) // Unique to Teacher
                intent.putExtra("EXTRA_GENDER", selectedGender)
                intent.putExtra("EXTRA_BIRTHDATE", date.text.toString().trim())

                // Important: Identify this as a Teacher
                intent.putExtra("EXTRA_ROLE", "Teacher")

                startActivity(intent)
            }
        }
    }

    private fun initViews() {
        // Initialize all views from activity_teacher_register.xml
        email = findViewById(R.id.email)
        firstName = findViewById(R.id.firstName)
        lastName = findViewById(R.id.lastName)

        // Ensure your XML has these IDs
        teachingPosField = findViewById(R.id.etTeachingPos)
        tilTeachingPos = findViewById(R.id.tilTeachingPos)

        date = findViewById(R.id.etDob)
        genderField = findViewById(R.id.etGender)
        nextButton = findViewById(R.id.btnNext)

        tilEmail = findViewById(R.id.tilEmail)
        tilFirstName = findViewById(R.id.tilFirstName)
        tilLastName = findViewById(R.id.tilLastName)
        tilGender = findViewById(R.id.tilGender)
        tilBirthDate = findViewById(R.id.tilBirthdate)
    }

    private fun setupDropdowns() {
        // Gender Dropdown
        val genderItems = listOf("Male", "Female")
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genderItems)
        genderField.setAdapter(genderAdapter)
        genderField.setOnItemClickListener { parent, _, position, _ ->
            selectedGender = parent.getItemAtPosition(position).toString()
        }

        // Teaching Position Dropdown
        val posItems = listOf("Teacher I", "Teacher II", "Teacher III", "Teacher IV", "Teacher V", "Teacher VI", "Teacher VII", "Master Teacher I", "Master Teacher II", "Master Teacher III", "Master Teacher IV", "Master Teacher V")
        val posAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, posItems)
        teachingPosField.setAdapter(posAdapter)
        teachingPosField.setOnItemClickListener { parent, _, position, _ ->
            selectedTeachingPos = parent.getItemAtPosition(position).toString()
        }
    }

    private fun setupDatePicker() {
        date.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(selectedYear, selectedMonth, selectedDay)
                    val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                    date.setText(formatter.format(selectedDate.time))
                },
                year,
                month,
                day
            )
            datePicker.datePicker.maxDate = System.currentTimeMillis() - 24 * 60 * 60 * 1000
            datePicker.show()
        }
    }

    private fun validateForm(): Boolean {
        val fields = listOf(email, firstName, lastName, teachingPosField, date, genderField)
        val tils = listOf(tilEmail, tilFirstName, tilLastName, tilTeachingPos, tilBirthDate, tilGender)

        var hasError = false
        for (i in fields.indices) {
            val field = fields[i]
            val til = tils[i]
            UIUtils.errorDisplay(this, til, field, true, "Please enter the needed details.")
            if (field.text.toString().trim().isEmpty()) {
                hasError = true
            }
        }
        return !hasError
    }
}