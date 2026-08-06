package com.example.gains.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gains.data.DataRepository
import com.example.gains.data.Exercise
import com.example.gains.data.LoggedSet
import com.example.gains.data.LoggedSetWithExercise
import com.example.gains.data.WorkoutSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutLoggerViewModel(
    private val sessionId: Long,
    private val repository: DataRepository
) : ViewModel() {

    val session: StateFlow<WorkoutSession?> = repository.getSessionById(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val exercises: StateFlow<List<Exercise>> = repository.allExercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loggedSets: StateFlow<List<LoggedSetWithExercise>> = repository.getLoggedSetsForSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun finishSession() {
        viewModelScope.launch {
            val currentSession = session.value ?: return@launch
            val updated = currentSession.copy(endTime = System.currentTimeMillis())
            repository.updateSession(updated)
        }
    }

    fun addSet(exerciseId: Int) {
        viewModelScope.launch {
            val currentSetsForExercise = loggedSets.value.filter { it.exerciseId == exerciseId }
            val nextSetNumber = currentSetsForExercise.size + 1
            val lastSet = currentSetsForExercise.lastOrNull()
            
            val defaultWeight = lastSet?.weight ?: 20.0
            val defaultReps = lastSet?.reps ?: 10

            val newSet = LoggedSet(
                sessionId = sessionId,
                exerciseId = exerciseId,
                setNumber = nextSetNumber,
                weight = defaultWeight,
                reps = defaultReps,
                isCompleted = false
            )
            repository.insertLoggedSet(newSet)
        }
    }

    fun updateSetWeight(setId: Int, weight: Double) {
        viewModelScope.launch {
            val set = loggedSets.value.find { it.id == setId } ?: return@launch
            val updated = LoggedSet(
                id = set.id,
                sessionId = set.sessionId,
                exerciseId = set.exerciseId,
                setNumber = set.setNumber,
                weight = weight,
                reps = set.reps,
                isCompleted = set.isCompleted
            )
            repository.updateLoggedSet(updated)
        }
    }

    fun updateSetReps(setId: Int, reps: Int) {
        viewModelScope.launch {
            val set = loggedSets.value.find { it.id == setId } ?: return@launch
            val updated = LoggedSet(
                id = set.id,
                sessionId = set.sessionId,
                exerciseId = set.exerciseId,
                setNumber = set.setNumber,
                weight = set.weight,
                reps = reps,
                isCompleted = set.isCompleted
            )
            repository.updateLoggedSet(updated)
        }
    }

    fun toggleSetCompleted(setId: Int) {
        viewModelScope.launch {
            val set = loggedSets.value.find { it.id == setId } ?: return@launch
            val updated = LoggedSet(
                id = set.id,
                sessionId = set.sessionId,
                exerciseId = set.exerciseId,
                setNumber = set.setNumber,
                weight = set.weight,
                reps = set.reps,
                isCompleted = !set.isCompleted
            )
            repository.updateLoggedSet(updated)
        }
    }

    fun deleteSet(setId: Int) {
        viewModelScope.launch {
            val set = loggedSets.value.find { it.id == setId } ?: return@launch
            val toDelete = LoggedSet(
                id = set.id,
                sessionId = set.sessionId,
                exerciseId = set.exerciseId,
                setNumber = set.setNumber,
                weight = set.weight,
                reps = set.reps,
                isCompleted = set.isCompleted
            )
            repository.deleteLoggedSet(toDelete)
        }
    }
}
