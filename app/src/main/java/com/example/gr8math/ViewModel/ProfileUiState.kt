package com.example.gr8math.ViewModel

sealed class ProfileUiState<out T> {
    object Loading : ProfileUiState<Nothing>()
    data class Success<T>(val data: T) : ProfileUiState<T>()
    data class Error(val message: String) : ProfileUiState<Nothing>()
}