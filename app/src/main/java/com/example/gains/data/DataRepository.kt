package com.example.gains.data

import com.example.gains.data.sync.CsvParser
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

interface DataRepository {
    val allExercises: Flow<List<Exercise>>
    val allExercisesWithSummary: Flow<List<ExerciseWithSummary>>
    val allSessions: Flow<List<WorkoutSession>>
    val allSessionsWithLabels: Flow<List<WorkoutSessionWithLabel>>
    val allLabels: Flow<List<WorkoutLabel>>
    val userProfile: Flow<UserProfile?>
    val allPlannedSessions: Flow<List<PlannedSession>>

    suspend fun insertSession(session: WorkoutSession): Long
    suspend fun updateSession(session: WorkoutSession)
    suspend fun deleteSession(session: WorkoutSession)

    suspend fun insertPlannedSession(plannedSession: PlannedSession): Long
    suspend fun deletePlannedSessionById(id: Long)

    suspend fun insertLoggedSet(loggedSet: LoggedSet): Long
    suspend fun updateLoggedSet(loggedSet: LoggedSet)
    suspend fun deleteLoggedSet(loggedSet: LoggedSet)

    fun getLoggedSetsForSession(sessionId: Long): Flow<List<LoggedSetWithExercise>>
    fun getSessionById(sessionId: Long): Flow<WorkoutSession?>
    
    suspend fun insertExercises(exercises: List<Exercise>)
    suspend fun syncExercises(sheetUrl: String): Result<Unit>

    // Labels
    suspend fun insertLabel(label: WorkoutLabel): Long
    suspend fun deleteLabel(label: WorkoutLabel)

    // User Profile
    suspend fun updateProfile(profile: UserProfile)

    // Exercise Details & History
    fun getExerciseById(exerciseId: Int): Flow<Exercise?>
    fun getHistoryForExercise(exerciseId: Int): Flow<List<LoggedSetWithSession>>
    suspend fun getLatestCompletedSetsForExercise(exerciseId: Int): List<LoggedSet>
    suspend fun updateExerciseNotes(exerciseId: Int, notes: String?)

    // Templates
    val allTemplatesWithDetails: Flow<List<WorkoutTemplateWithDetails>>
    fun getTemplateById(id: Long): Flow<WorkoutTemplate?>
    fun getTemplateSetsForTemplate(templateId: Long): Flow<List<TemplateSetWithExercise>>
    suspend fun insertTemplate(template: WorkoutTemplate): Long
    suspend fun updateTemplate(template: WorkoutTemplate)
    suspend fun deleteTemplateById(id: Long)
    suspend fun insertTemplateSet(templateSet: TemplateSet): Long
    suspend fun updateTemplateSet(templateSet: TemplateSet)
    suspend fun deleteTemplateSet(templateSet: TemplateSet)
    suspend fun createSessionFromTemplate(templateId: Long, name: String? = null, autoLoadPrevious: Boolean = false): Long
    suspend fun createTemplateFromSession(sessionId: Long, templateName: String): Long

    // Metrics
    val allMetricsWithLatest: Flow<List<MetricWithLatestEntry>>
    fun getEntriesForMetric(metricId: Long): Flow<List<MetricEntry>>
    suspend fun insertMetricEntry(entry: MetricEntry): Long
    suspend fun updateMetricEntry(entry: MetricEntry)
    suspend fun deleteMetricEntry(entry: MetricEntry)
    suspend fun updateMetricGoal(id: Long, targetValue: Float?, targetDate: Long?)
    suspend fun updateMetricSource(id: Long, source: String)
    suspend fun ensureDefaultMetricsSeeded()

    // External Activities & Health Connect
    val allExternalActivities: Flow<List<ExternalActivity>>
    suspend fun syncHealthConnect(context: android.content.Context): Result<Int>
}

class DefaultDataRepository(private val gainsDao: GainsDao) : DataRepository {
    override val allExercises: Flow<List<Exercise>> = gainsDao.getAllExercises()
    override val allExercisesWithSummary: Flow<List<ExerciseWithSummary>> = gainsDao.getAllExercisesWithSummary()
    override val allSessions: Flow<List<WorkoutSession>> = gainsDao.getAllSessions()
    override val allSessionsWithLabels: Flow<List<WorkoutSessionWithLabel>> = gainsDao.getAllSessionsWithLabels()
    override val allLabels: Flow<List<WorkoutLabel>> = gainsDao.getAllLabels()
    override val userProfile: Flow<UserProfile?> = gainsDao.getUserProfile()
    override val allPlannedSessions: Flow<List<PlannedSession>> = gainsDao.getAllPlannedSessions()

