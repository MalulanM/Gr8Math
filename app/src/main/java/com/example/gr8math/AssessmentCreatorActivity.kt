package com.example.gr8math // Make sure this matches your package name

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.AssessmentRequest
import com.example.gr8math.dataObject.Choice
import com.example.gr8math.dataObject.CurrentCourse
import com.example.gr8math.dataObject.Question
import com.example.gr8math.utils.ShowToast
import com.example.gr8math.utils.UIUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import okhttp3.ResponseBody
import retrofit2.Call
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
            tilQuestion.isErrorEnabled = false
            tilQuestion.boxStrokeColor = ContextCompat.getColor(context, R.color.til_stroke)
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

    fun isValid(): Boolean {
        var valid = true

        if (etQuestion.text.isNullOrBlank()) {
            UIUtils.errorDisplay(context, tilQuestion, etQuestion, true, context.getString(R.string.error_blank_field))
            valid = false
        }

        if (question.choices.isEmpty()) {
            UIUtils.errorDisplay(context, tilQuestion, etQuestion, true, context.getString(R.string.error_blank_field))
            valid = false
        } else {
            // Validate each choice
            choiceManagers.forEach { choiceManager ->
                if (!choiceManager.isValid()) {
                    valid = false
                }
            }

            // Validate answer key
            if (question.correctAnswerIndex == -1) {
                ShowToast.showMessage(context, "Please select an answer key for this question.")
                valid = false
            }
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

    lateinit var loadingLayout : View

    lateinit var loadingProgress : View

    lateinit var loadingText : TextView
    private lateinit var questionsContainer: LinearLayout
    private lateinit var btnAddQuestion: Button
    private lateinit var btnPublishAssessmentTest: Button
    private lateinit var toolbar: MaterialToolbar

    // List to hold the data and UI managers for each question
    private val questionManagers = mutableListOf<QuestionCardManager>()

    // Boolean to track if there are any unsaved changes
    private var hasUnsavedChanges = false

    // Hold Assessment details from intent
    private var assessmentNumber: Int? = 0

    private var assessmentQuarter: Int? = 0

    private var assessmentTitle: String? = null
    private var availableFrom: String? = null
    private var availableUntil: String? = null
    private var courseId: Int? = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assessment_creator)

        // Get data from intent
        assessmentNumber = intent.getStringExtra("EXTRA_ASSESSMENT_NUMBER")?.toIntOrNull() ?: 0
        assessmentTitle = intent.getStringExtra("EXTRA_ASSESSMENT_TITLE")
        availableFrom = intent.getStringExtra("EXTRA_AVAILABLE_FROM")
        availableUntil = intent.getStringExtra("EXTRA_AVAILABLE_UNTIL")
        assessmentQuarter = intent.getStringExtra("EXTRA_AVAILABLE_QUARTER")?.toIntOrNull() ?: 0
        courseId = CurrentCourse.courseId

        Log.e("CONTENT_PPPP", "${assessmentNumber}, ${assessmentTitle}, ${courseId}")
        toolbar = findViewById(R.id.toolbar)
        questionsContainer = findViewById(R.id.questionsContainer)
        btnAddQuestion = findViewById(R.id.btnAddQuestion)
        btnPublishAssessmentTest = findViewById(R.id.btnPublishAssessmentTest)
        loadingLayout =  findViewById<View>(R.id.loadingLayout)
        loadingProgress = findViewById<View>(R.id.loadingProgressBg)
        loadingText = findViewById<TextView>(R.id.loadingText)
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
                ShowToast.showMessage(this, "Please fix errors before publishing.")
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

        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
        val request = AssessmentRequest(
            course_id = courseId!!,
            title = assessmentTitle ?: "",
            start_time = availableFrom ?: "",
            end_time = availableUntil ?: "",
            assessment_items = questionManagers.size,
            assessment_number = assessmentNumber!!,
            assessment_quarter = assessmentQuarter!!,
            questions = questionManagers.map { manager ->
                val q = manager.question
                Question(
                    question_text = q.questionText,
                    choices = q.choices.mapIndexed { index, text ->
                        Choice(
                            choice_text = text,
                            is_correct = index == q.correctAnswerIndex
                        )
                    }
                )
            }
        )

        val apiService = ConnectURL.api
        val call = apiService.storeAssessment(request)

        call.enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: retrofit2.Response<ResponseBody>
            ) {

                val responseString = response.body()?.string() ?: response.errorBody()?.string()

                if (responseString.isNullOrEmpty()) {
                    Log.e("API_ERROR", "Empty response")
                    return
                }
                try {
                    val jsonObj = org.json.JSONObject(responseString)
                    val success = jsonObj.optBoolean("success", false)
                    val message = jsonObj.optString("message", "No message")
                    val dataArray = jsonObj.optJSONArray("data") ?: org.json.JSONArray()

                    if(success) {
                        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                        val intent = Intent(
                            this@AssessmentCreatorActivity,
                            TeacherClassPageActivity::class.java
                        ).apply {
                            intent.putExtra("toast_msg", message)
                        }
                        startActivity(intent)
                    }
                }  catch (e: Exception) {
                Log.e("API_ERROR", "Failed to parse response: ${e.localizedMessage}", e)
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
            }

            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(
                    this@AssessmentCreatorActivity,
                    "Network Error: ${t.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }


    private fun updatePublishButtonState() {
        // Publish button should be enabled if there's at least one question and there are unsaved changes.
        val canPublish = questionManagers.isNotEmpty() && hasUnsavedChanges
        btnPublishAssessmentTest.isEnabled = canPublish
        btnPublishAssessmentTest.alpha = if (canPublish) 1.0f else 0.5f
    }
}