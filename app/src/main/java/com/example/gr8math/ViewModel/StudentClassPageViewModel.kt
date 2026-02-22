package com.example.gr8math.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gr8math.Data.Repository.ClassPageRepository
import com.example.gr8math.Model.CurrentCourse
import com.example.gr8math.Services.SupabaseService
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch

// ... (ContentState and StudentNavEvent can stay in ClassPageStates.kt or here)

class StudentClassPageViewModel : ViewModel() {

    private val repository = ClassPageRepository()

    private val _contentState = MutableLiveData<ContentState>()
    val contentState: LiveData<ContentState> = _contentState

    private val _navEvent = MutableLiveData<StudentNavEvent?>()
    val navEvent: LiveData<StudentNavEvent?> = _navEvent

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
                Log.d("DEBUG_VM", "Checking Assessment: $assessmentId. Incoming StudentID: $studentId")

                // 1. RESOLVE STUDENT ID
                val validStudentId = if (studentId > 0) {
                    studentId
                } else {
                    Log.d("DEBUG_VM", "Invalid StudentID ($studentId). Fetching from DB...")
                    val fetchedId = repository.getStudentIdByUserId(CurrentCourse.userId)
                    Log.d("DEBUG_VM", "Resolved StudentID: $fetchedId")
                    fetchedId
                }

                if (validStudentId == null) {
                    Log.e("DEBUG_VM", "Could not find Student ID. Opening detail page.")
                    _navEvent.value = StudentNavEvent.ToAssessmentDetail(assessmentId)
                    return@launch
                }

                // We use checkAssessmentAvailability instead of hasAssessmentRecord
                val statusResult = repository.checkAssessmentAvailability(validStudentId, assessmentId)

                statusResult.onSuccess { status ->
                    Log.d("DEBUG_VM", "Assessment Status: $status")

                    when (status) {
                        com.example.gr8math.Data.Model.AssessmentStatus.HAS_RECORD,
                        com.example.gr8math.Data.Model.AssessmentStatus.DEADLINE_PASSED -> {
                            _navEvent.value = StudentNavEvent.ToAssessmentResult(assessmentId)
                        }

                        com.example.gr8math.Data.Model.AssessmentStatus.AVAILABLE -> {
                            _navEvent.value = StudentNavEvent.ToAssessmentDetail(assessmentId)
                        }
                    }
                }.onFailure { error ->
                    Log.e("DEBUG_VM", "Failed to check status", error)
                    _navEvent.value = StudentNavEvent.ToAssessmentDetail(assessmentId)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _navEvent.value = StudentNavEvent.ToAssessmentDetail(assessmentId)
            }
        }
    }

    fun clearNavEvent() {
        _navEvent.value = null
    }


    // Inside StudentClassPageViewModel
    val sectionName = MutableLiveData<String>()

    fun fetchSectionName(courseId: Int) {
        viewModelScope.launch {
            val name = repository.getSectionNameByCourseId(courseId)
            if (name != null) {
                sectionName.postValue(name)
            } else {
                sectionName.postValue("Class Details") // Fallback
            }
        }
    }

}