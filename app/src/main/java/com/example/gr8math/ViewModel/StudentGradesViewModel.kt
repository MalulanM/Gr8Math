package com.example.gr8math.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gr8math.Data.Model.StudentScore
import com.example.gr8math.Data.Repository.StudentGradesRepository
import com.example.gr8math.Model.CurrentCourse
import kotlinx.coroutines.launch

sealed class GradesState {
    object Loading : GradesState()
    data class Success(val data: List<StudentScore>) : GradesState()
    data class Error(val message: String) : GradesState()
}

class StudentGradesViewModel : ViewModel() {

    private val repository = StudentGradesRepository()
    private val _state = MutableLiveData<GradesState>()
    val state: LiveData<GradesState> = _state

    fun loadGrades() {
        // 1. Set Loading State (Important for UI feedback)
        _state.value = GradesState.Loading

        // 2. Launch Coroutine
        viewModelScope.launch {
            // 3. Fetch data using CurrentCourse singleton
            val result = repository.getStudentGrades(CurrentCourse.userId, CurrentCourse.courseId)

            // 4. Handle Result
            result.onSuccess { list ->
                _state.value = GradesState.Success(list)
            }.onFailure {
                _state.value = GradesState.Error(it.message ?: "Failed to load grades")
            }
        }
    }
}