package com.example.gr8math

import CompleteDllEntry
import DllDisplayResponse
import DllListResponse
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.CurrentCourse
import com.example.gr8math.utils.UIUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale

class DLLViewActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2

    // Date Navigation (Top)
    private lateinit var btnDatePrev: ImageButton
    private lateinit var btnDateNext: ImageButton
    private lateinit var tvDateHeader: TextView

    // Section Navigation (Sides of Card)
    lateinit var loadingLayout : View

    lateinit var loadingProgress : View

    lateinit var loadingText : TextView
    private lateinit var btnSectionPrev: ImageButton
    private lateinit var btnSectionNext: ImageButton

    var allDlls: List<CompleteDllEntry> = emptyList()
    var currentDllIndex: Int = 0 // Index for the 'Date Navigation' (Outer Level)
    private val courseId: Int = CurrentCourse.courseId // Assuming this is set globally

    private val dateTimeFormat =
        SimpleDateFormat("yyyy-MM-dd", Locale.US) // Input format (correct)

    private val simpleDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dll_view)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // --- 1. DATE NAVIGATION ---
        tvDateHeader = findViewById<TextView>(R.id.tvDateHeader)

        btnDatePrev = findViewById(R.id.btnDatePrev)
        btnDateNext = findViewById(R.id.btnDateNext)
        btnSectionPrev = findViewById(R.id.btnSectionPrev)
        btnSectionNext = findViewById(R.id.btnSectionNext)
        viewPager = findViewById(R.id.viewPager)

        setNavigationVisibility(View.INVISIBLE)


        btnDatePrev.setOnClickListener { navigateDll(-1) }
        btnDateNext.setOnClickListener { navigateDll(1) }

        btnSectionPrev.setOnClickListener { viewPager.currentItem -= 1 }
        btnSectionNext.setOnClickListener { viewPager.currentItem += 1 }
        loadingLayout =  findViewById<View>(R.id.loadingLayout)
        loadingProgress = findViewById<View>(R.id.loadingProgressBg)
        loadingText = findViewById<TextView>(R.id.loadingText)


        fetchAllDlls()

        // Disable user swiping with finger if you ONLY want arrows to work
        // viewPager.isUserInputEnabled = false


        // Hide Section Arrows on first/last pages
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                btnSectionPrev.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
                viewPager.adapter?.let { adapter ->
                    btnSectionNext.visibility = if (position == adapter.itemCount - 1) View.INVISIBLE else View.VISIBLE
                }
            }
        })

        // --- 3. BOTTOM NAV ---
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_dll
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_class -> {
                    startActivity(Intent(this, TeacherClassPageActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish()
                    true
                }
                R.id.nav_participants -> {
                    startActivity(Intent(this, TeacherParticipantsActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish()
                    true
                }
                R.id.nav_notifications -> {
                    startActivity(Intent(this, TeacherNotificationsActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish()
                    true
                }
                R.id.nav_dll -> {
                    startActivity(Intent(this, DailyLessonLogActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun setNavigationVisibility(visibility: Int) {
        btnDatePrev.visibility = visibility
        btnDateNext.visibility = visibility
        btnSectionPrev.visibility = visibility
        btnSectionNext.visibility = visibility
    }

    fun fetchAllDlls() {
        if (courseId == -1) {
            tvDateHeader.text = "Error: Course ID Missing"
            return
        }
        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
        ConnectURL.api.fetchAllDllsByCourse(courseId).enqueue(object : Callback<DllListResponse> {
            override fun onResponse(call: Call<DllListResponse>, response: Response<DllListResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    allDlls = response.body()?.dll ?: emptyList()
                    if (allDlls.isNotEmpty()) {

                        allDlls = allDlls.sortedBy { parseDate(it.main.available_from) }

                        // 2. Set the starting index to the SECOND-TO-LAST DLL (the 'Previous' one)
                        currentDllIndex = if (allDlls.size >= 2) {
                            allDlls.size - 2 // Index of the second-to-last item (the previous one)
                        } else {
                            0 // Default to the first (and only) item if there's only one DLL
                        }
                        displayCurrentDll()
                        setNavigationVisibility(View.VISIBLE)
                    }else {
                        tvDateHeader.text = "No DLLs Available"
                    }
                } else {
                    tvDateHeader.text = "Error Loading Data"
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    Toast.makeText(this@DLLViewActivity, "Failed to load DLLs (Code: ${response.code()})", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<DllListResponse>, t: Throwable) {
                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                tvDateHeader.text = "Network Error"
                Toast.makeText(this@DLLViewActivity, "Network error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun parseDate(dateString: String): Long {
        return try { dateTimeFormat.parse(dateString)?.time ?: 0 } catch (e: Exception) { 0 }
    }


    // 🌟 OUTER NAVIGATION LOGIC (Date/DLL)
    private fun navigateDll(direction: Int) {
        val newIndex = currentDllIndex + direction
        if (newIndex >= 0 && newIndex < allDlls.size) {
            currentDllIndex = newIndex
            displayCurrentDll()
        }
    }

    private fun displayCurrentDll() {
        if (allDlls.isEmpty() || currentDllIndex < 0 || currentDllIndex >= allDlls.size) return

        val currentDll = allDlls[currentDllIndex]

        // 1. Update Date Header (range display)
        val fromDate = formatDisplayDate(currentDll.main.available_from)
        val untilDate = formatDisplayDate(currentDll.main.available_until)
        tvDateHeader.text = "${fromDate} - ${untilDate}"

        // 2. Setup ViewPager with the currently selected DLL's data
        viewPager.adapter = DLLViewPagerAdapter(this, currentDll)

        // 3. Update Date Nav Arrows (Outer Level)
        btnDatePrev.visibility = if (currentDllIndex == 0) View.INVISIBLE else View.VISIBLE
        btnDateNext.visibility = if (currentDllIndex == allDlls.size - 1) View.INVISIBLE else View.VISIBLE
        // 4. Update Section Nav Arrows (Inner Level)
        // Reset section nav visibility after adapter update
        viewPager.currentItem = 0
        viewPager.adapter?.let { adapter ->
            btnSectionPrev.visibility = View.INVISIBLE
            btnSectionNext.visibility = if (adapter.itemCount > 1) View.VISIBLE else View.INVISIBLE
        }

        // Ensure section arrows are constrained to the current DLL
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                btnSectionPrev.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
                viewPager.adapter?.let { adapter ->
                    btnSectionNext.visibility = if (position == adapter.itemCount - 1) View.INVISIBLE else View.VISIBLE
                }
            }
        })
    }

    private fun formatDisplayDate(dateTimeString: String): String {
        return try {
            val date = dateTimeFormat.parse(dateTimeString)
            simpleDateFormat.format(date!!)
        } catch (e: Exception) {
            dateTimeString // Fallback
        }
    }


    // Adapter Class
    private inner class DLLViewPagerAdapter(
        activity: AppCompatActivity,
        private val dllData: CompleteDllEntry // Only the current DLL entry
    ) : FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment {
            // Pass the specific data slice corresponding to the current DLL index
            return when (position) {
                // Objectives uses dll_main data
                0 -> DLLObjectivesFragment.newInstance(dllData.main)

                // Resources uses dll_main (for Step 2 Content) and dll_reference list
                1 -> DLLResourcesFragment.newInstance(dllData.main, dllData.references)

                // Procedures uses dll_procedure list
                2 -> DLLProceduresFragment.newInstance(dllData.main, dllData.procedures)

                // Reflection uses dll_reflection list
                3 -> DLLReflectionFragment.newInstance(dllData.reflections)
                else -> DLLObjectivesFragment.newInstance(dllData.main)
            }
        }
    }




}