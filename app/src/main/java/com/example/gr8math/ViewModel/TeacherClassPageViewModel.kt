package com.example.gr8math.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gr8math.Data.Model.ClassContentItem
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
            // 1. Check if class exists first
            val name = repository.getSectionNameByCourseId(courseId)
            if (name == null) {
                _state.value = ContentState.Error("CLASS_DELETED")
                return@launch
            }

            // 2. Update name and get content
            _sectionName.postValue(name)
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
                    // CLASS IS DELETED!
                    _state.postValue(ContentState.Error("CLASS_DELETED"))
                }
            } catch (e: Exception) {
                _state.postValue(ContentState.Error(e.message ?: "Failed to fetch class"))
            }
        }
    }



    fun deleteContent(item: ClassContentItem) {
        viewModelScope.launch {
            val result = when (item) {
                is ClassContentItem.LessonItem -> repository.deleteLesson(item.id)
                is ClassContentItem.AssessmentItem -> repository.deleteAssessment(item.id)
                else -> return@launch
            }

            result.onSuccess {
                // Instantly remove the item from the UI state
                val currentState = _state.value
                if (currentState is ContentState.Success) {
                    val updatedList = currentState.data.filterNot {
                        (it is ClassContentItem.LessonItem && it.id == item.id) ||
                                (it is ClassContentItem.AssessmentItem && it.id == item.id)
                    }
                    _state.value = ContentState.Success(updatedList)
                }
            }
        }
    }
}