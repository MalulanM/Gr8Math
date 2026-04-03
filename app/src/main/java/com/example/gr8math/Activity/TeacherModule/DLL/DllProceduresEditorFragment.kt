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

class DllProceduresEditorFragment : Fragment(R.layout.fragment_dll_procedures_editor) {

    private val masterActivity get() = activity as DllMasterEditorActivity

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnSave = view.findViewById<Button>(R.id.btnSaveFragment)

        btnSave.setOnClickListener {
            // 1. Build the confirmation dialog
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle("Do you want to save changes?")
                .setPositiveButton("Yes") { _, _ ->
                    // 2. Only save if they confirm
                    saveProcedures(view, btnSave)
                }
                .setNegativeButton("No") { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                .show()

            // 3. Set button colors to Red to match your screenshot
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
            R.id.etAMon, R.id.etATue, R.id.etAWed, R.id.etAThu, R.id.etAFri,
            R.id.etBMon, R.id.etBTue, R.id.etBWed, R.id.etBThu, R.id.etBFri,
            R.id.etCMon, R.id.etCTue, R.id.etCWed, R.id.etCThu, R.id.etCFri,
            R.id.etDMon, R.id.etDTue, R.id.etDWed, R.id.etDThu, R.id.etDFri,
            R.id.etEMon, R.id.etETue, R.id.etEWed, R.id.etEThu, R.id.etEFri,
            R.id.etFMon, R.id.etFTue, R.id.etFWed, R.id.etFThu, R.id.etFFri,
            R.id.etGMon, R.id.etGTue, R.id.etGWed, R.id.etGThu, R.id.etGFri,
            R.id.etHMon, R.id.etHTue, R.id.etHWed, R.id.etHThu, R.id.etHFri,
            R.id.etIMon, R.id.etITue, R.id.etIWed, R.id.etIThu, R.id.etIFri,
            R.id.etJMon, R.id.etJTue, R.id.etJWed, R.id.etJThu, R.id.etJFri
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
                    .decodeList<DailyEntryProc>()

                withContext(Dispatchers.Main) {
                    val aIds = listOf(R.id.etAMon, R.id.etATue, R.id.etAWed, R.id.etAThu, R.id.etAFri)
                    val bIds = listOf(R.id.etBMon, R.id.etBTue, R.id.etBWed, R.id.etBThu, R.id.etBFri)
                    val cIds = listOf(R.id.etCMon, R.id.etCTue, R.id.etCWed, R.id.etCThu, R.id.etCFri)
                    val dIds = listOf(R.id.etDMon, R.id.etDTue, R.id.etDWed, R.id.etDThu, R.id.etDFri)
                    val eIds = listOf(R.id.etEMon, R.id.etETue, R.id.etEWed, R.id.etEThu, R.id.etEFri)
                    val fIds = listOf(R.id.etFMon, R.id.etFTue, R.id.etFWed, R.id.etFThu, R.id.etFFri)
                    val gIds = listOf(R.id.etGMon, R.id.etGTue, R.id.etGWed, R.id.etGThu, R.id.etGFri)
                    val hIds = listOf(R.id.etHMon, R.id.etHTue, R.id.etHWed, R.id.etHThu, R.id.etHFri)
                    val iIds = listOf(R.id.etIMon, R.id.etITue, R.id.etIWed, R.id.etIThu, R.id.etIFri)
                    val jIds = listOf(R.id.etJMon, R.id.etJTue, R.id.etJWed, R.id.etJThu, R.id.etJFri)

                    val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val startDate = dbFormat.parse(masterActivity.availableFrom)!!

                    for (i in 0 until 5) {
                        val cal = Calendar.getInstance().apply { time = startDate }
                        cal.add(Calendar.DAY_OF_YEAR, i)
                        val expectedDateStr = dbFormat.format(cal.time)

                        val entry = entries.find { it.entry_date == expectedDateStr }
                        if (entry != null) {
                            view.findViewById<EditText>(aIds[i]).setText(entry.review ?: "")
                            view.findViewById<EditText>(bIds[i]).setText(entry.purpose ?: "")
                            view.findViewById<EditText>(cIds[i]).setText(entry.example ?: "")

                            val disc = entry.discussion_proper ?: ""
                            var d1 = ""; var d2 = ""
                            if (disc.contains("#1") && disc.contains("#2")) {
                                val parts = disc.split("#2")
                                d1 = parts[0].replace("#1", "").trim()
                                d2 = parts[1].trim()
                            } else if (disc.contains("#1")) {
                                d1 = disc.replace("#1", "").trim()
                            } else if (disc.contains("#2")) {
                                d2 = disc.replace("#2", "").trim()
                            } else {
                                d1 = disc
                            }

                            view.findViewById<EditText>(dIds[i]).setText(d1)
                            view.findViewById<EditText>(eIds[i]).setText(d2)
                            view.findViewById<EditText>(fIds[i]).setText(entry.developing_mastery ?: "")
                            view.findViewById<EditText>(gIds[i]).setText(entry.application ?: "")
                            view.findViewById<EditText>(hIds[i]).setText(entry.generalization ?: "")
                            view.findViewById<EditText>(iIds[i]).setText(entry.evaluation ?: "")
                            view.findViewById<EditText>(jIds[i]).setText(entry.additional_act ?: "")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveProcedures(view: View, btnSave: Button) {
        if (masterActivity.dllMainId == -1) {
            ShowToast.showMessage(requireContext(), "Please save Objectives first.")
            return
        }

        fun getDayValues(ids: List<Int>): List<String> = ids.map { view.findViewById<EditText>(it).text.toString().trim() }

        val aList = getDayValues(listOf(R.id.etAMon, R.id.etATue, R.id.etAWed, R.id.etAThu, R.id.etAFri))
        val bList = getDayValues(listOf(R.id.etBMon, R.id.etBTue, R.id.etBWed, R.id.etBThu, R.id.etBFri))
        val cList = getDayValues(listOf(R.id.etCMon, R.id.etCTue, R.id.etCWed, R.id.etCThu, R.id.etCFri))
        val dList = getDayValues(listOf(R.id.etDMon, R.id.etDTue, R.id.etDWed, R.id.etDThu, R.id.etDFri))
        val eList = getDayValues(listOf(R.id.etEMon, R.id.etETue, R.id.etEWed, R.id.etEThu, R.id.etEFri))
        val fList = getDayValues(listOf(R.id.etFMon, R.id.etFTue, R.id.etFWed, R.id.etFThu, R.id.etFFri))
        val gList = getDayValues(listOf(R.id.etGMon, R.id.etGTue, R.id.etGWed, R.id.etGThu, R.id.etGFri))
        val hList = getDayValues(listOf(R.id.etHMon, R.id.etHTue, R.id.etHWed, R.id.etHThu, R.id.etHFri))
        val iList = getDayValues(listOf(R.id.etIMon, R.id.etITue, R.id.etIWed, R.id.etIThu, R.id.etIFri))
        val jList = getDayValues(listOf(R.id.etJMon, R.id.etJTue, R.id.etJWed, R.id.etJThu, R.id.etJFri))

        btnSave.isEnabled = false; btnSave.text = "SAVING..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = SupabaseService.client
                val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val startDate = dbFormat.parse(masterActivity.availableFrom)!!

                for (idx in 0 until 5) {
                    val cal = Calendar.getInstance().apply { time = startDate }
                    cal.add(Calendar.DAY_OF_YEAR, idx)
                    val dbDateStr = dbFormat.format(cal.time)

                    val d1 = dList[idx]
                    val d2 = eList[idx]
                    val discussionCombined = if (d1.isNotEmpty() || d2.isNotEmpty()) "${if(d1.isNotEmpty()) "#1 $d1 " else ""}${if(d2.isNotEmpty()) "#2 $d2" else ""}".trim() else ""

                    val existing = db.from("dll_daily_entry").select { filter { eq("main_id", masterActivity.dllMainId); eq("entry_date", dbDateStr) } }.decodeSingleOrNull<DailyEntryRef>()

                    if (existing != null) {
                        db.from("dll_daily_entry").update({
                            set("review", aList[idx])
                            set("purpose", bList[idx])
                            set("example", cList[idx])
                            set("discussion_proper", discussionCombined)
                            set("developing_mastery", fList[idx])
                            set("application", gList[idx])
                            set("generalization", hList[idx])
                            set("evaluation", iList[idx])
                            set("additional_act", jList[idx])
                        }) { filter { eq("id", existing.id) } }
                    } else {
                        val insertPayload = DailyEntryProc(
                            main_id = masterActivity.dllMainId,
                            entry_date = dbDateStr,
                            review = aList[idx],
                            purpose = bList[idx],
                            example = cList[idx],
                            discussion_proper = discussionCombined,
                            developing_mastery = fList[idx],
                            application = gList[idx],
                            generalization = hList[idx],
                            evaluation = iList[idx],
                            additional_act = jList[idx]
                        )
                        db.from("dll_daily_entry").insert(insertPayload)
                    }
                }

                withContext(Dispatchers.Main) {
                    btnSave.isEnabled = true; btnSave.text = "SAVE"
                    ShowToast.showMessage(requireContext(), "Procedures Saved!")
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
    private data class DailyEntryRef(val id: Int)
    @Serializable
    private data class DailyEntryProc(
        val id: Int? = null, val main_id: Int? = null, val entry_date: String? = null,
        val review: String? = null, val purpose: String? = null, val example: String? = null,
        val discussion_proper: String? = null, val developing_mastery: String? = null,
        val application: String? = null, val generalization: String? = null,
        val evaluation: String? = null, val additional_act: String? = null
    )
}