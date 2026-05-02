package com.example.gr8math.Activity.TeacherModule.Assessment

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import com.bumptech.glide.Glide
import com.example.gr8math.Data.Model.UiChoice
import com.example.gr8math.Data.Model.UiQuestion
import com.example.gr8math.Data.Repository.WordBankItem
import com.example.gr8math.Model.CurrentCourse
import com.example.gr8math.R
import com.example.gr8math.Services.TigrisService
import com.example.gr8math.Utils.NetworkUtils
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.Utils.UIUtils
import com.example.gr8math.ViewModel.AssessmentFetchState
import com.example.gr8math.ViewModel.AssessmentState
import com.example.gr8math.ViewModel.AssessmentViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable
import java.util.UUID

class AssessmentCreatorActivity : AppCompatActivity() {

    private val viewModel: AssessmentViewModel by viewModels()

    lateinit var loadingLayout: View
    lateinit var loadingProgress: View
    lateinit var loadingText: TextView
    private lateinit var questionsContainer: LinearLayout
    private lateinit var btnAddQuestion: Button
    private lateinit var btnPublishAssessmentTest: Button
    private lateinit var toolbar: MaterialToolbar


    private val questionManagers = mutableListOf<QuestionCardManager>()
    private var hasUnsavedChanges = false

    private var editAssessmentId: Int = -1
    private var assessmentNumber: Int = 0
    private var assessmentQuarter: Int = 0
    private var assessmentTitle: String = ""
    private var availableFrom: String = ""
    private var availableUntil: String = ""
    private var courseId: Int = 0

    private lateinit var timeLimitContainer: LinearLayout
    private var timeLimit: Int = 0
    private lateinit var etTimeLimit: TextInputEditText

    private lateinit var btnWordBank: Button


    private var pendingQuestionManager: QuestionCardManager? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val manager = pendingQuestionManager ?: return@let

            manager.question.pendingQuestionImageUri = it.toString()
            manager.showQuestionImage(it.toString())

