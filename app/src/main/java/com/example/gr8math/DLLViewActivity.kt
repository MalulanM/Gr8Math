package com.example.gr8math

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class DLLViewActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2

    // Date Navigation (Top)
    private lateinit var btnDatePrev: ImageButton
    private lateinit var btnDateNext: ImageButton

    // Section Navigation (Sides of Card)
    private lateinit var btnSectionPrev: ImageButton
    private lateinit var btnSectionNext: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dll_view)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // --- 1. DATE NAVIGATION ---
        val tvDateHeader = findViewById<TextView>(R.id.tvDateHeader)
        tvDateHeader.text = "January 1, 2026"

        btnDatePrev = findViewById(R.id.btnDatePrev)
        btnDateNext = findViewById(R.id.btnDateNext)

        btnDatePrev.setOnClickListener {
            // Frontend Logic: In real app, load previous day's data
            Toast.makeText(this, "Previous Date", Toast.LENGTH_SHORT).show()
        }

        btnDateNext.setOnClickListener {
            // Frontend Logic: In real app, load next day's data
            Toast.makeText(this, "Next Date", Toast.LENGTH_SHORT).show()
        }


        // --- 2. SECTION/CARD NAVIGATION ---
        viewPager = findViewById(R.id.viewPager)
        val adapter = DLLViewPagerAdapter(this)
        viewPager.adapter = adapter

        // Disable user swiping with finger if you ONLY want arrows to work
        // viewPager.isUserInputEnabled = false

        btnSectionPrev = findViewById(R.id.btnSectionPrev)
        btnSectionNext = findViewById(R.id.btnSectionNext)

        btnSectionPrev.setOnClickListener {
            if (viewPager.currentItem > 0) {
                viewPager.currentItem = viewPager.currentItem - 1
            }
        }

        btnSectionNext.setOnClickListener {
            if (viewPager.currentItem < adapter.itemCount - 1) {
                viewPager.currentItem = viewPager.currentItem + 1
            }
        }

        // Hide Section Arrows on first/last pages
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                btnSectionPrev.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
                btnSectionNext.visibility = if (position == adapter.itemCount - 1) View.INVISIBLE else View.VISIBLE
            }
        })

        // --- 3. BOTTOM NAV ---
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_dll
    }

    // Adapter Class
    private inner class DLLViewPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 4 // Objectives, Resources, Procedures, Reflection

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> DLLObjectivesFragment()
                1 -> DLLResourcesFragment()
                2 -> DLLProceduresFragment()
                3 -> DLLReflectionFragment()
                else -> DLLObjectivesFragment()
            }
        }
    }
}