    override suspend fun insertSession(session: WorkoutSession): Long = gainsDao.insertSession(session)
    override suspend fun updateSession(session: WorkoutSession) = gainsDao.updateSession(session)
    override suspend fun deleteSession(session: WorkoutSession) = gainsDao.deleteSession(session)

    override suspend fun insertPlannedSession(plannedSession: PlannedSession): Long = gainsDao.insertPlannedSession(plannedSession)
    override suspend fun deletePlannedSessionById(id: Long) = gainsDao.deletePlannedSessionById(id)

    override suspend fun insertLoggedSet(loggedSet: LoggedSet): Long = gainsDao.insertLoggedSet(loggedSet)
    override suspend fun updateLoggedSet(loggedSet: LoggedSet) = gainsDao.updateLoggedSet(loggedSet)
    override suspend fun deleteLoggedSet(loggedSet: LoggedSet) = gainsDao.deleteLoggedSet(loggedSet)

    override fun getLoggedSetsForSession(sessionId: Long): Flow<List<LoggedSetWithExercise>> =
        gainsDao.getLoggedSetsForSession(sessionId)

    override fun getSessionById(sessionId: Long): Flow<WorkoutSession?> =
        gainsDao.getSessionById(sessionId)

    override suspend fun insertExercises(exercises: List<Exercise>) = gainsDao.insertExercises(exercises)