            hasUnsavedChanges = true
            updatePublishButtonState()
        }
    }

    fun pickImage(manager: QuestionCardManager) {
        pendingQuestionManager = manager
        imagePickerLauncher.launch("image/*")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assessment_creator)

        initData()
        initViews()
        setupListeners()
        setupObservers()


        val btnRefresh = findViewById<Button>(R.id.btnRefresh)
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener {
                loadData()
            }
        }

        // Let the gatekeeper handle the network check and data loading
        loadData()
    }

    private fun loadData() {
        val noInternetView = findViewById<View>(R.id.no_internet_view)
        val scrollView = findViewById<View>(R.id.scrollView)

        // 1. Check for Internet
        if (!NetworkUtils.isConnected(this)) {
            // Show No Internet Screen, hide the editor form
            noInternetView?.visibility = View.VISIBLE
            scrollView?.visibility = View.GONE
            return
        }

        // 2. HAS INTERNET: Hide error screen, show editor form
        noInternetView?.visibility = View.GONE
        scrollView?.visibility = View.VISIBLE

        // 3. Fetch your actual data or setup a blank form
        if (editAssessmentId != -1) {
            viewModel.loadExistingAssessment(editAssessmentId)
        } else {
            if (questionManagers.isEmpty()) {
                addNewQuestion(null)
                hasUnsavedChanges = false
                updatePublishButtonState()
            }
        }
    }

    private fun initData() {
        editAssessmentId = intent.getIntExtra("EXTRA_EDIT_ASSESSMENT_ID", -1)
        assessmentNumber = intent.getStringExtra("EXTRA_ASSESSMENT_NUMBER")?.toIntOrNull() ?: 0
        assessmentTitle = intent.getStringExtra("EXTRA_ASSESSMENT_TITLE") ?: ""
        availableFrom = intent.getStringExtra("EXTRA_AVAILABLE_FROM") ?: ""
        availableUntil = intent.getStringExtra("EXTRA_AVAILABLE_UNTIL") ?: ""
        assessmentQuarter = intent.getStringExtra("EXTRA_AVAILABLE_QUARTER")?.toIntOrNull() ?: 0
        courseId = CurrentCourse.courseId
        timeLimit = intent.getIntExtra("EXTRA_TIME_LIMIT", 0)
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        questionsContainer = findViewById(R.id.questionsContainer)
        btnAddQuestion = findViewById(R.id.btnAddQuestion)

        btnWordBank = findViewById(R.id.btnWordBank)

        btnPublishAssessmentTest = findViewById(R.id.btnPublishAssessmentTest)
        loadingLayout = findViewById(R.id.loadingLayout)
        loadingProgress = findViewById(R.id.loadingProgressBg)
        loadingText = findViewById(R.id.loadingText)

        etTimeLimit = findViewById(R.id.etTimeLimit)
        etTimeLimit.setText(timeLimit.toString())

        val titlePrefix = if (editAssessmentId != -1) "Edit" else "Create"
        toolbar.title = assessmentTitle.ifEmpty { "$titlePrefix Assessment" }
        btnPublishAssessmentTest.text = if (editAssessmentId != -1) "Update Assessment" else "Publish Assessment"
    }


    private fun setupListeners() {
        toolbar.setNavigationOnClickListener { handleBackPress() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { handleBackPress() }
        })

        btnAddQuestion.setOnClickListener {
            addNewQuestion(null)
            hasUnsavedChanges = true
            updatePublishButtonState()
        }

        //FIX 2: Move the Word Bank listener here (out of setupObservers)
        btnWordBank.setOnClickListener {
            showWordBankDialog()
        }

        btnPublishAssessmentTest.setOnClickListener {
            if (validateAllQuestions()) {
                showSaveConfirmationDialog()
            }
        }

        etTimeLimit.addTextChangedListener(AfterTextChangedWatcher { text ->
            timeLimit = text.toIntOrNull() ?: 0
        })
    }

    private fun setupObservers() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is AssessmentState.Loading -> UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
                is AssessmentState.Success -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    ShowToast.showMessage(this, if (editAssessmentId != -1) "Assessment Test updated!" else "Assessment Test posted!")
                    setResult(RESULT_OK)
                    finish()
                }
                is AssessmentState.Error -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    MaterialAlertDialogBuilder(this)
                        .setMessage(state.message)
                        .setPositiveButton("OK", null)
                        .show()
                }
                is AssessmentState.Idle -> {}
            }
        }

        viewModel.fetchState.observe(this) { state ->
            when (state) {
                is AssessmentFetchState.Loading -> UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
                is AssessmentFetchState.Success -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    populateExistingQuestions(state.questions)
                    hasUnsavedChanges = false
                    updatePublishButtonState()
                }
                is AssessmentFetchState.Error -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    MaterialAlertDialogBuilder(this)
                        .setMessage(state.message)
                        .setPositiveButton("OK", null)
                        .show()
                    if (questionManagers.isEmpty()) addNewQuestion(null)
                }
            }
        }

        btnWordBank.setOnClickListener { showWordBankDialog() }
    }

    private fun populateExistingQuestions(existingQuestions: List<UiQuestion>) {
        questionsContainer.removeAllViews()
        questionManagers.clear()

        existingQuestions.forEach { uiQuestion ->
            val regex = Regex("^\\[(.*?)\\]\\s*(.*)$", RegexOption.DOT_MATCHES_ALL)
            val match = regex.find(uiQuestion.text)
            val type = match?.groupValues?.get(1) ?: "Multiple Choice"
            val rawContent = match?.groupValues?.get(2) ?: uiQuestion.text

            val parts = rawContent.split(Regex("\\s*\\|\\|\\|\\s*"))


            val allTextToCheck = uiQuestion.text + " " + uiQuestion.choices.joinToString(" ") { it.text }
            val ptsRegex = Regex("\\[\\s*(\\d+(\\.\\d+)?)\\s*pts?\\s*\\]", RegexOption.IGNORE_CASE)
            val ptsMatch = ptsRegex.find(allTextToCheck)
            var questionPoints = ptsMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1

            if (type == "Upload Image" && uiQuestion.choices.isNotEmpty()) {
                val rawNum = uiQuestion.choices[0].text.trim().toIntOrNull()
                if (rawNum != null) questionPoints = rawNum
            }

            // Clean tags out of the UI
            val stripPts = { s: String -> s.replace(Regex("\\[\\s*\\d+(\\.\\d+)?\\s*pts?\\s*\\]\\s*", RegexOption.IGNORE_CASE), "").trim() }
            val cleanQuestionText = stripPts(parts[0].trim())
            val imageUrl = if (parts.size > 1) parts[1].trim() else ""

            val correctIndex = uiQuestion.choices.indexOfFirst { it.isCorrect }

            // Pass perfectly clean choices to the UI
            val rawChoiceTexts = if (type == "Upload Image") mutableListOf() else uiQuestion.choices.map { stripPts(it.text) }.filter { it.isNotEmpty() }.toMutableList()
            val questionData = AssessmentQuestion(
                type = type,
                questionText = cleanQuestionText,
                imageUrl = imageUrl,
                choices = rawChoiceTexts,
                correctAnswerIndex = correctIndex,
                points = questionPoints
            )

            addNewQuestion(questionData)
        }
    }

    private suspend fun uploadUriToTigris(uriString: String): String? {
        if (!uriString.startsWith("content://")) return uriString

        return withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(uriString)
                val bytes = contentResolver.openInputStream(uri)?.readBytes() ?: return@withContext null

                val fileName = "${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"

                TigrisService.s3Client.putObject(PutObjectRequest {
                    bucket = TigrisService.BUCKET_NAME
                    key = "course_${courseId}/$fileName"
                    body = ByteStream.fromBytes(bytes)
                    contentType = "image/jpeg"
                })

                "https://app-media.fly.storage.tigris.dev/course_${courseId}/$fileName"

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun executePublishProcess() {
        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)

        lifecycleScope.launch {
            try {
                val uiQuestions = mutableListOf<UiQuestion>()
                var totalPoints = 0

                for (manager in questionManagers) {
                    val qData = manager.question
                    totalPoints += qData.points

                    if (qData.pendingQuestionImageUri.isNotEmpty() && qData.pendingQuestionImageUri.startsWith("content://")) {
                        val newUrl = uploadUriToTigris(qData.pendingQuestionImageUri)
                        if (newUrl != null) {
                            qData.imageUrl = newUrl
                            qData.pendingQuestionImageUri = ""
                        }
                    }

                    var formattedText = "[${qData.type}] ${qData.questionText.trim().replace(Regex("\\s+$"), "")}"

                    if (qData.imageUrl.isNotEmpty()) {
                        formattedText += " ||| ${qData.imageUrl}"
                    }

                    val finalChoices = qData.choices.mapIndexed { index, text ->
                        val isCorrect = if (qData.type == "Short Answer" || qData.type == "Paragraph" || qData.type == "Upload Image") true else index == qData.correctAnswerIndex

                        val formattedChoiceText = if (qData.type == "Upload Image") {
                            "${qData.points}" // Raw string for Upload Image
                        } else if (qData.type == "Short Answer" || qData.type == "Paragraph") {
                            "[${qData.points} pts] ${text.trim()}"
                        } else {
                            if (isCorrect) "[${qData.points} pts] ${text.trim()}" else text.trim()
                        }

                        UiChoice(text = formattedChoiceText, isCorrect = isCorrect)
                    }
                    uiQuestions.add(UiQuestion(text = formattedText, choices = finalChoices))
                }

                if (editAssessmentId != -1) {
                    viewModel.updateAssessment(CurrentCourse.userId, editAssessmentId, assessmentTitle, availableFrom, availableUntil, assessmentNumber, assessmentQuarter, totalPoints,  timeLimit,uiQuestions)
                } else {
                    viewModel.publishAssessment(CurrentCourse.userId, courseId, assessmentTitle, availableFrom, availableUntil, assessmentNumber, assessmentQuarter, totalPoints,  timeLimit,uiQuestions)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    ShowToast.showMessage(this@AssessmentCreatorActivity, "Error: ${e.message}")
                }
            }
        }
    }

    private fun showSaveConfirmationDialog() {
        val isEdit = editAssessmentId != -1
        MaterialAlertDialogBuilder(this)
            .setTitle("Are you sure you want to save this assessment?")
            .setNegativeButton("Yes") { _, _ -> executePublishProcess() }
            .setPositiveButton("No") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun addNewQuestion(existingData: AssessmentQuestion?) {
        val questionData = existingData ?: AssessmentQuestion()
        lateinit var questionManager: QuestionCardManager

        questionManager = QuestionCardManager(
            activity = this,
            container = questionsContainer,
            question = questionData,
            onQuestionChanged = {
                hasUnsavedChanges = true
                updatePublishButtonState()
            },
            onRemove = {
                questionsContainer.removeView(questionManager.cardView)
                questionManagers.remove(questionManager)
                hasUnsavedChanges = true
                updatePublishButtonState()
            }
        )
        questionManagers.add(questionManager)
    }

    private fun handleBackPress() {
        if (hasUnsavedChanges) showDiscardChangesDialog() else finish()
    }

    fun renderLiveMathPreview(container: LinearLayout, text: String, fontSizeSp: Float) {
        container.removeAllViews()
        container.gravity = android.view.Gravity.CENTER_VERTICAL

        val parts = text.split("(?<=\\$)|(?=\\$)".toRegex())
        var isInsideMath = false

        val density = resources.displayMetrics.density
        val heightPx = (fontSizeSp * 1.5f * density).toInt()

        parts.forEach { part ->
            if (part == "$") {
                isInsideMath = !isInsideMath
                return@forEach
            }

            if (isInsideMath) {
                val mathImg = ImageView(this).apply {
                    adjustViewBounds = true
                    scaleType = ImageView.ScaleType.FIT_CENTER

                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        heightPx
                    ).apply {
                        gravity = android.view.Gravity.CENTER_VERTICAL
                        setMargins(8, 0, 8, 0)
                    }
                }
                container.addView(mathImg)

                var cleanedPart = part.replace("\\placeholder{}", "□").trim()
                cleanedPart = cleanedPart.replace("\\bigm|_{x=}", "").trim()
                val encoded = java.net.URLEncoder.encode(cleanedPart, "UTF-8")

                val mathUrl = "https://latex.codecogs.com/png.image?\\dpi{110}\\bg_white $encoded"

                Glide.with(this).load(mathUrl).into(mathImg)
            } else {
                if (part.isNotBlank()) {
                    val tv = TextView(this).apply {
                        this.text = part
                        this.textSize = fontSizeSp
                        this.setTextColor(Color.BLACK)
                        this.gravity = android.view.Gravity.CENTER_VERTICAL
                    }
                    container.addView(tv)
                }
            }
        }
    }

    private fun showDiscardChangesDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Discard Changes?")
            .setMessage("You have unsaved content. If you go back, your changes will be lost.")
            .setNegativeButton("Yes") { _, _ -> finish() }
            .setPositiveButton("No") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun validateAllQuestions(): Boolean {
        if (questionManagers.isEmpty()) return false
        var hasErrors = false
        questionManagers.forEach { if (!it.isValid()) hasErrors = true }
        return !hasErrors
    }

    private fun updatePublishButtonState() {
        val canPublish = questionManagers.isNotEmpty() && hasUnsavedChanges
        btnPublishAssessmentTest.isEnabled = canPublish
        btnPublishAssessmentTest.alpha = if (canPublish) 1.0f else 0.5f
    }

    private fun loadPresetWordBankFromJson(): List<com.example.gr8math.Data.Repository.WordBankItem> {
        val bankList = mutableListOf<com.example.gr8math.Data.Repository.WordBankItem>()
        try {
            val jsonString = assets.open("gr8_math_bank.json").bufferedReader().use { it.readText() }
            val jsonArray = org.json.JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val topicObj = jsonArray.getJSONObject(i)
                val topicName = topicObj.getString("topic")
                val questionsArray = topicObj.getJSONArray("questions")

                val questionsList = mutableListOf<AssessmentQuestion>()
                for (j in 0 until questionsArray.length()) {
                    val qObj = questionsArray.getJSONObject(j)
                    val choices = mutableListOf<String>()
                    val cArr = qObj.getJSONArray("choices")
                    for (k in 0 until cArr.length()) choices.add(cArr.getString(k))

                    val correctAnswers = mutableListOf<String>()
                    val caArr = qObj.getJSONArray("correctAnswers")
                    for (k in 0 until caArr.length()) correctAnswers.add(caArr.getString(k))

                    val type = qObj.getString("type")
                    val points = if (qObj.has("points")) qObj.getInt("points") else 1
                    var correctIndex = -1
                    var correctText = ""
                    if (correctAnswers.isNotEmpty()) {
                        correctText = correctAnswers[0]
                        correctIndex = choices.indexOf(correctText)
                    }

                    questionsList.add(AssessmentQuestion(type, qObj.getString("question"), "", "", choices, correctIndex, points, correctText, ""))
                }
                bankList.add(com.example.gr8math.Data.Repository.WordBankItem(topicName, questionsList))
            }
        } catch (e: Exception) { e.printStackTrace() }
        return bankList
    }

    private fun showWordBankDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_word_bank, null)
        val dialog = MaterialAlertDialogBuilder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnCloseWordBank)
        val spinnerTopics = dialogView.findViewById<Spinner>(R.id.spinnerTopics)
        val llQuestions = dialogView.findViewById<LinearLayout>(R.id.llWordBankQuestions)
        val pbLoading = dialogView.findViewById<ProgressBar>(R.id.pbWordBankLoading)

        btnClose.setOnClickListener { dialog.dismiss() }

        val spinnerLabels = mutableListOf<String>()

        // 🌟 FIX 3: Explicitly use the Repository Type to match the fetch result
        val bankMap = mutableMapOf<String, com.example.gr8math.Data.Repository.WordBankItem>()

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, spinnerLabels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTopics.adapter = adapter

        // Helper to refresh UI
        fun rebuildSpinner(dynamicBank: List<com.example.gr8math.Data.Repository.WordBankItem>) {
            spinnerLabels.clear()
            bankMap.clear()

            // 1. Load Presets from JSON
            val presetTopics = loadPresetWordBankFromJson()
            if (presetTopics.isNotEmpty()) {
                spinnerLabels.add("─── PRESET TOPICS ───")
                presetTopics.forEach {
                    spinnerLabels.add(it.topic)
                    bankMap[it.topic] = it
                }
            }

            // 2. Add Past Assessments from dynamicBank
            if (dynamicBank.isNotEmpty()) {
                spinnerLabels.add("─── PAST ASSESSMENTS ───")
                dynamicBank.forEach {
                    spinnerLabels.add(it.topic)
                    bankMap[it.topic] = it
                }
            }
            adapter.notifyDataSetChanged()
            if (spinnerLabels.size > 1) spinnerTopics.setSelection(1)
        }

        val updateQuestionsList = { selectedBank: com.example.gr8math.Data.Repository.WordBankItem? ->
            llQuestions.removeAllViews()

            val questions = selectedBank?.questions ?: emptyList()

            if (questions.isEmpty()) {
                // --- MAIN CONTENT EMPTY STATE (MATCHING WEB) ---
                val emptyStateContainer = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = android.view.Gravity.CENTER
                    setPadding(64, 128, 64, 128)

                    // Web Design: border-2 border-dashed border-[#D1D8DD] rounded-2xl bg-[#F4F6F8]
                    val shape = android.graphics.drawable.GradientDrawable()
                    shape.cornerRadius = 32f
                    // setStroke parameters: width, color, dashWidth, dashGap
                    shape.setStroke(4, Color.parseColor("#D1D8DD"), 20f, 20f)
                    shape.setColor(Color.parseColor("#F4F6F8"))
                    background = shape

                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 32, 0, 32) }
                }

                // Circular Icon Background
                val iconContainer = FrameLayout(this).apply {
                    val iconShape = android.graphics.drawable.GradientDrawable()
                    iconShape.shape = android.graphics.drawable.GradientDrawable.OVAL
                    iconShape.setColor(Color.parseColor("#D1D8DD"))
                    background = iconShape
                    setPadding(32, 32, 32, 32)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 32 }
                }

                // Simple 'i' or '!' Icon to replicate the SVG
                val tvIcon = TextView(this).apply {
                    text = "!"
                    textSize = 24f
                    setTextColor(Color.parseColor("#888888"))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    gravity = android.view.Gravity.CENTER
                }
                iconContainer.addView(tvIcon)

                // Title
                val tvTitle = TextView(this).apply {
                    text = "NO QUESTIONS FOUND."
                    textSize = 16f
                    setTextColor(Color.parseColor("#222222"))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    gravity = android.view.Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 16 }
                }

                // Subtitle
                val tvSubtitle = TextView(this).apply {
                    text = "This topic currently has no recorded questions. Try selecting another topic or creating a new question manually."
                    textSize = 13f
                    setTextColor(Color.parseColor("#666666"))
                    gravity = android.view.Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                emptyStateContainer.addView(iconContainer)
                emptyStateContainer.addView(tvTitle)
                emptyStateContainer.addView(tvSubtitle)

                llQuestions.addView(emptyStateContainer)

            } else {
                // --- RENDER QUESTIONS ---
                questions.forEach { q ->
                    // 1. Change to RelativeLayout for absolute positioning
                    val card = RelativeLayout(this).apply {
                        setPadding(48, 48, 48, 48)

                        val defaultShape = android.graphics.drawable.GradientDrawable().apply {
                            cornerRadius = 32f
                            setStroke(4, Color.parseColor("#D1D8DD")) // Default Gray border
                            setColor(Color.WHITE)
                        }

                        val pressedShape = android.graphics.drawable.GradientDrawable().apply {
                            cornerRadius = 32f
                            setStroke(6, Color.parseColor("#EFBD31")) // Yellow border, slightly thicker
                            setColor(Color.WHITE)
                        }

                        background = defaultShape

                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { setMargins(0, 0, 0, 32) }

                        isClickable = true // Required for touch events to register on the layout

                        // Simulate 'hover' effect using touch events
                        setOnTouchListener { view, event ->
                            when (event.action) {
                                android.view.MotionEvent.ACTION_DOWN -> {
                                    view.background = pressedShape
                                }
                                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                                    view.background = defaultShape
                                }
                            }
                            false // Return false so the click event can still pass through to child buttons
                        }
                    }

                    // 2. The Type Badge (Top Right)
                    val tvType = TextView(this).apply {
                        id = View.generateViewId()
                        text = q.type.uppercase()
                        textSize = 10f
                        setTextColor(Color.parseColor("#1E4B95"))
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        setPadding(24, 12, 24, 12)

                        val badgeShape = android.graphics.drawable.GradientDrawable()
                        badgeShape.cornerRadius = 12f
                        badgeShape.setColor(Color.parseColor("#F4EFED"))
                        background = badgeShape

                        layoutParams = RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            addRule(RelativeLayout.ALIGN_PARENT_END) // Aligns to right
                            addRule(RelativeLayout.ALIGN_PARENT_TOP) // Aligns to top
                        }
                    }

                    // 3. The Question Text
                    val tvQ = TextView(this).apply {
                        id = View.generateViewId()
                        text = q.questionText
                        textSize = 15f
                        setTextColor(Color.parseColor("#222222"))
                        setTypeface(null, android.graphics.Typeface.BOLD)

                        layoutParams = RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            addRule(RelativeLayout.ALIGN_PARENT_START)
                            addRule(RelativeLayout.ALIGN_PARENT_TOP)
                            // Right margin (250) prevents long text from overlapping the badge
                            setMargins(0, 24, 250, 48)
                        }
                    }

                    // 4. The Action Button
                    val btnAdd = Button(this).apply {
                        text = "+ Add to Assessment"
                        isAllCaps = false
                        textSize = 13f
                        setTextColor(Color.parseColor("#1A4C8B"))
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        stateListAnimator = null // Removes Android's default button drop-shadow

                        val btnShape = android.graphics.drawable.GradientDrawable()
                        btnShape.cornerRadius = 24f
                        btnShape.setColor(Color.parseColor("#EBF0F5")) // Matches web 10% opacity blue
                        background = btnShape

                        layoutParams = RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            addRule(RelativeLayout.BELOW, tvQ.id) // Places button directly below text
                        }

                        setOnClickListener {
                            addNewQuestion(AssessmentQuestion(q.type, q.questionText, q.imageUrl, q.pendingQuestionImageUri, q.choices.toMutableList(), q.correctAnswerIndex, q.points, q.correctTextAnswer, q.pendingAnswerImageUri))
                            hasUnsavedChanges = true
                            updatePublishButtonState()
                            dialog.dismiss()
                        }
                    }

                    card.addView(tvType)
                    card.addView(tvQ)
                    card.addView(btnAdd)
                    llQuestions.addView(card)
                }
            }
        }

        spinnerTopics.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                val label = spinnerLabels[pos]
                if (!label.startsWith("───")) updateQuestionsList(bankMap[label])
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        rebuildSpinner(emptyList())

        lifecycleScope.launch {
            pbLoading.visibility = View.VISIBLE
            val dynamicRes = viewModel.fetchPastQuestionsForWordBank(CurrentCourse.userId)
            pbLoading.visibility = View.GONE
            if (dynamicRes.isSuccess) {
                rebuildSpinner(dynamicRes.getOrDefault(emptyList()))
            }
        }
        dialog.show()
    }
}

