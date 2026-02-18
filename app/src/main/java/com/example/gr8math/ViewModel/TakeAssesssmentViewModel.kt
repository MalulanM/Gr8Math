package com.example.gr8math.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gr8math.Data.Model.AssessmentFullDetails
import com.example.gr8math.Data.Repository.TakeAssessmentRepository
import com.example.gr8math.Model.CurrentCourse
import com.google.gson.Gson
import kotlinx.coroutines.launch

sealed class TakeAssessmentState {
    object Idle : TakeAssessmentState()
    object Loading : TakeAssessmentState()
    object Submitted : TakeAssessmentState() // Success
    data class Error(val message: String) : TakeAssessmentState()
}

class TakeAssessmentViewModel : ViewModel() {

    private val repository = TakeAssessmentRepository()

    // Assessment Data
    var assessment: AssessmentFullDetails? = null
        private set

    // State
    private val _state = MutableLiveData<TakeAssessmentState>(TakeAssessmentState.Idle)
    val state: LiveData<TakeAssessmentState> = _state

    // Navigation State
    private val _currentIndex = MutableLiveData(0)
    val currentIndex: LiveData<Int> = _currentIndex

    // Answers: Map<QuestionID, ChoiceID>
    private val _selectedAnswers = mutableMapOf<Int, Int>()

    // Initialize from Intent JSON
    fun parseAssessmentData(json: String) {
        try {
            // Using Gson to parse the JSON passed from the previous activity
            // We map it to the NEW AssessmentFullDetails model
            assessment = Gson().fromJson(json, AssessmentFullDetails::class.java)
        } catch (e: Exception) {
            _state.value = TakeAssessmentState.Error("Failed to parse assessment data")
        }
    }

    // Navigation Methods
    fun nextQuestion() {
        val max = (assessment?.questions?.size ?: 0) - 1
        if (_currentIndex.value!! < max) {
            _currentIndex.value = _currentIndex.value!! + 1
        }
    }

    fun prevQuestion() {
        if (_currentIndex.value!! > 0) {
            _currentIndex.value = _currentIndex.value!! - 1
        }
    }

    fun jumpToFirst() {
        _currentIndex.value = 0
    }

    // Answer Selection
    fun selectAnswer(questionId: Int, choiceId: Int) {
        _selectedAnswers[questionId] = choiceId
    }

    fun getSelectedAnswer(questionId: Int): Int? {
        return _selectedAnswers[questionId]
    }

    // Submission
    fun submitAssessment() {
        val currentAssessment = assessment ?: return
        val studentId = CurrentCourse.userId

        _state.value = TakeAssessmentState.Loading
        viewModelScope.launch {
            val result = repository.submitAssessment(studentId, currentAssessment, _selectedAnswers)

            result.onSuccess {
                _state.value = TakeAssessmentState.Submitted
            }.onFailure {
                _state.value = TakeAssessmentState.Error(it.message ?: "Submission failed")
            }
        }
    }
}