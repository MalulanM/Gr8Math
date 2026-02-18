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

class StudentRegisterActivity : AppCompatActivity() {

    private val viewModel: RegisterViewModel by viewModels()

    // UI Components
    lateinit var genderField: MaterialAutoCompleteTextView
    lateinit var date: EditText
    lateinit var registerButton: Button
    lateinit var email: EditText
    lateinit var password: EditText
    lateinit var confirmPassword: EditText
    lateinit var firstName: EditText
    lateinit var lastName: EditText
    lateinit var LRN: EditText
    lateinit var MessageBox: TextView
    lateinit var nextButton: Button
    lateinit var loadingLayout: View
    lateinit var loadingProgress: View
    lateinit var loadingText: TextView
    lateinit var tilEmail: TextInputLayout
    lateinit var tilFirstName: TextInputLayout
    lateinit var tilLastName: TextInputLayout
    lateinit var tilLRN: TextInputLayout
    lateinit var tilGender: TextInputLayout
    lateinit var tilBirthDate: TextInputLayout

    // Temp Data Storage
    var firstNameText = ""
    var lastNameText = ""
    var emailText = ""
    var LRNText = ""
    var birthDateText = ""
    var genderText = ""
    var selectedGender: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showRegisterInfo()
        setupViewModelObservers()
    }

    private fun setupViewModelObservers() {
        // 1. Loading State
        viewModel.isLoading.observe(this) { isLoading ->
            UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, isLoading)
            if (::nextButton.isInitialized) nextButton.isEnabled = !isLoading
            if (::registerButton.isInitialized) registerButton.isEnabled = !isLoading
        }

        // 2. Email Validation Observer
        viewModel.emailExists.observe(this) { exists ->
            if (exists) {
                UIUtils.errorDisplay(this, tilEmail, email, true, "Email already exists!", true)
            }
        }

        // 3. LRN Validation Observer
        viewModel.lrnExists.observe(this) { exists ->
            if (exists) {
                UIUtils.errorDisplay(this, tilLRN, LRN, true, "LRN already exists!", true)
            }
        }

        // 4. Proceed Observer (Both Valid)
        viewModel.canProceed.observe(this) { canProceed ->
            if (canProceed) {
                showPasswordRegistration()
            }
        }

        // 5. Final Registration Observer
        viewModel.registerState.observe(this) { result ->
            result.onSuccess { user ->
                showUserAgreement(user.firstLogin, user.id)
            }
            result.onFailure { error ->
                ShowToast.showMessage(this, error.message ?: "Registration Failed")
                if (::password.isInitialized) {
                    password.isEnabled = true
                    confirmPassword.isEnabled = true
                    registerButton.isEnabled = true
                }
            }
        }
    }

    fun showRegisterInfo() {
        setContentView(R.layout.register_activity)
        Init()

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        genderField.setOnItemClickListener { parent, _, position, _ ->
            selectedGender = parent.getItemAtPosition(position).toString()
        }

        date.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                this,
                { _, y, m, d ->
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(y, m, d)
                    val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                    date.setText(formatter.format(selectedDate.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.datePicker.maxDate = System.currentTimeMillis() - 24 * 60 * 60 * 1000
            datePicker.show()
        }

        nextButton.setOnClickListener {
            val fields = listOf(email, firstName, lastName, LRN, date, genderField)
            val tils = listOf(tilEmail, tilFirstName, tilLastName, tilLRN, tilBirthDate, tilGender)

            var hasError = false
            for (i in fields.indices) {
                UIUtils.errorDisplay(this, tils[i], fields[i], true, "Please enter the needed details.")
                if (fields[i].text.toString().trim().isEmpty()) {
                    hasError = true
                }
            }

            if (hasError) return@setOnClickListener

            firstNameText = firstName.text.toString().trim()
            lastNameText = lastName.text.toString().trim()
            emailText = email.text.toString().trim()
            LRNText = LRN.text.toString().trim()
            birthDateText = date.text.toString().trim()
            genderText = selectedGender.trim()

            if (!LRNText.matches(Regex("^\\d{12}$"))) {
                UIUtils.errorDisplay(this, tilLRN, LRN, true, "LRN requires at least 12 digits", true)
                return@setOnClickListener
            }

            // TRIGGER VALIDATION FOR BOTH
            viewModel.validateStudentDetails(emailText, LRNText)
        }
    }

    fun showPasswordRegistration() {
        setContentView(R.layout.change_password_activity)
        Init2()
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        val enableBtn = {
            registerButton.isEnabled = password.text.toString().isNotEmpty() && confirmPassword.text.toString().isNotEmpty()
        }

        password.setOnKeyListener { _, _, _ -> enableBtn(); false }
        confirmPassword.setOnKeyListener { _, _, _ -> enableBtn(); false }

        registerButton.setOnClickListener {
            UserRegister()
        }
    }

    fun UserRegister() {
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

        password.isEnabled = false
        confirmPassword.isEnabled = false

        viewModel.registerStudent(
            email = emailText,
            pass = passwordText,
            first = firstNameText,
            last = lastNameText,
            gender = genderText,
            birth = birthDateText,
            lrn = LRNText
        )
    }

    fun Init() {
        email = findViewById(R.id.email)
        firstName = findViewById(R.id.firstName)
        lastName = findViewById(R.id.lastName)
        LRN = findViewById(R.id.LRN)
        date = findViewById(R.id.etDob)
        genderField = findViewById(R.id.etGender)
        val items = listOf("Male", "Female")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items)
        genderField.setAdapter(adapter)

        MessageBox = findViewById(R.id.message)
        nextButton = findViewById(R.id.btnNext)
        tilEmail = findViewById(R.id.tilEmail)
        tilFirstName = findViewById(R.id.tilFirstName)
        tilLastName = findViewById(R.id.tilLastName)
        tilLRN = findViewById(R.id.tilLRN)
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
        val passwordPattern = Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,16}\$")
        return passwordPattern.matches(password)
    }

    private fun showUserAgreement(isFirstTime: Boolean, userId: Int?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_terms_and_conditions, null)
        val chkBoxAgree = dialogView.findViewById<CheckBox>(R.id.cbTerms)
        val btnProceed = dialogView.findViewById<Button>(R.id.btnProceed)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnProceed.isEnabled = false

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
                            this@StudentRegisterActivity,
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
                            this@StudentRegisterActivity,
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