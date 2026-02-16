package com.example.gr8math.Activity.TeacherModule.DLL

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.example.gr8math.R
import com.example.gr8math.Services.SupabaseService
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.Utils.UIUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DLLStep4Activity : AppCompatActivity() {

    private lateinit var reflectionDaysContainer: LinearLayout
    private lateinit var btnAddDay: Button
    private lateinit var btnSave: Button

    private var dllMainId: Int = -1
    private var dailyEntryId: Int = -1 // Target specific daily entry
    private var sectionTitle: String? = null

    private val dayManagers = mutableListOf<ReflectionDayManager>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dll_step4)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        reflectionDaysContainer = findViewById(R.id.reflectionDaysContainer)
        btnAddDay = findViewById(R.id.btnAddDay)
        btnSave = findViewById(R.id.btnSave)

        // 🌟 RETRIEVE EDIT INFO
        dllMainId = intent.getIntExtra(DLLEditActivity.EXTRA_DLL_MAIN_ID, -1)
        dailyEntryId = intent.getIntExtra("EXTRA_DAILY_ENTRY_ID", -1)
        sectionTitle = intent.getStringExtra(DLLEditActivity.EXTRA_SECTION_TITLE)

        // 1. Setup Back Navigation
        toolbar.setNavigationOnClickListener { handleBackPress() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { handleBackPress() }
        })

        // 🌟 STRICT EDIT MODE UI SETUP
        toolbar.title = sectionTitle ?: "Edit Reflection"
        btnAddDay.visibility = View.GONE
        btnSave.text = "SAVE"

        addDay()
        prefillSingleCardIfEditing()

        // 2. Setup Save Button
        btnSave.setOnClickListener {
            if (validateForms()) {
                updateReflectionInSupabase()
            }
        }
    }

    private fun prefillSingleCardIfEditing() {
        val manager = dayManagers.firstOrNull() ?: return

        // Fill Date
        manager.etDate.setText(intent.getStringExtra("EXTRA_ENTRY_DATE") ?: "")

        // 🌟 Strictly disable date editing
        manager.etDate.isEnabled = false
        manager.etDate.isFocusable = false
        manager.etDate.isClickable = false
        manager.btnRemove.visibility = View.GONE

        // Fill Fields
        manager.etReview.setText(intent.getStringExtra("Remarks") ?: "")
        manager.etReflection.setText(intent.getStringExtra("Reflection") ?: "")
    }

    // 🌟 SUPABASE UPDATE LOGIC 🌟
    private fun updateReflectionInSupabase() {
        if (dailyEntryId == -1) {
            Toast.makeText(this, "Error: Cannot save, Daily Entry ID missing.", Toast.LENGTH_LONG).show()
            return
        }

        val manager = dayManagers.firstOrNull() ?: return

        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Update reflection columns inside dll_daily_entry
                SupabaseService.client.from("dll_daily_entry").update({
                    set("remark", manager.etReview.text.toString().trim())
                    set("reflection", manager.etReflection.text.toString().trim())
                }) {
                    filter { eq("id", dailyEntryId) }
                }

                withContext(Dispatchers.Main) {
                    ShowToast.showMessage(this@DLLStep4Activity, "Updated Successfully!")
                    setResult(RESULT_OK)
                    finish()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnSave.isEnabled = true
                    btnSave.text = "SAVE"
                    ShowToast.showMessage(this@DLLStep4Activity, "Update Failed: ${e.message}")
                }
            }
        }
    }

    private fun handleBackPress() {
        var hasUnsavedData = false
        for (manager in dayManagers) {
            if (manager.hasData()) {
                hasUnsavedData = true
                break
            }
        }

        if (hasUnsavedData) {
            showDiscardDialog()
        } else {
            finish()
        }
    }

    private fun showDiscardDialog() {
        val titleView = TextView(this).apply {
            text = "Discard Changes?"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.colorText))
            setPadding(70, 50, 50, 10)
            try { typeface = ResourcesCompat.getFont(context, R.font.lexend) } catch (_: Exception) { }
        }

        val messageView = TextView(this).apply {
            text = "You have unsaved content. If you go\nback, your changes will be lost."
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.colorSubtleText))
            setPadding(70, 10, 50, 20)
            try { typeface = ResourcesCompat.getFont(context, R.font.lexend) } catch (_: Exception) { }
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setCustomTitle(titleView)
            .setView(messageView)
            .setNegativeButton("Yes") { _, _ -> finish() }
            .setPositiveButton("No") { dialogInterface, _ -> dialogInterface.dismiss() }
            .create()

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
    }

    private fun addDay() {
        val manager = ReflectionDayManager(this, reflectionDaysContainer) { managerToRemove ->
            reflectionDaysContainer.removeView(managerToRemove.view)
            dayManagers.remove(managerToRemove)
        }
        dayManagers.add(manager)
    }

    private fun validateForms(): Boolean {
        if (dayManagers.isEmpty()) {
            Toast.makeText(this, "Please add at least one day.", Toast.LENGTH_SHORT).show()
            return false
        }

        var allValid = true
        dayManagers.forEach { manager ->
            if (!manager.isValid()) {
                allValid = false
            }
        }
        return allValid
    }

    // --- Inner Class for Reflection Card ---
    inner class ReflectionDayManager(
        val context: Context,
        container: LinearLayout,
        val onRemove: (ReflectionDayManager) -> Unit
    ) {
        val view: View = LayoutInflater.from(context).inflate(R.layout.item_dll_reflection_card, container, false)

        val etDate: EditText = view.findViewById(R.id.etDate)
        val tilDate: TextInputLayout = view.findViewById(R.id.tilDate)
        val btnRemove: ImageButton = view.findViewById(R.id.btnRemoveDay)

        val etReview: EditText = view.findViewById(R.id.etReview)
        val tilReview: TextInputLayout = view.findViewById(R.id.tilReview)

        val etReflection: EditText = view.findViewById(R.id.etReflection)
        val tilReflection: TextInputLayout = view.findViewById(R.id.tilReflection)

        init {
            container.addView(view)
            etDate.setText("")

            // Removed DatePicker logic completely since this is strictly an Edit screen now.

            btnRemove.setOnClickListener { onRemove(this) }
        }

        fun hasData(): Boolean {
            return etReview.text.isNotEmpty() || etReflection.text.isNotEmpty()
        }

        fun isValid(): Boolean {
            var valid = true
            val errorMsg = "Required"

            if (etDate.text.toString().trim().isEmpty()) {
                UIUtils.errorDisplay(context, tilDate, etDate, true, errorMsg)
                valid = false
            }
            if (etReview.text.toString().trim().isEmpty()) {
                UIUtils.errorDisplay(context, tilReview, etReview, true, errorMsg)
                valid = false
            }
            if (etReflection.text.toString().trim().isEmpty()) {
                UIUtils.errorDisplay(context, tilReflection, etReflection, true, errorMsg)
                valid = false
            }
            return valid
        }
    }
}