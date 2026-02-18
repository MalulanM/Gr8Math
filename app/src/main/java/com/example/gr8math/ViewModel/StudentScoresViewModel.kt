package com.example.gr8math.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gr8math.Data.Model.StudentScore
import com.example.gr8math.Data.Repository.ScoreRepository
import kotlinx.coroutines.launch

sealed class ScoreState {
    object Loading : ScoreState()
    data class Success(val data: List<StudentScore>) : ScoreState()
    data class Error(val message: String) : ScoreState()
}

class StudentScoresViewModel : ViewModel() {

    private val repository = ScoreRepository()
    private val _state = MutableLiveData<ScoreState>()
    val state: LiveData<ScoreState> = _state

    fun loadScores(courseId: Int, studentId: Int) {
        _state.value = ScoreState.Loading
        viewModelScope.launch {
            val result = repository.getStudentScores(courseId, studentId)
            result.onSuccess { list ->
                _state.value = ScoreState.Success(list)
            }.onFailure {
                _state.value = ScoreState.Error(it.message ?: "Failed to load scores")
            }
        }
    }
}