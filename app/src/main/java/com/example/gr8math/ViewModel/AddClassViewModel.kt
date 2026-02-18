package com.example.gr8math.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gr8math.Data.Repository.AddClassRepository
import kotlinx.coroutines.launch

sealed class AddClassState {
    object Idle : AddClassState()
    object Loading : AddClassState()
    data class Success(val classCode: String) : AddClassState()
    data class Error(val message: String) : AddClassState()
}

class AddClassViewModel : ViewModel() {

    private val repository = AddClassRepository()

    private val _state = MutableLiveData<AddClassState>(AddClassState.Idle)
    val state: LiveData<AddClassState> = _state

    fun createClass(
        adviserId: Int,
        section: String,
        studentsStr: String,
        start: String?,
        end: String?
    ) {
        // 1. Basic Validation
        if (section.isBlank() || studentsStr.isBlank() || start.isNullOrEmpty() || end.isNullOrEmpty()) {
            _state.value = AddClassState.Error("Please enter the needed details.")
            return
        }

        val numStudents = studentsStr.toIntOrNull()
        if (numStudents == null || numStudents <= 0) {
            _state.value = AddClassState.Error("Invalid number of students.")
            return
        }

        // 2. Call Repository
        _state.value = AddClassState.Loading
        viewModelScope.launch {
            val result = repository.createClass(
                adviserId = adviserId,
                sectionName = section,
                numStudents = numStudents,
                startTime = start,
                endTime = end
            )

            result.onSuccess { code ->
                _state.value = AddClassState.Success(code)
            }.onFailure { error ->
                _state.value = AddClassState.Error(error.message ?: "Failed to create class")
            }
        }
    }

    fun resetState() {
        _state.value = AddClassState.Idle
    }
}