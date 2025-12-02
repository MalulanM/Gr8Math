package com.example.gr8math

import DllMain
import DllProcedureDisplay
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.Locale

class DLLProceduresFragment : Fragment() {

    private lateinit var container: LinearLayout
    private var proceduresList: List<DllProcedureDisplay> = emptyList()
    private var mainDllData: DllMain? = null

    companion object {
        private const val ARG_DLL_MAIN = "dll_main_data" // 🌟 ADD THE CONSTANT HERE
        private const val ARG_DLL_PROCS = "dll_procedures"

        // 🌟 UPDATE newInstance to accept DllMain
        fun newInstance(mainData: DllMain, procedures: List<DllProcedureDisplay>): DLLProceduresFragment {
            val fragment = DLLProceduresFragment()
            val args = Bundle()
            args.putSerializable(ARG_DLL_MAIN, mainData) // 🌟 Pass mainData
            args.putSerializable(ARG_DLL_PROCS, ArrayList(procedures))
            fragment.arguments = args
            return fragment
        }
    }
    // 1. Data Storage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        proceduresList = arguments?.getSerializable(ARG_DLL_PROCS) as? List<DllProcedureDisplay> ?: emptyList()
        // 🌟 RETRIEVE mainDllData
        mainDllData = arguments?.getSerializable(ARG_DLL_MAIN) as? DllMain
    }

    // 2. Edit Launcher
    @Suppress("UNCHECKED_CAST")
    private val editLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Since procedures are complex and multi-day, editing would require editing DLLEditActivity
            // to handle nested lists. For simple refresh, we just toast:
            Toast.makeText(context, "Procedures data refresh required.", Toast.LENGTH_SHORT).show()
            // *** In a real app, you would refetch the DLL list here. ***
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dll_procedures, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view.findViewById(R.id.dynamicContentContainer)
        val btnEdit = view.findViewById<View>(R.id.btnEdit)

        refreshView()

        // In DLLProceduresFragment.kt -> onViewCreated -> btnEdit.setOnClickListener

        btnEdit.setOnClickListener {
            if (proceduresList.isEmpty()) {
                Toast.makeText(context, "No procedures to edit.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val procToEdit = proceduresList[0] // 🌟 Edit the first record
            val mainDllId = mainDllData?.id ?: -1

            // 🌟 CORRECTED: Map ALL fields from the DllProcedureDisplay object.
            val editMap = linkedMapOf(
                // CRITICAL: Database ID and Date fields for API lookup
                DLLEditActivity.KEY_RECORD_ID to procToEdit.id.toString(),
                DLLEditActivity.KEY_DATE to procToEdit.date,

                "Review" to procToEdit.review,
                "Purpose" to procToEdit.purpose,
                "Example" to procToEdit.example,
                "Discussion Proper" to procToEdit.discussion_proper, // Matches UpdateProcedureRequest key discussion_proper
                "Developing Mastery" to procToEdit.developing_mastery, // Matches UpdateProcedureRequest key developing_mastery
                "Application" to procToEdit.application,
                "Generalization" to procToEdit.generalization,
                "Evaluation" to procToEdit.evaluation,
                "Additional Activities" to procToEdit.additional_act
            )

            val intent = Intent(requireContext(), DLLStep3Activity::class.java).apply {
                putExtra(DLLEditActivity.EXTRA_DLL_MAIN_ID, mainDllId)
                putExtra(DLLEditActivity.EXTRA_MODE_EDIT, true)


                val convertedFrom = convertDbDateToUiDate(mainDllData?.available_from)
                val convertedUntil = convertDbDateToUiDate(mainDllData?.available_until)

                putExtra("EXTRA_FROM", convertedFrom)
                putExtra("EXTRA_UNTIL", convertedUntil)

                // Pass ALL fields individually for DLLStep3Activity to retrieve them
                for ((key, value) in editMap) {
                    putExtra(key, value)
                }

                putExtra(DLLEditActivity.EXTRA_SECTION_TITLE, DLLEditActivity.EDIT_SECTION_OBJECTIVES)

            }

            editLauncher.launch(intent)
        }
    }

    // Inside DLLProceduresFragment class
    private fun convertDbDateToUiDate(dbDate: String?): String? {
        if (dbDate.isNullOrEmpty()) return null

        // Date format for the database (Input)
        val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        // Date format for the UI/Target Activity (Output)
        val uiFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)

        return try {
            val dateObject = dbFormat.parse(dbDate)
            if (dateObject != null) {
                uiFormat.format(dateObject)
            } else {
                dbDate // Return original if parsing fails (shouldn't happen if format is consistent)
            }
        } catch (e: Exception) {
            Log.e("DATE_CONVERT", "Failed to convert date: $dbDate", e)
            dbDate // Return original on failure
        }
    }

    private fun refreshView() {
        container.removeAllViews()

        if (proceduresList.isEmpty()) {
            container.addView(TextView(context).apply { text = "No procedures recorded for this DLL." })
            return
        }

        // Loop through each day's procedure
        for ((index, proc) in proceduresList.withIndex()) {
            // Add a header for the day
            val headerView = TextView(context).apply {
                text = "PROCEDURE DAY ${index + 1} (${proc.date})"
                textSize = 16f
            }
            container.addView(headerView)

            // Display all fields for that day using the fixed titles
            val fields = mapOf(
                "Review" to proc.review,
                "Purpose" to proc.purpose,
                "Example" to proc.example,
                "Discussion Proper" to proc.discussion_proper,
                "Developing Mastery" to proc.developing_mastery,
                "Application" to proc.application,
                "Generalization" to proc.generalization,
                "Evaluation" to proc.evaluation,
                "Additional Activities" to proc.additional_act
            )

            for ((title, content) in fields) {
                if (content.isNotBlank()) { // Only display fields that have content
                    val sectionView = layoutInflater.inflate(R.layout.item_dll_section_display, container, false)
                    sectionView.findViewById<TextView>(R.id.tvSectionTitle).text = title
                    sectionView.findViewById<TextView>(R.id.tvSectionContent).text = content
                    container.addView(sectionView)
                }
            }
        }
    }
}