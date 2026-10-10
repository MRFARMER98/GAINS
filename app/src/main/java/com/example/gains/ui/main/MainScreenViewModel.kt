package com.example.gains.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gains.data.DailyNutrientSummary
import com.example.gains.data.DataRepository
import com.example.gains.data.ExerciseWithSummary
import com.example.gains.data.FoodItem
import com.example.gains.data.FoodNutrient
import com.example.gains.data.FoodServing
import com.example.gains.data.LoggedFoodEntry
import com.example.gains.data.PlannedSession
import com.example.gains.data.UserProfile
import com.example.gains.data.WorkoutLabel
import com.example.gains.data.WorkoutSession
import com.example.gains.data.WorkoutSessionWithLabel
import com.example.gains.domain.ActiveCalorieMacroSplit
import com.example.gains.domain.ActivityAllocationStrategy
import com.example.gains.domain.ActivityLevel
import com.example.gains.domain.BiologicalSex
import com.example.gains.domain.CalibrationCheckInState
import com.example.gains.domain.MacroPreset
import com.example.gains.domain.NutritionCalculator
import com.example.gains.domain.NutritionEngineMode
import com.example.gains.domain.NutritionGoalType
import com.example.gains.domain.NutritionTargets
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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

    val weeklyDashboardSummary: StateFlow<WeeklyDashboardSummary> = combine(
        uiState,
        allPlannedSessions
    ) { state, planned ->
        val sessions = (state as? MainScreenUiState.Success)?.sessions ?: emptyList()
        val now = System.currentTimeMillis()
        val cal = java.util.Calendar.getInstance().apply {
            val dayOfWeek = get(java.util.Calendar.DAY_OF_WEEK)
            val daysFromMonday = if (dayOfWeek == java.util.Calendar.SUNDAY) 6 else dayOfWeek - java.util.Calendar.MONDAY
            add(java.util.Calendar.DAY_OF_YEAR, -daysFromMonday)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val mondayStart = cal.timeInMillis

        val sdfMonth = SimpleDateFormat("MMM d", Locale.ENGLISH)
        val startLabel = sdfMonth.format(Date(mondayStart)).uppercase()
        val sundayEndCal = java.util.Calendar.getInstance().apply {
            timeInMillis = mondayStart
            add(java.util.Calendar.DAY_OF_YEAR, 6)
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
            set(java.util.Calendar.MILLISECOND, 999)
        }
        val endLabel = sdfMonth.format(Date(sundayEndCal.timeInMillis)).uppercase()
        val weekRangeLabel = "$startLabel – $endLabel"
        val sundayEnd = sundayEndCal.timeInMillis

        val dayLetters = listOf("M", "T", "W", "T", "F", "S", "S")
        val dayShorts = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

        val todayCal = java.util.Calendar.getInstance()
        val todayYear = todayCal.get(java.util.Calendar.YEAR)
        val todayDayOfYear = todayCal.get(java.util.Calendar.DAY_OF_YEAR)

        var completedThisWeek = 0
        var totalActiveMinutes = 0L

        val daysList = (0..6).map { dayIndex ->
            val dayCal = java.util.Calendar.getInstance().apply {
                timeInMillis = mondayStart
                add(java.util.Calendar.DAY_OF_YEAR, dayIndex)
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val dayStart = dayCal.timeInMillis
            val dayEndCal = java.util.Calendar.getInstance().apply {
                timeInMillis = dayStart
                set(java.util.Calendar.HOUR_OF_DAY, 23)
                set(java.util.Calendar.MINUTE, 59)
                set(java.util.Calendar.SECOND, 59)
                set(java.util.Calendar.MILLISECOND, 999)
            }
            val dayEnd = dayEndCal.timeInMillis
            val isToday = dayCal.get(java.util.Calendar.YEAR) == todayYear && dayCal.get(java.util.Calendar.DAY_OF_YEAR) == todayDayOfYear
            val isFuture = dayStart > now && !isToday

            val completedSession = sessions.find { it.timestamp in dayStart..dayEnd }
            val plannedSession = planned.find { 
                val pCal = java.util.Calendar.getInstance().apply { timeInMillis = it.dateTimestamp }
                pCal.get(java.util.Calendar.YEAR) == dayCal.get(java.util.Calendar.YEAR) && pCal.get(java.util.Calendar.DAY_OF_YEAR) == dayCal.get(java.util.Calendar.DAY_OF_YEAR)
            }

            if (completedSession != null) {
                completedThisWeek++
                if (completedSession.endTime > completedSession.timestamp) {
                    totalActiveMinutes += (completedSession.endTime - completedSession.timestamp) / (1000 * 60)
                } else {
                    totalActiveMinutes += 45
                }
            }

            WeekDayStatus(
                dayOfWeekLetter = dayLetters[dayIndex],
                dayOfWeekShort = dayShorts[dayIndex],
                dayOfMonth = dayCal.get(java.util.Calendar.DAY_OF_MONTH),
                dateTimestamp = dayStart,
                isToday = isToday,
                isFuture = isFuture,
                hasCompletedWorkout = completedSession != null,
                hasPlannedWorkout = plannedSession != null && completedSession == null,
                sessionName = completedSession?.name ?: plannedSession?.name
            )
        }

        val plannedCount = planned.count { it.dateTimestamp in mondayStart..sundayEnd }
        val targetCount = maxOf(4, completedThisWeek + plannedCount)

        WeeklyDashboardSummary(
            weekRangeLabel = weekRangeLabel,
            completedWorkoutsCount = completedThisWeek,
            targetWorkoutsCount = targetCount,
            days = daysList,
            totalActiveMinutes = totalActiveMinutes
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        WeeklyDashboardSummary(
            weekRangeLabel = "",
            completedWorkoutsCount = 0,
            targetWorkoutsCount = 4,
            days = emptyList(),
            totalActiveMinutes = 0L
        )
    )

    private val _hcSyncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val hcSyncState: StateFlow<SyncState> = _hcSyncState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultMetricsSeeded()
            repository.ensureDefaultFoodsSeeded()
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
        birthDateTimestamp: Long? = null,
        biologicalSex: String = "MALE"
    ) {
        viewModelScope.launch {
            val current = repository.userProfile.firstOrNull() ?: UserProfile()
            repository.updateProfile(
                current.copy(
                    id = 1,
                    name = name,
                    photoUri = photoUri,
                    height = height,
                    age = age,
                    birthDateTimestamp = birthDateTimestamp,
                    currentWeight = currentWeight,
                    biologicalSex = biologicalSex
                )
            )
        }
    }

    // Food Tracker State Management
    private val _selectedFoodDateTimestamp = MutableStateFlow(getStartOfDayTimestamp(System.currentTimeMillis()))
    val selectedFoodDateTimestamp: StateFlow<Long> = _selectedFoodDateTimestamp.asStateFlow()

    val dailyActiveCaloriesBurned: StateFlow<Float> = combine(
        _selectedFoodDateTimestamp,
        repository.allSessionsWithLabels,
        repository.allExternalActivities,
        repository.userProfile
    ) { selectedDate, sessions, externals, profile ->
        val endOfDay = selectedDate + (24L * 60 * 60 * 1000L) - 1L
        val weight = profile?.currentWeight ?: 78.0

        // 1. GAINS Workouts
        val sessionBurn = sessions
            .filter { it.timestamp in selectedDate..endOfDay }
            .sumOf { s ->
                val durationMins = if (s.endTime > s.timestamp) {
                    ((s.endTime - s.timestamp) / 60000L).coerceIn(15L, 180L).toInt()
                } else {
                    45
                }
                NutritionCalculator.calculateGymSessionBurnKcal(durationMins, weight).toDouble()
            }

        // 2. Health Connect External Activities (discounted by 20% conservative factor)
        val externalBurn = externals
            .filter { it.startTime in selectedDate..endOfDay }
            .sumOf { act ->
                val base = act.caloriesKcal ?: ((act.durationSeconds / 60.0) * 7.0)
                base * 0.80
            }

        (sessionBurn + externalBurn).toFloat()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val nutritionTargets: StateFlow<NutritionTargets> = combine(
        repository.userProfile,
        dailyActiveCaloriesBurned
    ) { profile, activeBurn ->
        val weight = profile?.currentWeight
        val height = profile?.height
        val age = profile?.calculatedAge
        val sex = try { BiologicalSex.valueOf(profile?.biologicalSex ?: "MALE") } catch (e: Exception) { BiologicalSex.MALE }
        val activity = try { ActivityLevel.valueOf(profile?.activityLevel ?: "MODERATE") } catch (e: Exception) { ActivityLevel.MODERATE }
        val engineMode = try { NutritionEngineMode.valueOf(profile?.nutritionEngineMode ?: "CLASSIC_FORMULA") } catch (e: Exception) { NutritionEngineMode.CLASSIC_FORMULA }
        val allocStrategy = try { ActivityAllocationStrategy.valueOf(profile?.activityAllocationStrategy ?: "REALTIME_DAILY_BURN") } catch (e: Exception) { ActivityAllocationStrategy.REALTIME_DAILY_BURN }
        val macroSplit = try { ActiveCalorieMacroSplit.valueOf(profile?.activeCalorieMacroSplit ?: "CARBS_PRIORITY") } catch (e: Exception) { ActiveCalorieMacroSplit.CARBS_PRIORITY }
        val preset = try { MacroPreset.valueOf(profile?.macroPreset ?: "BALANCED_SPORTS") } catch (e: Exception) { MacroPreset.BALANCED_SPORTS }
        val goal = try { NutritionGoalType.valueOf(profile?.nutritionGoalType ?: "MAINTENANCE") } catch (e: Exception) { NutritionGoalType.MAINTENANCE }

        NutritionCalculator.calculateTargets(
            weightKg = weight,
            heightCm = height,
            ageYears = age,
            sex = sex,
            activityLevel = activity,
            engineMode = engineMode,
            allocationStrategy = allocStrategy,
            activeMacroSplit = macroSplit,
            macroPreset = preset,
            goalType = goal,
            weeklyRatePercent = profile?.weeklyRatePercent,
            targetWeightKg = null,
            targetDateTimestamp = profile?.targetGoalDateTimestamp,
            activeCaloriesBurnedToday = activeBurn,
            customCalories = profile?.customCaloriesTarget,
            customProteinG = profile?.customProteinTargetG,
            customCarbsG = profile?.customCarbsTargetG,
            customFatG = profile?.customFatTargetG,
            customFiberG = profile?.customFiberTargetG
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        NutritionCalculator.calculateTargets(null, null, null)
    )

    fun updateNutritionPlan(
        activityLevel: ActivityLevel,
        engineMode: NutritionEngineMode,
        allocationStrategy: ActivityAllocationStrategy,
        activeMacroSplit: ActiveCalorieMacroSplit,
        macroPreset: MacroPreset,
        goalType: NutritionGoalType,
        weeklyRatePercent: Float? = null,
        targetDateTimestamp: Long? = null,
        customCalories: Float? = null,
        customProteinG: Float? = null,
        customCarbsG: Float? = null,
        customFatG: Float? = null,
        customFiberG: Float? = null
    ) {
        viewModelScope.launch {
            val current = repository.userProfile.firstOrNull() ?: UserProfile()
            repository.updateProfile(
                current.copy(
                    activityLevel = activityLevel.name,
                    nutritionEngineMode = engineMode.name,
                    activityAllocationStrategy = allocationStrategy.name,
                    activeCalorieMacroSplit = activeMacroSplit.name,
                    macroPreset = macroPreset.name,
                    nutritionGoalType = goalType.name,
                    weeklyRatePercent = weeklyRatePercent,
                    targetGoalDateTimestamp = targetDateTimestamp,
                    customCaloriesTarget = customCalories,
                    customProteinTargetG = customProteinG,
                    customCarbsTargetG = customCarbsG,
                    customFatTargetG = customFatG,
                    customFiberTargetG = customFiberG
                )
            )
        }
    }

    val weeklyCalibrationState: StateFlow<CalibrationCheckInState?> = combine(
        repository.userProfile,
        repository.allMetricsWithLatest
    ) { profile, metrics ->
        val weightMetric = metrics.find { it.name.lowercase().contains("weight") } ?: return@combine null
        val entries = repository.getEntriesForMetric(weightMetric.id).firstOrNull() ?: emptyList()
        val now = System.currentTimeMillis()
        val fourteenDaysAgo = now - (14L * 24 * 60 * 60 * 1000L)
        val recentEntries = entries.filter { it.timestamp >= fourteenDaysAgo }.sortedBy { it.timestamp }

        if (recentEntries.size < 3) return@combine null

        val lastDismissed = profile?.lastCalibrationDismissedTimestamp ?: 0L
        if (now - lastDismissed < 7L * 24 * 60 * 60 * 1000L) return@combine null

        val oldest = recentEntries.first()
        val latest = recentEntries.last()
        val daysBetween = ((latest.timestamp - oldest.timestamp) / (1000L * 60 * 60 * 24)).coerceAtLeast(3L)
        val observedWeeklyDelta = ((latest.value - oldest.value) / daysBetween.toDouble()) * 7.0

        val targetWeeklyDelta = when (profile?.nutritionGoalType) {
            "CUT_AGGRESSIVE" -> -0.8
            "CUT_MODERATE" -> -0.5
            "LEAN_BULK" -> 0.25
            else -> 0.0
        }

        NutritionCalculator.evaluateWeeklyCalibration(
            currentTdeeKcal = 2500,
            observedSevenDayWeightDeltaKg = observedWeeklyDelta,
            targetWeeklyWeightDeltaKg = targetWeeklyDelta,
            weighInCount = recentEntries.size,
            loggedDaysCount = 5
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun applyCalibrationAdjustment(deltaKcal: Int) {
        viewModelScope.launch {
            val current = repository.userProfile.firstOrNull() ?: UserProfile()
            val currentTarget = current.customCaloriesTarget ?: 2400f
            repository.updateProfile(
                current.copy(
                    customCaloriesTarget = (currentTarget + deltaKcal).coerceAtLeast(1200f),
                    lastCalibrationDismissedTimestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun dismissCalibration() {
        viewModelScope.launch {
            val current = repository.userProfile.firstOrNull() ?: UserProfile()
            repository.updateProfile(
                current.copy(
                    lastCalibrationDismissedTimestamp = System.currentTimeMillis()
                )
            )
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val loggedFoodEntries: StateFlow<List<LoggedFoodEntry>> = _selectedFoodDateTimestamp
        .flatMapLatest { timestamp -> repository.getLoggedFoodEntriesForDate(timestamp) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyNutrientSummary: StateFlow<DailyNutrientSummary> = loggedFoodEntries
        .map { entries ->
            DailyNutrientSummary(
                caloriesKcal = entries.sumOf { it.caloriesKcal.toDouble() }.toFloat(),
                proteinG = entries.sumOf { it.proteinG.toDouble() }.toFloat(),
                carbsG = entries.sumOf { it.carbsG.toDouble() }.toFloat(),
                fatG = entries.sumOf { it.fatG.toDouble() }.toFloat(),
                fiberG = entries.sumOf { it.fiberG.toDouble() }.toFloat(),
                saltG = entries.sumOf { it.saltG.toDouble() }.toFloat(),
                saturatedFatG = entries.sumOf { it.saturatedFatG.toDouble() }.toFloat(),
                sugarsG = entries.sumOf { it.sugarsG.toDouble() }.toFloat()
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyNutrientSummary())

    private val _foodSearchQuery = MutableStateFlow("")
    val foodSearchQuery: StateFlow<String> = _foodSearchQuery.asStateFlow()

    @OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val foodSearchResults: StateFlow<List<FoodItem>> = _foodSearchQuery
        .debounce(300L)
        .distinctUntilChanged()
        .flatMapLatest { query -> repository.searchFoodItems(query.trim()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFoodSearchQuery(query: String) {
        _foodSearchQuery.value = query
    }

    fun setSelectedFoodDate(timestamp: Long) {
        _selectedFoodDateTimestamp.value = getStartOfDayTimestamp(timestamp)
    }

    fun changeSelectedFoodDate(daysDelta: Int) {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = _selectedFoodDateTimestamp.value
            add(java.util.Calendar.DAY_OF_YEAR, daysDelta)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        _selectedFoodDateTimestamp.value = cal.timeInMillis
    }

    fun logFoodItem(
        food: com.example.gains.data.FoodItem,
        nutrient: com.example.gains.data.FoodNutrient?,
        serving: com.example.gains.data.FoodServing?,
        gramWeightTotal: Float,
        mealType: String
    ) {
        viewModelScope.launch {
            val totalGrams = gramWeightTotal.coerceAtLeast(1f)
            val multiplier = totalGrams / 100f

            val unit = if (food.perUnit == "100ml") "ml" else "g"
            val entry = LoggedFoodEntry(
                dateTimestamp = _selectedFoodDateTimestamp.value,
                timestamp = System.currentTimeMillis(),
                mealType = mealType,
                foodId = food.id,
                foodName = food.name,
                brandName = food.brand,
                servingDescription = serving?.description ?: "${totalGrams.toInt()}$unit",
                servingQuantity = 1.0f,
                gramWeightTotal = totalGrams,
                caloriesKcal = if (food.isVerified) (nutrient?.caloriesKcal ?: 0f) * multiplier else 0f,
                proteinG = if (food.isVerified) (nutrient?.proteinG ?: 0f) * multiplier else 0f,
                carbsG = if (food.isVerified) (nutrient?.carbsG ?: 0f) * multiplier else 0f,
                fatG = if (food.isVerified) (nutrient?.fatG ?: 0f) * multiplier else 0f,
                fiberG = if (food.isVerified) (nutrient?.fiberG ?: 0f) * multiplier else 0f,
                saltG = if (food.isVerified) (nutrient?.saltG ?: 0f) * multiplier else 0f,
                saturatedFatG = if (food.isVerified) (nutrient?.saturatedFatG ?: 0f) * multiplier else 0f,
                sugarsG = if (food.isVerified) (nutrient?.sugarsG ?: 0f) * multiplier else 0f,
                imageUrl = food.imageUrl
            )
            repository.insertLoggedFoodEntry(entry)
        }
    }

    val recipesWithDetails: StateFlow<List<com.example.gains.data.FoodRecipeWithDetails>> = repository.allRecipesWithDetails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedIngredientFilters = MutableStateFlow<Set<String>>(emptySet())
    val selectedIngredientFilters: StateFlow<Set<String>> = _selectedIngredientFilters.asStateFlow()

    val filteredRecipes: StateFlow<List<com.example.gains.data.FoodRecipeWithDetails>> = combine(
        recipesWithDetails,
        _selectedIngredientFilters
    ) { recipes, filters ->
        if (filters.isEmpty()) {
            recipes
        } else {
            recipes.filter { r ->
                val recipeIngredientNames = r.ingredients.map { it.foodName.lowercase() }
                filters.any { filter ->
                    recipeIngredientNames.any { it.contains(filter.lowercase()) }
                }
            }.sortedByDescending { r ->
                val recipeIngredientNames = r.ingredients.map { it.foodName.lowercase() }
                filters.count { filter -> recipeIngredientNames.any { it.contains(filter.lowercase()) } }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleIngredientFilter(ingredientName: String) {
        val lower = ingredientName.trim().lowercase()
        if (lower.isBlank()) return
        _selectedIngredientFilters.update { current ->
            if (current.contains(lower)) current - lower else current + lower
        }
    }

    fun clearIngredientFilters() {
        _selectedIngredientFilters.value = emptySet()
    }

    fun saveRecipe(recipe: com.example.gains.data.FoodRecipe, ingredients: List<com.example.gains.data.FoodRecipeIngredient>) {
        viewModelScope.launch {
            repository.saveRecipe(recipe, ingredients)
        }
    }

    fun deleteRecipe(recipeId: Long) {
        viewModelScope.launch {
            repository.deleteRecipe(recipeId)
        }
    }

    fun logRecipeAsMeal(recipeId: Long, servingsToLog: Float, mealType: String) {
        viewModelScope.launch {
            repository.logRecipeAsMeal(recipeId, servingsToLog, mealType, _selectedFoodDateTimestamp.value)
        }
    }

    fun deleteLoggedFoodEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteLoggedFoodEntryById(id)
        }
    }

    private fun getStartOfDayTimestamp(millis: Long): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = millis
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
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

data class WeekDayStatus(
    val dayOfWeekLetter: String,
    val dayOfWeekShort: String,
    val dayOfMonth: Int,
    val dateTimestamp: Long,
    val isToday: Boolean,
    val isFuture: Boolean,
    val hasCompletedWorkout: Boolean,
    val hasPlannedWorkout: Boolean,
    val sessionName: String? = null
)

data class WeeklyDashboardSummary(
    val weekRangeLabel: String,
    val completedWorkoutsCount: Int,
    val targetWorkoutsCount: Int,
    val days: List<WeekDayStatus>,
    val totalActiveMinutes: Long
)
