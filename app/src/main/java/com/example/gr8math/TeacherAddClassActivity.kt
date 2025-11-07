package com.example.gr8math // Make sure this matches your package name

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TeacherAddClassActivity : AppCompatActivity() {

    private lateinit var etStartTime: TextInputEditText
    private lateinit var etEndTime: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_add_class)

        // Find views
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        val createButton: Button = findViewById(R.id.btnCreateClass)
        etStartTime = findViewById(R.id.etStartTime)
        etEndTime = findViewById(R.id.etEndTime)

        // 1. Make the toolbar's back button work
        toolbar.setNavigationOnClickListener {
            finish() // Closes this "floating" page
        }

        // 2. Set click listener for the "Create Class" button
        createButton.setOnClickListener {
            // UPDATED: This now shows the "Class Code" dialog
            showClassCodeDialog()
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
    private fun showClassCodeDialog() {
        // Inflate the custom dialog layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_class_code, null)

        // Find the views inside the custom dialog
        val btnCloseDialog = dialogView.findViewById<ImageButton>(R.id.btnCloseDialog)
        val btnCopyCode = dialogView.findViewById<Button>(R.id.btnCopyCode)
        val tvClassCode = dialogView.findViewById<TextView>(R.id.tvClassCode)

        // TODO: This is where you would get the *real* class code from your API
        // For now, we'll use the placeholder text.
        val classCode = tvClassCode.text.toString()

        // Build the dialog
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false) // User can't click outside to close
            .create()

        // --- Set Click Listeners for the Dialog ---

        // 1. Close Button
        btnCloseDialog.setOnClickListener {
            dialog.dismiss() // Close the dialog
        }

        // 2. Copy Button
        btnCopyCode.setOnClickListener {
            // Get clipboard service
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            // Create a ClipData object
            val clip = ClipData.newPlainText("Class Code", classCode)
            // Set the data to the clipboard
            clipboard.setPrimaryClip(clip)

            // Show a "Copied!" message
            Toast.makeText(this, "Copied to clipboard!", Toast.LENGTH_SHORT).show()

            // Optional: Close the dialog after copying
            // dialog.dismiss()
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
            .build()

        picker.addOnPositiveButtonClickListener {
            // Create a Calendar instance to format the time
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, picker.hour)
            calendar.set(Calendar.MINUTE, picker.minute)

            // Format the time into "h:mm a" (e.g., "7:00 AM")
            val format = SimpleDateFormat("h:mm a", Locale.getDefault())
            timeEditText.setText(format.format(calendar.time))
        }

        picker.show(supportFragmentManager, "TIME_PICKER_TAG")
    }
}