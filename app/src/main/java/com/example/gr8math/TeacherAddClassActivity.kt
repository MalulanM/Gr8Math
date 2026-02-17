package com.example.gr8math // Make sure this matches your package name

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.ClassData
import com.example.gr8math.utils.ShowToast
import com.example.gr8math.utils.UIUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import okhttp3.ResponseBody
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TeacherAddClassActivity : AppCompatActivity() {

    private lateinit var etStartTime: TextInputEditText
    private lateinit var etEndTime: TextInputEditText

    private lateinit var etSection: TextInputEditText

    private lateinit var etNumStudents: TextInputEditText

    private lateinit var tilSection : TextInputLayout
    private lateinit var tilStartTime : TextInputLayout
    private lateinit var tilEndTime: TextInputLayout
    private lateinit var tilNumStudents : TextInputLayout


    private lateinit var btnCreateClass: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_add_class)
        val id = intent.getIntExtra("id",0)
        // Find views
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        val createButton: Button = findViewById(R.id.btnCreateClass)
        etStartTime = findViewById(R.id.etStartTime)
        etEndTime = findViewById(R.id.etEndTime)
        etSection = findViewById(R.id.etSection)
        etNumStudents = findViewById(R.id.etNumStudents)
        btnCreateClass = findViewById(R.id.btnCreateClass)
        tilSection = findViewById(R.id.tilSection)
        tilNumStudents = findViewById(R.id.tilNumStudents)
        tilStartTime = findViewById(R.id.tilStartTime)
        tilEndTime = findViewById(R.id.tilEndTime)

        // 1. Make the toolbar's back button work
        toolbar.setNavigationOnClickListener {
            finish() // Closes this "floating" page
        }

        // 2. Set click listener for the "Create Class" button
        createButton.setOnClickListener {
            val fields = listOf(etSection, etNumStudents, etStartTime, etEndTime)
            val tils = listOf(tilSection, tilNumStudents, tilStartTime, tilEndTime)

            var hasError = false
            for (i in fields.indices) {
                val field = fields[i]
                val til = tils[i]
                UIUtils.errorDisplay(this@TeacherAddClassActivity,til, field, true, "Please enter the needed details.")
                if (field.text.toString().trim().isEmpty()) {
                    hasError = true
                }
            }

            if (!hasError) {
                addClass(id)
            }
        }

        // 3. Set click listeners for the time fields
        etStartTime.setOnClickListener {
            showTimePicker(etStartTime, "Select Start Time")
        }
        etEndTime.setOnClickListener {
            showTimePicker(etEndTime, "Select End Time")
        }
    }

    /**
     * Shows the "Class Code" dialog.
     */
    private fun showClassCodeDialog(code : String) {
        // Inflate the custom dialog layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_class_code, null)

        // Find the views inside the custom dialog
        val btnCloseDialog = dialogView.findViewById<ImageButton>(R.id.btnCloseDialog)
        val btnCopyCode = dialogView.findViewById<Button>(R.id.btnCopyCode)
        val tvClassCode = dialogView.findViewById<TextView>(R.id.tvClassCode)
        tvClassCode.text = code

        val classCode = tvClassCode.text.toString()

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false) // User can't click outside to close
            .create()


        btnCloseDialog.setOnClickListener {
            dialog.dismiss()
            setResult(RESULT_OK)
            finish()
        }

        // 2. Copy Button
        btnCopyCode.setOnClickListener {
            // Get clipboard service
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            // Create a ClipData object
            val clip = ClipData.newPlainText("Class Code", classCode)
            // Set the data to the clipboard
            clipboard.setPrimaryClip(clip)


            ShowToast.showMessage(this, "Copied to clipboard!")
            
        }

        // Make the dialog background transparent so our CardView's rounded corners are visible
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Show the dialog
        dialog.show()
    }

    /**
     * Shows a MaterialTimePicker dialog and sets the selected time
     * in the provided TextInputEditText.
     */
    private fun showTimePicker(timeEditText: TextInputEditText, title: String) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(12)
            .setMinute(0)
            .setTitleText(title)
            .setTheme(R.style.Theme_Gr8_TimePicker_Blue)
            .build()

        picker.addOnPositiveButtonClickListener {
            // Create a Calendar instance to format the time
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, picker.hour)
            calendar.set(Calendar.MINUTE, picker.minute)

            // Format the time into "h:mm a" (e.g., "7:00 AM")
            val format = SimpleDateFormat("h:mm a", Locale.getDefault())
            timeEditText.setText(format.format(calendar.time))

            val backendFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            timeEditText.tag = backendFormat.format(calendar.time)
        }

        picker.show(supportFragmentManager, "TIME_PICKER_TAG")
    }

    private fun addClass(id: Int){
        val apiService = ConnectURL.api

        setInputsEnabled(false)

        val classData = ClassData (
            adviserId = id,
            className = etSection.text.toString(),
            arrivalTime = etStartTime.tag.toString(),
            dismissalTime = etEndTime.tag.toString(),
            classSize = etNumStudents.text.toString().toInt()
        )

        val call = apiService.saveClass(classData)
        call.enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: retrofit2.Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {

                val responseString = response.errorBody()?.string() ?: response.body()?.string()
                val jsonObj = org.json.JSONObject(responseString)
                val success = jsonObj.optBoolean("success")
                val message = jsonObj.optString("message")
                val data = jsonObj.optString("data")
                if (response.isSuccessful && success) {
                    ShowToast.showMessage(this@TeacherAddClassActivity, message)

                    showClassCodeDialog(data)
                } else {
                    ShowToast.showMessage(this@TeacherAddClassActivity, message)
                    setInputsEnabled(true)
                }
            }
            override fun onFailure(call: retrofit2.Call<ResponseBody>, t: Throwable) {

                ShowToast.showMessage(this@TeacherAddClassActivity, "Error: ${t.message}")
                setInputsEnabled(true)
            }
        })


    }

    private fun setInputsEnabled(enabled: Boolean) {
        etSection.isEnabled = enabled
        etNumStudents.isEnabled = enabled
        etStartTime.isEnabled = enabled
        etEndTime.isEnabled = enabled
        btnCreateClass.isEnabled = enabled
    }
}