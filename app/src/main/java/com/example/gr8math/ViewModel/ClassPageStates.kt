package com.example.gr8math.ViewModel

import com.example.gr8math.Data.Model.ClassContentItem

// Shared UI State for both Student and Teacher
sealed class ContentState {
    object Loading : ContentState()
    data class Success(val data: List<ClassContentItem>) : ContentState()
    data class Error(val message: String) : ContentState()
}

// Navigation Events (Specific to Student, but safe to keep here or in its own file)
sealed class StudentNavEvent {
    data class ToLesson(val id: Int) : StudentNavEvent()
    data class ToAssessmentDetail(val id: Int) : StudentNavEvent()
    data class ToAssessmentResult(val id: Int) : StudentNavEvent()
}