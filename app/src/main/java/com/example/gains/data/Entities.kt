package com.example.gains.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val muscleGroup: String,
    val notes: String? = null
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
    val birthDateTimestamp: Long? = null,
    val currentWeight: Double? = null
) {
    val calculatedAge: Int?
        get() {
            val dob = birthDateTimestamp ?: return age
            if (dob <= 0L) return age
            val dobCal = java.util.Calendar.getInstance().apply { timeInMillis = dob }
            val nowCal = java.util.Calendar.getInstance()
            var years = nowCal.get(java.util.Calendar.YEAR) - dobCal.get(java.util.Calendar.YEAR)
            if (nowCal.get(java.util.Calendar.DAY_OF_YEAR) < dobCal.get(java.util.Calendar.DAY_OF_YEAR)) {
                years--
            }
            return years.coerceAtLeast(0)
        }
}

@Entity(tableName = "planned_sessions")
data class PlannedSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateTimestamp: Long,
    val name: String,
    val workoutType: String = "GYM",
    val labelId: Int? = null,
    val isCompleted: Boolean = false,
    val templateId: Long? = null
)

@Entity(tableName = "workout_templates")
data class WorkoutTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val workoutType: String = "GYM",
    val labelId: Int? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "template_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplate::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
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
        Index(value = ["templateId"]),
        Index(value = ["exerciseId"])
    ]
)
data class TemplateSet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val templateId: Long,
    val exerciseId: Int,
    val setNumber: Int,
    val targetWeight: Double,
    val targetReps: Int
)

@Entity(tableName = "metric_definitions")
data class MetricDefinition(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val unit: String,
    val isSystem: Boolean,
    val displayOrder: Int,
    val targetValue: Float? = null,
    val targetDate: Long? = null,
    val source: String = "MANUAL"
)

@Entity(
    tableName = "metric_entries",
    foreignKeys = [
        ForeignKey(
            entity = MetricDefinition::class,
            parentColumns = ["id"],
            childColumns = ["metricId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["metricId"])
    ]
)
data class MetricEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val metricId: Long,
    val timestamp: Long,
    val value: Float,
    val externalId: String? = null
)

@Entity(
    tableName = "external_activities",
    indices = [
        Index(value = ["externalId"], unique = true)
    ]
)
data class ExternalActivity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val externalId: String,
    val title: String,
    val activityType: String,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val distanceMeters: Double? = null,
    val caloriesKcal: Double? = null,
    val sourceApp: String? = null
)
