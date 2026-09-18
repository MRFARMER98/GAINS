package com.example.gains.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gains.data.DataRepository
import com.example.gains.data.ExerciseWithSummary
import com.example.gains.data.PlannedSession
import com.example.gains.data.UserProfile
import com.example.gains.data.WorkoutLabel
import com.example.gains.data.WorkoutSession
import com.example.gains.data.WorkoutSessionWithLabel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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

    val uiState: StateFlow<MainScreenUiState> = combine(
        repository.allSessionsWithLabels,
        repository.userProfile
    ) { sessions, profile ->
        MainScreenUiState.Success(sessions, profile) as MainScreenUiState
    }
        .catch { emit(MainScreenUiState.Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainScreenUiState.Loading)

    val allLabels: StateFlow<List<WorkoutLabel>> = repository.allLabels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exercisesWithSummary: StateFlow<List<ExerciseWithSummary>> = repository.allExercisesWithSummary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlannedSessions: StateFlow<List<PlannedSession>> = repository.allPlannedSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTemplates: StateFlow<List<com.example.gains.data.WorkoutTemplateWithDetails>> = repository.allTemplatesWithDetails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    val allMetrics: StateFlow<List<com.example.gains.data.MetricWithLatestEntry>> = repository.allMetricsWithLatest
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExternalActivities: StateFlow<List<com.example.gains.data.ExternalActivity>> = repository.allExternalActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _hcSyncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val hcSyncState: StateFlow<SyncState> = _hcSyncState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultMetricsSeeded()
        }
    }

    fun syncHealthConnect(context: android.content.Context) {
        viewModelScope.launch {
            _hcSyncState.value = SyncState.Syncing
            val result = repository.syncHealthConnect(context)
            if (result.isSuccess) {
                _hcSyncState.value = SyncState.Success
            } else {
                _hcSyncState.value = SyncState.Error(result.exceptionOrNull()?.message ?: "Sync failed")
            }
        }
    }

    fun updateMetricSource(id: Long, source: String) {
        viewModelScope.launch {
            repository.updateMetricSource(id, source)
        }
    }

    fun logMetric(metricId: Long, value: Float, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repository.insertMetricEntry(
                com.example.gains.data.MetricEntry(
                    metricId = metricId,
                    timestamp = timestamp,
                    value = value
                )
            )
        }
    }

    fun getMetricEntriesFlow(metricId: Long): kotlinx.coroutines.flow.Flow<List<com.example.gains.data.MetricEntry>> {
        return repository.getEntriesForMetric(metricId)
    }

    fun updateMetricGoal(id: Long, targetValue: Float?, targetDate: Long?) {
        viewModelScope.launch {
            repository.updateMetricGoal(id, targetValue, targetDate)
        }
    }

    fun updateMetricEntry(entry: com.example.gains.data.MetricEntry) {
        viewModelScope.launch {
            repository.updateMetricEntry(entry)
        }
    }

    fun deleteMetricEntry(entry: com.example.gains.data.MetricEntry) {
        viewModelScope.launch {
            repository.deleteMetricEntry(entry)
        }
    }

    fun createSessionFromTemplate(templateId: Long, autoLoadPrevious: Boolean = true, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val sessionId = repository.createSessionFromTemplate(templateId, autoLoadPrevious = autoLoadPrevious)
            if (sessionId > 0) {
                onCreated(sessionId)
            }
        }
    }

    fun createNewTemplate(name: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val templateId = repository.insertTemplate(
                com.example.gains.data.WorkoutTemplate(
                    name = name.ifBlank { "New Routine" },
                    createdAt = System.currentTimeMillis()
                )
            )
            onCreated(templateId)
        }
    }

    fun deleteTemplate(templateId: Long) {
        viewModelScope.launch {
            repository.deleteTemplateById(templateId)
        }
    }

    fun schedulePlannedSession(dateTimestamp: Long, name: String, workoutType: String, labelId: Int? = null, templateId: Long? = null) {
        viewModelScope.launch {
            val plannedSession = PlannedSession(
                dateTimestamp = dateTimestamp,
                name = name,
                workoutType = workoutType,
                labelId = labelId,
                templateId = templateId
            )
            repository.insertPlannedSession(plannedSession)
        }
    }

    fun deletePlannedSession(id: Long) {
        viewModelScope.launch {
            repository.deletePlannedSessionById(id)
        }
    }

    fun startPlannedSession(planned: PlannedSession, autoLoadPrevious: Boolean = true, onSessionCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val sessionId = if (planned.templateId != null && planned.templateId > 0) {
                repository.createSessionFromTemplate(planned.templateId, planned.name, autoLoadPrevious = autoLoadPrevious)
            } else {
                val session = WorkoutSession(
                    timestamp = System.currentTimeMillis(),
                    name = planned.name,
                    workoutType = planned.workoutType,
                    labelId = planned.labelId
                )
                repository.insertSession(session)
            }
            repository.deletePlannedSessionById(planned.id)
            onSessionCreated(sessionId)
        }
    }

    fun startNewSession(workoutType: String, onSessionCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
            val session = WorkoutSession(
                timestamp = System.currentTimeMillis(),
                name = dayOfWeek,
                workoutType = workoutType
            )
            val sessionId = repository.insertSession(session)
            onSessionCreated(sessionId)
        }
    }

    fun deleteSession(session: WorkoutSessionWithLabel) {
        viewModelScope.launch {
            val toDelete = WorkoutSession(
                id = session.id,
                timestamp = session.timestamp,
                name = session.name,
                workoutType = session.workoutType,
                endTime = session.endTime,
                labelId = session.labelId
            )
            repository.deleteSession(toDelete)
        }
    }

    fun createLabel(name: String, colorHex: String) {
        viewModelScope.launch {
            repository.insertLabel(WorkoutLabel(name = name, colorHex = colorHex))
        }
    }

    fun deleteLabel(label: WorkoutLabel) {
        viewModelScope.launch {
            repository.deleteLabel(label)
        }
    }

    fun saveProfile(
        name: String,
        photoUri: String?,
        height: Double?,
        age: Int?,
        currentWeight: Double?,
        birthDateTimestamp: Long? = null
    ) {
        viewModelScope.launch {
            repository.updateProfile(
                UserProfile(
                    id = 1,
                    name = name,
                    photoUri = photoUri,
                    height = height,
                    age = age,
                    birthDateTimestamp = birthDateTimestamp,
                    currentWeight = currentWeight
                )
            )
        }
    }

    fun syncDatabase() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            val sheetUrl = "https://docs.google.com/spreadsheets/d/e/2PACX-1vQHNjnhDMrr2uFeODRHpm5Ab9rHZwVMYO-YLMA7I2ADv8y5Xw-e-j71Gq0am8_EWFEs9R_wDCA6bkXI/pub?output=csv"
            val result = repository.syncExercises(sheetUrl)
            
            if (result.isSuccess) {
                _syncState.value = SyncState.Success
                delay(2000)
                _syncState.value = SyncState.Idle
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                _syncState.value = SyncState.Error(errorMsg)
                delay(4000)
                _syncState.value = SyncState.Idle
            }
        }
    }
}

sealed interface MainScreenUiState {
    object Loading : MainScreenUiState
    data class Error(val throwable: Throwable) : MainScreenUiState
    data class Success(
        val sessions: List<WorkoutSessionWithLabel>,
        val userProfile: UserProfile?
    ) : MainScreenUiState
}
