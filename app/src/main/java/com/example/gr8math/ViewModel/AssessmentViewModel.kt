package com.example.gr8math.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gr8math.Data.Model.AssessmentInsert
import com.example.gr8math.Data.Model.AssessmentUpdate
import com.example.gr8math.Data.Model.UiQuestion
import com.example.gr8math.Data.Repository.AssessmentRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

sealed class AssessmentState {
    object Idle : AssessmentState()
    object Loading : AssessmentState()
    object Success : AssessmentState()
    data class Error(val message: String) : AssessmentState()
}

sealed class AssessmentFetchState {
    object Loading : AssessmentFetchState()
    data class Success(val questions: List<UiQuestion>) : AssessmentFetchState()
    data class Error(val message: String) : AssessmentFetchState()
}

class AssessmentViewModel : ViewModel() {

    private val repository = AssessmentRepository()

    private val _state = MutableLiveData<AssessmentState>(AssessmentState.Idle)
    val state: LiveData<AssessmentState> = _state

    private val _fetchState = MutableLiveData<AssessmentFetchState>()
    val fetchState: LiveData<AssessmentFetchState> = _fetchState

    fun loadExistingAssessment(assessmentId: Int) {
        _fetchState.value = AssessmentFetchState.Loading
        viewModelScope.launch {
            val result = repository.getAssessmentQuestions(assessmentId)
            result.onSuccess {
                _fetchState.value = AssessmentFetchState.Success(it)
            }.onFailure {
                _fetchState.value = AssessmentFetchState.Error(it.message ?: "Failed to load")
            }
        }
    }

    fun publishAssessment(
        courseId: Int, title: String, rawStartTime: String, rawEndTime: String,
        assessmentNumber: Int, assessmentQuarter: Int, questions: List<UiQuestion>
    ) {
        _state.value = AssessmentState.Loading
        val startTime = convertToIso(rawStartTime)
        val endTime = convertToIso(rawEndTime)

        if (startTime == null || endTime == null) {
            _state.value = AssessmentState.Error("Invalid date format.")
            return
        }

        val metaData = AssessmentInsert(courseId, title, startTime, endTime, questions.size, assessmentNumber, assessmentQuarter)

        viewModelScope.launch {
            val result = repository.createAssessment(metaData, questions)
            if (result.isSuccess) _state.value = AssessmentState.Success
            else _state.value = AssessmentState.Error("Failed to publish")
        }
    }


    fun updateAssessment(
        assessmentId: Int, title: String, rawStartTime: String, rawEndTime: String,
        assessmentNumber: Int, assessmentQuarter: Int, questions: List<UiQuestion>
    ) {
        _state.value = AssessmentState.Loading
        val startTime = convertToIso(rawStartTime)
        val endTime = convertToIso(rawEndTime)

        if (startTime == null || endTime == null) {
            _state.value = AssessmentState.Error("Invalid date format.")
            return
        }

        val updateData = AssessmentUpdate(title, startTime, endTime, questions.size, assessmentNumber, assessmentQuarter)

        viewModelScope.launch {
            val result = repository.updateAssessment(assessmentId, updateData, questions)
            if (result.isSuccess) _state.value = AssessmentState.Success
            else _state.value = AssessmentState.Error("Failed to update")
        }
    }

    private fun convertToIso(raw: String): String? {
        return try {
            val inputFormat = SimpleDateFormat("MM/dd/yy - hh:mm a", Locale.US)
            val date = inputFormat.parse(raw) ?: return null
            val outputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            outputFormat.timeZone = TimeZone.getTimeZone("UTC")
            outputFormat.format(date)
        } catch (e: Exception) { null }
    }
}