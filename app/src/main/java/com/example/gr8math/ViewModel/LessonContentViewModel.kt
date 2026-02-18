package com.example.gr8math.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gr8math.Data.Model.LessonEntity
import com.example.gr8math.Data.Model.LessonInsert
import com.example.gr8math.Data.Repository.LessonRepository
import kotlinx.coroutines.launch

sealed class LessonState {
    object Idle : LessonState()
    object Loading : LessonState()
    object Saved : LessonState() // Success for Save/Update

    data class Error(val message: String) : LessonState()
    data class ContentLoaded(val lesson: LessonEntity) : LessonState()
}

class LessonContentViewModel : ViewModel() {

    private val repository = LessonRepository()
    private val _state = MutableLiveData<LessonState>(LessonState.Idle)
    val state: LiveData<LessonState> = _state

    fun loadLesson(lessonId: Int) {
        _state.value = LessonState.Loading
        viewModelScope.launch {
            val result = repository.getLesson(lessonId)
            result.onSuccess { lesson ->
                // Pass the whole lesson object
                _state.value = LessonState.ContentLoaded(lesson)
            }.onFailure {
                _state.value = LessonState.Error("Failed to load lesson: ${it.message}")
            }
        }
    }

    fun saveLesson(courseId: Int, weekNumber: String, title: String, content: String) {
        if (content.isBlank()) {
            _state.value = LessonState.Error("Cannot save an empty lesson")
            return
        }

        val weekInt = weekNumber.toIntOrNull() ?: 0 // Handle conversion safely

        val lessonData = LessonInsert(
            courseId = courseId,
            weekNumber = weekInt,
            lessonTitle = title,
            lessonContent = content
        )

        _state.value = LessonState.Loading
        viewModelScope.launch {
            val result = repository.createLesson(lessonData)
            result.onSuccess {
                _state.value = LessonState.Saved
            }.onFailure {
                _state.value = LessonState.Error("Save failed: ${it.message}")
            }
        }
    }

    fun updateLesson(lessonId: Int, courseId: Int, weekNumber: String, title: String, content: String) {
        if (content.isBlank()) {
            _state.value = LessonState.Error("Cannot save an empty lesson")
            return
        }

        val weekInt = weekNumber.toIntOrNull() ?: 0

        val lessonData = LessonInsert(
            courseId = courseId,
            weekNumber = weekInt,
            lessonTitle = title,
            lessonContent = content
        )

        _state.value = LessonState.Loading
        viewModelScope.launch {
            val result = repository.updateLesson(lessonId, lessonData)
            result.onSuccess {
                _state.value = LessonState.Saved
            }.onFailure {
                _state.value = LessonState.Error("Update failed: ${it.message}")
            }
        }
    }
}