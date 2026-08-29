package com.example.gains.data

import com.example.gains.data.sync.CsvParser
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.Flow

interface DataRepository {
    val allExercises: Flow<List<Exercise>>
    val allSessions: Flow<List<WorkoutSession>>
    val allSessionsWithLabels: Flow<List<WorkoutSessionWithLabel>>
    val allLabels: Flow<List<WorkoutLabel>>
    val userProfile: Flow<UserProfile?>

    suspend fun insertSession(session: WorkoutSession): Long
    suspend fun updateSession(session: WorkoutSession)
    suspend fun deleteSession(session: WorkoutSession)

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
    suspend fun updateExerciseNotes(exerciseId: Int, notes: String?)
}

class DefaultDataRepository(private val gainsDao: GainsDao) : DataRepository {
    override val allExercises: Flow<List<Exercise>> = gainsDao.getAllExercises()
    override val allSessions: Flow<List<WorkoutSession>> = gainsDao.getAllSessions()
    override val allSessionsWithLabels: Flow<List<WorkoutSessionWithLabel>> = gainsDao.getAllSessionsWithLabels()
    override val allLabels: Flow<List<WorkoutLabel>> = gainsDao.getAllLabels()
    override val userProfile: Flow<UserProfile?> = gainsDao.getUserProfile()

    override suspend fun insertSession(session: WorkoutSession): Long = gainsDao.insertSession(session)
    override suspend fun updateSession(session: WorkoutSession) = gainsDao.updateSession(session)
    override suspend fun deleteSession(session: WorkoutSession) = gainsDao.deleteSession(session)

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
}
