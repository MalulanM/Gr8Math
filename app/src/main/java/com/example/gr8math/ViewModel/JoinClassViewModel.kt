package com.example.gr8math.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gr8math.Data.Model.JoinClassSuccess
import com.example.gr8math.Data.Repository.JoinClassRepository
import kotlinx.coroutines.launch

sealed class JoinClassState {
    object Idle : JoinClassState()
    object Loading : JoinClassState()
    data class Success(val data: JoinClassSuccess) : JoinClassState()
    data class Error(val message: String) : JoinClassState()
}

class JoinClassViewModel : ViewModel() {

    private val repository = JoinClassRepository()
    private val _state = MutableLiveData<JoinClassState>(JoinClassState.Idle)
    val state: LiveData<JoinClassState> = _state

    fun joinClass(userId: Int, classCode: String) {
        if (classCode.isBlank()) {
            _state.value = JoinClassState.Error("Enter code before submitting")
            return
        }

        _state.value = JoinClassState.Loading
        viewModelScope.launch {
            val result = repository.joinClass(userId, classCode)
            result.onSuccess { successData ->
                _state.value = JoinClassState.Success(successData)
            }.onFailure { error ->
                _state.value = JoinClassState.Error(error.message ?: "Failed to join class")
            }
        }
    }

    // Reset state if needed (e.g. after error dialog)
    fun resetState() {
        _state.value = JoinClassState.Idle
    }
}