package com.example.gr8math.Activity.TeacherModule.Assessment

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.gr8math.Data.Model.UiChoice
import com.example.gr8math.Data.Model.UiQuestion
import com.example.gr8math.Model.CurrentCourse
import com.example.gr8math.R
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.Utils.UIUtils
import com.example.gr8math.ViewModel.AssessmentState
import com.example.gr8math.ViewModel.AssessmentViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.Serializable

class AssessmentCreatorActivity : AppCompatActivity() {

    private val viewModel: AssessmentViewModel by viewModels()

    lateinit var loadingLayout: View
    lateinit var loadingProgress: View
    lateinit var loadingText: TextView
    private lateinit var questionsContainer: LinearLayout
    private lateinit var btnAddQuestion: Button
    private lateinit var btnPublishAssessmentTest: Button
    private lateinit var toolbar: MaterialToolbar

    // Logic Managers
    private val questionManagers = mutableListOf<QuestionCardManager>()
    private var hasUnsavedChanges = false

    // Data passed from Intent
    private var assessmentNumber: Int = 0
    private var assessmentQuarter: Int = 0
    private var assessmentTitle: String = ""
    private var availableFrom: String = ""
    private var availableUntil: String = ""
    private var courseId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assessment_creator)

        initData()
        initViews()
        setupListeners()
        setupObservers()

        // Add first question
        addNewQuestion()
        hasUnsavedChanges = false
        updatePublishButtonState()
    }

    private fun initData() {
        assessmentNumber = intent.getStringExtra("EXTRA_ASSESSMENT_NUMBER")?.toIntOrNull() ?: 0
        assessmentTitle = intent.getStringExtra("EXTRA_ASSESSMENT_TITLE") ?: ""
        availableFrom = intent.getStringExtra("EXTRA_AVAILABLE_FROM") ?: ""
        availableUntil = intent.getStringExtra("EXTRA_AVAILABLE_UNTIL") ?: ""
        assessmentQuarter = intent.getStringExtra("EXTRA_AVAILABLE_QUARTER")?.toIntOrNull() ?: 0
        courseId = CurrentCourse.courseId
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        questionsContainer = findViewById(R.id.questionsContainer)
        btnAddQuestion = findViewById(R.id.btnAddQuestion)
        btnPublishAssessmentTest = findViewById(R.id.btnPublishAssessmentTest)
        loadingLayout = findViewById(R.id.loadingLayout)
        loadingProgress = findViewById(R.id.loadingProgressBg)
        loadingText = findViewById(R.id.loadingText)

        toolbar.title = assessmentTitle.ifEmpty { "Create Assessment" }
    }

    private fun setupListeners() {
        toolbar.setNavigationOnClickListener { handleBackPress() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { handleBackPress() }
        })

        btnAddQuestion.setOnClickListener {
            addNewQuestion()
            hasUnsavedChanges = true
            updatePublishButtonState()
        }

        btnPublishAssessmentTest.setOnClickListener {
            if (validateAllQuestions()) {
                showSaveConfirmationDialog()
            } else {
                ShowToast.showMessage(this, "Please check for errors.")
            }
        }
    }

    private fun setupObservers() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is AssessmentState.Loading -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
                }
                is AssessmentState.Success -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    ShowToast.showMessage(this, "Assessment published successfully!")
                    setResult(RESULT_OK)
                    finish()
                }
                is AssessmentState.Error -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    ShowToast.showMessage(this, state.message)
                }
                is AssessmentState.Idle -> {}
            }
        }
    }

    // --- LOGIC: Extract Data from UI and Send to ViewModel ---
    private fun saveAssessment() {
        // 1. Convert Managers to UI Models
        val uiQuestions = questionManagers.map { manager ->
            val qData = manager.question
            UiQuestion(
                text = qData.questionText,
                choices = qData.choices.mapIndexed { index, text ->
                    UiChoice(
                        text = text,
                        isCorrect = (index == qData.correctAnswerIndex)
                    )
                }
            )
        }

        // 2. Call ViewModel
        viewModel.publishAssessment(
            courseId = courseId,
            title = assessmentTitle,
            rawStartTime = availableFrom,
            rawEndTime = availableUntil,
            assessmentNumber = assessmentNumber,
            assessmentQuarter = assessmentQuarter,
            questions = uiQuestions
        )
    }

    // --- UI HELPERS ---

    private fun addNewQuestion() {
        val newQuestion = AssessmentQuestion()
        lateinit var questionManager: QuestionCardManager

        questionManager = QuestionCardManager(
            context = this,
            container = questionsContainer,
            question = newQuestion,
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

    private fun showDiscardChangesDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_discard_assessment_title)
            .setMessage(R.string.dialog_discard_assessment_message)
            .setNegativeButton(R.string.discard_action) { _, _ -> finish() }
            .setPositiveButton(R.string.cancel_action) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showSaveConfirmationDialog() {
        val customMessage = TextView(this).apply {
            text = getString(R.string.dialog_save_assessment_message)
            setTextColor(ContextCompat.getColor(this@AssessmentCreatorActivity, R.color.colorText))
            textSize = 18f
            setPadding(60, 50, 60, 30)
            try { typeface = ResourcesCompat.getFont(this@AssessmentCreatorActivity, R.font.lexend) } catch (_: Exception) {}
        }
        MaterialAlertDialogBuilder(this)
            .setCustomTitle(customMessage)
            .setNegativeButton(R.string.yes) { _, _ -> saveAssessment() }
            .setPositiveButton(R.string.no) { dialog, _ -> dialog.dismiss() }
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
}

// =========================================================================
// HELPER CLASSES (These must be in the file or separate files)
// =========================================================================

data class AssessmentQuestion(
    var questionText: String = "",
    val choices: MutableList<String> = mutableListOf(),
    var correctAnswerIndex: Int = -1
) : Serializable

class QuestionCardManager(
    private val context: Context,
    private val container: LinearLayout,
    val question: AssessmentQuestion,
    private val onQuestionChanged: () -> Unit,
    private val onRemove: () -> Unit
) {
    val cardView: View = LayoutInflater.from(context)
        .inflate(R.layout.layout_assessment_question_card, container, false)
    private val etQuestion: TextInputEditText = cardView.findViewById(R.id.etQuestion)
    private val tilQuestion: TextInputLayout = cardView.findViewById(R.id.tilQuestion)
    private val choicesContainer: LinearLayout = cardView.findViewById(R.id.choicesContainer)
    private val btnAddChoices: Button = cardView.findViewById(R.id.btnAddChoices)
    private val tvAnswerKey: TextView = cardView.findViewById(R.id.tvAnswerKey)
    private val ibRemoveQuestion: ImageButton = cardView.findViewById(R.id.ibRemoveQuestion)

    private val choiceManagers = mutableListOf<ChoiceItemManager>()

    init {
        container.addView(cardView)

        etQuestion.setText(question.questionText)
        etQuestion.addTextChangedListener(AfterTextChangedWatcher { text ->
            question.questionText = text
            tilQuestion.error = null
            tilQuestion.isErrorEnabled = false
            onQuestionChanged()
        })

        question.choices.forEach { addChoiceItem(it) }

        btnAddChoices.setOnClickListener {
            addChoiceItem("")
            onQuestionChanged()
        }

        tvAnswerKey.setOnClickListener {
            if (question.choices.isNotEmpty()) {
                showAnswerKeySelectionDialog()
            }
        }

        ibRemoveQuestion.setOnClickListener { onRemove() }
        updateAnswerKeyVisibility()
    }

    private fun addChoiceItem(initialText: String) {
        val newChoiceManager = ChoiceItemManager(context, choicesContainer, initialText) { index, newText ->
            if (index != -1 && index < question.choices.size) {
                question.choices[index] = newText
            }
            onQuestionChanged()
        }
        choiceManagers.add(newChoiceManager)
        question.choices.add(initialText)
        updateAnswerKeyVisibility()
        onQuestionChanged()
    }

    private fun updateAnswerKeyVisibility() {
        tvAnswerKey.visibility = if (question.choices.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun showAnswerKeySelectionDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_select_answer_key, null)
        val btnCloseDialog = dialogView.findViewById<ImageButton>(R.id.btnCloseDialog)
        val btnProceed = dialogView.findViewById<Button>(R.id.btnProceed)
        val answerKeyChoicesContainer = dialogView.findViewById<LinearLayout>(R.id.answerKeyChoicesContainer)

        val checkboxes = mutableListOf<CheckBox>()
        question.choices.forEachIndexed { index, choiceText ->
            val checkBox = CheckBox(context).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                text = choiceText
                textSize = 16f
                isChecked = (index == question.correctAnswerIndex)
            }
            checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    checkboxes.forEach { if (it != buttonView) it.isChecked = false }
                }
            }
            answerKeyChoicesContainer.addView(checkBox)
            checkboxes.add(checkBox)
        }

        val dialog = MaterialAlertDialogBuilder(context).setView(dialogView).setCancelable(false).create()
        btnCloseDialog.setOnClickListener { dialog.dismiss() }
        btnProceed.setOnClickListener {
            val selectedIndex = checkboxes.indexOfFirst { it.isChecked }
            if (selectedIndex != -1) {
                question.correctAnswerIndex = selectedIndex
                onQuestionChanged()
                dialog.dismiss()
            }
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    fun isValid(): Boolean {
        var valid = true
        if (etQuestion.text.isNullOrBlank()) {
            UIUtils.errorDisplay(context, tilQuestion, etQuestion, true, "Required")
            valid = false
        }
        if (question.choices.isEmpty()) valid = false
        choiceManagers.forEach { if (!it.isValid()) valid = false }
        if (question.correctAnswerIndex == -1) valid = false
        return valid
    }
}

