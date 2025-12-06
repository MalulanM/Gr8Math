package com.example.gr8math

import DllMain
import DllReferenceDisplay
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
import com.example.gr8math.DLLEditActivity.Companion.KEY_RECORD_ID
import java.text.SimpleDateFormat
import java.util.Locale

class DLLResourcesFragment : Fragment() {

    private lateinit var container: LinearLayout
    private var mainData: DllMain? = null
    private var resourcesList: List<DllReferenceDisplay> = emptyList()

    private var isEditMode = false
    private var dllMainId: Int = -1
    private var sectionTitle: String? = null

    companion object {
        private const val ARG_DLL_MAIN = "dll_main_data"
        private const val ARG_DLL_REFS = "dll_references"

        fun newInstance(mainData: DllMain, references: List<DllReferenceDisplay>): DLLResourcesFragment {
            val fragment = DLLResourcesFragment()
            val args = Bundle()
            args.putSerializable(ARG_DLL_MAIN, mainData)
            args.putSerializable(ARG_DLL_REFS, ArrayList(references))
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

    private val resourcesEditLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Trigger the main DLLViewActivity to refresh its data
            (requireActivity() as? DLLViewActivity)?.fetchAllDlls()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainData = arguments?.getSerializable(ARG_DLL_MAIN) as? DllMain
        resourcesList = arguments?.getSerializable(ARG_DLL_REFS) as? List<DllReferenceDisplay> ?: emptyList()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dll_resources, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view.findViewById(R.id.dynamicContentContainer)
        val btnEdit = view.findViewById<View>(R.id.btnEdit)

        refreshView()

        btnEdit.setOnClickListener {
            if (mainData == null) return@setOnClickListener

            // We must pass the original references list so DLLStep2 can rebuild the cards.
            // We'll pass the ID and date of the *first* resource record for the API update.
            val resourceToEdit = resourcesList.firstOrNull()

            // Collect resource texts into a simple array for DLLStep2 to unpack
            val resourceTexts = resourcesList.map { it.reference_text }.toTypedArray()

            val intent = Intent(requireContext(), DLLStep2Activity::class.java).apply {
                // 1. EDIT FLAGS
                putExtra(EXTRA_MODE_EDIT, true)
                putExtra(EXTRA_SECTION_TITLE, DLLEditActivity.SECTION_RESOURCES)

                // 2. CRITICAL IDs & DATES
                putExtra(EXTRA_DLL_MAIN_ID, mainData!!.id)
                putExtra(KEY_RECORD_ID, resourceToEdit?.id ?: -1) // ID of the specific reference record

                val convertedFrom = convertDbDateToUiDate(mainData!!.available_from)
                val convertedUntil = convertDbDateToUiDate(mainData!!.available_until)

                putExtra("EXTRA_FROM", convertedFrom)
                putExtra("EXTRA_UNTIL", convertedUntil)

                // 3. DATA FOR PREFILL
                putExtra("EXTRA_STEP2_CONTENT", mainData!!.content_standard)
                putExtra("EXTRA_PREFILL_RESOURCES_ARRAY", resourceTexts) // The list of resource texts
                putExtra("EXTRA_PREFILL_DATE", resourceToEdit?.date ?: "") // The date for the first card
            }
            resourcesEditLauncher.launch(intent)
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

        // 1. Display Content Title (from Step 2 content, stored in dll_main content_standard field)
        val contentTitleView = layoutInflater.inflate(R.layout.item_dll_section_display, container, false)
        contentTitleView.findViewById<TextView>(R.id.tvSectionTitle).text = "Content"
        contentTitleView.findViewById<TextView>(R.id.tvSectionContent).text = mainData?.content_standard ?: "N/A"
        container.addView(contentTitleView)

        // 2. Aggregate and Display Resources
        if (resourcesList.isEmpty()) {
            container.addView(TextView(context).apply { text = "No resources recorded." })
            return
        }

        // Group resources by reference_title (which is your Step 2 content, e.g., "Numbers and Number Sense")
        val groupedResources = resourcesList.groupBy { it.reference_title }

        // Since you're iterating over resources, we'll display them grouped by the title they share.
        // It's likely better to display them by date if multiple days are involved, but based on your structure:

        for ((title, resources) in groupedResources) {
            val resourcesHeaderView = layoutInflater.inflate(R.layout.item_dll_section_display, container, false)
            resourcesHeaderView.findViewById<TextView>(R.id.tvSectionTitle).text = "Learning Resources (${title})"

            val resourcesText = resources.joinToString(separator = "\n• ", prefix = "• ") {
                it.reference_text
            }
            resourcesHeaderView.findViewById<TextView>(R.id.tvSectionContent).text = resourcesText
            container.addView(resourcesHeaderView)
        }
    }
}