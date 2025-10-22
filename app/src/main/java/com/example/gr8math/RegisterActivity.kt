package com.example.gr8math  // <-- match your actual package

import android.os.Bundle
import android.widget.EditText
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import android.app.DatePickerDialog
import android.content.Intent
import android.widget.ArrayAdapter
import android.widget.TextView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast
import android.util.Log


class RegisterActivity : AppCompatActivity() {
    lateinit  var genderField: MaterialAutoCompleteTextView
    lateinit  var date: EditText
    lateinit  var registerButton: Button

    lateinit var email: EditText
    lateinit var password: EditText

    lateinit var confirmPassword: EditText
    lateinit var firstName: EditText
    lateinit var lastName: EditText
    lateinit var LRN: EditText

    lateinit var MessageBox : TextView

    var selectedGender: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_activity)
        Init()
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        genderField.setOnItemClickListener { parent, view, position, id ->
            selectedGender = parent.getItemAtPosition(position).toString()
        }

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


                    val formatter = SimpleDateFormat("MM/dd/yy", Locale.getDefault())
                    val formattedDate = formatter.format(selectedDate.time)


                    date.setText(formattedDate)
                },
                year,
                month,
                day
            )
            datePicker.datePicker.maxDate = System.currentTimeMillis() -  24 * 60 * 60 * 1000
            datePicker.show()
        }

        registerButton.setOnClickListener {
            UserRegister()
        }

    }

    fun Init() {
         email = findViewById<EditText>(R.id.email)
         password = findViewById<EditText>(R.id.password)
         confirmPassword = findViewById<EditText>(R.id.confirmPass)
        registerButton = findViewById<Button>(R.id.btnSubmit)
         firstName = findViewById<EditText>(R.id.firstName)
         lastName = findViewById<EditText>(R.id.lastName)
         LRN = findViewById<EditText>(R.id.LRN)
        date = findViewById<EditText>(R.id.etDob)
        genderField = findViewById<MaterialAutoCompleteTextView>(R.id.etGender)
        val items = listOf("Male", "Female", "Prefer not to say")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items)
        genderField.setAdapter(adapter)
        MessageBox = findViewById<TextView>(R.id.message)
    }

    fun isValidPassword(password: String): Boolean {
        val passwordPattern = Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z\\d])[A-Za-z\\d[^A-Za-z\\d]]{8,16}\$")
        return passwordPattern.matches(password)
    }

    fun UserRegister() {
        val emailText = email.text.toString().trim()
        val passwordText = password.text.toString().trim()
        val confirmPassText = confirmPassword.text.toString().trim()
        val firstNameText = firstName.text.toString().trim()
        val lastNameText = lastName.text.toString().trim()
        val LRNText = LRN.text.toString().trim()
        val birthDateText = date.text.toString().trim()
        val genderText = selectedGender.trim()


        if(emailText.isEmpty() || passwordText.isEmpty() || firstNameText.isEmpty() ||
            lastNameText.isEmpty() || LRNText.isEmpty() || birthDateText.isEmpty() || genderText.isEmpty() || LRNText.length != 12 || !LRNText.all{it.isDigit()}) {
            ShowToast.showMessage(this@RegisterActivity, "Please input valid credentials")
         return
        }

        if(passwordText != confirmPassText){
            ShowToast.showMessage(this@RegisterActivity, "Passwords do not match")
          return
        }

        if(!isValidPassword(passwordText)){
             ShowToast.showMessage(this@RegisterActivity, "Password Requirement:\n- 8-16 characters\n- At least one uppercase letter\n- At least one number\n- At least one special character")
          return
        }

        val apiService = ConnectURL.api
        // API call
            val user = User(
            firstName = firstNameText,
            lastName = lastNameText,
            emailAdd = emailText,
            passwordHash = passwordText,
            passwordHashConfirmation = confirmPassText,
            gender = genderText,
            birthdate = birthDateText,
            roles = "Student",
            LRN = LRNText
        )

        val call = apiService.registerUser(user)
        call.enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                try {
                    Log.e(
                        "RegisterResponse",
                        "Code: ${response.code()} | Body: ${
                            response.body()?.string()
                        } | ErrorBody: ${response.errorBody()?.string()}"
                    )
                    if (response.isSuccessful) {
                        ShowToast.showMessage(this@RegisterActivity, "You have successfully created an account, please wait for it to be approved.")
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            val intent = Intent(this@RegisterActivity, AppLoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        }, 3000)
                    } else {
                        ShowToast.showMessage(this@RegisterActivity, "An error occurred while handling the response.")
                    }
                } catch (e: Exception) {
                    Log.e("registerAcc", "Exception: ${e.message}", e)
                    ShowToast.showMessage(this@RegisterActivity, "An error occurred while handling the response.")
                }
            }


            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("RetrofitError", "onFailure: ${t.localizedMessage}", t)
                ShowToast.showMessage(this@RegisterActivity, "Failed to connect to server. Check your internet connection.")
            }
        })
    }



}
