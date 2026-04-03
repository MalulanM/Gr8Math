package com.example.gr8math.Activity.TeacherModule.DLL

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

enum class DayKey { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY }

class DllEditorViewModel : ViewModel() {

    // --- TOGGLES ---
    val isCsWeekly = MutableStateFlow(false)
    val isPsWeekly = MutableStateFlow(false)

    // --- PART 1: Objectives ---
    val contentStandards = MutableStateFlow(emptyWeekMap())
    val performanceStandards = MutableStateFlow(emptyWeekMap())
    val learningCompetencies = MutableStateFlow(emptyWeekMap())

    // --- PART 2: Resources ---
    val teacherGuide = MutableStateFlow(emptyWeekMap())
    val learnerMaterials = MutableStateFlow(emptyWeekMap())
    val textbookPages = MutableStateFlow(emptyWeekMap())
    val additionalMaterials = MutableStateFlow(emptyWeekMap())
    val otherReferences = MutableStateFlow(emptyWeekMap())

    // --- PART 3: Procedures ---
    val procedures = MutableStateFlow(
        mutableMapOf(
            "A" to emptyWeekMap(), "B" to emptyWeekMap(), "C" to emptyWeekMap(),
            "D" to emptyWeekMap(), "E" to emptyWeekMap(), "F" to emptyWeekMap(),
            "G" to emptyWeekMap(), "H" to emptyWeekMap(), "I" to emptyWeekMap(),
            "J" to emptyWeekMap()
        )
    )

    // --- PART 4: Reflection ---
    val remarks = MutableStateFlow("")
    val reflection = MutableStateFlow(
        mutableMapOf("A" to "", "B" to "", "C" to "", "D" to "", "E" to "", "F" to "", "G" to "")
    )

    private fun emptyWeekMap(): MutableMap<DayKey, String> {
        return mutableMapOf(
            DayKey.MONDAY to "", DayKey.TUESDAY to "", DayKey.WEDNESDAY to "",
            DayKey.THURSDAY to "", DayKey.FRIDAY to ""
        )
    }

    // Helper to update deeply nested Maps (like Procedures)
    fun updateProcedure(procKey: String, day: DayKey, text: String) {
        val currentMap = procedures.value.toMutableMap()
        val currentWeek = currentMap[procKey]?.toMutableMap() ?: emptyWeekMap()
        currentWeek[day] = text
        currentMap[procKey] = currentWeek
        procedures.value = currentMap
    }

    fun updateReflection(refKey: String, text: String) {
        val currentMap = reflection.value.toMutableMap()
        currentMap[refKey] = text
        reflection.value = currentMap
    }
}