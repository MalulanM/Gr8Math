package com.example.gr8math.Activity.LoginAndRegister

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.gr8math.Activity.PrivacyPolicyActivity
import com.example.gr8math.Activity.TermsAndConditionsActivity
import com.example.gr8math.R
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.Utils.UIUtils
import com.example.gr8math.ViewModel.RegisterViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TeacherRegisterActivity : AppCompatActivity() {

    // 1. Initialize ViewModel
    private val viewModel: RegisterViewModel by viewModels()

    // UI Components
    lateinit var genderField: MaterialAutoCompleteTextView
    lateinit var date: EditText
    lateinit var email: EditText
    lateinit var firstName: EditText
    lateinit var lastName: EditText
    lateinit var teachingPos: MaterialAutoCompleteTextView
    lateinit var MessageBox: TextView
    lateinit var addButton: Button // Next Button

    lateinit var loadingLayout: View
    lateinit var loadingProgress: View
    lateinit var loadingText: TextView

    lateinit var tilEmail: TextInputLayout
    lateinit var tilFirstName: TextInputLayout
    lateinit var tilLastName: TextInputLayout
    lateinit var tilTeachingPos: TextInputLayout
    lateinit var tilGender: TextInputLayout
    lateinit var tilBirthDate: TextInputLayout

    // Password Screen UI
    lateinit var password: EditText
    lateinit var confirmPassword: EditText
    lateinit var registerButton: Button // Save Button

    // Temp Data Storage
    var selectedGender = ""
    var selectedTeachingPos = ""
    var firstNameTemp = ""
    var lastNameTemp = ""
    var emailTemp = ""
    var birthDateTemp = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showRegisterInfo()
        setupViewModelObservers()
    }

    // ----------------------------------------------------------
    // VIEW MODEL OBSERVERS
    // ----------------------------------------------------------
    private fun setupViewModelObservers() {
        // A. Observe Loading State
        viewModel.isLoading.observe(this) { isLoading ->
            UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, isLoading)

            // Disable buttons to prevent double-clicks
            if (::addButton.isInitialized) addButton.isEnabled = !isLoading
            if (::registerButton.isInitialized) registerButton.isEnabled = !isLoading
        }


        viewModel.emailExists.observe(this) { exists ->
            if (exists) {

                UIUtils.errorDisplay(
                    this,
                    tilEmail,
                    email,
                    true,
                    "Email already exists!",
                    true
                )
            } else {
                showPasswordRegistration()
            }
        }

        // C. Observe Final Registration Result (Step 2)
        viewModel.registerState.observe(this) { result ->
            result.onSuccess { user ->
                showUserAgreement(user.firstLogin, user.id)
            }
            result.onFailure { error ->
                ShowToast.showMessage(this, error.message ?: "Registration Failed")

                // Re-enable input if it failed
                if (::password.isInitialized) {
                    password.isEnabled = true
                    confirmPassword.isEnabled = true
                    registerButton.isEnabled = true
                }
            }
        }
    }

    // ----------------------------------------------------------
    // SCREEN 1: DETAILS
    // ----------------------------------------------------------
    fun showRegisterInfo() {
        setContentView(R.layout.activity_teacher_register)
        Init()

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        genderField.setOnItemClickListener { parent, _, position, _ ->
            selectedGender = parent.getItemAtPosition(position).toString()
        }

        teachingPos.setOnItemClickListener { parent, _, position, _ ->
            selectedTeachingPos = parent.getItemAtPosition(position).toString()
        }

        date.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                this,
                { _, y, m, d ->
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(y, m, d)
                    val formatted = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                    date.setText(formatted.format(selectedDate.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.datePicker.maxDate = System.currentTimeMillis() - 86400000
            datePicker.show()
        }

        addButton.setOnClickListener {
            val fields = listOf(email, firstName, lastName, teachingPos, date, genderField)
            val tils = listOf(tilEmail, tilFirstName, tilLastName, tilTeachingPos, tilBirthDate, tilGender)

            var hasError = false
            for (i in fields.indices) {
                UIUtils.errorDisplay(
                    this,
                    tils[i],
                    fields[i],
                    true,
                    "Please enter the needed details"
                )
                if (fields[i].text.toString().trim().isEmpty()) hasError = true
            }

            if (hasError) return@setOnClickListener

            // Save temp data
            firstNameTemp = firstName.text.toString().trim()
            lastNameTemp = lastName.text.toString().trim()
            emailTemp = email.text.toString().trim()
            birthDateTemp = date.text.toString().trim()

            // CRITICAL CHANGE: Check Email existence instead of going directly to password
            viewModel.checkEmail(emailTemp)
        }
    }

    // ----------------------------------------------------------
    // SCREEN 2: PASSWORD
    // ----------------------------------------------------------
    fun showPasswordRegistration() {
        setContentView(R.layout.change_password_activity)
        Init2()

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        registerButton.isEnabled = false

        val enableBtn = {
            registerButton.isEnabled =
                password.text.toString().isNotEmpty() &&
                        confirmPassword.text.toString().isNotEmpty()
        }

        password.setOnKeyListener { _, _, _ -> enableBtn(); false }
        confirmPassword.setOnKeyListener { _, _, _ -> enableBtn(); false }

        registerButton.setOnClickListener { registerFinal() }
    }

    // ----------------------------------------------------------
    // REGISTER LOGIC
    // ----------------------------------------------------------
    fun registerFinal() {
        val passwordText = password.text.toString().trim()
        val confirmPassText = confirmPassword.text.toString().trim()

        if (passwordText != confirmPassText) {
            ShowToast.showMessage(this, "Passwords do not match")
            return
        }

        if (!isValidPassword(passwordText)) {
            ShowToast.showMessage(this, "Password Invalid")
            return
        }

        // Disable inputs while loading
        password.isEnabled = false
        confirmPassword.isEnabled = false
        registerButton.isEnabled = false

        // Trigger Final Registration
        viewModel.registerTeacher(
            email = emailTemp,
            pass = passwordText,
            first = firstNameTemp,
            last = lastNameTemp,
            gender = selectedGender,
            birth = birthDateTemp,
            position = selectedTeachingPos
        )
    }

    // ----------------------------------------------------------
    // INITIALIZATION & HELPERS
    // ----------------------------------------------------------
    fun Init() {
        email = findViewById(R.id.email)
        firstName = findViewById(R.id.firstName)
        lastName = findViewById(R.id.lastName)
        teachingPos = findViewById(R.id.etTeachingPos)
        date = findViewById(R.id.etDob)
        genderField = findViewById(R.id.etGender)

        genderField.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, listOf("Male", "Female"))
        )

        teachingPos.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                listOf(
                    "Teacher I", "Teacher II", "Teacher III", "Teacher IV",
                    "Teacher V", "Teacher VI", "Teacher VII",
                    "Master I", "Master II", "Master III", "Master IV", "Master V"
                )
            )
        )

        MessageBox = findViewById(R.id.message)
        addButton = findViewById(R.id.btnNext)

        tilEmail = findViewById(R.id.tilEmail)
        tilFirstName = findViewById(R.id.tilFirstName)
        tilLastName = findViewById(R.id.tilLastName)
        tilTeachingPos = findViewById(R.id.tilTeachingPos)
        tilGender = findViewById(R.id.tilGender)
        tilBirthDate = findViewById(R.id.tilBirthdate)

        loadingLayout = findViewById(R.id.loadingLayout)
        loadingProgress = findViewById(R.id.loadingProgressBg)
        loadingText = findViewById(R.id.loadingText)
    }

    fun Init2() {
        password = findViewById(R.id.etNewPass)
        confirmPassword = findViewById(R.id.etRePass)
        registerButton = findViewById(R.id.btnSave)

        loadingLayout = findViewById(R.id.loadingLayout)
        loadingProgress = findViewById(R.id.loadingProgressBg)
        loadingText = findViewById(R.id.loadingText)
    }

    fun isValidPassword(password: String): Boolean {
        val pattern = Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,16}$")
        return pattern.matches(password)
    }

    // ----------------------------------------------------------
    // TERMS & CONDITIONS
    // ----------------------------------------------------------
    private fun showUserAgreement(isFirstTime: Boolean, userId: Int?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_terms_and_conditions, null)
        val chkBoxAgree = dialogView.findViewById<CheckBox>(R.id.cbTerms)
        val btnProceed = dialogView.findViewById<Button>(R.id.btnProceed)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnProceed.isEnabled = false

        // Hyperlinks logic
        val fullText = chkBoxAgree.text.toString()
        val spannable = SpannableString(fullText)
        val setLinkStyle = { ds: TextPaint ->
            ds.isUnderlineText = true
            ds.color = resources.getColor(R.color.colorMatisse, theme)
        }

        val termsLinkText = "Terms and Conditions"
        val termsStart = fullText.indexOf(termsLinkText)
        if (termsStart != -1) {
            spannable.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    startActivity(
                        Intent(
                            this@TeacherRegisterActivity,
                            TermsAndConditionsActivity::class.java
                        )
                    )
                }
                override fun updateDrawState(ds: TextPaint) { setLinkStyle(ds) }
            }, termsStart, termsStart + termsLinkText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        val privacyLinkText = "Privacy Policy"
        val privacyStart = fullText.indexOf(privacyLinkText)
        if (privacyStart != -1) {
            spannable.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    startActivity(
                        Intent(
                            this@TeacherRegisterActivity,
                            PrivacyPolicyActivity::class.java
                        )
                    )
                }
                override fun updateDrawState(ds: TextPaint) { setLinkStyle(ds) }
            }, privacyStart, privacyStart + privacyLinkText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        chkBoxAgree.text = spannable
        chkBoxAgree.movementMethod = LinkMovementMethod.getInstance()
        chkBoxAgree.setOnCheckedChangeListener { _, checked -> btnProceed.isEnabled = checked }

        btnProceed.setOnClickListener {
            dialog.dismiss()

            // Update status
            if (userId != null && isFirstTime) {
                 viewModel.updateFirstLogin(userId)
            }

            UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)

            Handler(Looper.getMainLooper()).postDelayed({
                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                val nextIntent = Intent(this, AppLoginActivity::class.java)
                nextIntent.putExtra("toast_msg", "Registered successfully")
                startActivity(nextIntent)
                setResult(RESULT_OK)
                finish()
            }, 1000)
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }
}