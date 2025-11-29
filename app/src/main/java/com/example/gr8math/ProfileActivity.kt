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
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    // UI Elements
    private lateinit var etFirstName: TextInputEditText
    private lateinit var ivEditFirstName: ImageView

    private lateinit var etLastName: TextInputEditText
    private lateinit var ivEditLastName: ImageView

    private lateinit var etLRN: TextInputEditText
    private lateinit var ivEditLRN: ImageView

    private lateinit var etDob: TextInputEditText
    private lateinit var etGender: MaterialAutoCompleteTextView
    private lateinit var ivProfile: ImageView
    private lateinit var tvChangePassword: TextView

    // Badge Edit Icon
    private lateinit var ivEditBadges: ImageView

    // State trackers for edit mode
    private var isEditingFirstName = false
    private var isEditingLastName = false
    private var isEditingLRN = false

    // FIX 1: Use 'by lazy' to prevent crash on startup (getString requires Context)
    private val allBadges by lazy {
        listOf(
            Badge(1, getString(R.string.badge_first_ace_list_title), getString(R.string.badge_first_ace_dialog_title), getString(R.string.badge_first_ace_desc), getString(R.string.badge_date_placeholder), R.drawable.badge_firstace, true),
            Badge(2, getString(R.string.badge_first_timer_list_title), getString(R.string.badge_first_timer_dialog_title), getString(R.string.badge_first_timer_desc), getString(R.string.badge_date_placeholder), R.drawable.badge_firsttimer, true),
            Badge(3, getString(R.string.badge_first_escape_list_title), getString(R.string.badge_first_escape_dialog_title), getString(R.string.badge_first_escape_desc), getString(R.string.badge_date_locked), R.drawable.badge_firstescape, false),
            Badge(4, getString(R.string.badge_perfect_escape_list_title), getString(R.string.badge_perfect_escape_dialog_title), getString(R.string.badge_perfect_escape_desc), getString(R.string.badge_date_placeholder), R.drawable.badge_perfectescape, true),
            Badge(5, getString(R.string.badge_first_explo_list_title), getString(R.string.badge_first_explo_dialog_title), getString(R.string.badge_first_explo_desc), getString(R.string.badge_date_locked), R.drawable.badge_firstexplo, false),
            Badge(6, getString(R.string.badge_full_explo_list_title), getString(R.string.badge_full_explo_dialog_title), getString(R.string.badge_full_explo_desc), getString(R.string.badge_date_placeholder), R.drawable.badge_fullexplo, true),
            Badge(7, getString(R.string.badge_three_quarter_list_title), getString(R.string.badge_three_quarter_dialog_title), getString(R.string.badge_three_quarter_desc), getString(R.string.badge_date_locked), R.drawable.badge_threequarter, false),
            Badge(8, getString(R.string.badge_triple_ace_list_title), getString(R.string.badge_triple_ace_dialog_title), getString(R.string.badge_triple_ace_desc), getString(R.string.badge_date_placeholder), R.drawable.badge_tripleace, true)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_profile)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // --- Find Views ---
        etFirstName = findViewById(R.id.etFirstName)
        ivEditFirstName = findViewById(R.id.ivEditFirstName)

        etLastName = findViewById(R.id.etLastName)
        ivEditLastName = findViewById(R.id.ivEditLastName)

        etLRN = findViewById(R.id.etLRN)
        ivEditLRN = findViewById(R.id.ivEditLRN)

        etDob = findViewById(R.id.etDob)
        etGender = findViewById(R.id.etGender)
        ivProfile = findViewById(R.id.ivProfile)
        tvChangePassword = findViewById(R.id.tvChangePassword)

        // IMPORTANT: Ensure this ID exists in your XML layout inside the Badges card
        ivEditBadges = findViewById(R.id.ivEditBadges)

        // --- Initialize View Mode ---
        setEditMode(etFirstName, ivEditFirstName, false)
        setEditMode(etLastName, ivEditLastName, false)
        setEditMode(etLRN, ivEditLRN, false)

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

        ivEditLRN.setOnClickListener {
            isEditingLRN = !isEditingLRN
            if (isEditingLRN) {
                setEditMode(etLRN, ivEditLRN, true)
                etLRN.requestFocus()
                showKeyboard(etLRN)
            } else {
                setEditMode(etLRN, ivEditLRN, false)
                saveData("LRN", etLRN.text.toString())
            }
        }

        // --- Badge Selection Dialog Click ---
        ivEditBadges.setOnClickListener {
            showBadgeSelectionDialog()
        }

        tvChangePassword.setOnClickListener {
            val intent = Intent(this, PasswordCreationActivity::class.java)
            startActivity(intent)
        }

        val genderItems = listOf("Male", "Female")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genderItems)
        etGender.setAdapter(adapter)

        etDob.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, day ->
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(year, month, day)
                    val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.US)
                    etDob.setText(formatter.format(selectedDate.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }
    }

    // --- Show Badge Selection Dialog with Confirmation ---
    private fun showBadgeSelectionDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_badges, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        // 1. Filter for ACQUIRED badges only
        val acquiredBadges = allBadges.filter { it.isAcquired }

        // 2. Setup RecyclerView
        val rvSelection = dialogView.findViewById<RecyclerView>(R.id.rvBadgeSelection)
        rvSelection.layoutManager = LinearLayoutManager(this)

        // Pass the filtered list to the adapter
        val adapter = BadgeSelectionAdapter(acquiredBadges)
        rvSelection.adapter = adapter

        // 3. Setup Buttons
        dialogView.findViewById<View>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        // FIX 2: Intercept Save Click to show Custom Confirmation Dialog
        dialogView.findViewById<View>(R.id.btnSaveSelection).setOnClickListener {
            val selected = adapter.selectedBadges

            // Validation
            if (selected.isEmpty()) {
                Toast.makeText(this, "No badges selected to display", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create Custom Message View (Align Left, Bold)
            val messageView = TextView(this).apply {
                text = "Are you sure you want to save\nthis badges?"
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                // gravity = Gravity.CENTER // REMOVED THIS to keep standard alignment
                setTextColor(ContextCompat.getColor(context, R.color.colorText))
                setPadding(70, 60, 50, 10) // Adjusted padding
                try {
                    typeface = ResourcesCompat.getFont(context, R.font.lexend)
                } catch (_: Exception) { }
            }

            // Build Confirmation Dialog
            val confirmDialog = MaterialAlertDialogBuilder(this)
                .setView(messageView)

                // SWAPPED: "No" is now Positive (Right), "Yes" is now Negative (Left)
                .setPositiveButton("No") { dialogInterface, _ ->
                    dialogInterface.dismiss() // Just close confirmation
                }
                .setNegativeButton("Yes") { dialogInterface, _ ->
                    // --- SAVE LOGIC ---
                    Toast.makeText(this, "Displaying ${selected.size} badges on profile", Toast.LENGTH_SHORT).show()
                    // TODO: Update backend/UI with 'selected' list

                    dialogInterface.dismiss() // Close confirmation
                    dialog.dismiss()          // Close selection list
                }
                .create()

            confirmDialog.show()

            // Set Buttons to Red
            confirmDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
            confirmDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun setEditMode(editText: EditText, iconView: ImageView, isEditing: Boolean) {
        editText.isEnabled = isEditing
        editText.isFocusable = isEditing
        editText.isFocusableInTouchMode = isEditing

        if (isEditing) {
            iconView.setImageResource(R.drawable.ic_check)
            iconView.setColorFilter(ContextCompat.getColor(this, R.color.colorMatisse))
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