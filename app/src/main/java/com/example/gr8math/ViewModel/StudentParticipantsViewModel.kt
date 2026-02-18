package com.example.gr8math.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gr8math.Data.Model.ParticipantsStateData
import com.example.gr8math.Data.Repository.StudentParticipantsRepository
import com.example.gr8math.Model.CurrentCourse
import kotlinx.coroutines.launch

// 1. Define the Parent Sealed Class
sealed class StudentParticipantsState {
    // 2. Children MUST inherit from 'StudentParticipantsState'
    object Loading : StudentParticipantsState()
    data class Success(val data: ParticipantsStateData) : StudentParticipantsState()
    data class Error(val message: String) : StudentParticipantsState()
}

class StudentParticipantsViewModel : ViewModel() {

    private val repository = StudentParticipantsRepository()

    // 3. LiveData must hold 'StudentParticipantsState'
    private val _state = MutableLiveData<StudentParticipantsState>()
    val state: LiveData<StudentParticipantsState> = _state

    fun loadParticipants() {
        val courseId = CurrentCourse.courseId
        if (courseId == 0) return

        // 4. Use 'StudentParticipantsState' when setting values
        _state.value = StudentParticipantsState.Loading

        viewModelScope.launch {
            val result = repository.getParticipants(courseId)
            result.onSuccess { data ->
                _state.value = StudentParticipantsState.Success(data)
            }.onFailure {
                _state.value = StudentParticipantsState.Error(it.message ?: "Failed to load participants")
            }
        }
    }
}