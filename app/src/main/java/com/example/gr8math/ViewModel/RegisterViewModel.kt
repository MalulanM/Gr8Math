package com.example.gr8math.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gr8math.Data.Model.UserProfile
import com.example.gr8math.Data.Repository.AuthRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {

    private val repository = AuthRepository()

    // Validation States
    private val _emailExists = MutableLiveData<Boolean>()
    val emailExists: LiveData<Boolean> = _emailExists

    private val _lrnExists = MutableLiveData<Boolean>()
    val lrnExists: LiveData<Boolean> = _lrnExists

    // New: Signal to move to next screen if validations pass
    private val _canProceed = MutableLiveData<Boolean>()
    val canProceed: LiveData<Boolean> = _canProceed

    // Registration States
    private val _registerState = MutableLiveData<Result<UserProfile>>()
    val registerState: LiveData<Result<UserProfile>> = _registerState

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // --- NEW VALIDATION FUNCTION ---
    fun validateStudentDetails(email: String, lrn: String) {
        _isLoading.value = true
        _canProceed.value = false // Reset

        viewModelScope.launch {
            // Run checks in parallel for speed
            val emailCheckDeferred = async { repository.checkEmailExists(email) }
            val lrnCheckDeferred = async { repository.checkLrnExists(lrn) }

            val isEmailTaken = emailCheckDeferred.await()
            val isLrnTaken = lrnCheckDeferred.await()

            // Update UI
            _emailExists.value = isEmailTaken
            _lrnExists.value = isLrnTaken
            _isLoading.value = false

            // Only proceed if NEITHER is taken
            if (!isEmailTaken && !isLrnTaken) {
                _canProceed.value = true
            }
        }
    }

    fun checkEmail(email: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val exists = repository.checkEmailExists(email)
            _emailExists.value = exists
            _isLoading.value = false
        }
    }

    // --- REGISTRATION ---
    fun registerStudent(
        email: String, pass: String, first: String, last: String,
        gender: String, birth: String, lrn: String
    ) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.registerStudent(email, pass, first, last, gender, birth, lrn)
            _registerState.value = result
            _isLoading.value = false
        }
    }

    fun registerTeacher(
        email: String, pass: String, first: String, last: String,
        gender: String, birth: String, position: String
    ) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.registerTeacher(email, pass, first, last, gender, birth, position)
            _registerState.value = result
            _isLoading.value = false
        }
    }

    fun updateFirstLogin(userId: Int) {
        viewModelScope.launch {
            repository.updateFirstLoginStatus(userId)
        }
    }
}