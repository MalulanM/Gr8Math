package com.example.gr8math.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gr8math.Data.Repository.ClassPageRepository
import com.example.gr8math.Model.CurrentCourse
import kotlinx.coroutines.launch

class TeacherClassPageViewModel : ViewModel() {

    private val repository = ClassPageRepository()

    private val _state = MutableLiveData<ContentState>()
    val state: LiveData<ContentState> = _state
    private val _sectionName = MutableLiveData<String>()
    val sectionName: LiveData<String> = _sectionName

    fun loadContent(forceReload: Boolean = false) {
        val courseId = CurrentCourse.courseId
        if (courseId == 0) {
            _state.value = ContentState.Error("Invalid Course ID")
            return
        }

        if (!forceReload && _state.value is ContentState.Success) return

        _state.value = ContentState.Loading
        viewModelScope.launch {
            val result = repository.getClassContent(courseId)
            result.onSuccess { list ->
                _state.value = ContentState.Success(list)
            }.onFailure {
                _state.value = ContentState.Error(it.message ?: "Failed to load content")
            }
        }
    }

    fun fetchSectionName(courseId: Int) {
        viewModelScope.launch {
            try {
                val name = repository.getSectionNameByCourseId(courseId)
                if (name != null) {
                    _sectionName.postValue(name)
                } else {
                    _sectionName.postValue("Class Details")
                }
            } catch (e: Exception) {
                _sectionName.postValue("Class Details")
            }
        }
    }
}