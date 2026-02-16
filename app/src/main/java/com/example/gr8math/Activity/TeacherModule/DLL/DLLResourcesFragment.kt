package com.example.gr8math.Activity.TeacherModule.DLL

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
import androidx.lifecycle.lifecycleScope
import com.example.gr8math.Data.Repository.DllDailyEntryEntity
import com.example.gr8math.R
import com.example.gr8math.Services.SupabaseService
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale

class DLLResourcesFragment : Fragment() {

    private lateinit var container: LinearLayout
    private var dailyEntryData: DllDailyEntryEntity? = null
    private var resourcesList: List<DllReferenceEntity> = emptyList()

    companion object {
        private const val ARG_DAILY_ENTRY = "daily_entry_data"

        fun newInstance(entryData: DllDailyEntryEntity): DLLResourcesFragment {
            val fragment = DLLResourcesFragment()
            val args = Bundle()
            args.putSerializable(ARG_DAILY_ENTRY, entryData)
            fragment.arguments = args
            return fragment
        }
    }

    private val resourcesEditLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            fetchReferences()
            val targetMainId = dailyEntryData?.mainId
            if (targetMainId != null) {
                (requireActivity() as? DLLViewActivity)?.fetchDailyEntries(targetMainId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dailyEntryData = arguments?.getSerializable(ARG_DAILY_ENTRY) as? DllDailyEntryEntity
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

        fetchReferences()

        btnEdit.setOnClickListener {
            if (dailyEntryData == null) return@setOnClickListener

            val resourceToEdit = resourcesList.firstOrNull()

            // Collect resource texts into an array (links/pages)
            val resourceTexts = resourcesList.map { it.referenceText ?: "" }.toTypedArray()

            // Use the reference title (sub-lesson) or fallback to daily entry content
            val currentRefTitle = resourceToEdit?.referenceTitle ?: dailyEntryData?.contentStandard ?: ""

            val intent = Intent(requireContext(), DLLStep2Activity::class.java).apply {
                putExtra(DLLEditActivity.EXTRA_MODE_EDIT, true)
                putExtra(DLLEditActivity.EXTRA_SECTION_TITLE, DLLEditActivity.SECTION_RESOURCES)

                putExtra(DLLEditActivity.EXTRA_DLL_MAIN_ID, dailyEntryData!!.mainId)
                putExtra("EXTRA_DAILY_ENTRY_ID", dailyEntryData!!.id)
                putExtra(DLLEditActivity.KEY_RECORD_ID, resourceToEdit?.id ?: -1)

                // Date is taken from the main daily entry record
                val convertedDate = convertDbDateToUiDate(dailyEntryData!!.entryDate)
                putExtra("EXTRA_ENTRY_DATE", convertedDate)

                // Prefill Data
                putExtra("EXTRA_STEP2_CONTENT", currentRefTitle)
                putExtra("EXTRA_PREFILL_RESOURCES_ARRAY", resourceTexts)
            }
            resourcesEditLauncher.launch(intent)
        }
    }

    private fun fetchReferences() {
        val entryId = dailyEntryData?.id ?: return

        lifecycleScope.launch {
            try {
                val refs = withContext(Dispatchers.IO) {
                    SupabaseService.client.from("dll_references")
                        .select {
                            filter { eq("daily_entry_id", entryId) }
                        }.decodeList<DllReferenceEntity>()
                }

                // 🌟 SAFETY CHECK: Only proceed if the fragment is still active
                if (!isAdded || context == null) return@launch

                resourcesList = refs
                refreshView()
            } catch (e: Exception) {
                Log.e("DLLResources", "Failed to fetch references", e)

                // 🌟 SAFETY CHECK: Use getContext() instead of requireContext()
                context?.let {
                    Toast.makeText(it, "Failed to load resources", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun refreshView() {
        container.removeAllViews()

        // 1. Display "Content" (This is your sub-lesson/ref_title)
        // Use the title from the first reference found, otherwise use daily entry's standard
        val displayTitle = resourcesList.firstOrNull()?.referenceTitle
            ?: dailyEntryData?.contentStandard
            ?: "N/A"

        val contentTitleView = layoutInflater.inflate(R.layout.item_dll_section_display, container, false)
        contentTitleView.findViewById<TextView>(R.id.tvSectionTitle).text = "Content"
        contentTitleView.findViewById<TextView>(R.id.tvSectionContent).text = displayTitle
        container.addView(contentTitleView)

        // 2. Display "Learning Resources" (Aggregated ref_texts)
        if (resourcesList.isEmpty()) {
            container.addView(TextView(context).apply {
                text = "No resources recorded for this day."
                setPadding(0, 20, 0, 0)
            })
            return
        }

        val resourcesHeaderView = layoutInflater.inflate(R.layout.item_dll_section_display, container, false)
        // Fixed title as requested
        resourcesHeaderView.findViewById<TextView>(R.id.tvSectionTitle).text = "Learning Resources"

        // Aggregate all resource texts (links, pages, etc.) into one bulleted string
        val resourcesText = resourcesList.joinToString(separator = "\n• ", prefix = "• ") {
            it.referenceText ?: ""
        }

        resourcesHeaderView.findViewById<TextView>(R.id.tvSectionContent).text = resourcesText
        container.addView(resourcesHeaderView)
    }

    private fun convertDbDateToUiDate(dbDate: String?): String? {
        if (dbDate.isNullOrEmpty()) return null
        val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val uiFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
        return try {
            val dateObject = dbFormat.parse(dbDate)
            if (dateObject != null) uiFormat.format(dateObject) else dbDate
        } catch (e: Exception) {
            dbDate
        }
    }

    @Serializable
    data class DllReferenceEntity(
        val id: Int,
        @SerialName("daily_entry_id") val dailyEntryId: Int,
        @SerialName("reference_title") val referenceTitle: String? = null,
        @SerialName("reference_text") val referenceText: String? = null
    ) : java.io.Serializable
}