package com.example.gr8math

import DllMain
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

class DLLObjectivesFragment : Fragment() {

    private lateinit var container: LinearLayout
    // Change to nullable and initialize empty
    private var objectivesData: LinkedHashMap<String, String> = LinkedHashMap()
    private var mainDllData: DllMain? = null
    // 1. Data Storage
    companion object {
        private const val ARG_DLL_MAIN = "dll_main_data"
        fun newInstance(mainData: DllMain): DLLObjectivesFragment {
            val fragment = DLLObjectivesFragment()
            val args = Bundle()
            args.putSerializable(ARG_DLL_MAIN, mainData)
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
            val updatedData = result.data?.getSerializableExtra(DLLEditActivity.EXTRA_DATA_MAP) as? HashMap<String, String>
            if (updatedData != null) {
                // Update local data and redraw
                objectivesData = LinkedHashMap(updatedData)
                refreshView()
                Toast.makeText(context, "Objectives Updated!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val fullEditLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Trigger the main DLLViewActivity to refresh its data
            (requireActivity() as? DLLViewActivity)?.fetchAllDlls()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mainData = arguments?.getSerializable(ARG_DLL_MAIN) as? DllMain
        if (mainData != null) {
            // Objectives fields
            objectivesData["Content Standard"] = mainData.content_standard
            objectivesData["Performance Standard"] = mainData.performance_standard
            objectivesData["Learning Competencies"] = mainData.learning_comp

        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dll_objectives, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view.findViewById(R.id.dynamicContentContainer)
        val btnEdit = view.findViewById<View>(R.id.btnEdit)

        // Initial Load
        refreshView()

        btnEdit.setOnClickListener {
            val mainData = arguments?.getSerializable(ARG_DLL_MAIN) as? DllMain
            if (mainData == null) return@setOnClickListener



            val dllViewActivity = requireActivity() as? DLLViewActivity
            val currentDll = dllViewActivity?.allDlls[dllViewActivity.currentDllIndex]

            if (mainData == null || currentDll == null) return@setOnClickListener

            val intent = Intent(requireContext(), DailyLessonLogActivity::class.java).apply {
                // 1. Tell the activity it's in EDIT MODE
                putExtra(DLLEditActivity.EXTRA_MODE_EDIT, true)
                // 2. Tell the activity WHAT SECTION it is editing
                putExtra(DLLEditActivity.EXTRA_SECTION_TITLE, DLLEditActivity.EDIT_SECTION_OBJECTIVES)

                val convertedFrom = convertDbDateToUiDate(mainDllData?.available_from)
                val convertedUntil = convertDbDateToUiDate(mainDllData?.available_until)

                putExtra("EXTRA_FROM", convertedFrom)
                putExtra("EXTRA_UNTIL", convertedUntil)
                // Pass the actual data for pre-filling
                putExtra("EXTRA_CONTENT_STD", currentDll.main.content_standard)
                putExtra("EXTRA_PERF_STD", currentDll.main.performance_standard)
                putExtra("EXTRA_COMPETENCIES", currentDll.main.learning_comp)

                // Pass the DLL ID required for the API call
                putExtra(DLLEditActivity.EXTRA_DLL_MAIN_ID, currentDll.main.id)
            }
            // Use the fragment's launcher if defined, or just startActivity
            startActivity(intent)
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
        // Use the loaded data for display
        if (objectivesData.isEmpty()) {
            container.addView(TextView(context).apply { text = "No objectives defined for this DLL." })
            return
        }
        for ((title, content) in objectivesData) {
            val sectionView = layoutInflater.inflate(R.layout.item_dll_section_display, container, false)
            sectionView.findViewById<TextView>(R.id.tvSectionTitle).text = title
            sectionView.findViewById<TextView>(R.id.tvSectionContent).text = content
            container.addView(sectionView)
        }
    }
}