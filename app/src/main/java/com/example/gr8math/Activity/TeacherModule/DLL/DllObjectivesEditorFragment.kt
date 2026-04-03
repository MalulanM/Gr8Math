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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DllObjectivesEditorFragment : Fragment(R.layout.fragment_dll_objectives_editor) {

    private val masterActivity get() = activity as DllMasterEditorActivity

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnSave = view.findViewById<Button>(R.id.btnSaveFragment)

        btnSave.setOnClickListener {
            // 🌟 1. SHOW CONFIRMATION MODAL
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle("Do you want to save changes?")
                .setPositiveButton("Yes") { _, _ ->
                    // 🌟 2. ONLY SAVE IF YES IS PRESSED
                    saveObjectives(view, btnSave)
                }
                .setNegativeButton("No", null)
                .show()

            // 🌟 3. MATCH RED COLOR DESIGN
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
            R.id.etCsMonday, R.id.etCsTuesday, R.id.etCsWednesday, R.id.etCsThursday, R.id.etCsFriday,
            R.id.etPsMonday, R.id.etPsTuesday, R.id.etPsWednesday, R.id.etPsThursday, R.id.etPsFriday,
            R.id.etLcMonday, R.id.etLcTuesday, R.id.etLcWednesday, R.id.etLcThursday, R.id.etLcFriday
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
                    .decodeList<DailyEntryFull>()

                withContext(Dispatchers.Main) {
                    val csIds = listOf(R.id.etCsMonday, R.id.etCsTuesday, R.id.etCsWednesday, R.id.etCsThursday, R.id.etCsFriday)
                    val psIds = listOf(R.id.etPsMonday, R.id.etPsTuesday, R.id.etPsWednesday, R.id.etPsThursday, R.id.etPsFriday)
                    val lcIds = listOf(R.id.etLcMonday, R.id.etLcTuesday, R.id.etLcWednesday, R.id.etLcThursday, R.id.etLcFriday)

                    val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val startDate = dbFormat.parse(masterActivity.availableFrom)!!

                    for (i in 0 until 5) {
                        val cal = Calendar.getInstance().apply { time = startDate }
                        cal.add(Calendar.DAY_OF_YEAR, i)
                        val expectedDateStr = dbFormat.format(cal.time)

                        val entryForDay = entries.find { it.entry_date == expectedDateStr }
                        if (entryForDay != null) {
                            view.findViewById<EditText>(csIds[i]).setText(entryForDay.content_standard ?: "")
                            view.findViewById<EditText>(psIds[i]).setText(entryForDay.performance_standard ?: "")
                            view.findViewById<EditText>(lcIds[i]).setText(entryForDay.learning_comp ?: "")
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun saveObjectives(view: View, btnSave: Button) {
        val csList = listOf(R.id.etCsMonday, R.id.etCsTuesday, R.id.etCsWednesday, R.id.etCsThursday, R.id.etCsFriday).map { view.findViewById<EditText>(it).text.toString().trim() }
        val psList = listOf(R.id.etPsMonday, R.id.etPsTuesday, R.id.etPsWednesday, R.id.etPsThursday, R.id.etPsFriday).map { view.findViewById<EditText>(it).text.toString().trim() }
        val lcList = listOf(R.id.etLcMonday, R.id.etLcTuesday, R.id.etLcWednesday, R.id.etLcThursday, R.id.etLcFriday).map { view.findViewById<EditText>(it).text.toString().trim() }

        btnSave.isEnabled = false; btnSave.text = "SAVING..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = SupabaseService.client
                ensureMainRecordExists()

                val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val startDate = dbFormat.parse(masterActivity.availableFrom)!!

                for (i in 0 until 5) {
                    val cal = Calendar.getInstance().apply { time = startDate }
                    cal.add(Calendar.DAY_OF_YEAR, i)
                    val dbDateStr = dbFormat.format(cal.time)

                    val existing = db.from("dll_daily_entry")
                        .select { filter { eq("main_id", masterActivity.dllMainId); eq("entry_date", dbDateStr) } }
                        .decodeSingleOrNull<DailyEntryRef>()

                    if (existing != null) {
                        db.from("dll_daily_entry").update({
                            set("content_standard", csList[i])
                            set("performance_standard", psList[i])
                            set("learning_comp", lcList[i])
                        }) { filter { eq("id", existing.id) } }
                    } else {
                        val insertPayload = ObjectiveInsert(masterActivity.dllMainId, dbDateStr, csList[i], psList[i], lcList[i])
                        db.from("dll_daily_entry").insert(insertPayload)
                    }
                }

                withContext(Dispatchers.Main) {
                    btnSave.isEnabled = true; btnSave.text = "SAVE"
                    ShowToast.showMessage(requireContext(), "Objectives Saved!")
                    masterActivity.switchToViewMode()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnSave.isEnabled = true; btnSave.text = "SAVE"
                    ShowToast.showMessage(requireContext(), "Error: ${e.message}")
                }
            }
        }
    }

    private suspend fun ensureMainRecordExists() {
        if (masterActivity.dllMainId != -1) return
        val db = SupabaseService.client
        val cc = db.from("course_content").select { filter { eq("section_id", masterActivity.courseId) } }.decodeSingleOrNull<CourseContentRef>() ?: throw Exception("Course missing")

        val mainInsertPayload = DllMainInsert(cc.id, masterActivity.quarterNumber, masterActivity.weekNumber, masterActivity.availableFrom, masterActivity.availableUntil)
        val main = db.from("dll_main").insert(mainInsertPayload) { select() }.decodeSingle<DllMainRef>()
        withContext(Dispatchers.Main) { masterActivity.dllMainId = main.id; masterActivity.isEditMode = true }
    }

    @Serializable
    private data class CourseContentRef(val id: Int)
    @Serializable
    private data class DllMainRef(val id: Int)
    @Serializable
    private data class DailyEntryRef(val id: Int)
    @Serializable
    private data class DailyEntryFull(val id: Int, val entry_date: String, val content_standard: String? = null, val performance_standard: String? = null, val learning_comp: String? = null)
    @Serializable
    private data class DllMainInsert(val course_id: Int, val quarter_number: Int, val week_number: Int, val available_from: String, val available_until: String)
    @Serializable
    private data class ObjectiveInsert(val main_id: Int, val entry_date: String, val content_standard: String, val performance_standard: String, val learning_comp: String)
}