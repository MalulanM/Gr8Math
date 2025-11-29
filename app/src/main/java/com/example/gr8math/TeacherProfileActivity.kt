package com.example.gr8math

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gr8math.adapter.Badge
import com.example.gr8math.adapter.BadgeSelectionAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TeacherProfileActivity : AppCompatActivity() {

    // UI Elements
    private lateinit var etFirstName: TextInputEditText
    private lateinit var ivEditFirstName: ImageView

    private lateinit var etLastName: TextInputEditText
    private lateinit var ivEditLastName: ImageView

    private lateinit var etTeachingPos: MaterialAutoCompleteTextView
    private lateinit var etDob: TextInputEditText
    private lateinit var etGender: MaterialAutoCompleteTextView
    private lateinit var tvChangePassword: TextView

    // Achievement Edit Icon
    private lateinit var ivEditAchievements: ImageView

    // State trackers for edit mode
    private var isEditingFirstName = false
    private var isEditingLastName = false

    // Reuse Badge system for Achievements
    private val allAchievements by lazy {
        listOf(
            Badge(1, getString(R.string.badge_first_ace_list_title), getString(R.string.badge_first_ace_dialog_title), "", "", R.drawable.badge_firstace, true),
            Badge(2, getString(R.string.badge_first_timer_list_title), getString(R.string.badge_first_timer_dialog_title), "", "", R.drawable.badge_firsttimer, true),
            Badge(3, getString(R.string.badge_triple_ace_list_title), getString(R.string.badge_triple_ace_dialog_title), "", "", R.drawable.badge_tripleace, true)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_profile)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // --- Find Views ---
        etFirstName = findViewById(R.id.etFirstName)
        ivEditFirstName = findViewById(R.id.ivEditFirstName)

        etLastName = findViewById(R.id.etLastName)
        ivEditLastName = findViewById(R.id.ivEditLastName)

        etTeachingPos = findViewById(R.id.etTeachingPos)
        etDob = findViewById(R.id.etDob)
        etGender = findViewById(R.id.etGender)
        tvChangePassword = findViewById(R.id.tvChangePassword)
        ivEditAchievements = findViewById(R.id.ivEditBadges) // Reusing the ID from layout

        // --- Initialize View Mode ---
        setEditMode(etFirstName, ivEditFirstName, false)
        setEditMode(etLastName, ivEditLastName, false)

        // --- Setup Dropdowns ---
        val genderItems = listOf("Male", "Female")
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genderItems)
        etGender.setAdapter(genderAdapter)

        val posItems = listOf("Teacher I", "Teacher II", "Teacher III", "Teacher IV", "Teacher V", "Teacher VI", "Teacher VII", "Master Teacher I", "Master Teacher II", "Master Teacher III", "Master Teacher IV", "Master Teacher V")
        val posAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, posItems)
        etTeachingPos.setAdapter(posAdapter)

        // --- Setup Click Listeners ---

        ivEditFirstName.setOnClickListener {
            isEditingFirstName = !isEditingFirstName
            if (isEditingFirstName) {
                setEditMode(etFirstName, ivEditFirstName, true)
                etFirstName.requestFocus()
                showKeyboard(etFirstName)
            } else {
                setEditMode(etFirstName, ivEditFirstName, false)
                saveData("First Name", etFirstName.text.toString())
            }
        }

        ivEditLastName.setOnClickListener {
            isEditingLastName = !isEditingLastName
            if (isEditingLastName) {
                setEditMode(etLastName, ivEditLastName, true)
                etLastName.requestFocus()
                showKeyboard(etLastName)
            } else {
                setEditMode(etLastName, ivEditLastName, false)
                saveData("Last Name", etLastName.text.toString())
            }
        }

        etTeachingPos.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position).toString()
            saveData("Teaching Position", selected)
        }

        etGender.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position).toString()
            saveData("Gender", selected)
        }

        // --- UPDATE: Achievement Dialog Call ---
        ivEditAchievements.setOnClickListener {
            showAddAchievementDialog()
        }

        tvChangePassword.setOnClickListener {
            // Reusing Password Creation Activity with Teacher context
            val intent = Intent(this, PasswordCreationActivity::class.java)
            intent.putExtra("EXTRA_ROLE", "Teacher")
            startActivity(intent)
        }

        etDob.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, day ->
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(year, month, day)
                    val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.US)
                    val formattedDate = formatter.format(selectedDate.time)
                    etDob.setText(formattedDate)
                    saveData("Birthdate", formattedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }
    }

    // --- NEW LOGIC: Achievement Dialog Flow ---
    private fun showAddAchievementDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_achievement, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val etYear = dialogView.findViewById<TextInputEditText>(R.id.etYearReceived)
        val btnUpload = dialogView.findViewById<View>(R.id.btnUploadCertificate)
        val btnSave = dialogView.findViewById<View>(R.id.btnSaveAchievement)
        val btnClose = dialogView.findViewById<View>(R.id.btnCloseDialog)

        // 1. Year Picker
        etYear.setOnClickListener {
            showYearPickerDialog(etYear)
        }

        // 2. Upload Certificate
        btnUpload.setOnClickListener {
            showUploadSourceDialog()
        }

        // 3. Save Logic
        btnSave.setOnClickListener {
            val name = dialogView.findViewById<TextInputEditText>(R.id.etAchievementName).text.toString()
            if(name.isEmpty()) {
                Toast.makeText(this, "Please enter achievement name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Show Confirmation
            showSaveConfirmation(dialog)
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    // --- Year-Only Picker ---
    private fun showYearPickerDialog(editText: EditText) {
        val themedContext = android.view.ContextThemeWrapper(this, R.style.Theme_Gr8Math)

        val numberPicker = android.widget.NumberPicker(themedContext)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        numberPicker.minValue = 1980
        numberPicker.maxValue = currentYear + 10
        numberPicker.value = currentYear
        numberPicker.wrapSelectorWheel = false
        numberPicker.descendantFocusability = android.widget.NumberPicker.FOCUS_BLOCK_DESCENDANTS

        val titleView = TextView(this).apply {
            text = "Select Year"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, R.color.colorMatisse))
            setPadding(0, 40, 0, 20)
            try { typeface = ResourcesCompat.getFont(context, R.font.lexend) } catch (_: Exception) { }
        }

        try {
            val count = numberPicker.childCount
            for (i in 0 until count) {
                val child = numberPicker.getChildAt(i)
                if (child is EditText) {
                    child.setTextColor(ContextCompat.getColor(this, R.color.colorText))
                    child.textSize = 18f
                    child.typeface = ResourcesCompat.getFont(this, R.font.lexend)
                    child.isFocusable = false
                }
            }
        } catch (e: Exception) { }

        val pickerDialog = MaterialAlertDialogBuilder(this)
            .setCustomTitle(titleView)
            .setView(numberPicker)
            .setPositiveButton("OK") { _, _ ->
                editText.setText(numberPicker.value.toString())
            }
            // FIX: Changed "Cancel" to "CANCEL" here
            .setNegativeButton("CANCEL", null)
            .create()

        pickerDialog.show()

        pickerDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.apply {
            setTextColor(ContextCompat.getColor(context, R.color.colorMatisse))
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
            setTypeface(typeface, Typeface.BOLD)
        }

        pickerDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.apply {
            setTextColor(ContextCompat.getColor(context, R.color.colorRed))
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
            setTypeface(typeface, Typeface.BOLD)
        }
    }

    // --- Upload Source (Bottom Sheet) ---
    private fun showUploadSourceDialog() {
        val sheetView = LayoutInflater.from(this).inflate(R.layout.dialog_upload_certificate_source, null)
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        bottomSheetDialog.setContentView(sheetView)

        (sheetView.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)

        sheetView.findViewById<View>(R.id.btnPhotoAlbum).setOnClickListener {
            Toast.makeText(this, "Opening Photo Album...", Toast.LENGTH_SHORT).show()
            bottomSheetDialog.dismiss()
        }

        sheetView.findViewById<View>(R.id.btnCamera).setOnClickListener {
            Toast.makeText(this, "Opening Camera...", Toast.LENGTH_SHORT).show()
            bottomSheetDialog.dismiss()
        }

        sheetView.findViewById<View>(R.id.btnCancelUpload).setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    // --- Save Confirmation Dialog ---
    private fun showSaveConfirmation(parentDialog: androidx.appcompat.app.AlertDialog) {
        val messageView = TextView(this).apply {
            text = "Are you sure you want to save\nthis achievements?"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            // Left aligned by default (Removed Gravity.CENTER)
            setTextColor(ContextCompat.getColor(context, R.color.colorText))
            setPadding(70, 60, 50, 10)
            try { typeface = ResourcesCompat.getFont(context, R.font.lexend) } catch (_: Exception) { }
        }

        val confirmDialog = MaterialAlertDialogBuilder(this)
            .setView(messageView)

            // FIX: SWAPPED POSITIONS
            // "No" is now the Positive Button (Right Side)
            .setPositiveButton("No") { dialogInterface, _ ->
                dialogInterface.dismiss() // Just dismiss
            }
            // "Yes" is now the Negative Button (Left Side)
            .setNegativeButton("Yes") { dialogInterface, _ ->
                // Actual Saving Logic
                Toast.makeText(this, "Achievement Saved!", Toast.LENGTH_SHORT).show()
                dialogInterface.dismiss() // Close confirmation
                parentDialog.dismiss()    // Close main dialog
            }
            .create()

        confirmDialog.show()

        // FIX: Ensure both are Red
        confirmDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
        confirmDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
    }

    private fun setEditMode(editText: EditText, iconView: ImageView, isEditing: Boolean) {
        editText.isEnabled = isEditing
        editText.isFocusable = isEditing
        editText.isFocusableInTouchMode = isEditing

        if (isEditing) {
            iconView.setImageResource(R.drawable.ic_check)
            iconView.setColorFilter(ContextCompat.getColor(this, R.color.colorDarkCyan))
        } else {
            iconView.setImageResource(R.drawable.ic_edit)
            iconView.setColorFilter(ContextCompat.getColor(this, R.color.colorDarkCyan))
        }
    }

    private fun saveData(field: String, value: String) {
        Toast.makeText(this, "$field updated to: $value", Toast.LENGTH_SHORT).show()
    }

    private fun showKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }
}