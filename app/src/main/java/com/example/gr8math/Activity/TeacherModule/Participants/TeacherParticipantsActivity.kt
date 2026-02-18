package com.example.gr8math.Activity.TeacherModule.Participants

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gr8math.Activity.TeacherModule.DLL.DLLViewActivity
import com.example.gr8math.Activity.TeacherModule.StudentScoresActivity
import com.example.gr8math.Activity.TeacherModule.ClassManager.TeacherClassPageActivity
import com.example.gr8math.Activity.TeacherModule.DLL.DLLViewActivityMain
import com.example.gr8math.Activity.TeacherModule.Notification.TeacherNotificationsActivity
import com.example.gr8math.Data.Model.Participant
import com.example.gr8math.Model.CurrentCourse
import com.example.gr8math.R
import com.example.gr8math.Utils.NotificationHelper
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.ViewModel.ParticipantsState
import com.example.gr8math.ViewModel.ParticipantsViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView

class TeacherParticipantsActivity : AppCompatActivity() {

    private val viewModel: ParticipantsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_participants)

        setupToolbar()
        setupBottomNav()
        setupObservers()

        viewModel.loadParticipants(CurrentCourse.courseId)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupObservers() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is ParticipantsState.Loading -> {
                    // Optional: Show loading
                }
                is ParticipantsState.Success -> {
                    val students = state.data
                    if (students.isEmpty()) {
                        ShowToast.showMessage(this, "No students found.")
                    } else {
                        setupPodium(students)
                        setupRecyclerView(students)
                    }
                }
                is ParticipantsState.Error -> {
                    ShowToast.showMessage(this, state.message)
                }
            }
        }
    }

    private fun setupPodium(participants: List<Participant>) {
        // Rank 1
        if (participants.isNotEmpty()) {
            val p1 = participants[0]
            findViewById<TextView>(R.id.tvRank1Num).text = "1st"
            findViewById<TextView>(R.id.tvRank1Name).text = p1.name
            findViewById<MaterialCardView>(R.id.cardRank1).setOnClickListener {
                openScoresPage(p1.id, p1.name)
            }
        } else {
            findViewById<TextView>(R.id.tvRank1Name).text = "-"
        }

        // Rank 2
        if (participants.size > 1) {
            val p2 = participants[1]
            findViewById<TextView>(R.id.tvRank2Num).text = "2nd"
            findViewById<TextView>(R.id.tvRank2Name).text = p2.name
            findViewById<MaterialCardView>(R.id.cardRank2).setOnClickListener {
                openScoresPage(p2.id, p2.name)
            }
        }

        // Rank 3
        if (participants.size > 2) {
            val p3 = participants[2]
            findViewById<TextView>(R.id.tvRank3Num).text = "3rd"
            findViewById<TextView>(R.id.tvRank3Name).text = p3.name
            findViewById<MaterialCardView>(R.id.cardRank3).setOnClickListener {
                openScoresPage(p3.id, p3.name)
            }
        }
    }

    private fun setupRecyclerView(students: List<Participant>) {
        val rv = findViewById<RecyclerView>(R.id.rvParticipants)

        val remainingStudents = if (students.size > 3) students.drop(3) else emptyList()

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = ParticipantsAdapter(remainingStudents) { student ->
            openScoresPage(student.id, student.name)
        }
    }

    private fun openScoresPage(id: Int, name: String) {
        val intent = Intent(this, StudentScoresActivity::class.java)
        intent.putExtra("EXTRA_STUDENT_ID", id)
        intent.putExtra("EXTRA_STUDENT_NAME", name)
        startActivity(intent)
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_participants

        // This should now be resolved because of the import
        NotificationHelper.fetchUnreadCount(bottomNav)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_class -> {
                    startActivity(
                        Intent(this, TeacherClassPageActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish()
                    true
                }
                R.id.nav_participants -> true
                R.id.nav_notifications -> {
                    startActivity(
                        Intent(this, TeacherNotificationsActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish()
                    true
                }
                R.id.nav_dll -> {
                    startActivity(
                        Intent(this, DLLViewActivityMain::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}