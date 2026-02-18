package com.example.gr8math.Activity.TeacherModule.Profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gr8math.Data.Repository.TeacherAchievementEntity
import com.example.gr8math.Data.Repository.TeacherProfileData
import com.example.gr8math.Data.Repository.TeacherProfileRepository
import com.example.gr8math.Model.CurrentCourse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TeacherProfileViewModel : ViewModel() {

    private val repository = TeacherProfileRepository()
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState

    var currentTeacherId: Int? = null

    fun loadProfile(userId: Int) {
        _uiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            repository.getProfileData(userId).onSuccess { data ->
                currentTeacherId = data.teacher?.id
                _uiState.value = ProfileUiState.Success(data)
            }.onFailure { exception ->
                _uiState.value = ProfileUiState.Error(exception.message ?: "Failed to load profile")
            }
        }
    }

    fun updateUserProfile(userId: Int, dbColumn: String, value: String) {
        _uiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            if (repository.updateUserField(userId, dbColumn, value).isSuccess) loadProfile(userId)
            else _uiState.value = ProfileUiState.Error("Failed to update user profile")
        }
    }

    fun updateTeacherProfile(userId: Int, dbColumn: String, value: String) {
        _uiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            currentTeacherId?.let { tId ->
                if (repository.updateTeacherField(tId, dbColumn, value).isSuccess) loadProfile(userId)
                else _uiState.value = ProfileUiState.Error("Failed to update teacher profile")
            }
        }
    }

    fun uploadAndUpdateProfilePicture(userId: Int, imageBytes: ByteArray, mimeType: String) {
        _uiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            repository.uploadProfilePicture(userId, imageBytes, mimeType).onSuccess { publicUrl ->
                if (repository.updateUserField(userId, "profile_pic", publicUrl).isSuccess) loadProfile(userId)
                else _uiState.value = ProfileUiState.Error("Failed to link image to profile")
            }.onFailure { exception ->
                _uiState.value = ProfileUiState.Error(exception.message ?: "Failed to upload image")
            }
        }
    }

    fun addAchievement(userId: Int, description: String, dateAcquired: String, imageBytes: ByteArray?, mimeType: String?) {
        _uiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            // Check if the user actually has a valid Teacher ID in the database
            if (currentTeacherId == null) {
                _uiState.value = ProfileUiState.Error("Error: No teacher record found for this user.")
                return@launch
            }

            currentTeacherId?.let { tId ->
                var uploadedCertUrl: String? = null

                // 1. If an image was selected, upload it to Storage first
                if (imageBytes != null && mimeType != null) {
                    val uploadResult = repository.uploadCertificate(userId, imageBytes, mimeType)
                    if (uploadResult.isSuccess) {
                        uploadedCertUrl = uploadResult.getOrNull()
                    } else {
                        // 🌟 Show exact storage error
                        val errorMsg = uploadResult.exceptionOrNull()?.message ?: "Unknown storage error"
                        _uiState.value = ProfileUiState.Error("Upload failed: $errorMsg")
                        return@launch
                    }
                }

                // 2. Save the Database Record
                val achievement = TeacherAchievementEntity(
                    teacherId = tId,
                    achievementDesc = description,
                    dateAcquired = dateAcquired,
                    certificate = uploadedCertUrl
                )

                val dbResult = repository.addAchievement(achievement)
                if (dbResult.isSuccess) {
                    loadProfile(userId)
                } else {
                    val errorMsg = dbResult.exceptionOrNull()?.message ?: "Unknown DB error"
                    _uiState.value = ProfileUiState.Error("DB Error: $errorMsg")
                }
            }
        }
    }

    fun deleteAchievement(achievementId: Int, imageUrl: String?) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            val result = repository.deleteAchievement(achievementId, imageUrl)

            if (result.isSuccess) {
                loadProfile(CurrentCourse.userId)
            } else {
                _uiState.value = ProfileUiState.Error("Failed to delete")
            }
        }
    }
}


sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val data: TeacherProfileData) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}