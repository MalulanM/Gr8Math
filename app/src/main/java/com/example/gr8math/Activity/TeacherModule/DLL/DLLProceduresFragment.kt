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

class DLLProceduresFragment : Fragment() {

    private lateinit var container: LinearLayout
    private var dailyEntryData: DllDailyEntryEntity? = null

    companion object {
        private const val ARG_DAILY_ENTRY = "daily_entry_data"

        // Receives the specific daily entry from the ViewPager adapter
        fun newInstance(entryData: DllDailyEntryEntity): DLLProceduresFragment {
            val fragment = DLLProceduresFragment()
            val args = Bundle()
            args.putSerializable(ARG_DAILY_ENTRY, entryData)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dailyEntryData = arguments?.getSerializable(ARG_DAILY_ENTRY) as? DllDailyEntryEntity
    }

    // Edit Launcher (Triggers a refresh of the parent activity when returning)
    private val proceduresEditLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val targetMainId = dailyEntryData?.mainId
            if (targetMainId != null) {
                (requireActivity() as? DLLViewActivity)?.fetchDailyEntries(targetMainId)
            }
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

        btnEdit.setOnClickListener {
            if (dailyEntryData == null) {
                Toast.makeText(context, "No procedures to edit.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(requireContext(), DLLStep3Activity::class.java).apply {
                // 1. EDIT FLAGS
                putExtra(DLLEditActivity.EXTRA_MODE_EDIT, true)
                putExtra(DLLEditActivity.EXTRA_SECTION_TITLE, DLLEditActivity.SECTION_PROCEDURES)

                // 2. CRITICAL IDs & DATES
                putExtra(DLLEditActivity.EXTRA_DLL_MAIN_ID, dailyEntryData!!.mainId)
                putExtra("EXTRA_DAILY_ENTRY_ID", dailyEntryData!!.id)

                val convertedDate = convertDbDateToUiDate(dailyEntryData!!.entryDate)
                putExtra("EXTRA_ENTRY_DATE", convertedDate)

                // 3. PASS ALL FIELDS FOR PREFILL
                // Ensure these keys match what DLLStep3Activity expects!
                putExtra("Review", dailyEntryData!!.review ?: "")
                putExtra("Purpose", dailyEntryData!!.purpose ?: "")
                putExtra("Example", dailyEntryData!!.example ?: "")
                putExtra("Discussion Proper", dailyEntryData!!.discussionProper ?: "")
                putExtra("Developing Mastery", dailyEntryData!!.developingMastery ?: "")
                putExtra("Application", dailyEntryData!!.application ?: "")
                putExtra("Generalization", dailyEntryData!!.generalization ?: "")
                putExtra("Evaluation", dailyEntryData!!.evaluation ?: "")
                putExtra("Additional Activities", dailyEntryData!!.additionalAct ?: "")
            }

            proceduresEditLauncher.launch(intent)
        }
    }

    private fun refreshView() {
        container.removeAllViews()

        if (dailyEntryData == null) {
            container.addView(TextView(context).apply { text = "No procedures recorded for this day." })
            return
        }

        // Map the fields from the single daily entry
        val fields = mapOf(
            "Review" to dailyEntryData?.review,
            "Purpose" to dailyEntryData?.purpose,
            "Example" to dailyEntryData?.example,
            "Discussion Proper" to dailyEntryData?.discussionProper,
            "Developing Mastery" to dailyEntryData?.developingMastery,
            "Application" to dailyEntryData?.application,
            "Generalization" to dailyEntryData?.generalization,
            "Evaluation" to dailyEntryData?.evaluation,
            "Additional Activities" to dailyEntryData?.additionalAct
        )

        var hasContent = false

        for ((title, content) in fields) {
            if (!content.isNullOrBlank()) {
                hasContent = true
                val sectionView = layoutInflater.inflate(R.layout.item_dll_section_display, container, false)
                sectionView.findViewById<TextView>(R.id.tvSectionTitle).text = title
                sectionView.findViewById<TextView>(R.id.tvSectionContent).text = content
                container.addView(sectionView)
            }
        }

        if (!hasContent) {
            container.addView(TextView(context).apply { text = "No procedures recorded for this day." })
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