package com.example.gr8math.Activity

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.gr8math.Activity.StudentModule.Assessment.AssessmentResultActivity
import com.example.gr8math.Model.CurrentCourse
import com.example.gr8math.R
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.Utils.UIUtils
import com.example.gr8math.ViewModel.TakeAssessmentState
import com.example.gr8math.ViewModel.TakeAssessmentViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.concurrent.TimeUnit
import android.text.Editable
import android.text.TextWatcher
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.view.Gravity
import android.text.InputType
import android.net.Uri
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.gr8math.Services.TigrisService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import com.bumptech.glide.Glide
import java.util.UUID
import android.widget.CompoundButton
import androidx.core.content.res.ResourcesCompat
import java.net.URLEncoder // Added for proper LaTeX encoding

class TakeAssessmentActivity : AppCompatActivity() {

    private val viewModel: TakeAssessmentViewModel by viewModels()

    // Views
    private lateinit var tvTimer: TextView
    private lateinit var questionHeaderContainer: LinearLayout
    private lateinit var dynamicAnswerContainer: LinearLayout
    private lateinit var btnPrevious: Button
    private lateinit var btnNext: Button
    private lateinit var loadingLayout: View
    private lateinit var loadingProgress: View
    private lateinit var loadingText: TextView
    private lateinit var toolbar: MaterialToolbar

    private var countDownTimer: CountDownTimer? = null
    private var currentUploadQuestionId: Int? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { imageUri ->
            val qId = currentUploadQuestionId ?: return@let
            uploadImageToTigrisDirectly(imageUri, qId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_take_assessment)

        initViews()

        val json = intent.getStringExtra("assessment_data")
        if (json != null) {
            viewModel.parseAssessmentData(json)
        } else {
            ShowToast.showMessage(this, "Error loading assessment")
            finish()
            return
        }

        setupObservers()
        startTimer(60 * 60 * 1000)
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tvTimer = findViewById(R.id.tvTimer)
        questionHeaderContainer = findViewById(R.id.questionHeaderContainer)
        dynamicAnswerContainer = findViewById(R.id.dynamicAnswerContainer)
        btnPrevious = findViewById(R.id.btnPrevious)
        btnNext = findViewById(R.id.btnNext)
        loadingLayout = findViewById(R.id.loadingLayout)
        loadingProgress = findViewById(R.id.loadingProgressBg)
        loadingText = findViewById(R.id.loadingText)

        toolbar.setNavigationOnClickListener { finish() }

        btnNext.setOnClickListener {
            val total = viewModel.assessment?.questions?.size ?: 0
            val current = viewModel.currentIndex.value ?: 0
            if (current < total - 1) viewModel.nextQuestion() else showReviewDialog()
        }

        btnPrevious.setOnClickListener { viewModel.prevQuestion() }
    }

