package com.example.gains.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class LoggedSetWithExercise(
    val id: Int,
    val sessionId: Long,
    val exerciseId: Int,
    val exerciseName: String,
    val exerciseMuscleGroup: String,
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val isCompleted: Boolean
)

data class WorkoutSessionWithLabel(
    val id: Long,
    val timestamp: Long,
    val name: String,
    val workoutType: String,
    val endTime: Long,
    val labelId: Int?,
    val labelName: String?,
    val labelColorHex: String?
)

data class LoggedSetWithSession(
    val id: Int,
    val sessionId: Long,
    val sessionTimestamp: Long,
    val sessionName: String,
    val exerciseId: Int,
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val isCompleted: Boolean
)

@Dao
interface GainsDao {
    // Exercises
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExercise(exercise: Exercise): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExercises(exercises: List<Exercise>)

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Query("SELECT * FROM exercises")
    suspend fun getAllExercisesList(): List<Exercise>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getExerciseCount(): Int

    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    fun getExerciseById(exerciseId: Int): Flow<Exercise?>

    @Query("UPDATE exercises SET notes = :notes WHERE id = :exerciseId")
    suspend fun updateExerciseNotes(exerciseId: Int, notes: String?)

    @Query("""
        SELECT s.id, s.sessionId, w.timestamp AS sessionTimestamp, w.name AS sessionName,
               s.exerciseId, s.setNumber, s.weight, s.reps, s.isCompleted
        FROM logged_sets s
        INNER JOIN workout_sessions w ON s.sessionId = w.id
        WHERE s.exerciseId = :exerciseId AND s.isCompleted = 1
        ORDER BY w.timestamp DESC, s.id ASC
    """)
    fun getHistoryForExercise(exerciseId: Int): Flow<List<LoggedSetWithSession>>

    // Workout Sessions
    @Query("SELECT * FROM workout_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

    @Query("""
        SELECT s.id, s.timestamp, s.name, s.workoutType, s.endTime, s.labelId,
               l.name AS labelName, l.colorHex AS labelColorHex
        FROM workout_sessions s
        LEFT JOIN workout_labels l ON s.labelId = l.id
        ORDER BY s.timestamp DESC
    """)
    fun getAllSessionsWithLabels(): Flow<List<WorkoutSessionWithLabel>>

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    fun getSessionById(sessionId: Long): Flow<WorkoutSession?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession): Long

    @Update
    suspend fun updateSession(session: WorkoutSession)

    @Delete
    suspend fun deleteSession(session: WorkoutSession)

    // Logged Sets
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoggedSet(loggedSet: LoggedSet): Long

    @Update
    suspend fun updateLoggedSet(loggedSet: LoggedSet)

    @Delete
    suspend fun deleteLoggedSet(loggedSet: LoggedSet)

    @Query("""
        SELECT s.id, s.sessionId, s.exerciseId, e.name AS exerciseName, e.muscleGroup AS exerciseMuscleGroup,
               s.setNumber, s.weight, s.reps, s.isCompleted
        FROM logged_sets s
        INNER JOIN exercises e ON s.exerciseId = e.id
        WHERE s.sessionId = :sessionId
        ORDER BY s.id ASC
    """)
    fun getLoggedSetsForSession(sessionId: Long): Flow<List<LoggedSetWithExercise>>

    // Workout Labels
    @Query("SELECT * FROM workout_labels ORDER BY name ASC")
    fun getAllLabels(): Flow<List<WorkoutLabel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabel(label: WorkoutLabel): Long

    @Delete
    suspend fun deleteLabel(label: WorkoutLabel)

    @Query("UPDATE workout_sessions SET labelId = NULL WHERE labelId = :labelId")
    suspend fun clearSessionLabelId(labelId: Int)

    // User Profile
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfile)
}
