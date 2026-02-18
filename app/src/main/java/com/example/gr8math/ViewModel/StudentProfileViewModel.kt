package com.example.gr8math.Activity.StudentModule.Profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gr8math.Data.Repository.StudentProfileData
import com.example.gr8math.Data.Repository.StudentProfileRepository
import com.example.gr8math.ViewModel.ProfileUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StudentProfileViewModel : ViewModel() {
    private val repo = StudentProfileRepository()
    private val _uiState = MutableStateFlow<ProfileUiState<StudentProfileData>>(ProfileUiState.Loading)
    val uiState = _uiState.asStateFlow()

    fun loadProfile(userId: Int) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            repo.getStudentProfile(userId)
                .onSuccess { _uiState.value = ProfileUiState.Success(it) }
                .onFailure { _uiState.value = ProfileUiState.Error(it.message ?: "Failed to load profile") }
        }
    }

    fun updateProfilePic(userId: Int, bytes: ByteArray, mime: String) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            repo.uploadToTigris(userId, bytes, mime).onSuccess { url ->
                // Save the new URL to the user table
                repo.updateField(userId, "user", "profile_pic", url).onSuccess {
                    loadProfile(userId) // Refresh to clear loading state
                }
            }.onFailure {
                _uiState.value = ProfileUiState.Error("Failed to upload profile picture")
            }
        }
    }

    fun updateField(userId: Int, table: String, field: String, value: String) {
        viewModelScope.launch {
            repo.updateField(userId, table, field, value).onFailure {
                _uiState.value = ProfileUiState.Error("Failed to update $field")
            }
        }
    }

    fun updateDisplayedBadges(studentId: Int, selectedBadgeIds: List<Int>, userId: Int) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading

            repo.updateBadgeRanks(studentId, selectedBadgeIds).onSuccess {
                loadProfile(userId)
            }.onFailure {
                _uiState.value = ProfileUiState.Error("Failed to update badges.")
            }
        }
    }
}