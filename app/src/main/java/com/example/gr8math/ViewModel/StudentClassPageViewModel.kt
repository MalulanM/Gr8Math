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

    fun loadContent() {
        _contentState.value = ContentState.Loading
        viewModelScope.launch {
            val result = repository.getClassContent(CurrentCourse.courseId)
            result.onSuccess {
                _contentState.value = ContentState.Success(it)
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