    private fun setupObservers() {
        viewModel.currentIndex.observe(this) { index ->
            val data = viewModel.assessment ?: return@observe
            if (index < data.questions.size) loadQuestionUI(index)
        }

        viewModel.state.observe(this) { state ->
            when (state) {
                is TakeAssessmentState.Loading -> UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
                is TakeAssessmentState.Submitted -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    countDownTimer?.cancel()
                    val intent = Intent(this, AssessmentResultActivity::class.java)
                    intent.putExtra("assessment_id", viewModel.assessment?.id)
                    intent.putExtra("student_id", CurrentCourse.userId)
                    intent.putExtra("is_newly_completed", true)
                    startActivity(intent)
                    finish()
                }
                is TakeAssessmentState.Error -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    ShowToast.showMessage(this, state.message)
                }
                else -> {}
            }
        }
    }

    private fun loadQuestionUI(index: Int) {
        val question = viewModel.assessment!!.questions[index]
        dynamicAnswerContainer.removeAllViews()
        questionHeaderContainer.removeAllViews()

        var rawText = question.questionText
        var questionImageUrl: String? = null

        if (rawText.contains(" ||| ")) {
            val parts = rawText.split(" ||| ")
            rawText = parts[0]
            questionImageUrl = parts[1]
        }

        val match = Regex("^\\[(.*?)\\]\\s*(.*)$").find(rawText)
        val qType = match?.groupValues?.get(1) ?: "Multiple Choice"
        var actualQuestion = match?.groupValues?.get(2) ?: rawText

        actualQuestion = actualQuestion.replace(Regex("\\[\\d+(\\.\\d+)?\\s*pts\\]\\s*", RegexOption.IGNORE_CASE), "")

        renderMathInContainer(questionHeaderContainer, "${index + 1}. $actualQuestion", 18f)

        val savedAnswer = viewModel.getSelectedAnswer(question.id)

        if (!questionImageUrl.isNullOrEmpty()) {
            val qImageView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 500).apply {
                    setMargins(0, 16, 0, 32)
                }
                scaleType = ImageView.ScaleType.CENTER_INSIDE
            }
            dynamicAnswerContainer.addView(qImageView)
            Glide.with(this).load(questionImageUrl).into(qImageView)
        }

        when (qType) {
            "Multiple Choice", "Checkboxes", "Dropdown" -> {
                question.choices.forEach { choice ->
                    val cleanChoiceText = choice.choiceText.replace(Regex("^\\[\\d+(\\.\\d+)?\\s*pts\\]\\s*", RegexOption.IGNORE_CASE), "")

                    val choiceRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, 8, 0, 8)
                        isClickable = true
                        // FIX: Use method call to avoid API 26 ambiguity
                        setFocusable(true)
                        setBackgroundResource(android.R.drawable.list_selector_background)
                    }

                    val selectorView = (if (qType == "Checkboxes") CheckBox(this) else RadioButton(this)) as CompoundButton
                    selectorView.apply {
                        val isSelected = if (qType == "Checkboxes") {
                            (savedAnswer as? Set<Int>)?.contains(choice.id) == true
                        } else {
                            (savedAnswer as? Int) == choice.id
                        }
                        this.isChecked = isSelected
                        this.isClickable = false
                    }

                    val choiceTextContainer = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }

                    renderMathInContainer(choiceTextContainer, cleanChoiceText, 16f)

                    choiceRow.addView(selectorView)
                    choiceRow.addView(choiceTextContainer)

                    choiceRow.setOnClickListener {
                        if (qType == "Checkboxes") {
                            val current = (viewModel.getSelectedAnswer(question.id) as? MutableSet<Int>) ?: mutableSetOf()
                            if (current.contains(choice.id)) current.remove(choice.id) else current.add(choice.id)
                            viewModel.selectAnswer(question.id, current)
                            selectorView.isChecked = current.contains(choice.id)
                        } else {
                            viewModel.selectAnswer(question.id, choice.id)
                            loadQuestionUI(index)
                        }
                    }
                    dynamicAnswerContainer.addView(choiceRow)
                }
            }

            "Short Answer", "Paragraph" -> {
                val et = EditText(this).apply {
                    hint = "Type your answer here..."
                    setText(savedAnswer as? String ?: "")
                    textSize = 16f
                    setTextColor(resources.getColor(R.color.colorText))
                    setBackgroundResource(android.R.drawable.edit_text)
                    if (qType == "Paragraph") {
                        minLines = 4
                        gravity = Gravity.TOP
                        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    }
                    addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                            viewModel.selectAnswer(question.id, s.toString())
                        }
                        override fun afterTextChanged(s: Editable?) {}
                    })
                }
                dynamicAnswerContainer.addView(et)
            }

            "Upload Image" -> {
                val savedUrl = savedAnswer as? String
                val btn = Button(this).apply {
                    text = if (savedUrl.isNullOrEmpty()) "Select Image to Upload" else "Change Image"
                    setOnClickListener {
                        currentUploadQuestionId = question.id
                        pickImageLauncher.launch("image/*")
                    }
                }
                dynamicAnswerContainer.addView(btn)

                if (!savedUrl.isNullOrEmpty()) {
                    val imageView = ImageView(this).apply {
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 500).apply {
                            setMargins(0, 24, 0, 0)
                        }
                        scaleType = ImageView.ScaleType.CENTER_INSIDE
                    }
                    dynamicAnswerContainer.addView(imageView)
                    Glide.with(this).load(savedUrl).into(imageView)
                }
            }
        }

        val total = viewModel.assessment!!.questions.size
        btnPrevious.visibility = if (index > 0) View.VISIBLE else View.GONE
        btnNext.text = if (index == total - 1) "Submit" else "Next"
    }

    private fun renderMathInContainer(container: LinearLayout, text: String, fontSizeSp: Float) {
        val parts = text.split("(?<=\\$)|(?=\\$)".toRegex())
        var isInsideMath = false

        parts.forEach { part ->
            if (part == "$") {
                isInsideMath = !isInsideMath
                return@forEach
            }

            if (isInsideMath) {
                val mathImg = ImageView(this).apply {
                    adjustViewBounds = true
                    // Scaled height for better formula visibility
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        (fontSizeSp * 3).toInt()
                    ).apply {
                        gravity = Gravity.CENTER_VERTICAL
                    }
                    setPadding(8, 0, 8, 0)
                }
                container.addView(mathImg)

                // CLEAN AND ENCODE LATEX
                // 1. Remove MathLive specific placeholder tags that break standard renderers
                val cleanedPart = part.replace("\\placeholder{}", "□").trim()
                // 2. Proper URL Encoding for LaTeX symbols
                val encoded = URLEncoder.encode(cleanedPart, "UTF-8")

                // Using a more robust LaTeX API
                val mathUrl = "https://latex.codecogs.com/png.image?\\dpi{130}\\bg_white $encoded"

                Glide.with(this)
                    .load(mathUrl)
                    .into(mathImg)
            } else {
                if (part.isNotBlank()) {
                    val tv = TextView(this).apply {
                        this.text = part
                        this.textSize = fontSizeSp
                        this.setTextColor(resources.getColor(R.color.colorText))
                        // FIX: Use typeface and ResourcesCompat
                        this.typeface = ResourcesCompat.getFont(this@TakeAssessmentActivity, R.font.lexend)
                    }
                    container.addView(tv)
                }
            }
        }
    }

    private fun showReviewDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_review_title)
            .setMessage(R.string.dialog_review_message)
            .setNegativeButton("Review") { _, _ -> viewModel.jumpToFirst() }
            .setPositiveButton("Submit") { _, _ -> viewModel.submitAssessment() }
            .show()
    }

    private fun startTimer(durationMillis: Long) {
        countDownTimer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60
                val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                tvTimer.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            }
            override fun onFinish() {
                tvTimer.text = "00:00:00"
                ShowToast.showMessage(this@TakeAssessmentActivity, "Time is up! Submitting...")
                viewModel.submitAssessment()
            }
        }.start()
    }

    private fun uploadImageToTigrisDirectly(uri: Uri, questionId: Int) {
        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri) ?: throw Exception("Failed to open image.")
                val bytes = inputStream.readBytes()
                inputStream.close()
                val courseId = CurrentCourse.courseId
                val fileName = "course_${courseId}/${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
                val request = PutObjectRequest {
                    bucket = TigrisService.BUCKET_NAME
                    key = fileName
                    body = ByteStream.fromBytes(bytes)
                    contentType = "image/jpeg"
                }
                TigrisService.s3Client.putObject(request)
                val publicUrl = "https://${TigrisService.BUCKET_NAME}.fly.storage.tigris.dev/$fileName"
                withContext(Dispatchers.Main) {
                    viewModel.selectAnswer(questionId, publicUrl)
                    loadQuestionUI(viewModel.currentIndex.value ?: 0)
                    ShowToast.showMessage(this@TakeAssessmentActivity, "Image uploaded successfully!")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { ShowToast.showMessage(this@TakeAssessmentActivity, "Upload failed: ${e.message}") }
            } finally {
                withContext(Dispatchers.Main) { UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false) }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}