// =========================================================================
// HELPER CLASSES
// =========================================================================

data class AssessmentQuestion(
    var type: String = "Multiple Choice",
    var questionText: String = "",
    var imageUrl: String = "",
    var pendingQuestionImageUri: String = "",
    val choices: MutableList<String> = mutableListOf(),
    var correctAnswerIndex: Int = -1,
    var points: Int = 1,
    var correctTextAnswer: String = "",
    var pendingAnswerImageUri: String = ""
) : Serializable


class QuestionCardManager(
    private val activity: AssessmentCreatorActivity,
    private val container: LinearLayout,
    val question: AssessmentQuestion,
    private val onQuestionChanged: () -> Unit,
    private val onRemove: () -> Unit
) {
    val cardView: View = LayoutInflater.from(activity).inflate(R.layout.layout_assessment_question_card, container, false)

    // Views
    private val spinnerType: Spinner = cardView.findViewById(R.id.spinnerQuestionType)
    private val btnUploadImage: ImageButton = cardView.findViewById(R.id.btnUploadImage)

    private val rlImagePreviewContainer: RelativeLayout = cardView.findViewById(R.id.rlImagePreviewContainer)
    private val ivUploadedImage: ImageView = cardView.findViewById(R.id.ivUploadedImage)

    private val etQuestion: TextInputEditText = cardView.findViewById(R.id.etQuestion)
    private val tilQuestion: TextInputLayout = cardView.findViewById(R.id.tilQuestion)
    private val tvPreviewLabel: TextView = cardView.findViewById(R.id.tvPreviewLabel)

    private val llMathPreview: LinearLayout = cardView.findViewById(R.id.llMathPreview)
    private val choicesContainer: LinearLayout = cardView.findViewById(R.id.choicesContainer)
    private val btnAddChoices: Button = cardView.findViewById(R.id.btnAddChoices)
    private val ibRemoveQuestion: ImageButton = cardView.findViewById(R.id.ibRemoveQuestion)
    private val ibRemoveImage: FrameLayout = cardView.findViewById(R.id.ibRemoveImage)
    private val choiceManagers = mutableListOf<ChoiceItemManager>()

    // Answer Key UI Bindings
    private val tvAnswerKey: TextView = cardView.findViewById(R.id.tvAnswerKey)
    private val ivAnswerKeyErrorIcon: ImageView = cardView.findViewById(R.id.ivAnswerKeyErrorIcon)
    private val tvAnswerKeyErrorMsg: TextView = cardView.findViewById(R.id.tvAnswerKeyErrorMsg)
    private val tvKeySetLabel: TextView = cardView.findViewById(R.id.tvKeySetLabel)

    init {
        container.addView(cardView)

        val types = activity.resources.getStringArray(R.array.question_types)
        spinnerType.setSelection(types.indexOf(question.type).takeIf { it >= 0 } ?: 0)
        spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                val newType = types[position]
                if (question.type != newType) {
                    question.type = newType

                    question.choices.clear()
                    question.correctAnswerIndex = -1
                    question.correctTextAnswer = ""
                    question.pendingAnswerImageUri = ""
                    choiceManagers.clear()
                    choicesContainer.removeAllViews()

                    if (newType == "Short Answer" || newType == "Paragraph" || newType == "Upload Image") {
                        btnAddChoices.visibility = View.GONE
                        addChoiceItem("")
                    } else {
                        btnAddChoices.visibility = View.VISIBLE
                        addChoiceItem("")
                    }
                    onQuestionChanged()
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        btnUploadImage.setOnClickListener { activity.pickImage(this) }

        ibRemoveImage.setOnClickListener {
            question.pendingQuestionImageUri = ""
            question.imageUrl = ""
            rlImagePreviewContainer.visibility = View.GONE
            onQuestionChanged()
        }

        val initialQuestionImage = question.imageUrl.ifEmpty { question.pendingQuestionImageUri }
        if (initialQuestionImage.isNotEmpty()) {
            showQuestionImage(initialQuestionImage)
        }

        etQuestion.setText(question.questionText)
        etQuestion.addTextChangedListener(AfterTextChangedWatcher { text ->
            question.questionText = text
            tilQuestion.error = null
            tilQuestion.isErrorEnabled = false
            tilQuestion.boxBackgroundColor = Color.WHITE
            tilQuestion.boxStrokeColor = Color.parseColor("#1E4B95")

            if (text.contains("$")) {
                tvPreviewLabel.visibility = View.VISIBLE
                llMathPreview.visibility = View.VISIBLE
                activity.renderLiveMathPreview(llMathPreview, text, 14f)
            } else {
                tvPreviewLabel.visibility = View.GONE
                llMathPreview.visibility = View.GONE
            }
            onQuestionChanged()
        })

        val initialQText = question.questionText
        if (initialQText.contains("$")) {
            tvPreviewLabel.visibility = View.VISIBLE
            llMathPreview.visibility = View.VISIBLE
            activity.renderLiveMathPreview(llMathPreview, initialQText, 14f)
        }

        val initialChoices = question.choices.toList()
        question.choices.clear()

        if (initialChoices.isEmpty() && (question.type == "Short Answer" || question.type == "Paragraph" || question.type == "Upload Image")) {
            addChoiceItem("")
        } else if (initialChoices.isNotEmpty()) {
            initialChoices.forEach { addChoiceItem(it) }
        }

        btnAddChoices.setOnClickListener {
            addChoiceItem("")
            onQuestionChanged()
        }

        tvAnswerKey.setOnClickListener {
            if (question.choices.isNotEmpty()) showAnswerKeySelectionDialog()
        }

        ibRemoveQuestion.setOnClickListener { onRemove() }
        updateAnswerKeyVisibility()
    }

    fun showQuestionImage(uriOrUrl: String) {
        rlImagePreviewContainer.visibility = View.VISIBLE
        Glide.with(activity).load(uriOrUrl).into(ivUploadedImage)
        tilQuestion.error = null
        tilQuestion.isErrorEnabled = false
    }

    private fun addChoiceItem(initialText: String) {
        val newChoiceManager = ChoiceItemManager(activity, choicesContainer, initialText, question.type, question.points) { index, newText, pts ->
            if (index != -1 && index < question.choices.size) {
                question.choices[index] = newText
                if (question.type == "Short Answer" || question.type == "Paragraph" || question.type == "Upload Image") {
                    question.correctTextAnswer = newText
                }
            }
            onQuestionChanged()
        }

        choiceManagers.add(newChoiceManager)
        question.choices.add(initialText)
        updateAnswerKeyVisibility()
    }

    private fun updateAnswerKeyVisibility() {
        ivAnswerKeyErrorIcon.visibility = View.GONE
        tvAnswerKeyErrorMsg.visibility = View.GONE

        // 🌟 FIX: "Answer Key" is ALWAYS visible for these 3 types so they can set the points!
        if (question.type == "Short Answer" || question.type == "Paragraph" || question.type == "Upload Image") {
            tvAnswerKey.visibility = View.VISIBLE
            tvKeySetLabel.visibility = View.VISIBLE
            tvKeySetLabel.text = "Key Set (${question.points}pt)"
        } else {
            tvAnswerKey.visibility = if (question.choices.isNotEmpty()) View.VISIBLE else View.GONE

            if (question.correctAnswerIndex != -1) {
                tvKeySetLabel.visibility = View.VISIBLE
                tvKeySetLabel.text = "Key Set (${question.points}pt)"
            } else {
                tvKeySetLabel.visibility = View.GONE
            }
        }
    }

    private fun showAnswerKeySelectionDialog() {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_select_answer_key, null)
        val btnCloseDialog = dialogView.findViewById<ImageButton>(R.id.btnCloseDialog)
        val btnProceed = dialogView.findViewById<Button>(R.id.btnProceed)
        val answerKeyChoicesContainer = dialogView.findViewById<LinearLayout>(R.id.answerKeyChoicesContainer)
        val etDialogPoints = dialogView.findViewById<EditText>(R.id.etDialogPoints)

        etDialogPoints.setText(question.points.toString())

        val checkboxes = mutableListOf<CheckBox>()
        val isChoiceType = question.type == "Multiple Choice" || question.type == "Dropdown" || question.type == "Checkboxes"

        // 🌟 FIX: Only render checkboxes if it's a multiple choice style question!
        if (isChoiceType) {
            question.choices.forEachIndexed { index, choiceText ->
                val checkBox = CheckBox(activity).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 120)
                    text = choiceText.replace(Regex("^\\[\\d+(\\.\\d+)?\\s*pts\\]\\s*"), "").trim()
                    textSize = 16f
                    typeface = ResourcesCompat.getFont(activity, R.font.lexend)
                    buttonTintList = ColorStateList.valueOf(Color.parseColor("#1A4C8B"))
                    isChecked = (index == question.correctAnswerIndex)
                    setPadding(24, 0, 0, 0)
                }

                checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked && (question.type == "Multiple Choice" || question.type == "Dropdown")) {
                        checkboxes.forEach { if (it != buttonView) it.isChecked = false }
                    }
                }
                answerKeyChoicesContainer.addView(checkBox)
                checkboxes.add(checkBox)
            }
        } else {
            val tvInfo = TextView(activity).apply {
                text = "Points apply to the whole question."
                textSize = 14f
                setTextColor(Color.GRAY)
                setPadding(0, 0, 0, 16)
            }
            answerKeyChoicesContainer.addView(tvInfo)
        }

        val dialog = MaterialAlertDialogBuilder(activity).setView(dialogView).create()
        btnCloseDialog.setOnClickListener { dialog.dismiss() }
        btnProceed.setOnClickListener {
            if (isChoiceType) {
                val selectedIndex = checkboxes.indexOfFirst { it.isChecked }
                if (selectedIndex != -1) {
                    question.correctAnswerIndex = selectedIndex
                    question.points = etDialogPoints.text.toString().toIntOrNull() ?: 1

                    onQuestionChanged()
                    updateAnswerKeyVisibility()
                    dialog.dismiss()
                } else {
//                    Toast.makeText(activity, "Select the correct answer", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Instantly save points for Short Answer/Para/Image without needing a checkbox!
                question.correctAnswerIndex = 0
                question.points = etDialogPoints.text.toString().toIntOrNull() ?: 1

                onQuestionChanged()
                updateAnswerKeyVisibility()
                dialog.dismiss()
            }
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    fun isValid(): Boolean {
        var isCardValid = true

        if (etQuestion.text.isNullOrBlank() && question.pendingQuestionImageUri.isEmpty() && question.imageUrl.isEmpty()) {
            tilQuestion.boxStrokeColor = Color.parseColor("#ED1F24")
            tilQuestion.boxBackgroundColor = Color.parseColor("#FFF5F5") // Light red background
            tilQuestion.error = "Please enter a question or attach an image"
            isCardValid = false
        } else {
            tilQuestion.boxStrokeColor = Color.parseColor("#D1D8DD")
            tilQuestion.boxBackgroundColor = Color.WHITE
            tilQuestion.error = null
            tilQuestion.isErrorEnabled = false
        }

        if (choiceManagers.isEmpty()) {
//            Toast.makeText(activity, "Please add at least one choice", Toast.LENGTH_SHORT).show()
            isCardValid = false
        }

        choiceManagers.forEach {
            if (!it.isValid()) isCardValid = false
        }

        if (question.type != "Short Answer" && question.type != "Paragraph" && question.type != "Upload Image") {
            if (question.correctAnswerIndex == -1) {

                ivAnswerKeyErrorIcon.visibility = View.VISIBLE
                tvAnswerKeyErrorMsg.visibility = View.VISIBLE
                tvAnswerKeyErrorMsg.text = "Please set an answer key"
                tvAnswerKeyErrorMsg.setTextColor(Color.parseColor("#ED1F24"))
                tvKeySetLabel.visibility = View.GONE
                isCardValid = false
            } else {
                ivAnswerKeyErrorIcon.visibility = View.GONE
                tvAnswerKeyErrorMsg.visibility = View.GONE
            }
        }

        return isCardValid
    }
}

class ChoiceItemManager(
    private val context: Context,
    private val container: LinearLayout,
    initialText: String,
    private val type: String,
    private val initialPoints: Int,
    private val onChoiceChanged: (index: Int, newText: String, points: Int) -> Unit
) {
    val itemView: View = LayoutInflater.from(context).inflate(R.layout.layout_assessment_choice_item, container, false)

    val etAnswerChoice: TextInputEditText = itemView.findViewById(R.id.etAnswerChoice)
    private val tilAnswerChoice: TextInputLayout = itemView.findViewById(R.id.tilAnswerChoice)
    private val etPoints: TextInputEditText = itemView.findViewById(R.id.etPoints)
    private val tilPoints: TextInputLayout = itemView.findViewById(R.id.tilPoints)

    private val tvChoicePreviewLabel: TextView = itemView.findViewById(R.id.tvChoicePreviewLabel)
    private val llChoiceMathPreview: LinearLayout = itemView.findViewById(R.id.llChoiceMathPreview)

    init {
        container.addView(itemView)

        etAnswerChoice.setText(initialText)
        etPoints.setText(initialPoints.toString())

        // 🌟 FORCE POINTS BOX TO HIDE EVERYWHERE to match web (points managed via Answer Key button now)
        tilPoints.visibility = View.GONE

        // TYPE-SPECIFIC UI CONFIGURATION
        if (type == "Upload Image") {
            tilAnswerChoice.startIconDrawable = null

            // Dashed Background & Styling
            etAnswerChoice.setText("Answer must be an uploaded image. No key required. Points saved.")
            etAnswerChoice.isEnabled = false
            etAnswerChoice.setTextColor(Color.parseColor("#666666"))
            tilAnswerChoice.boxStrokeWidth = 0
            tilAnswerChoice.boxStrokeWidthFocused = 0
            tilAnswerChoice.boxBackgroundColor = Color.TRANSPARENT
            etAnswerChoice.background = ContextCompat.getDrawable(context, R.drawable.bg_dashed_upload)
            etAnswerChoice.setPadding(32, 48, 32, 48)
            etAnswerChoice.gravity = android.view.Gravity.CENTER

        } else if (type == "Short Answer" || type == "Paragraph") {
            tilAnswerChoice.startIconDrawable = null
            tilAnswerChoice.hint = if (type == "Short Answer") "Correct Answer" else "Expected answer or grading criteria..."

            // Bold Blue text matching the web
            etAnswerChoice.setTextColor(Color.parseColor("#1E4B95"))
            etAnswerChoice.setTypeface(null, android.graphics.Typeface.BOLD)

            if (type == "Paragraph") {
                etAnswerChoice.minLines = 4
                etAnswerChoice.gravity = android.view.Gravity.TOP or android.view.Gravity.START
            }
        }

        etAnswerChoice.addTextChangedListener(AfterTextChangedWatcher { text ->
            tilAnswerChoice.error = null
            tilAnswerChoice.isErrorEnabled = false
            if (type != "Upload Image") tilAnswerChoice.boxBackgroundColor = Color.TRANSPARENT
            syncData()

            if (type != "Upload Image" && text.contains("$")) {
                tvChoicePreviewLabel.visibility = View.VISIBLE
                llChoiceMathPreview.visibility = View.VISIBLE
                (context as? AssessmentCreatorActivity)?.renderLiveMathPreview(llChoiceMathPreview, text, 14f)
            } else {
                tvChoicePreviewLabel.visibility = View.GONE
                llChoiceMathPreview.visibility = View.GONE
            }
        })

        if (type != "Upload Image" && initialText.contains("$")) {
            tvChoicePreviewLabel.visibility = View.VISIBLE
            llChoiceMathPreview.visibility = View.VISIBLE
            (context as? AssessmentCreatorActivity)?.renderLiveMathPreview(llChoiceMathPreview, initialText, 14f)
        }
    }

    private fun syncData() {
        val index = container.indexOfChild(itemView)
        val currentText = if (type == "Upload Image") "" else etAnswerChoice.text.toString().trim()
        val currentPoints = etPoints.text.toString().toIntOrNull() ?: 1

        onChoiceChanged(index, currentText, currentPoints)
    }

    fun isValid(): Boolean {
        var valid = true

        if (type != "Upload Image" && etAnswerChoice.text.isNullOrBlank()) {
            tilAnswerChoice.boxStrokeColor = Color.parseColor("#ED1F24")
            tilAnswerChoice.boxBackgroundColor = Color.parseColor("#FFF5F5") // Light red background
            tilAnswerChoice.error = "Please enter a choice"
            valid = false
        } else {
            tilAnswerChoice.boxStrokeColor = Color.parseColor("#D1D8DD")
            tilAnswerChoice.boxBackgroundColor = Color.TRANSPARENT
            tilAnswerChoice.error = null
            tilAnswerChoice.isErrorEnabled = false
        }

        return valid
    }
}

class AfterTextChangedWatcher(private val afterTextChanged: (String) -> Unit) : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(s: Editable?) { afterTextChanged.invoke(s.toString()) }
}