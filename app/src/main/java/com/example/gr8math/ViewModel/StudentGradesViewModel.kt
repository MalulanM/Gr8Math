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
    //  1. Add studentId to the Success state
    data class Success(val studentId: Int, val data: List<StudentScore>) : GradesState()
    data class Error(val message: String) : GradesState()
}

class StudentGradesViewModel : ViewModel() {

    private val repository = StudentGradesRepository()
    private val _state = MutableLiveData<GradesState>()
    val state: LiveData<GradesState> = _state

    fun loadGrades() {
        _state.value = GradesState.Loading

        viewModelScope.launch {
            val result = repository.getStudentGrades(CurrentCourse.userId, CurrentCourse.courseId)

            //  2. Handle the Pair result
            result.onSuccess { pair ->
                val fetchedStudentId = pair.first
                val list = pair.second
                _state.value = GradesState.Success(fetchedStudentId, list)
            }.onFailure {
                _state.value = GradesState.Error(it.message ?: "Failed to load grades")
            }
        }
    }
}