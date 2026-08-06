package com.example.gains.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gains.data.DataRepository
import com.example.gains.data.WorkoutSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface SyncState {
    object Idle : SyncState
    object Syncing : SyncState
    object Success : SyncState
    data class Error(val message: String) : SyncState
}

class MainScreenViewModel(private val repository: DataRepository) : ViewModel() {

    val uiState: StateFlow<MainScreenUiState> = repository.allSessions
        .map<List<WorkoutSession>, MainScreenUiState> { sessions ->
            MainScreenUiState.Success(sessions)
        }
        .catch { emit(MainScreenUiState.Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainScreenUiState.Loading)

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    fun startNewSession(workoutType: String, onSessionCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
            val displayType = when(workoutType) {
                "RUN" -> "Running"
                "HYROX" -> "Hyrox"
                else -> "Gym"
            }
            val session = WorkoutSession(
                timestamp = System.currentTimeMillis(),
                name = "$displayType Session on $dateStr",
                workoutType = workoutType
            )
            val sessionId = repository.insertSession(session)
            onSessionCreated(sessionId)
        }
    }

    fun deleteSession(session: WorkoutSession) {
        viewModelScope.launch {
            repository.deleteSession(session)
        }
    }

    fun syncDatabase() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            // Fetch CSV directly from Google Sheets published export URL
            val sheetUrl = "https://docs.google.com/spreadsheets/d/e/2PACX-1vQHNjnhDMrr2uFeODRHpm5Ab9rHZwVMYO-YLMA7I2ADv8y5Xw-e-j71Gq0am8_EWFEs9R_wDCA6bkXI/pub?output=csv"
            val result = repository.syncExercises(sheetUrl)
            
            if (result.isSuccess) {
                _syncState.value = SyncState.Success
                delay(2000) // Keep the success message visible for 2 seconds
                _syncState.value = SyncState.Idle
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                _syncState.value = SyncState.Error(errorMsg)
                delay(4000) // Show error for 4 seconds
                _syncState.value = SyncState.Idle
            }
        }
    }
}

sealed interface MainScreenUiState {
    object Loading : MainScreenUiState
    data class Error(val throwable: Throwable) : MainScreenUiState
    data class Success(val sessions: List<WorkoutSession>) : MainScreenUiState
}
