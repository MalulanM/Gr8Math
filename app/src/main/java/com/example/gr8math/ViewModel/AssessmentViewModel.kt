package com.example.gr8math.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gr8math.Data.Model.AssessmentInsert
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

class AssessmentViewModel : ViewModel() {

    private val repository = AssessmentRepository()
    private val _state = MutableLiveData<AssessmentState>(AssessmentState.Idle)
    val state: LiveData<AssessmentState> = _state

    fun publishAssessment(
        courseId: Int,
        title: String,
        rawStartTime: String,
        rawEndTime: String,
        assessmentNumber: Int,
        assessmentQuarter: Int,
        questions: List<UiQuestion>
    ) {
        _state.value = AssessmentState.Loading

        // 1. Format Dates to ISO (Supabase requires YYYY-MM-DD HH:MM:SS)
        val startTime = convertToIso(rawStartTime)
        val endTime = convertToIso(rawEndTime)

        if (startTime == null || endTime == null) {
            _state.value = AssessmentState.Error("Invalid date format.")
            return
        }

        // 2. Prepare Data
        val metaData = AssessmentInsert(
            courseId = courseId,
            title = title,
            startTime = startTime,
            endTime = endTime,
            assessmentItems = questions.size,
            assessmentNumber = assessmentNumber,
            assessmentQuarter = assessmentQuarter
        )

        // 3. Launch Repository Call
        viewModelScope.launch {
            val result = repository.createAssessment(metaData, questions)
            result.onSuccess {
                _state.value = AssessmentState.Success
            }.onFailure {
                _state.value = AssessmentState.Error(it.message ?: "Failed to publish")
            }
        }
    }

    private fun convertToIso(raw: String): String? {
        return try {
            val inputFormat = SimpleDateFormat("MM/dd/yy - hh:mm a", Locale.US)
            val date = inputFormat.parse(raw) ?: return null

            val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            outputFormat.format(date)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}