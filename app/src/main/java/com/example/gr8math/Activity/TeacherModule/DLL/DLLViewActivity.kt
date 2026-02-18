package com.example.gr8math.Activity.TeacherModule.DLL

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.gr8math.Data.Repository.DllDailyEntryEntity
import com.example.gr8math.Data.Repository.DllRepository
import com.example.gr8math.R
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.Utils.UIUtils
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class DLLViewActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnDatePrev: ImageButton
    private lateinit var btnDateNext: ImageButton
    private lateinit var tvDateHeader: TextView

    private lateinit var loadingLayout: View
    private lateinit var loadingProgress: View
    private lateinit var loadingText: TextView
    private lateinit var btnSectionPrev: ImageButton
    private lateinit var btnSectionNext: ImageButton

    private val repository = DllRepository()
    private var targetMainId: Int = -1

    var dailyEntries: List<DllDailyEntryEntity> = emptyList()
    var currentDayIndex: Int = 0

    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val uiDateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dll_view)

        initViews()

        targetMainId = intent.getIntExtra("target_dll_id", -1)

        if (targetMainId != -1) {
            fetchDailyEntries(targetMainId)
        } else {
            tvDateHeader.text = "Error: Invalid DLL ID"
        }
    }

    override fun onRestart() {
        super.onRestart()
        if (targetMainId != -1) {
            fetchDailyEntries(targetMainId, isRefresh = true)
        }
    }

    private fun initViews() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        tvDateHeader = findViewById(R.id.tvDateHeader)
        btnDatePrev = findViewById(R.id.btnDatePrev)
        btnDateNext = findViewById(R.id.btnDateNext)
        btnSectionPrev = findViewById(R.id.btnSectionPrev)
        btnSectionNext = findViewById(R.id.btnSectionNext)
        viewPager = findViewById(R.id.viewPager)

        loadingLayout = findViewById(R.id.loadingLayout)
        loadingProgress = findViewById(R.id.loadingProgressBg)
        loadingText = findViewById(R.id.loadingText)

        setNavigationVisibility(View.INVISIBLE)

        btnDatePrev.setOnClickListener { navigateDay(-1) }
        btnDateNext.setOnClickListener { navigateDay(1) }
        btnSectionPrev.setOnClickListener { viewPager.currentItem -= 1 }
        btnSectionNext.setOnClickListener { viewPager.currentItem += 1 }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                btnSectionPrev.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
                viewPager.adapter?.let { adapter ->
                    btnSectionNext.visibility = if (position == adapter.itemCount - 1) View.INVISIBLE else View.VISIBLE
                }
            }
        })
    }

    private fun setNavigationVisibility(visibility: Int) {
        btnDatePrev.visibility = visibility
        btnDateNext.visibility = visibility
    }

    fun fetchDailyEntries(mainId: Int, isRefresh: Boolean = false) {
        if (!isRefresh) UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)

        lifecycleScope.launch {
            val result = repository.getDailyEntries(mainId)
            UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)

            result.onSuccess { entries ->
                if (entries.isNotEmpty()) {
                    dailyEntries = entries
                    displayCurrentDay()
                    setNavigationVisibility(View.VISIBLE)
                }
            }.onFailure {
                ShowToast.showMessage(this@DLLViewActivity, "Failed to fetch details.")
            }
        }
    }

    private fun navigateDay(direction: Int) {
        val newIndex = currentDayIndex + direction
        if (newIndex in dailyEntries.indices) {
            currentDayIndex = newIndex
            displayCurrentDay()
        }
    }

    private fun displayCurrentDay() {
        if (dailyEntries.isEmpty() || currentDayIndex !in dailyEntries.indices) return

        val currentEntry = dailyEntries[currentDayIndex]
        tvDateHeader.text = formatDisplayDate(currentEntry.entryDate)

        viewPager.adapter = DLLViewPagerAdapter(this, currentEntry)
        viewPager.currentItem = 0

        btnDatePrev.visibility = if (currentDayIndex == 0) View.INVISIBLE else View.VISIBLE
        btnDateNext.visibility = if (currentDayIndex == dailyEntries.size - 1) View.INVISIBLE else View.VISIBLE
    }

    private fun formatDisplayDate(dateString: String): String {
        return try {
            val date = dbDateFormat.parse(dateString)
            if (date != null) uiDateFormat.format(date) else dateString
        } catch (e: Exception) { dateString }
    }

    private inner class DLLViewPagerAdapter(
        activity: AppCompatActivity,
        private val dailyEntry: DllDailyEntryEntity
    ) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 4
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> DLLObjectivesFragment.newInstance(dailyEntry)
                1 -> DLLResourcesFragment.newInstance(dailyEntry)
                2 -> DLLProceduresFragment.newInstance(dailyEntry)
                3 -> DLLReflectionFragment.newInstance(dailyEntry)
                else -> DLLObjectivesFragment.newInstance(dailyEntry)
            }
        }
    }
}