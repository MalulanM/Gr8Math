package com.example.gr8math

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

class DLLProceduresFragment : Fragment() {

    private lateinit var container: LinearLayout

    // 1. Data Storage
    private var procedureData = linkedMapOf(
        "Review" to "Recall previous lesson about algebraic expressions.",
        "Purpose" to "Establish purpose for the lesson on polynomials.",
        "Example" to "Show examples of polynomials.",
        "Discussion Proper #1" to "Discuss the definition of a polynomial.",
        "Developing Mastery" to "Group activity: Identify polynomials.",
        "Application" to "Real-world application of polynomials.",
        "Generalization" to "Summarize key points.",
        "Evaluation" to "Short quiz.",
        "Additional Activities" to "Assignment: Workbook page 20."
    )

    // 2. Edit Launcher
    @Suppress("UNCHECKED_CAST")
    private val editLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val updatedData = result.data?.getSerializableExtra(DLLEditActivity.EXTRA_DATA_MAP) as? HashMap<String, String>
            if (updatedData != null) {
                // IMPORTANT: Restore order if needed, though HashMap might shuffle.
                // For a real app, passing a List of Objects is safer for order than a Map.
                // But for now, we reload it.
                procedureData = LinkedHashMap(updatedData)
                refreshView()
                Toast.makeText(context, "Procedures Updated!", Toast.LENGTH_SHORT).show()
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
            val intent = Intent(requireContext(), DLLEditActivity::class.java).apply {
                putExtra(DLLEditActivity.EXTRA_SECTION_TITLE, "Edit Procedures")
                putExtra(DLLEditActivity.EXTRA_DATA_MAP, HashMap(procedureData))
            }
            editLauncher.launch(intent)
        }
    }

    private fun refreshView() {
        container.removeAllViews()
        for ((title, content) in procedureData) {
            val sectionView = layoutInflater.inflate(R.layout.item_dll_section_display, container, false)
            sectionView.findViewById<TextView>(R.id.tvSectionTitle).text = title
            sectionView.findViewById<TextView>(R.id.tvSectionContent).text = content
            container.addView(sectionView)
        }
    }
}