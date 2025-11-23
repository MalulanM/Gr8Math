package com.example.gr8math

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView

class TeacherParticipantsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_participants)

        // --- Toolbar ---
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // --- Bottom Navigation ---
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_participants

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_class -> {
                    finish()
                    true
                }
                R.id.nav_participants -> true
                R.id.nav_notifications -> {
                    startActivity(Intent(this, TeacherNotificationsActivity::class.java))
                    false
                }
                else -> false
            }
        }

        // --- Dummy Data ---
        val allParticipants = listOf(
            Participant(1, "Dela Cruz, Juan", 1),
            Participant(2, "Santos, Maria", 2),
            Participant(3, "Reyes, Jose", 3),
            Participant(4, "Garcia, Ana", 4),
            Participant(5, "Lim, Kevin", 5),
            Participant(6, "Tan, David", 6),
            Participant(7, "Yap, Sarah", 7)
        )

        // --- Setup Podium (Top 3) ---
        setupPodium(allParticipants)

        // --- Setup RecyclerView (4th onwards) ---
        val remainingParticipants = allParticipants.drop(3)
        val rvParticipants = findViewById<RecyclerView>(R.id.rvParticipants)
        rvParticipants.layoutManager = LinearLayoutManager(this)
        rvParticipants.adapter = ParticipantsAdapter(remainingParticipants) { student ->
            openScoresPage(student.name)
        }
    }

    private fun setupPodium(participants: List<Participant>) {
        // 1st Place
        if (participants.isNotEmpty()) {
            val p1 = participants[0]
            findViewById<TextView>(R.id.tvRank1Num).text = "1st" // Separate Rank
            findViewById<TextView>(R.id.tvRank1Name).text = p1.name // Separate Name
            findViewById<MaterialCardView>(R.id.cardRank1).setOnClickListener { openScoresPage(p1.name) }
        }

        // 2nd Place
        if (participants.size > 1) {
            val p2 = participants[1]
            findViewById<TextView>(R.id.tvRank2Num).text = "2nd"
            findViewById<TextView>(R.id.tvRank2Name).text = p2.name
            findViewById<MaterialCardView>(R.id.cardRank2).setOnClickListener { openScoresPage(p2.name) }
        }

        // 3rd Place
        if (participants.size > 2) {
            val p3 = participants[2]
            findViewById<TextView>(R.id.tvRank3Num).text = "3rd"
            findViewById<TextView>(R.id.tvRank3Name).text = p3.name
            findViewById<MaterialCardView>(R.id.cardRank3).setOnClickListener { openScoresPage(p3.name) }
        }
    }

    private fun openScoresPage(studentName: String) {
        val intent = Intent(this, StudentScoresActivity::class.java)
        intent.putExtra("EXTRA_STUDENT_NAME", studentName)
        startActivity(intent)
    }
}