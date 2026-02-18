package com.example.gr8math.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gr8math.Data.Model.ClassUiModel
import com.example.gr8math.Data.Repository.ClassRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

sealed class ClassState {
    object Loading : ClassState()
    object Empty : ClassState()
    data class Success(val data: List<ClassUiModel>) : ClassState()
    data class Error(val message: String) : ClassState()
}

class ClassManagerViewModel : ViewModel() {

    private val repository = ClassRepository()

    private val _classState = MutableLiveData<ClassState>()
    val classState: LiveData<ClassState> = _classState

    private val _searchHistory = MutableLiveData<List<String>>()
    val searchHistory: LiveData<List<String>> = _searchHistory

    private var searchJob: Job? = null

    fun loadClasses(userId: Int, role: String, forceReload: Boolean = false) {
        if (!forceReload && _classState.value is ClassState.Success) {
            return
        }
        _classState.value = ClassState.Loading
        viewModelScope.launch {
            val result = repository.getClasses(userId, role)
            result.onSuccess { rawList ->
                if (rawList.isEmpty()) {
                    _classState.value = ClassState.Empty
                } else {
                    _classState.value = ClassState.Success(mapToUi(rawList))
                }
            }.onFailure {
                _classState.value = ClassState.Error(it.message ?: "Failed to load classes")
            }
        }
    }

    fun searchClasses(userId: Int, role: String, query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // Debounce typing
            if (query.isBlank()) {
                return@launch
            }

            // Optional: Show loading indicator specifically for search
            // _classState.value = ClassState.Loading

            repository.saveSearchHistory(userId, query)

            val result = repository.getClasses(userId, role, query)
            result.onSuccess { rawList ->
                if (rawList.isEmpty()) {
                    _classState.value = ClassState.Empty
                } else {
                    _classState.value = ClassState.Success(mapToUi(rawList))
                }
            }.onFailure {
                _classState.value = ClassState.Error(it.message ?: "Search failed")
            }
        }
    }

    fun loadHistory(userId: Int) {
        viewModelScope.launch {
            val history = repository.getSearchHistory(userId)
            _searchHistory.value = history
        }
    }

    private fun mapToUi(list: List<com.example.gr8math.Data.Model.ClassEntity>): List<ClassUiModel> {
        val inputFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

        return list.map { item ->
            val arrival = item.arrivalTime
            val dismiss = item.dismissalTime

            val schedule = if (arrival != null && dismiss != null) {
                try {
                    "${outputFormat.format(inputFormat.parse(arrival)!!)} - ${outputFormat.format(inputFormat.parse(dismiss)!!)}"
                } catch (e: Exception) { "Time N/A" }
            } else {
                "Time N/A"
            }

            val cId = item.courseContent?.firstOrNull()?.id ?: 0

            val tName = item.adviser?.let { "${it.firstName} ${it.lastName}" } ?: "Unknown Teacher"

            ClassUiModel(
                id = item.id,
                sectionName = item.className,
                schedule = schedule,
                studentCount = item.classSize,
                courseId = cId,
                teacherName = tName
            )
        }
    }
}