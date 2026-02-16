package com.example.gr8math.Activity.TeacherModule.DLL

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.gr8math.Data.Repository.DllDailyEntryEntity
import com.example.gr8math.R
import java.text.SimpleDateFormat
import java.util.Locale

class DLLObjectivesFragment : Fragment() {

    private lateinit var container: LinearLayout
    private var objectivesData: LinkedHashMap<String, String> = LinkedHashMap()
    private var dailyEntryData: DllDailyEntryEntity? = null

    companion object {
        private const val ARG_DAILY_ENTRY = "daily_entry_data"

        fun newInstance(entryData: DllDailyEntryEntity): DLLObjectivesFragment {
            val fragment = DLLObjectivesFragment()
            val args = Bundle()
            args.putSerializable(ARG_DAILY_ENTRY, entryData)
            fragment.arguments = args
            return fragment
        }
    }

    // Edit Launcher (For partial updates)
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

    // Full Edit Launcher (For full database refreshes)
    private val fullEditLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Trigger the main DLLViewActivity to refresh its data
            // Note: Make sure fetchDailyEntries() is NOT private in DLLViewActivity
            val targetMainId = dailyEntryData?.mainId
            if (targetMainId != null) {
                (requireActivity() as? DLLViewActivity)?.fetchDailyEntries(targetMainId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load the new Daily Entry data type
        dailyEntryData = arguments?.getSerializable(ARG_DAILY_ENTRY) as? DllDailyEntryEntity

        if (dailyEntryData != null) {
            // Objectives fields from dll_daily_entry
            objectivesData["Content Standard"] = dailyEntryData?.contentStandard ?: "N/A"
            objectivesData["Performance Standard"] = dailyEntryData?.performanceStandard ?: "N/A"
            objectivesData["Learning Competencies"] = dailyEntryData?.learningComp ?: "N/A"
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
            if (dailyEntryData == null) return@setOnClickListener

            val intent = Intent(requireContext(), DailyLessonLogActivity::class.java).apply {
                // 1. Tell the activity it's in EDIT MODE
                putExtra(DLLEditActivity.EXTRA_MODE_EDIT, true)
                // 2. Tell the activity WHAT SECTION it is editing
                putExtra(DLLEditActivity.EXTRA_SECTION_TITLE, DLLEditActivity.EDIT_SECTION_OBJECTIVES)

                // Pass the specific date of this entry
                val convertedDate = convertDbDateToUiDate(dailyEntryData?.entryDate)
                putExtra("EXTRA_ENTRY_DATE", convertedDate)

                // Pass the actual data for pre-filling
                putExtra("EXTRA_CONTENT_STD", dailyEntryData?.contentStandard)
                putExtra("EXTRA_PERF_STD", dailyEntryData?.performanceStandard)
                putExtra("EXTRA_COMPETENCIES", dailyEntryData?.learningComp)

                // Pass IDs so the Edit Activity knows what to update in the database
                putExtra("EXTRA_DAILY_ENTRY_ID", dailyEntryData?.id)
                putExtra(DLLEditActivity.EXTRA_DLL_MAIN_ID, dailyEntryData?.mainId)
            }

            // Launch the intent
            editLauncher.launch(intent)
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
                dbDate // Return original if parsing fails
            }
        } catch (e: Exception) {
            dbDate // Return original on failure
        }
    }

    private fun refreshView() {
        container.removeAllViews()

        if (objectivesData.isEmpty()) {
            container.addView(TextView(context).apply { text = "No objectives defined for this day." })
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