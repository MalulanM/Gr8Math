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

    // --- FIX IS HERE ---
    fun onAssessmentClicked(assessmentId: Int, studentId: Int = -1) {
        viewModelScope.launch {
            try {
                Log.d("DEBUG_VM", "Checking Assessment: $assessmentId. Incoming StudentID: $studentId")

                // 1. RESOLVE STUDENT ID
                // FIX: Check if ID is > 0. If it is 0 (from notification default), we MUST fetch it.
                val validStudentId = if (studentId > 0) {
                    studentId
                } else {
                    Log.d("DEBUG_VM", "Invalid StudentID ($studentId). Fetching from DB for UserID: ${CurrentCourse.userId}")
                    val fetchedId = repository.getStudentIdByUserId(CurrentCourse.userId)
                    Log.d("DEBUG_VM", "Resolved StudentID: $fetchedId")
                    fetchedId
                }

                if (validStudentId == null) {
                    Log.e("DEBUG_VM", "Could not find Student ID. Opening detail page.")
                    _navEvent.value = StudentNavEvent.ToAssessmentDetail(assessmentId)
                    return@launch
                }

                // 2. CHECK RECORD
                // Now we query with the CORRECT Student ID (e.g., 25), not 0.
                val hasRecord = repository.hasAssessmentRecord(validStudentId, assessmentId)
                Log.d("DEBUG_VM", "Has Record: $hasRecord")

                // 3. NAVIGATE
                if (hasRecord) {
                    _navEvent.value = StudentNavEvent.ToAssessmentResult(assessmentId)
                } else {
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