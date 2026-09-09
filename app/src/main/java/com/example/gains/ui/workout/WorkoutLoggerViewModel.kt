package com.example.gains.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gains.data.DataRepository
import com.example.gains.data.Exercise
import com.example.gains.data.LoggedSet
import com.example.gains.data.LoggedSetWithExercise
import com.example.gains.data.WorkoutLabel
import com.example.gains.data.WorkoutSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutLoggerViewModel(
    val sessionId: Long,
    val templateId: Long = 0L,
    val isTemplateMode: Boolean = false,
    val plannedId: Long = 0L,
    private val repository: DataRepository
) : ViewModel() {

    val session: StateFlow<WorkoutSession?> = if (!isTemplateMode && sessionId > 0) {
        repository.getSessionById(sessionId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    } else {
        kotlinx.coroutines.flow.MutableStateFlow(null)
    }

    val template: StateFlow<com.example.gains.data.WorkoutTemplate?> = if (isTemplateMode && templateId > 0) {
        repository.getTemplateById(templateId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    } else {
        kotlinx.coroutines.flow.MutableStateFlow(null)
    }

    val exercises: StateFlow<List<Exercise>> = repository.allExercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val templateSets: StateFlow<List<com.example.gains.data.TemplateSetWithExercise>> = if (isTemplateMode && templateId > 0) {
        repository.getTemplateSetsForTemplate(templateId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        kotlinx.coroutines.flow.MutableStateFlow(emptyList())
    }

    val loggedSets: StateFlow<List<LoggedSetWithExercise>> = if (!isTemplateMode && sessionId > 0) {
        repository.getLoggedSetsForSession(sessionId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        kotlinx.coroutines.flow.MutableStateFlow(emptyList())
    }

    val allLabels: StateFlow<List<WorkoutLabel>> = repository.allLabels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveAsTemplate(templateName: String) {
        viewModelScope.launch {
            val sets = loggedSets.value
            val activeSession = session.value
            val newTemplate = com.example.gains.data.WorkoutTemplate(
                name = templateName.ifBlank { activeSession?.name ?: "New Template" },
                workoutType = activeSession?.workoutType ?: "GYM",
                labelId = activeSession?.labelId
            )
            val newTemplateId = repository.insertTemplate(newTemplate)
            sets.forEach { s ->
                repository.insertTemplateSet(
                    com.example.gains.data.TemplateSet(
                        templateId = newTemplateId,
                        exerciseId = s.exerciseId,
                        setNumber = s.setNumber,
                        targetWeight = s.weight,
                        targetReps = s.reps
                    )
                )
            }
        }
    }

    fun saveTemplate(name: String, labelId: Int?) {
        viewModelScope.launch {
            if (templateId > 0) {
                val current = template.value
                val updated = com.example.gains.data.WorkoutTemplate(
                    id = templateId,
                    name = name.ifBlank { "Untitled Routine" },
                    workoutType = current?.workoutType ?: "GYM",
                    labelId = labelId,
                    notes = current?.notes,
                    createdAt = current?.createdAt ?: System.currentTimeMillis()
                )
                repository.updateTemplate(updated)
            }
        }
    }

    fun startSessionFromTemplate(onSessionCreated: (Long) -> Unit) {
        viewModelScope.launch {
            if (templateId > 0) {
                val newSessionId = repository.createSessionFromTemplate(templateId)
                if (newSessionId > 0) {
                    if (plannedId > 0) {
                        repository.deletePlannedSessionById(plannedId)
                    }
                    onSessionCreated(newSessionId)
                }
            }
        }
    }

    fun finishSession() {
        viewModelScope.launch {
            val currentSession = session.value ?: return@launch
            val updated = currentSession.copy(endTime = System.currentTimeMillis())
            repository.updateSession(updated)
        }
    }

    fun deleteSession() {
        viewModelScope.launch {
            val currentSession = session.value ?: return@launch
            repository.deleteSession(currentSession)
        }
    }

    fun assignLabelToSession(labelId: Int?) {
        viewModelScope.launch {
            if (isTemplateMode) {
                if (templateId > 0) {
                    val current = template.value
                    if (current != null) {
                        repository.updateTemplate(current.copy(labelId = labelId))
                    }
                }
            } else {
                val currentSession = session.value ?: return@launch
                val updated = currentSession.copy(labelId = labelId)
                repository.updateSession(updated)
            }
        }
    }

    fun addSet(exerciseId: Int) {
        viewModelScope.launch {
            if (isTemplateMode) {
                if (templateId > 0) {
                    val currentSetsForExercise = templateSets.value.filter { it.exerciseId == exerciseId }
                    val nextSetNumber = currentSetsForExercise.size + 1
                    val lastSet = currentSetsForExercise.lastOrNull()

                    val defaultWeight = lastSet?.targetWeight ?: 20.0
                    val defaultReps = lastSet?.targetReps ?: 10

                    val newSet = com.example.gains.data.TemplateSet(
                        templateId = templateId,
                        exerciseId = exerciseId,
                        setNumber = nextSetNumber,
                        targetWeight = defaultWeight,
                        targetReps = defaultReps
                    )
                    repository.insertTemplateSet(newSet)
                }
            } else {
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
    }

    fun updateSetWeight(setId: Int, weight: Double) {
        viewModelScope.launch {
            if (isTemplateMode) {
                val set = templateSets.value.find { it.id == setId } ?: return@launch
                val updated = com.example.gains.data.TemplateSet(
                    id = set.id,
                    templateId = set.templateId,
                    exerciseId = set.exerciseId,
                    setNumber = set.setNumber,
                    targetWeight = weight,
                    targetReps = set.targetReps
                )
                repository.updateTemplateSet(updated)
            } else {
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
    }

    fun updateSetReps(setId: Int, reps: Int) {
        viewModelScope.launch {
            if (isTemplateMode) {
                val set = templateSets.value.find { it.id == setId } ?: return@launch
                val updated = com.example.gains.data.TemplateSet(
                    id = set.id,
                    templateId = set.templateId,
                    exerciseId = set.exerciseId,
                    setNumber = set.setNumber,
                    targetWeight = set.targetWeight,
                    targetReps = reps
                )
                repository.updateTemplateSet(updated)
            } else {
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
    }

    fun toggleSetCompleted(setId: Int) {
        viewModelScope.launch {
            if (!isTemplateMode) {
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
    }

    fun deleteSet(setId: Int) {
        viewModelScope.launch {
            if (isTemplateMode) {
                val set = templateSets.value.find { it.id == setId } ?: return@launch
                val toDelete = com.example.gains.data.TemplateSet(
                    id = set.id,
                    templateId = set.templateId,
                    exerciseId = set.exerciseId,
                    setNumber = set.setNumber,
                    targetWeight = set.targetWeight,
                    targetReps = set.targetReps
                )
                repository.deleteTemplateSet(toDelete)
            } else {
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
}