class ChoiceItemManager(
    private val context: Context,
    private val container: LinearLayout,
    initialText: String,
    private val onChoiceChanged: (index: Int, newText: String) -> Unit
) {
    val itemView: View = LayoutInflater.from(context).inflate(R.layout.layout_assessment_choice_item, container, false)
    private val etAnswerChoice: TextInputEditText = itemView.findViewById(R.id.etAnswerChoice)
    private val tilAnswerChoice: TextInputLayout = itemView.findViewById(R.id.tilAnswerChoice)

    init {
        container.addView(itemView)
        etAnswerChoice.setText(initialText)
        etAnswerChoice.addTextChangedListener(AfterTextChangedWatcher { text ->
            val index = container.indexOfChild(itemView)
            onChoiceChanged(index, text)
            tilAnswerChoice.error = null
        })
    }

    fun isValid(): Boolean {
        return if (etAnswerChoice.text.isNullOrBlank()) {
            UIUtils.errorDisplay(context, tilAnswerChoice, etAnswerChoice, true, "Required")
            false
        } else {
            UIUtils.errorDisplay(context, tilAnswerChoice, etAnswerChoice, false, "")
            true
        }
    }
}

class AfterTextChangedWatcher(private val afterTextChanged: (String) -> Unit) : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(s: Editable?) { afterTextChanged.invoke(s.toString()) }
}