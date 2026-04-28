package com.example.gr8math.Activity.TeacherModule.Participants

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
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

    override fun onResume() {
        super.onResume()
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener(null)
        bottomNav.selectedItemId = R.id.nav_participants
        NotificationHelper.fetchUnreadCount(bottomNav)
        setupBottomNav()
    }

    private fun setupObservers() {
        viewModel.state.observe(this) { state ->

            val scrollView = findViewById<View>(R.id.scrollView)
            val emptyLayout = findViewById<View>(R.id.emptyStateLayout)

            when (state) {
                is ParticipantsState.Loading -> {

                }
                is ParticipantsState.Success -> {
                    val students = state.data
                    if (students.isEmpty()) {
                        scrollView.visibility = View.GONE
                        emptyLayout.visibility = View.VISIBLE
                    } else {
                        scrollView.visibility = View.VISIBLE
                        emptyLayout.visibility = View.GONE
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
        val ivTrophy1 = findViewById<ImageView>(R.id.ivTrophy1)
        val ivTrophy2 = findViewById<ImageView>(R.id.ivTrophy2)
        val ivTrophy3 = findViewById<ImageView>(R.id.ivTrophy3)
        val cardRank1 = findViewById<MaterialCardView>(R.id.cardRank1)
        val cardRank2 = findViewById<MaterialCardView>(R.id.cardRank2)
        val cardRank3 = findViewById<MaterialCardView>(R.id.cardRank3)

        // Reset visibility completely (hide everything by default)
        ivTrophy1.visibility = View.INVISIBLE
        ivTrophy2.visibility = View.INVISIBLE
        ivTrophy3.visibility = View.INVISIBLE
        cardRank1.visibility = View.INVISIBLE
        cardRank2.visibility = View.INVISIBLE
        cardRank3.visibility = View.INVISIBLE

        // Rank 1 (Top Center)
        if (participants.isNotEmpty()) {
            val p1 = participants[0]
            ivTrophy1.visibility = View.VISIBLE
            cardRank1.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvRank1Num).text = "1st"
            findViewById<TextView>(R.id.tvRank1Name).text = p1.name
            cardRank1.setOnClickListener {
                openScoresPage(p1.id, p1.name)
            }
        }

        // Rank 2 (Left)
        if (participants.size > 1) {
            val p2 = participants[1]
            ivTrophy2.visibility = View.VISIBLE
            cardRank2.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvRank2Num).text = "2nd"
            findViewById<TextView>(R.id.tvRank2Name).text = p2.name
            cardRank2.setOnClickListener {
                openScoresPage(p2.id, p2.name)
            }
        }

        // Rank 3 (Right)
        if (participants.size > 2) {
            val p3 = participants[2]
            ivTrophy3.visibility = View.VISIBLE
            cardRank3.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvRank3Num).text = "3rd"
            findViewById<TextView>(R.id.tvRank3Name).text = p3.name
            cardRank3.setOnClickListener {
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
        NotificationHelper.fetchUnreadCount(bottomNav)

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