package com.example.gr8math.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gr8math.Data.Model.StudentNotificationUI
import com.example.gr8math.Data.Model.TeacherNotificationUI
import com.example.gr8math.Data.Repository.NotificationRepository
import kotlinx.coroutines.launch

// State for TEACHERS
sealed class NotifState {
    object Loading : NotifState()
    data class Success(val data: List<TeacherNotificationUI>) : NotifState()
    data class Error(val message: String) : NotifState()
}

// State for STUDENTS (New)
sealed class StudentNotifState {
    object Loading : StudentNotifState()
    data class Success(val data: List<StudentNotificationUI>) : StudentNotifState()
    data class Error(val message: String) : StudentNotifState()
}

class NotificationsViewModel : ViewModel() {

    private val repository = NotificationRepository()

    // --- Teacher LiveData ---
    private val _teacherState = MutableLiveData<NotifState>()
    val teacherState: LiveData<NotifState> = _teacherState

    // --- Student LiveData ---
    private val _studentState = MutableLiveData<StudentNotifState>()
    val studentState: LiveData<StudentNotifState> = _studentState

    // 1. Load Teacher Data
    fun loadTeacherNotifications(userId: Int, courseId: Int) {
        _teacherState.value = NotifState.Loading
        viewModelScope.launch {
            val result = repository.getTeacherNotifications(userId, courseId)
            result.onSuccess { list ->
                _teacherState.value = NotifState.Success(list)
            }.onFailure {
                _teacherState.value = NotifState.Error(it.message ?: "Failed to load")
            }
        }
    }

    // 2. Load Student Data
    fun loadStudentNotifications(userId: Int, courseId: Int) {
        _studentState.value = StudentNotifState.Loading
        viewModelScope.launch {
            // Ensure your Repository has getStudentNotifications (from previous step)
            val result = repository.getStudentNotifications(userId, courseId)
            result.onSuccess { list ->
                _studentState.value = StudentNotifState.Success(list)
            }.onFailure {
                _studentState.value = StudentNotifState.Error(it.message ?: "Failed to load")
            }
        }
    }

    // 3. Shared Actions (Mark as Read is same for both)
    fun markRead(id: Int) {
        viewModelScope.launch { repository.markAsRead(id) }
    }

    fun markAllRead(ids: List<Int>) {
        viewModelScope.launch { repository.markAllAsRead(ids) }
    }
}