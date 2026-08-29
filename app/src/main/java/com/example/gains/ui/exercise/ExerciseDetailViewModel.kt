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
    val est1RM: Double
)

sealed interface ExerciseDetailUiState {
    object Loading : ExerciseDetailUiState
    data class Error(val message: String) : ExerciseDetailUiState
    data class Success(
        val exercise: Exercise,
        val maxWeight: Double,
        val est1RM: Double,
        val maxSessionVolume: Double,
        val totalSets: Int,
        val totalReps: Int,
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
            val est1RM = completedSets.maxOfOrNull { calculate1RM(it.weight, it.reps) } ?: 0.0
            val totalSets = completedSets.size
            val totalReps = completedSets.sumOf { it.reps }

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
                    val max1RM = group.sets.maxOfOrNull { calculate1RM(it.weight, it.reps) } ?: 0.0
                    ExerciseProgressPoint(
                        timestamp = group.sessionTimestamp,
                        maxWeight = maxW,
                        est1RM = max1RM
                    )
                }

            ExerciseDetailUiState.Success(
                exercise = exercise,
                maxWeight = maxWeight,
                est1RM = est1RM,
                maxSessionVolume = maxSessionVolume,
                totalSets = totalSets,
                totalReps = totalReps,
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

    companion object {
        fun calculate1RM(weight: Double, reps: Int): Double {
            if (reps <= 0) return 0.0
            if (reps == 1) return weight
            // Epley Formula: W * (1 + R/30) rounded to 1 decimal
            val unrounded = weight * (1.0 + reps / 30.0)
            return (unrounded * 10.0).roundToInt() / 10.0
        }
    }
}
