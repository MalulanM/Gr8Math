package com.example.gr8math.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gr8math.Data.Model.QuarterlyReportData
import com.example.gr8math.Data.Repository.QuarterlyReportRepository
import com.example.gr8math.Model.CurrentCourse
import kotlinx.coroutines.launch

sealed class ReportState {
    object Loading : ReportState()
    data class Success(val data: QuarterlyReportData) : ReportState()
    data class Error(val message: String) : ReportState()
}

class QuarterlyReportViewModel : ViewModel() {

    private val repository = QuarterlyReportRepository()
    private val _state = MutableLiveData<ReportState>()
    val state: LiveData<ReportState> = _state

    fun loadReport(studentId: Int) {
        val courseId = CurrentCourse.courseId
        val targetQuarter = 1

        _state.value = ReportState.Loading
        viewModelScope.launch {
            val result = repository.getQuarterlyReport(courseId, studentId, targetQuarter)
            result.onSuccess { data ->
                _state.value = ReportState.Success(data)
            }.onFailure {
                _state.value = ReportState.Error(it.message ?: "Failed to load report")
            }
        }
    }
}