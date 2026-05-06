package com.example.gr8math.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gr8math.Data.Repository.ClassPageRepository
import com.example.gr8math.Model.CurrentCourse
import kotlinx.coroutines.launch

class StudentClassPageViewModel : ViewModel() {

    private val repository = ClassPageRepository()

    private val _contentState = MutableLiveData<ContentState>()
    val contentState: LiveData<ContentState> = _contentState

    private val _navEvent = MutableLiveData<StudentNavEvent?>()
    val navEvent: LiveData<StudentNavEvent?> = _navEvent
    private val _sectionName = MutableLiveData<String>()
    val sectionName: LiveData<String> = _sectionName

    fun fetchSectionName(courseId: Int) {
        viewModelScope.launch {
            val name = repository.getSectionNameByCourseId(courseId)
            if (!name.isNullOrEmpty()) {
                _sectionName.postValue(name)
            } else {
                _sectionName.postValue("Class Detail")
            }
        }
    }

    fun loadContent(forceReload: Boolean = false) {
        val courseId = CurrentCourse.courseId
        if (courseId == 0) {
            _contentState.value = ContentState.Error("Invalid Course ID")
            return
        }

        if (!forceReload && _contentState.value is ContentState.Success) return

        _contentState.value = ContentState.Loading

        viewModelScope.launch {
            // 1. Check if class exists first
            val name = repository.getSectionNameByCourseId(courseId)
            if (name == null) {
                // This triggers the specific UI state in the Activity
                _contentState.postValue(ContentState.Error("CLASS_DELETED"))
                return@launch
            }

            // 2. Update name and get content
            _sectionName.postValue(name)
            val result = repository.getClassContent(courseId)
            result.onSuccess { list ->
                _contentState.value = ContentState.Success(list)
            }.onFailure {
                _contentState.value = ContentState.Error(it.message ?: "Failed to load content")
            }
        }
    }

    fun onAssessmentClicked(assessmentId: Int, studentId: Int = -1) {
        viewModelScope.launch {
            try {
                val validStudentId = if (studentId > 0) {
                    studentId
                } else {
                    repository.getStudentIdByUserId(CurrentCourse.userId)
                }

                if (validStudentId == null) {
                    _navEvent.value = StudentNavEvent.ToAssessmentDetail(assessmentId)
                    return@launch
                }

                val statusResult = repository.checkAssessmentAvailability(validStudentId, assessmentId)

                statusResult.onSuccess { status ->
                    when (status) {
                        com.example.gr8math.Data.Model.AssessmentStatus.HAS_RECORD,
                        com.example.gr8math.Data.Model.AssessmentStatus.DEADLINE_PASSED -> {
                            _navEvent.value = StudentNavEvent.ToAssessmentResult(assessmentId)
                        }
                        com.example.gr8math.Data.Model.AssessmentStatus.AVAILABLE -> {
                            _navEvent.value = StudentNavEvent.ToAssessmentDetail(assessmentId)
                        }
                    }
                }.onFailure {
                    _navEvent.value = StudentNavEvent.ToAssessmentDetail(assessmentId)
                }

            } catch (e: Exception) {
                _navEvent.value = StudentNavEvent.ToAssessmentDetail(assessmentId)
            }
        }
    }

    fun clearNavEvent() {
        _navEvent.value = null
    }
}