package com.example.gr8math // Make sure this matches your package name

import android.app.Activity
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
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.Serializable

// --- Data Classes to hold assessment structure ---
data class AssessmentQuestion(
    var questionText: String = "",
    val choices: MutableList<String> = mutableListOf(),
    var correctAnswerIndex: Int = -1 // Index of the correct answer
) : Serializable

// --- HELPER CLASS ---
/**
 * A simple TextWatcher to only listen for the afterTextChanged event.
 */
class AfterTextChangedWatcher(private val afterTextChanged: (String) -> Unit) : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(s: Editable?) {
        afterTextChanged.invoke(s.toString())
    }
}

// --- HELPER FUNCTION ---
/**
 * Helper extension function to convert DP to PX.
 */
fun Int.dpToPx(context: Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}

// This class will manage the views for a single question card
class QuestionCardManager(
    private val context: Context,
    private val container: LinearLayout, // The LinearLayout to add the card to
    val question: AssessmentQuestion, // The data for this question
    private val onQuestionChanged: () -> Unit, // Callback for any change in the question data
    private val onRemove: () -> Unit // Callback to remove this card
) {
    val cardView: View = LayoutInflater.from(context)
        .inflate(R.layout.layout_assessment_question_card, container, false)
    private val etQuestion: TextInputEditText = cardView.findViewById(R.id.etQuestion)
    private val tilQuestion: TextInputLayout = cardView.findViewById(R.id.tilQuestion)
    private val choicesContainer: LinearLayout = cardView.findViewById(R.id.choicesContainer)
    private val btnAddChoices: Button = cardView.findViewById(R.id.btnAddChoices)
    private val tvAnswerKey: TextView = cardView.findViewById(R.id.tvAnswerKey)
    private val ibRemoveQuestion: ImageButton = cardView.findViewById(R.id.ibRemoveQuestion)

    private val choiceManagers = mutableListOf<ChoiceItemManager>() // Manages individual choice views

    init {
        container.addView(cardView) // Add the card to the container

        etQuestion.setText(question.questionText)
        etQuestion.addTextChangedListener(AfterTextChangedWatcher { text ->
            question.questionText = text
            tilQuestion.error = null // Clear error on text change
            onQuestionChanged()
        })

        // Add existing choices
        question.choices.forEach { addChoiceItem(it) }

        btnAddChoices.setOnClickListener {
            addChoiceItem("") // Add an empty choice
            onQuestionChanged()
        }

        tvAnswerKey.setOnClickListener {
            if (question.choices.isNotEmpty()) {
                showAnswerKeySelectionDialog()
            } else {
                Toast.makeText(context, "Please add choices first.", Toast.LENGTH_SHORT).show()
            }
        }

        // Set click listener for the delete button
        ibRemoveQuestion.setOnClickListener {
            onRemove() // Call the remove callback
        }

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
        question.choices.add(initialText) // Add to data model immediately
        updateAnswerKeyVisibility()
        onQuestionChanged()
    }

    private fun updateAnswerKeyVisibility() {
        tvAnswerKey.visibility = if (question.choices.isNotEmpty()) View.VISIBLE else View.GONE
    }

    //
    // --- NO LONGER NEEDED: updateAnswerKeyText() was removed to keep the button text stationary ---
    //

    private fun showAnswerKeySelectionDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_select_answer_key, null)
        val btnCloseDialog = dialogView.findViewById<ImageButton>(R.id.btnCloseDialog)
        val btnProceed = dialogView.findViewById<Button>(R.id.btnProceed)
        val answerKeyChoicesContainer = dialogView.findViewById<LinearLayout>(R.id.answerKeyChoicesContainer)

        // Create checkboxes for each choice
        val checkboxes = mutableListOf<CheckBox>()
        question.choices.forEachIndexed { index, choiceText ->
            val checkBox = CheckBox(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 8.dpToPx(context)) // Add some bottom margin
                }
                text = choiceText
                textSize = 16f
                typeface = ResourcesCompat.getFont(context, R.font.lexend)
                isChecked = (index == question.correctAnswerIndex) // Pre-select if already chosen
            }
            // Only one checkbox can be selected at a time
            checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    checkboxes.forEach { otherCheckbox ->
                        if (otherCheckbox != buttonView) {
                            otherCheckbox.isChecked = false
                        }
                    }
                }
            }
            answerKeyChoicesContainer.addView(checkBox)
            checkboxes.add(checkBox)
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCloseDialog.setOnClickListener {
            dialog.dismiss()
        }

        btnProceed.setOnClickListener {
            val selectedIndex = checkboxes.indexOfFirst { it.isChecked }
            if (selectedIndex != -1) {
                question.correctAnswerIndex = selectedIndex
                // --- REMOVED: updateAnswerKeyText() ---
                onQuestionChanged()
                dialog.dismiss()
            } else {
                Toast.makeText(context, "Please select an answer key.", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }


    // Validation method for the question card
    fun isValid(): Boolean {
        var valid = true
        if (etQuestion.text.isNullOrBlank()) {
            tilQuestion.error = context.getString(R.string.error_blank_field)
            valid = false
        } else {
            tilQuestion.error = null
        }

        if (question.choices.isEmpty()) {
            // If there are no choices, it's only valid if you don't require choices.
        } else {
            // Validate each choice
            choiceManagers.forEach { choiceManager ->
                if (!choiceManager.isValid()) {
                    valid = false
                }
            }
        }

        // Validate if an answer key is selected if choices exist
        if (question.choices.isNotEmpty() && question.correctAnswerIndex == -1) {
            // --- REMOVED: No longer changing text color ---
            valid = false
        }

        return valid
    }
}

// This class will manage the views for a single answer choice input
class ChoiceItemManager(
    private val context: Context,
    private val container: LinearLayout,
    initialText: String,
    private val onChoiceChanged: (index: Int, newText: String) -> Unit // Callback to update the parent's data model
) {
    val itemView: View = LayoutInflater.from(context)
        .inflate(R.layout.layout_assessment_choice_item, container, false)
    private val etAnswerChoice: TextInputEditText = itemView.findViewById(R.id.etAnswerChoice)
    private val tilAnswerChoice: TextInputLayout = itemView.findViewById(R.id.tilAnswerChoice)

    init {
        container.addView(itemView)
        etAnswerChoice.setText(initialText)
        etAnswerChoice.addTextChangedListener(AfterTextChangedWatcher { text ->
            // Find its index in the parent's container to pass back
            val index = container.indexOfChild(itemView)
            onChoiceChanged(index, text)
            tilAnswerChoice.error = null // Clear error on text change
        })
    }

    // Validation method for the choice item
    fun isValid(): Boolean {
        return if (etAnswerChoice.text.isNullOrBlank()) {
            tilAnswerChoice.error = context.getString(R.string.error_blank_field)
            false
        } else {
            tilAnswerChoice.error = null
            true
        }
    }
}


class AssessmentCreatorActivity : AppCompatActivity() {

    private lateinit var questionsContainer: LinearLayout
    private lateinit var btnAddQuestion: Button
    private lateinit var btnPublishAssessmentTest: Button
    private lateinit var toolbar: MaterialToolbar

    // List to hold the data and UI managers for each question
    private val questionManagers = mutableListOf<QuestionCardManager>()

    // Boolean to track if there are any unsaved changes
    private var hasUnsavedChanges = false

    // Hold Assessment details from intent
    private var assessmentNumber: String? = null
    private var assessmentTitle: String? = null
    private var availableFrom: String? = null
    private var availableUntil: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assessment_creator)

        // Get data from intent
        assessmentNumber = intent.getStringExtra("EXTRA_ASSESSMENT_NUMBER")
        assessmentTitle = intent.getStringExtra("EXTRA_ASSESSMENT_TITLE")
        availableFrom = intent.getStringExtra("EXTRA_AVAILABLE_FROM")
        availableUntil = intent.getStringExtra("EXTRA_AVAILABLE_UNTIL")

        toolbar = findViewById(R.id.toolbar)
        questionsContainer = findViewById(R.id.questionsContainer)
        btnAddQuestion = findViewById(R.id.btnAddQuestion)
        btnPublishAssessmentTest = findViewById(R.id.btnPublishAssessmentTest)

        // Set the title from the intent
        toolbar.title = assessmentTitle ?: "Create Assessment"

        toolbar.setNavigationOnClickListener {
            handleBackPress()
        }

        // Handle system back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })

        btnAddQuestion.setOnClickListener {
            addNewQuestion()
            hasUnsavedChanges = true // Adding a question counts as a change
            updatePublishButtonState()
        }

        btnPublishAssessmentTest.setOnClickListener {
            if (validateAllQuestions()) {
                showSaveConfirmationDialog(isPublishing = true)
            } else {
                Toast.makeText(this, "Please fix errors before publishing.", Toast.LENGTH_SHORT).show()
            }
        }

        // Add the first question automatically on start
        addNewQuestion()
        // Set hasUnsavedChanges to false initially, only adding a question isn't a "change" yet
        hasUnsavedChanges = false
        updatePublishButtonState()
    }

    private fun addNewQuestion() {
        val newQuestion = AssessmentQuestion()

        // We declare questionManager first so we can reference it
        // inside its own 'onRemove' callback.
        lateinit var questionManager: QuestionCardManager
        questionManager = QuestionCardManager(
            context = this,
            container = questionsContainer,
            question = newQuestion,
            onQuestionChanged = {
                // Callback to update hasUnsavedChanges whenever a question's data changes
                hasUnsavedChanges = true
                updatePublishButtonState()
            },
            onRemove = {
                // Callback for when the delete icon is clicked
                questionsContainer.removeView(questionManager.cardView) // Remove from UI
                questionManagers.remove(questionManager) // Remove from data list
                hasUnsavedChanges = true // Deleting is a change
                updatePublishButtonState()
            }
        )

        questionManagers.add(questionManager)
    }

    private fun handleBackPress() {
        if (hasUnsavedChanges) {
            showDiscardChangesDialog()
        } else {
            finish()
        }
    }

    private fun showDiscardChangesDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_discard_assessment_title)
            .setMessage(R.string.dialog_discard_assessment_message)
            .setNegativeButton(R.string.discard_action) { _, _ ->
                finish() // Discard changes and exit
            }
            .setPositiveButton(R.string.cancel_action) { dialog, _ ->
                dialog.dismiss() // Stay on the page
            }
            .show()
    }

    private fun showSaveConfirmationDialog(isPublishing: Boolean) {
        val messageResId = if (isPublishing) R.string.dialog_save_assessment_message else R.string.dialog_save_message

        val customMessage = TextView(this).apply {
            text = getString(messageResId)
            setTextColor(ContextCompat.getColor(this@AssessmentCreatorActivity, R.color.colorText))
            textSize = 18f
            setPadding(60, 50, 60, 30)
            try {
                val typeface = ResourcesCompat.getFont(this@AssessmentCreatorActivity, R.font.lexend)
                this.typeface = typeface
            } catch (e: Exception) {
                // Font not found
            }
        }
        MaterialAlertDialogBuilder(this)
            .setCustomTitle(customMessage)
            .setNegativeButton(R.string.yes) { _, _ ->
                // Perform save/publish logic here
                saveAssessment(isPublishing)
            }
            .setPositiveButton(R.string.no) { dialog, _ ->
                dialog.dismiss() // Stay on the page
            }
            .show()
    }

    private fun validateAllQuestions(): Boolean {
        var allValid = true
        if (questionManagers.isEmpty()) {
            Toast.makeText(this, "Please add at least one question.", Toast.LENGTH_SHORT).show()
            return false
        }
        questionManagers.forEach { manager ->
            if (!manager.isValid()) {
                allValid = false
            }
        }
        return allValid
    }

    private fun saveAssessment(publish: Boolean) {
        // TODO: Implement actual saving/publishing logic here

        val assessmentData = questionManagers.map { it.question }

        // For demonstration, just log the data and show a toast
        println("--- SAVING ASSESSMENT ---")
        println("Title: $assessmentTitle ($assessmentNumber)")
        println("Available: $availableFrom to $availableUntil")
        assessmentData.forEachIndexed { qIndex, question ->
            println("Question ${qIndex + 1}: ${question.questionText}")
            question.choices.forEachIndexed { cIndex, choice ->
                println("  Choice ${cIndex + 1}: $choice")
            }
            println("  Correct Answer Index: ${question.correctAnswerIndex}")
        }
        println("-------------------------")

        Toast.makeText(this, if (publish) "Assessment Published!" else "Assessment Saved!", Toast.LENGTH_SHORT).show()
        hasUnsavedChanges = false // Reset after saving

        setResult(Activity.RESULT_OK)
        finish() // Exit after saving/publishing
    }

    private fun updatePublishButtonState() {
        // Publish button should be enabled if there's at least one question and there are unsaved changes.
        val canPublish = questionManagers.isNotEmpty() && hasUnsavedChanges
        btnPublishAssessmentTest.isEnabled = canPublish
        btnPublishAssessmentTest.alpha = if (canPublish) 1.0f else 0.5f
    }
}