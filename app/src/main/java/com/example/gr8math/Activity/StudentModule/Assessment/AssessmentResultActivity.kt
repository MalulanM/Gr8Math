package com.example.gr8math.Activity.StudentModule.Assessment

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.gr8math.Activity.StudentModule.ClassManager.StudentClassPageActivity
import com.example.gr8math.R
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.Utils.UIUtils
import com.example.gr8math.ViewModel.AssessmentResultViewModel
import com.example.gr8math.ViewModel.ResultState
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.xml.KonfettiView
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class AssessmentResultActivity : AppCompatActivity() {

    private val viewModel: AssessmentResultViewModel by viewModels()

    private lateinit var tvAssessmentNumber: TextView
    private lateinit var tvAssessmentTitle: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvNumberOfItems: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvCompletionMessage: TextView

    lateinit var loadingLayout: View
    lateinit var loadingProgress: View
    lateinit var loadingText: TextView

    private lateinit var konfettiView: KonfettiView
    private var mediaPlayer: MediaPlayer? = null

    private var isNewlyCompleted: Boolean = false
    private var studentId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assessment_result)

        initViews()
        setupObservers()

        val assessmentId = intent.getIntExtra("assessment_id", 0)

        isNewlyCompleted = intent.getBooleanExtra("is_newly_completed", false)

        studentId = intent.getIntExtra("student_id", 0)

        Log.e("ASSESwderf", assessmentId.toString())
        if (assessmentId != 0) {
            viewModel.loadResult(assessmentId)
        } else {
            ShowToast.showMessage(this, "Invalid Assessment ID")
            navigateToClassPage()
        }
    }

    private fun initViews() {
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            navigateToClassPage()
        }

        tvAssessmentNumber = findViewById(R.id.tvAssessmentNumber)
        tvAssessmentTitle = findViewById(R.id.tvAssessmentTitle)
        tvScore = findViewById(R.id.tvScore)
        tvNumberOfItems = findViewById(R.id.tvNumberOfItems)
        tvDate = findViewById(R.id.tvDate)
        tvCompletionMessage = findViewById(R.id.tvCompletionMessage)

        loadingLayout = findViewById(R.id.loadingLayout)
        loadingProgress = findViewById(R.id.loadingProgressBg)
        loadingText = findViewById(R.id.loadingText)
        konfettiView = findViewById(R.id.konfettiView)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        navigateToClassPage()
        super.onBackPressed()
    }

    private fun setupObservers() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is ResultState.Loading -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
                }
                is ResultState.Success -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    val data = state.data

                    tvAssessmentNumber.text = "Assessment ${data.assessmentNumber}"
                    tvAssessmentTitle.text = data.title
                    tvCompletionMessage.text = "Assessment Test completed!"
                    tvNumberOfItems.text = "Number of items: ${data.assessmentItems}"

                    val df = DecimalFormat("#.##")
                    tvScore.text = "Score: ${df.format(data.score)}"

                    tvDate.text = "Date Accomplished: ${formatDate(data.dateAccomplished)}"

                    if (isNewlyCompleted) {
                        viewModel.checkAndAwardBadges(studentId, data.score, data.assessmentItems)
                    }
                }
                is ResultState.Error -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    ShowToast.showMessage(this, state.message)
                }
            }
        }
        viewModel.newBadges.observe(this) { badges ->
            for (badgeName in badges) {
                showBadgeDialog(badgeName)
            }
        }
    }

    private fun showBadgeDialog(badgeName: String) {
        val imageResource = when (badgeName) {
            "First-Timer" -> R.drawable.badge_firsttimer
            "First Ace!" -> R.drawable.badge_firstace
            "Three-Quarter Score!" -> R.drawable.badge_threequarter
            "Triple Ace" -> R.drawable.badge_tripleace
            else -> R.drawable.badge_firsttimer
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_badge_acquired, null)

        val ivBadge = dialogView.findViewById<ImageView>(R.id.ivDialogBadge)
        val tvBadgeTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnClose)

        ivBadge.setImageResource(imageResource)
        tvBadgeTitle.text = "$badgeName Badge!"

        mediaPlayer = MediaPlayer.create(this, R.raw.game_win)
        mediaPlayer?.start()

        val party = Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def, 0x1E4B95),
            position = Position.Relative(0.5, 0.3),
            emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100)
        )
        konfettiView.start(party)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setOnDismissListener {
                mediaPlayer?.release()
                mediaPlayer = null
            }
            .create()


        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun formatDate(dateString: String): String {
        val formats = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd"
        )

        for (format in formats) {
            try {
                val inputFormat = SimpleDateFormat(format, Locale.US)
                if (!format.contains("XXX")) {
                    inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                }

                val date = inputFormat.parse(dateString)
                if (date != null) {
                    val outputFormat = SimpleDateFormat("MMM. dd, yyyy", Locale.US)
                    return outputFormat.format(date)
                }
            } catch (e: Exception) {
                continue
            }
        }
        return dateString
    }

    private fun navigateToClassPage() {
        val intent = Intent(this, StudentClassPageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
}