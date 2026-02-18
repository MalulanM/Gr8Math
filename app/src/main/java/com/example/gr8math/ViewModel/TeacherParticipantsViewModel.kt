package com.example.gr8math.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gr8math.Data.Model.Participant
import com.example.gr8math.Data.Repository.TeacherParticipantsRepository
import kotlinx.coroutines.launch

sealed class ParticipantsState {
    object Loading : ParticipantsState()
    data class Success(val data: List<Participant>) : ParticipantsState()
    data class Error(val message: String) : ParticipantsState()
}

class ParticipantsViewModel : ViewModel() {

    private val repository = TeacherParticipantsRepository()
    private val _state = MutableLiveData<ParticipantsState>()
    val state: LiveData<ParticipantsState> = _state

    fun loadParticipants(courseId: Int) {
        _state.value = ParticipantsState.Loading
        viewModelScope.launch {
            val result = repository.getStudents(courseId)
            result.onSuccess { list ->
                _state.value = ParticipantsState.Success(list)
            }.onFailure {
                _state.value = ParticipantsState.Error(it.message ?: "Failed to load participants")
            }
        }
    }
}