    override suspend fun syncExercises(sheetUrl: String): Result<Unit> {
        val client = HttpClient(OkHttp)
        return try {
            val response = client.get(sheetUrl)
            val csvText = response.bodyAsText()
            val parsedExercises = CsvParser.parseExercises(csvText)
            
            if (parsedExercises.isEmpty()) {
                return Result.failure(Exception("No exercises found in Sheet CSV"))
            }

            // Sync with existing database records
            val existing = gainsDao.getAllExercisesList()
            val existingByName = existing.associateBy { it.name.lowercase() }

            for (sheetExercise in parsedExercises) {
                val match = existingByName[sheetExercise.name.lowercase()]
                if (match != null) {
                    if (match.muscleGroup != sheetExercise.muscleGroup) {
                        gainsDao.updateExercise(match.copy(muscleGroup = sheetExercise.muscleGroup))
                    }
                } else {
                    gainsDao.insertExercise(sheetExercise)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            client.close()
        }
    }

    override suspend fun insertLabel(label: WorkoutLabel): Long = gainsDao.insertLabel(label)

    override suspend fun deleteLabel(label: WorkoutLabel) {
        gainsDao.clearSessionLabelId(label.id)
        gainsDao.deleteLabel(label)
    }

    override suspend fun updateProfile(profile: UserProfile) {
        gainsDao.insertOrUpdateProfile(profile)
    }

    override fun getExerciseById(exerciseId: Int): Flow<Exercise?> =
        gainsDao.getExerciseById(exerciseId)

    override fun getHistoryForExercise(exerciseId: Int): Flow<List<LoggedSetWithSession>> =
        gainsDao.getHistoryForExercise(exerciseId)

    override suspend fun updateExerciseNotes(exerciseId: Int, notes: String?) =
        gainsDao.updateExerciseNotes(exerciseId, notes)

    // Templates
    override val allTemplatesWithDetails: Flow<List<WorkoutTemplateWithDetails>> =
        gainsDao.getAllTemplatesWithDetails()

    override fun getTemplateById(id: Long): Flow<WorkoutTemplate?> =
        gainsDao.getTemplateById(id)

    override fun getTemplateSetsForTemplate(templateId: Long): Flow<List<TemplateSetWithExercise>> =
        gainsDao.getTemplateSetsForTemplate(templateId)

    override suspend fun insertTemplate(template: WorkoutTemplate): Long =
        gainsDao.insertTemplate(template)

    override suspend fun updateTemplate(template: WorkoutTemplate) =
        gainsDao.updateTemplate(template)

    override suspend fun deleteTemplateById(id: Long) =
        gainsDao.deleteTemplateById(id)

    override suspend fun insertTemplateSet(templateSet: TemplateSet): Long =
        gainsDao.insertTemplateSet(templateSet)

    override suspend fun getLatestCompletedSetsForExercise(exerciseId: Int): List<LoggedSet> =
        gainsDao.getLatestCompletedSetsForExercise(exerciseId)

    override suspend fun updateTemplateSet(templateSet: TemplateSet) =
        gainsDao.updateTemplateSet(templateSet)

    override suspend fun deleteTemplateSet(templateSet: TemplateSet) =
        gainsDao.deleteTemplateSet(templateSet)

    override suspend fun createSessionFromTemplate(templateId: Long, name: String?, autoLoadPrevious: Boolean): Long {
        val template = gainsDao.getTemplateByIdSync(templateId) ?: return 0L
        val sessionName = name ?: template.name
        val newSession = WorkoutSession(
            timestamp = System.currentTimeMillis(),
            name = sessionName,
            workoutType = template.workoutType,
            labelId = template.labelId
        )
        val sessionId = gainsDao.insertSession(newSession)
        val templateSets = gainsDao.getTemplateSetsList(templateId)

        if (autoLoadPrevious) {
            val exerciseGrouped = templateSets.groupBy { it.exerciseId }
            exerciseGrouped.forEach { (exId, tSets) ->
                val prevSets = gainsDao.getLatestCompletedSetsForExercise(exId)
                if (prevSets.isNotEmpty()) {
                    prevSets.forEachIndexed { index, ps ->
                        gainsDao.insertLoggedSet(
                            LoggedSet(
                                sessionId = sessionId,
                                exerciseId = exId,
                                setNumber = index + 1,
                                weight = ps.weight,
                                reps = ps.reps,
                                isCompleted = false
                            )
                        )
                    }
                } else {
                    tSets.forEach { ts ->
                        gainsDao.insertLoggedSet(
                            LoggedSet(
                                sessionId = sessionId,
                                exerciseId = ts.exerciseId,
                                setNumber = ts.setNumber,
                                weight = ts.targetWeight,
                                reps = ts.targetReps,
                                isCompleted = false
                            )
                        )
                    }
                }
            }
        } else {
            templateSets.forEach { ts ->
                gainsDao.insertLoggedSet(
                    LoggedSet(
                        sessionId = sessionId,
                        exerciseId = ts.exerciseId,
                        setNumber = ts.setNumber,
                        weight = ts.targetWeight,
                        reps = ts.targetReps,
                        isCompleted = false
                    )
                )
            }
        }
        return sessionId
    }

    override suspend fun createTemplateFromSession(sessionId: Long, templateName: String): Long {
        val sets = gainsDao.getLoggedSetsForSession(sessionId) // Flow, let's get logged sets
        val newTemplate = WorkoutTemplate(
            name = templateName,
            createdAt = System.currentTimeMillis()
        )
        val templateId = gainsDao.insertTemplate(newTemplate)
        return templateId
    }

    override val allMetricsWithLatest: Flow<List<MetricWithLatestEntry>> =
        gainsDao.getMetricsWithLatestEntries()

    override fun getEntriesForMetric(metricId: Long): Flow<List<MetricEntry>> =
        gainsDao.getEntriesForMetric(metricId)

    override suspend fun insertMetricEntry(entry: MetricEntry): Long =
        gainsDao.insertMetricEntry(entry)

    override suspend fun updateMetricEntry(entry: MetricEntry) =
        gainsDao.updateMetricEntry(entry)

    override suspend fun deleteMetricEntry(entry: MetricEntry) =
        gainsDao.deleteMetricEntry(entry)

    override suspend fun updateMetricGoal(id: Long, targetValue: Float?, targetDate: Long?) =
        gainsDao.updateMetricGoal(id, targetValue, targetDate)

    override suspend fun updateMetricSource(id: Long, source: String) =
        gainsDao.updateMetricSource(id, source)

    override suspend fun ensureDefaultMetricsSeeded() {
        val all = gainsDao.getAllMetricDefinitions().firstOrNull() ?: emptyList()
        if (all.isEmpty()) {
            val country = java.util.Locale.getDefault().country
            val unit = if (country == "US" || country == "LR" || country == "MM") "lbs" else "kg"
            gainsDao.insertMetricDefinition(
                MetricDefinition(name = "Body Weight", unit = unit, isSystem = true, displayOrder = 1)
            )
        }
    }

    override val allExternalActivities: Flow<List<ExternalActivity>> =
        gainsDao.getAllExternalActivities()

    override suspend fun syncHealthConnect(context: android.content.Context): Result<Int> {
        return com.example.gains.data.health.HealthConnectManager.syncData(context, gainsDao)
    }
}
