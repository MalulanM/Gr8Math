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

class DllResourcesEditorFragment : Fragment(R.layout.fragment_dll_resources_editor) {

    private val masterActivity get() = activity as DllMasterEditorActivity

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val btnSave = view.findViewById<Button>(R.id.btnSaveFragment)

        btnSave.setOnClickListener {
            // 1. Build the confirmation dialog
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle("Do you want to save changes?")
                .setPositiveButton("Yes") { _, _ ->
                    // 2. Only save if confirmed
                    saveResources(view, btnSave)
                }
                .setNegativeButton("No") { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                .show()

            // 3. Set button colors to Red to match your design
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
            R.id.etTgMonday, R.id.etTgTuesday, R.id.etTgWednesday, R.id.etTgThursday, R.id.etTgFriday,
            R.id.etLmMonday, R.id.etLmTuesday, R.id.etLmWednesday, R.id.etLmThursday, R.id.etLmFriday,
            R.id.etTbMonday, R.id.etTbTuesday, R.id.etTbWednesday, R.id.etTbThursday, R.id.etTbFriday,
            R.id.etAmMonday, R.id.etAmTuesday, R.id.etAmWednesday, R.id.etAmThursday, R.id.etAmFriday,
            R.id.etOrMonday, R.id.etOrTuesday, R.id.etOrWednesday, R.id.etOrThursday, R.id.etOrFriday
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
                    .decodeList<DailyEntryForRefs>()

                val entryIds = entries.map { it.id }
                if (entryIds.isEmpty()) return@launch

                val refs = db.from("dll_references")
                    .select { filter { isIn("daily_entry_id", entryIds) } }
                    .decodeList<ReferenceRow>()

                withContext(Dispatchers.Main) {
                    val tgIds = listOf(R.id.etTgMonday, R.id.etTgTuesday, R.id.etTgWednesday, R.id.etTgThursday, R.id.etTgFriday)
                    val lmIds = listOf(R.id.etLmMonday, R.id.etLmTuesday, R.id.etLmWednesday, R.id.etLmThursday, R.id.etLmFriday)
                    val tbIds = listOf(R.id.etTbMonday, R.id.etTbTuesday, R.id.etTbWednesday, R.id.etTbThursday, R.id.etTbFriday)
                    val amIds = listOf(R.id.etAmMonday, R.id.etAmTuesday, R.id.etAmWednesday, R.id.etAmThursday, R.id.etAmFriday)
                    val orIds = listOf(R.id.etOrMonday, R.id.etOrTuesday, R.id.etOrWednesday, R.id.etOrThursday, R.id.etOrFriday)

                    val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val startDate = dbFormat.parse(masterActivity.availableFrom)!!

                    for (i in 0 until 5) {
                        val cal = Calendar.getInstance().apply { time = startDate }
                        cal.add(Calendar.DAY_OF_YEAR, i)
                        val expectedDateStr = dbFormat.format(cal.time)

                        val entry = entries.find { it.entry_date == expectedDateStr }
                        if (entry != null) {
                            val dayRefs = refs.filter { it.daily_entry_id == entry.id }

                            view.findViewById<EditText>(tgIds[i]).setText(dayRefs.find { it.reference_title == "1. Teacher's Guide pages" }?.reference_text ?: "")
                            view.findViewById<EditText>(lmIds[i]).setText(dayRefs.find { it.reference_title == "2. Learner's Materials' pages" }?.reference_text ?: "")
                            view.findViewById<EditText>(tbIds[i]).setText(dayRefs.find { it.reference_title == "3. Textbook pages" }?.reference_text ?: "")
                            view.findViewById<EditText>(amIds[i]).setText(dayRefs.find { it.reference_title == "4. Additional Materials" }?.reference_text ?: "")
                            view.findViewById<EditText>(orIds[i]).setText(dayRefs.find { it.reference_title == "5. Other References" }?.reference_text ?: "")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveResources(view: View, btnSave: Button) {
        if (masterActivity.dllMainId == -1) {
            ShowToast.showMessage(requireContext(), "Please save Objectives first.")
            return
        }

        val tgs = listOf(R.id.etTgMonday, R.id.etTgTuesday, R.id.etTgWednesday, R.id.etTgThursday, R.id.etTgFriday)
        val lms = listOf(R.id.etLmMonday, R.id.etLmTuesday, R.id.etLmWednesday, R.id.etLmThursday, R.id.etLmFriday)
        val tbs = listOf(R.id.etTbMonday, R.id.etTbTuesday, R.id.etTbWednesday, R.id.etTbThursday, R.id.etTbFriday)
        val ams = listOf(R.id.etAmMonday, R.id.etAmTuesday, R.id.etAmWednesday, R.id.etAmThursday, R.id.etAmFriday)
        val ors = listOf(R.id.etOrMonday, R.id.etOrTuesday, R.id.etOrWednesday, R.id.etOrThursday, R.id.etOrFriday)

        btnSave.isEnabled = false; btnSave.text = "SAVING..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = SupabaseService.client
                val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val startDate = dbFormat.parse(masterActivity.availableFrom)!!

                for (i in 0 until 5) {
                    val cal = Calendar.getInstance().apply { time = startDate }
                    cal.add(Calendar.DAY_OF_YEAR, i)
                    val dbDateStr = dbFormat.format(cal.time)

                    val existing = db.from("dll_daily_entry").select { filter { eq("main_id", masterActivity.dllMainId); eq("entry_date", dbDateStr) } }.decodeSingleOrNull<DailyEntryForRefs>()

                    val dailyId = if (existing != null) {
                        existing.id
                    } else {
                        db.from("dll_daily_entry").insert(DailyEntryInsert(masterActivity.dllMainId, dbDateStr)) { select() }.decodeSingle<DailyEntryForRefs>().id
                    }

                    db.from("dll_references").delete { filter { eq("daily_entry_id", dailyId) } }

                    val refs = mutableListOf<ReferenceInsert>()
                    fun addRef(title: String, id: Int) {
                        val text = view.findViewById<EditText>(id).text.toString().trim()
                        if (text.isNotEmpty()) refs.add(ReferenceInsert(dailyId, title, text))
                    }

                    addRef("1. Teacher's Guide pages", tgs[i])
                    addRef("2. Learner's Materials' pages", lms[i])
                    addRef("3. Textbook pages", tbs[i])
                    addRef("4. Additional Materials", ams[i])
                    addRef("5. Other References", ors[i])

                    if (refs.isNotEmpty()) db.from("dll_references").insert(refs)
                }

                withContext(Dispatchers.Main) {
                    btnSave.isEnabled = true; btnSave.text = "SAVE"
                    ShowToast.showMessage(requireContext(), "Resources Saved!")
                    masterActivity.switchToViewMode()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    btnSave.isEnabled = true; btnSave.text = "SAVE"
                    ShowToast.showMessage(requireContext(), "Error: ${e.message}")
                }
            }
        }
    }

    @Serializable
    private data class DailyEntryForRefs(val id: Int, val entry_date: String)
    @Serializable
    private data class ReferenceRow(val id: Int, val daily_entry_id: Int, val reference_title: String, val reference_text: String)

    @Serializable
    private data class DailyEntryInsert(val main_id: Int, val entry_date: String)
    @Serializable
    private data class ReferenceInsert(val daily_entry_id: Int, val reference_title: String, val reference_text: String)
}