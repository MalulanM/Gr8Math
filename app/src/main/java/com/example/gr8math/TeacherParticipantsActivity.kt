package com.example.gr8math

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.CurrentCourse
import com.example.gr8math.utils.NotificationHelper
import com.example.gr8math.utils.ShowToast
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call

class TeacherParticipantsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_participants)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_participants

        NotificationHelper.fetchUnreadCount(bottomNav)

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
                    startActivity(Intent(this, DLLViewActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish()
                    true
                }
                else -> false
            }
        }




        displayAllStudents()
    }

    private fun displayAllStudents() {

        val call = ConnectURL.api.getStudents(CurrentCourse.courseId)

        call.enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: retrofit2.Response<ResponseBody>
            ) {
                val body = response.body()?.string()
                if (body == null) {
                    ShowToast.showMessage(this@TeacherParticipantsActivity, "Empty server response.")
                    return
                }

                try {
                    val json = JSONObject(body)
                    if (!json.getBoolean("success")) {
                        ShowToast.showMessage(this@TeacherParticipantsActivity, "No students found.")
                        return
                    }

                    val array = json.getJSONArray("students")
                    val students = mutableListOf<Participant>()

                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)

                        val fullName =
                            item.getString("last_name") + ", " + item.getString("first_name")

                        students.add(
                            Participant(
                                id = item.getInt("student_id"),
                                name = fullName,
                                rank = i + 1
                            )
                        )
                    }

                    // TOP 3
                    setupPodium(students)


                    val rv = findViewById<RecyclerView>(R.id.rvParticipants)
                    rv.layoutManager = LinearLayoutManager(this@TeacherParticipantsActivity)
                    rv.adapter = ParticipantsAdapter(students.drop(3)) { student ->
                        openScoresPage(student.id, student.name)
                    }

                } catch (e: Exception) {
                    Log.e("displayStudents", e.message ?: "Error")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("RetrofitError", "onFailure: ${t.localizedMessage}", t)
                ShowToast.showMessage(this@TeacherParticipantsActivity, "Failed to connect to server. Check your internet connection.")
            }
        })
    }

    private fun setupPodium(participants: List<Participant>) {

        if (participants.isNotEmpty()) {
            val p1 = participants[0]
            findViewById<TextView>(R.id.tvRank1Num).text = "1st"
            findViewById<TextView>(R.id.tvRank1Name).text = p1.name
            findViewById<MaterialCardView>(R.id.cardRank1).setOnClickListener {
                openScoresPage(p1.id, p1.name)
            }
        }

        if (participants.size > 1) {
            val p2 = participants[1]
            findViewById<TextView>(R.id.tvRank2Num).text = "2nd"
            findViewById<TextView>(R.id.tvRank2Name).text = p2.name
            findViewById<MaterialCardView>(R.id.cardRank2).setOnClickListener {
                openScoresPage(p2.id, p2.name)
            }
        }

        if (participants.size > 2) {
            val p3 = participants[2]
            findViewById<TextView>(R.id.tvRank3Num).text = "3rd"
            findViewById<TextView>(R.id.tvRank3Name).text = p3.name
            findViewById<MaterialCardView>(R.id.cardRank3).setOnClickListener {
                openScoresPage(p3.id, p3.name)
            }
        }
    }

    private fun openScoresPage(id: Int, name: String) {
        val intent = Intent(this, StudentScoresActivity::class.java)
        intent.putExtra("EXTRA_STUDENT_ID", id)
        intent.putExtra("EXTRA_STUDENT_NAME", name)
        startActivity(intent)
    }
}
