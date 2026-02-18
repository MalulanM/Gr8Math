package com.example.gr8math.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gr8math.Data.Model.AssessmentFullDetails
import com.example.gr8math.Data.Repository.AssessmentDetailRepository
import com.google.gson.Gson // Using Gson to match your existing TakeAssessment logic
import kotlinx.coroutines.launch

sealed class AssessmentDetailState {
    object Loading : AssessmentDetailState()
    data class Success(val assessment: AssessmentFullDetails, val jsonString: String) : AssessmentDetailState()
    data class Error(val message: String) : AssessmentDetailState()
}

class AssessmentDetailViewModel : ViewModel() {

    private val repository = AssessmentDetailRepository()
    private val _state = MutableLiveData<AssessmentDetailState>()
    val state: LiveData<AssessmentDetailState> = _state

    fun loadAssessment(id: Int) {
        _state.value = AssessmentDetailState.Loading
        viewModelScope.launch {
            val result = repository.getAssessmentDetails(id)
            result.onSuccess { assessment ->
                // Convert to JSON string for the intent (Legacy support for TakeAssessmentActivity)
                // We use Gson here because your existing TakeAssessmentActivity likely uses Gson
                val json = Gson().toJson(assessment)
                _state.value = AssessmentDetailState.Success(assessment, json)
            }.onFailure {
                _state.value = AssessmentDetailState.Error(it.message ?: "Failed to load assessment")
            }
        }
    }
}