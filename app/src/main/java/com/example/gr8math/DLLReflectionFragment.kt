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

class DLLReflectionFragment : Fragment() {

    private lateinit var container: LinearLayout

    // 1. Data Storage
    private var reflectionData = linkedMapOf(
        "Remarks" to "Lesson finished successfully.",
        "Reflection" to "80% of students passed the evaluation."
    )

    // 2. Edit Launcher
    @Suppress("UNCHECKED_CAST")
    private val editLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val updatedData = result.data?.getSerializableExtra(DLLEditActivity.EXTRA_DATA_MAP) as? HashMap<String, String>
            if (updatedData != null) {
                reflectionData = LinkedHashMap(updatedData)
                refreshView()
                Toast.makeText(context, "Reflection Updated!", Toast.LENGTH_SHORT).show()
            }
        }
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
            val intent = Intent(requireContext(), DLLEditActivity::class.java).apply {
                putExtra(DLLEditActivity.EXTRA_SECTION_TITLE, "Edit Reflection")
                putExtra(DLLEditActivity.EXTRA_DATA_MAP, HashMap(reflectionData))
            }
            editLauncher.launch(intent)
        }
    }

    private fun refreshView() {
        container.removeAllViews()
        for ((title, content) in reflectionData) {
            val sectionView = layoutInflater.inflate(R.layout.item_dll_section_display, container, false)
            sectionView.findViewById<TextView>(R.id.tvSectionTitle).text = title
            sectionView.findViewById<TextView>(R.id.tvSectionContent).text = content
            container.addView(sectionView)
        }
    }
}