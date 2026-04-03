package com.example.gr8math.Activity.TeacherModule.DLL

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.gr8math.R
import com.example.gr8math.Services.SupabaseService
import com.example.gr8math.Utils.ShowToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.json.JSONObject

class DllReflectionEditorFragment : Fragment(R.layout.fragment_dll_reflection_editor) {

    private val masterActivity get() = activity as DllMasterEditorActivity

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnSave = view.findViewById<Button>(R.id.btnSaveFragment)

        btnSave.setOnClickListener {
            // 1. Build and show the confirmation dialog
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle("Do you want to save changes?") // Bold/Large title
                .setPositiveButton("Yes") { _, _ ->
                    // 2. Only save if confirmed
                    saveReflection(view, btnSave)
                }
                .setNegativeButton("No") { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                .show()

            // 3. Make buttons Red to match your screenshot
            val redColor = android.graphics.Color.parseColor("#E53935")
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(redColor)
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(redColor)
        }

        if (masterActivity.dllMainId != -1) {
            prefillExistingData(view)
        }
        applyViewState(view, btnSave)
    }

    private fun applyViewState(view: View, btnSave: Button) {
        val isEdit = masterActivity.isEditable
        btnSave.visibility = if (isEdit) View.VISIBLE else View.GONE

        val inputIds = listOf(
            R.id.etRemarks, R.id.etRefA, R.id.etRefB, R.id.etRefC, R.id.etRefD, R.id.etRefE, R.id.etRefF, R.id.etRefG
        )
        for (id in inputIds) {
            val et = view.findViewById<EditText>(id)
            et.isFocusable = isEdit
            et.isFocusableInTouchMode = isEdit
            et.isCursorVisible = isEdit
        }
    }

    private fun prefillExistingData(view: View) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = SupabaseService.client
                val entries = db.from("dll_daily_entry")
                    .select { filter { eq("main_id", masterActivity.dllMainId) } }
                    .decodeList<DailyEntryReflection>()

                val firstEntry = entries.firstOrNull { !it.remark.isNullOrBlank() || !it.reflection.isNullOrBlank() }

                withContext(Dispatchers.Main) {
                    if (firstEntry != null) {
                        view.findViewById<EditText>(R.id.etRemarks).setText(firstEntry.remark ?: "")

                        val refStr = firstEntry.reflection
                        if (!refStr.isNullOrBlank()) {
                            try {
                                val json = JSONObject(refStr)
                                view.findViewById<EditText>(R.id.etRefA).setText(json.optString("A. No. of learners who earned 80% in the evaluation", ""))
                                view.findViewById<EditText>(R.id.etRefB).setText(json.optString("B. No. of learners who require additional activities for remediation who scored below 80%", ""))
                                view.findViewById<EditText>(R.id.etRefC).setText(json.optString("C. Did the remedial lessons work? No. of learners who have caught up with the lesson", ""))
                                view.findViewById<EditText>(R.id.etRefD).setText(json.optString("D. No. of learners who continue to require remediation", ""))
                                view.findViewById<EditText>(R.id.etRefE).setText(json.optString("E. Which of my teaching strategies worked well? Why did these work?", ""))
                                view.findViewById<EditText>(R.id.etRefF).setText(json.optString("F. What difficulties did I encounter which my principal or supervisor can help me solve?", ""))
                                view.findViewById<EditText>(R.id.etRefG).setText(json.optString("G. What innovation or localized materials did I use/discover which I wish to share with other teachers?", ""))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveReflection(view: View, btnSave: Button) {
        if (masterActivity.dllMainId == -1) {
            ShowToast.showMessage(requireContext(), "Please save Objectives first.")
            return
        }

        val remarks = view.findViewById<EditText>(R.id.etRemarks).text.toString().trim()

        val reflectionJson = JSONObject().apply {
            put("A. No. of learners who earned 80% in the evaluation", view.findViewById<EditText>(R.id.etRefA).text.toString().trim())
            put("B. No. of learners who require additional activities for remediation who scored below 80%", view.findViewById<EditText>(R.id.etRefB).text.toString().trim())
            put("C. Did the remedial lessons work? No. of learners who have caught up with the lesson", view.findViewById<EditText>(R.id.etRefC).text.toString().trim())
            put("D. No. of learners who continue to require remediation", view.findViewById<EditText>(R.id.etRefD).text.toString().trim())
            put("E. Which of my teaching strategies worked well? Why did these work?", view.findViewById<EditText>(R.id.etRefE).text.toString().trim())
            put("F. What difficulties did I encounter which my principal or supervisor can help me solve?", view.findViewById<EditText>(R.id.etRefF).text.toString().trim())
            put("G. What innovation or localized materials did I use/discover which I wish to share with other teachers?", view.findViewById<EditText>(R.id.etRefG).text.toString().trim())
        }.toString()

        btnSave.isEnabled = false
        btnSave.text = "SAVING..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = SupabaseService.client
                val existingEntries = db.from("dll_daily_entry")
                    .select { filter { eq("main_id", masterActivity.dllMainId) } }
                    .decodeList<DailyEntryRef>()

                if (existingEntries.isNotEmpty()) {
                    val updateMap: Map<String, String> = mapOf(
                        "remark" to remarks,
                        "reflection" to reflectionJson
                    )

                    db.from("dll_daily_entry").update(updateMap) {
                        filter { eq("main_id", masterActivity.dllMainId) }
                    }
                }

                withContext(Dispatchers.Main) {
                    btnSave.isEnabled = true
                    btnSave.text = "SAVE"
                    ShowToast.showMessage(requireContext(), "Reflection & Remarks Saved!")
                    masterActivity.switchToViewMode()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    btnSave.isEnabled = true
                    btnSave.text = "SAVE"
                    ShowToast.showMessage(requireContext(), "Error: ${e.message}")
                }
            }
        }
    }

    @Serializable
    private data class DailyEntryRef(val id: Int)

    @Serializable
    private data class DailyEntryReflection(val id: Int, val remark: String? = null, val reflection: String? = null)
}