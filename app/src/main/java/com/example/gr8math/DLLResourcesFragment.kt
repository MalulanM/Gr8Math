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

class DLLResourcesFragment : Fragment() {

    private lateinit var container: LinearLayout

    // 1. Data Storage
    private var resourcesData = linkedMapOf(
        "Content" to "Numbers and Number Sense",
        "Learning Resources" to "• Math Book pp. 17-18\n• Youtube Video Link"
    )

    // 2. Edit Launcher
    @Suppress("UNCHECKED_CAST")
    private val editLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val updatedData = result.data?.getSerializableExtra(DLLEditActivity.EXTRA_DATA_MAP) as? HashMap<String, String>
            if (updatedData != null) {
                resourcesData = LinkedHashMap(updatedData)
                refreshView()
                Toast.makeText(context, "Resources Updated!", Toast.LENGTH_SHORT).show()
            }
        }
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
            val intent = Intent(requireContext(), DLLEditActivity::class.java).apply {
                putExtra(DLLEditActivity.EXTRA_SECTION_TITLE, "Edit Content & Resources")
                putExtra(DLLEditActivity.EXTRA_DATA_MAP, HashMap(resourcesData))
            }
            editLauncher.launch(intent)
        }
    }

    private fun refreshView() {
        container.removeAllViews()
        for ((title, content) in resourcesData) {
            val sectionView = layoutInflater.inflate(R.layout.item_dll_section_display, container, false)
            sectionView.findViewById<TextView>(R.id.tvSectionTitle).text = title
            sectionView.findViewById<TextView>(R.id.tvSectionContent).text = content
            container.addView(sectionView)
        }
    }
}