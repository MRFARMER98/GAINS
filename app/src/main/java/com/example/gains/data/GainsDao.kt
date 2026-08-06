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

    // Workout Sessions
    @Query("SELECT * FROM workout_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

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
}
