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

class DLLReflectionFragment : Fragment() {

    private lateinit var container: LinearLayout
    private var dailyEntryData: DllDailyEntryEntity? = null

    companion object {
        private const val ARG_DAILY_ENTRY = "daily_entry_data"

        // Pass the single day's data instead of a list
        fun newInstance(entryData: DllDailyEntryEntity): DLLReflectionFragment {
            val fragment = DLLReflectionFragment()
            val args = Bundle()
            args.putSerializable(ARG_DAILY_ENTRY, entryData)
            fragment.arguments = args
            return fragment
        }
    }

    // Edit Launcher (Triggers a refresh of the parent activity when returning)
    private val reflectionEditLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
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
        return inflater.inflate(R.layout.fragment_dll_reflection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view.findViewById(R.id.dynamicContentContainer)
        val btnEdit = view.findViewById<View>(R.id.btnEdit)

        refreshView()

        btnEdit.setOnClickListener {
            if (dailyEntryData == null) {
                Toast.makeText(context, "No reflection to edit.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Launch DLLStep4Activity for single-step edit
            val intent = Intent(requireContext(), DLLStep4Activity::class.java).apply {

                // --- A. EDIT FLAGS & CONTEXT ---
                putExtra(DLLEditActivity.EXTRA_MODE_EDIT, true)
                putExtra(DLLEditActivity.EXTRA_SECTION_TITLE, DLLEditActivity.SECTION_REFLECTION)

                // --- B. CRITICAL IDs (Passed for API) ---
                putExtra(DLLEditActivity.EXTRA_DLL_MAIN_ID, dailyEntryData!!.mainId)
                putExtra("EXTRA_DAILY_ENTRY_ID", dailyEntryData!!.id)

                val convertedDate = convertDbDateToUiDate(dailyEntryData!!.entryDate)
                putExtra("EXTRA_ENTRY_DATE", convertedDate)

                // --- C. PREFILL DATA (Fields needed by DLLStep4Activity) ---
                putExtra("Remarks", dailyEntryData!!.remark ?: "")
                putExtra("Reflection", dailyEntryData!!.reflection ?: "")
            }
            reflectionEditLauncher.launch(intent)
        }
    }

    private fun refreshView() {
        container.removeAllViews()

        if (dailyEntryData == null) {
            container.addView(TextView(context).apply { text = "No reflection recorded for this day." })
            return
        }

        val remark = dailyEntryData?.remark
        val reflection = dailyEntryData?.reflection

        // If both are empty, just show a placeholder
        if (remark.isNullOrBlank() && reflection.isNullOrBlank()) {
            container.addView(TextView(context).apply { text = "No reflection recorded for this day." })
            return
        }

        // Display Remarks
        if (!remark.isNullOrBlank()) {
            val remarksView = layoutInflater.inflate(R.layout.item_dll_section_display, container, false)
            remarksView.findViewById<TextView>(R.id.tvSectionTitle).text = "Remarks"
            remarksView.findViewById<TextView>(R.id.tvSectionContent).text = remark
            container.addView(remarksView)
        }

        // Display Reflection
        if (!reflection.isNullOrBlank()) {
            val reflectionView = layoutInflater.inflate(R.layout.item_dll_section_display, container, false)
            reflectionView.findViewById<TextView>(R.id.tvSectionTitle).text = "Reflection"
            reflectionView.findViewById<TextView>(R.id.tvSectionContent).text = reflection
            container.addView(reflectionView)
        }
    }

    private fun convertDbDateToUiDate(dbDate: String?): String? {
        if (dbDate.isNullOrEmpty()) return null

        val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val uiFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)

        return try {
            val dateObject = dbFormat.parse(dbDate)
            if (dateObject != null) {
                uiFormat.format(dateObject)
            } else {
                dbDate
            }
        } catch (e: Exception) {
            dbDate
        }
    }
}