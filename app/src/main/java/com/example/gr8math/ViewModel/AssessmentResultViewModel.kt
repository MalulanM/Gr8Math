package com.example.gr8math.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gr8math.Data.Model.AssessmentResultUiModel
import com.example.gr8math.Data.Repository.AssessmentResultRepository
import com.example.gr8math.Model.CurrentCourse
import kotlinx.coroutines.launch

sealed class ResultState {
    object Loading : ResultState()
    data class Success(val data: AssessmentResultUiModel) : ResultState()
    data class Error(val message: String) : ResultState()
}

class AssessmentResultViewModel : ViewModel() {

    private val repository = AssessmentResultRepository()
    private val _state = MutableLiveData<ResultState>()
    val state: LiveData<ResultState> = _state

    fun loadResult(assessmentId: Int) {
        val userId = CurrentCourse.userId
        _state.value = ResultState.Loading

        viewModelScope.launch {
            val result = repository.getAssessmentResult(userId, assessmentId)
            result.onSuccess { data ->
                _state.value = ResultState.Success(data)
            }.onFailure {
                _state.value = ResultState.Error(it.message ?: "Failed to load results")
            }
        }
    }
}