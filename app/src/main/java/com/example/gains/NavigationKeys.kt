package com.example.gains

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey

@Serializable data class WorkoutLogger(
    val sessionId: Long = 0L,
    val templateId: Long = 0L,
    val isTemplateMode: Boolean = false,
    val isPlannedMode: Boolean = false,
    val plannedId: Long = 0L
) : NavKey

@Serializable data class ExerciseDetail(val exerciseId: Int) : NavKey

