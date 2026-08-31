package com.example.gains.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gains.data.DataRepository
import com.example.gains.data.Exercise
import com.example.gains.data.LoggedSetWithSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class ExerciseSessionHistoryGroup(
    val sessionId: Long,
    val sessionTimestamp: Long,
    val sessionName: String,
    val sets: List<LoggedSetWithSession>,
    val maxWeightInSession: Double,
    val sessionVolume: Double
)

data class ExerciseProgressPoint(
    val timestamp: Long,
    val maxWeight: Double,
    val maxReps: Int,
    val sessionVolume: Double,
    val repsAtMaxWeight: Int
)

sealed interface ExerciseDetailUiState {
    object Loading : ExerciseDetailUiState
    data class Error(val message: String) : ExerciseDetailUiState
    data class Success(
        val exercise: Exercise,
        val maxWeight: Double,
        val maxReps: Int,
        val maxSessionVolume: Double,
        val repsAtMaxWeight: Int,
        val bestSetId: Int?,
        val progressPoints: List<ExerciseProgressPoint>,
        val historyGroups: List<ExerciseSessionHistoryGroup>
    ) : ExerciseDetailUiState
}

class ExerciseDetailViewModel(
    private val exerciseId: Int,
    private val repository: DataRepository
) : ViewModel() {

    val uiState: StateFlow<ExerciseDetailUiState> = combine(
        repository.getExerciseById(exerciseId),
        repository.getHistoryForExercise(exerciseId)
    ) { exercise, historySets ->
        if (exercise == null) {
            ExerciseDetailUiState.Error("Exercise not found")
        } else {
            val completedSets = historySets.filter { it.isCompleted }
            
            val maxWeight = completedSets.maxOfOrNull { it.weight } ?: 0.0
            val maxReps = completedSets.maxOfOrNull { it.reps } ?: 0
            val repsAtMaxWeight = if (maxWeight > 0.0) {
                completedSets.filter { it.weight == maxWeight }.maxOfOrNull { it.reps } ?: 0
            } else 0

            // Group sets by session
            val groupedBySession = completedSets
                .groupBy { it.sessionId }
                .map { (sessionId, sets) ->
                    val firstSet = sets.first()
                    val sessionMaxWeight = sets.maxOfOrNull { it.weight } ?: 0.0
                    val volume = sets.sumOf { it.weight * it.reps }
                    ExerciseSessionHistoryGroup(
                        sessionId = sessionId,
                        sessionTimestamp = firstSet.sessionTimestamp,
                        sessionName = firstSet.sessionName,
                        sets = sets.sortedBy { it.setNumber },
                        maxWeightInSession = sessionMaxWeight,
                        sessionVolume = volume
                    )
                }
                .sortedByDescending { it.sessionTimestamp }

            val maxSessionVolume = groupedBySession.maxOfOrNull { it.sessionVolume } ?: 0.0

            // Progression chart points (ordered chronologically ASC)
            val progressPoints = groupedBySession
                .sortedBy { it.sessionTimestamp }
                .map { group ->
                    val maxW = group.maxWeightInSession
                    val maxR = group.sets.maxOfOrNull { it.reps } ?: 0
                    val repsAtMaxW = if (maxW > 0.0) {
                        group.sets.filter { it.weight == maxW }.maxOfOrNull { it.reps } ?: 0
                    } else 0
                    ExerciseProgressPoint(
                        timestamp = group.sessionTimestamp,
                        maxWeight = maxW,
                        maxReps = maxR,
                        sessionVolume = group.sessionVolume,
                        repsAtMaxWeight = repsAtMaxW
                    )
                }

            val allTimeBestSet = completedSets.maxWithOrNull(compareBy({ it.weight }, { it.reps }))
            val bestSetId = if (allTimeBestSet != null && allTimeBestSet.weight > 0) allTimeBestSet.id else null

            ExerciseDetailUiState.Success(
                exercise = exercise,
                maxWeight = maxWeight,
                maxReps = maxReps,
                maxSessionVolume = maxSessionVolume,
                repsAtMaxWeight = repsAtMaxWeight,
                bestSetId = bestSetId,
                progressPoints = progressPoints,
                historyGroups = groupedBySession
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExerciseDetailUiState.Loading
    )

    fun saveNotes(notes: String) {
        viewModelScope.launch {
            repository.updateExerciseNotes(exerciseId, if (notes.isBlank()) null else notes)
        }
    }
}
