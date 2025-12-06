package com.example.gr8math

import DllReflectionDisplay
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
import com.example.gr8math.DLLEditActivity.Companion.EXTRA_DLL_MAIN_ID
import com.example.gr8math.DLLEditActivity.Companion.EXTRA_MODE_EDIT
import com.example.gr8math.DLLEditActivity.Companion.EXTRA_SECTION_TITLE
import com.example.gr8math.DLLEditActivity.Companion.KEY_DATE
import com.example.gr8math.DLLEditActivity.Companion.KEY_RECORD_ID
import java.text.SimpleDateFormat
import java.util.Locale

class DLLReflectionFragment : Fragment() {

    private lateinit var container: LinearLayout
    private var reflectionsList: List<DllReflectionDisplay> = emptyList()

    // 1. Data Storage

    companion object {
        private const val ARG_DLL_REFLEC = "dll_reflections"
        fun newInstance(reflections: List<DllReflectionDisplay>): DLLReflectionFragment {
            val fragment = DLLReflectionFragment()
            val args = Bundle()
            args.putSerializable(ARG_DLL_REFLEC, ArrayList(reflections))
            fragment.arguments = args
            return fragment
        }
    }

    // 2. Edit Launcher
    @Suppress("UNCHECKED_CAST")
    private val editLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {

        }
    }

    private val reflectionEditLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Trigger the main DLLViewActivity to refresh its data
            (requireActivity() as? DLLViewActivity)?.fetchAllDlls()

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retrieve data
        reflectionsList = arguments?.getSerializable(ARG_DLL_REFLEC) as? List<DllReflectionDisplay> ?: emptyList()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dll_reflection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view.findViewById(R.id.dynamicContentContainer)
        val btnEdit = view.findViewById<View>(R.id.btnEdit)

        refreshView()

        btnEdit.setOnClickListener {
            if (reflectionsList.isEmpty()) {
                Toast.makeText(context, "No reflection to edit.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val reflectionToEdit = reflectionsList[0]

            // 1. Get the current DLL main data from the parent Activity
            val dllViewActivity = requireActivity() as? DLLViewActivity
            val currentDll = dllViewActivity?.allDlls?.get(dllViewActivity.currentDllIndex)

            if (currentDll == null) return@setOnClickListener

            // 2. Launch DLLStep4Activity for single-step edit
            val intent = Intent(requireContext(), DLLStep4Activity::class.java).apply {

                // --- A. EDIT FLAGS & CONTEXT ---
                putExtra(EXTRA_MODE_EDIT, true)
                putExtra(EXTRA_SECTION_TITLE, DLLEditActivity.SECTION_REFLECTION)

                // --- B. CRITICAL IDs (Passed for API) ---
                // NOTE: We pass the specific record ID for the update API, not the main DLL ID.
                putExtra(EXTRA_DLL_MAIN_ID, currentDll.main.id)
                putExtra(KEY_RECORD_ID, reflectionToEdit.id)

                // --- C. PREFILL DATA (Fields needed by DLLStep4Activity) ---
                // We pass the fields of the specific record being edited
                putExtra(KEY_DATE, reflectionToEdit.date)
                putExtra("Remarks", reflectionToEdit.remark)
                putExtra("Reflection", reflectionToEdit.reflection)

                val convertedFrom = convertDbDateToUiDate(currentDll.main.available_from)
                val convertedUntil = convertDbDateToUiDate(currentDll.main.available_until)

                putExtra("EXTRA_FROM", convertedFrom)
                putExtra("EXTRA_UNTIL", convertedUntil)
            }
            reflectionEditLauncher.launch(intent)
        }
    }

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

            dbDate // Return original on failure
        }
    }

    private fun refreshView() {
        container.removeAllViews()

        if (reflectionsList.isEmpty()) {
            container.addView(TextView(context).apply { text = "No reflection recorded for this DLL." })
            return
        }

        for ((index, reflection) in reflectionsList.withIndex()) {

            // Header for the specific day
            val headerView = TextView(context).apply {
                text = "REFLECTION DAY ${index + 1} (${reflection.date})"
                textSize = 16f
                // Style this well
            }
            container.addView(headerView)

            // Display Remarks
            val remarksView = layoutInflater.inflate(R.layout.item_dll_section_display, container, false)
            remarksView.findViewById<TextView>(R.id.tvSectionTitle).text = "Remarks"
            remarksView.findViewById<TextView>(R.id.tvSectionContent).text = reflection.remark
            container.addView(remarksView)

            // Display Reflection
            val reflectionView = layoutInflater.inflate(R.layout.item_dll_section_display, container, false)
            reflectionView.findViewById<TextView>(R.id.tvSectionTitle).text = "Reflection"
            reflectionView.findViewById<TextView>(R.id.tvSectionContent).text = reflection.reflection
            container.addView(reflectionView)
        }
    }
}