package com.example.gains.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val muscleGroup: String
)

@Entity(tableName = "workout_labels")
data class WorkoutLabel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val colorHex: String
)

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val name: String,
    val workoutType: String = "GYM",
    val endTime: Long = 0L,
    val labelId: Int? = null
)

@Entity(
    tableName = "logged_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["exerciseId"])
    ]
)
data class LoggedSet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Long,
    val exerciseId: Int,
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val isCompleted: Boolean = false
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1, // Enforce single active profile
    val name: String = "Wouter",
    val photoUri: String? = null,
    val height: Double? = null,
    val age: Int? = null,
    val currentWeight: Double? = null
)
