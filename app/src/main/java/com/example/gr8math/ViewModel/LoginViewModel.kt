package com.example.gr8math.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gr8math.Data.Model.UserProfile
import com.example.gr8math.Data.Repository.AuthRepository
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    fun login(email: String, pass: String) {

        val emailError = if (email.isBlank()) "Please enter your account credentials." else null
        val passError = if (pass.isBlank()) "Please enter your account credentials." else null

        if (emailError != null || passError != null) {
            _loginState.value = LoginState.InputError(emailError, passError)
            return
        }

        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            val result = repository.login(email, pass)

            result.onSuccess { user ->
                _loginState.value = LoginState.Success(user)
            }

            result.onFailure { error ->
                _loginState.value = LoginState.Error(error.message ?: "Login Failed")
            }
        }
    }

}

sealed class LoginState {
    object Loading : LoginState()
    data class Success(val user: UserProfile) : LoginState()
    data class ShowTerms(val user: UserProfile) : LoginState()
    data class Error(val message: String) : LoginState()
    data class InputError(val emailMsg: String?, val passMsg: String?) : LoginState()
}