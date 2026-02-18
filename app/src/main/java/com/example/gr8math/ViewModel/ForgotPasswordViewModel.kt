package com.example.gr8math.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gr8math.Data.Repository.AuthRepository
import kotlinx.coroutines.launch

class ForgotPasswordViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _state = MutableLiveData<ForgotState>()
    val state: LiveData<ForgotState> = _state

    // Step 1: Send Code
    fun sendCode(email: String) {
        if (email.isBlank()) {
            _state.value = ForgotState.Error("Please enter your email")
            return
        }
        _state.value = ForgotState.Loading
        viewModelScope.launch {
            val result = repository.sendPasswordResetCode(email)
            result.onSuccess {
                _state.value = ForgotState.CodeSent
            }.onFailure {
                _state.value = ForgotState.Error(it.message ?: "Failed to send code")
            }
        }
    }

    // Step 2: Verify Code
    fun verifyCode(email: String, code: String) {
        if (code.isBlank()) {
            _state.value = ForgotState.Error("Please enter the code")
            return
        }
        _state.value = ForgotState.Loading
        viewModelScope.launch {
            val result = repository.verifyRecoveryCode(email, code)
            result.onSuccess {
                _state.value = ForgotState.CodeVerified // Move to next screen
            }.onFailure {
                _state.value = ForgotState.Error("Invalid code or expired")
            }
        }
    }

    // Step 3: Update Password
    fun updatePassword(newPass: String) {
        _state.value = ForgotState.Loading
        viewModelScope.launch {
            val result = repository.updateUserPassword(newPass)
            result.onSuccess {
                _state.value = ForgotState.PasswordUpdated // Done
            }.onFailure {
                _state.value = ForgotState.Error(it.message ?: "Failed to update password")
            }
        }
    }
}

// Simple State Management
sealed class ForgotState {
    object Loading : ForgotState()
    object CodeSent : ForgotState()
    object CodeVerified : ForgotState()
    object PasswordUpdated : ForgotState()
    data class Error(val message: String) : ForgotState()
}