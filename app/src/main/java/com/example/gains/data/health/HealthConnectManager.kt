package com.example.gains.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.gains.data.ExternalActivity
import com.example.gains.data.GainsDao
import com.example.gains.data.MetricDefinition
import com.example.gains.data.MetricEntry
import java.time.Instant
import java.time.temporal.ChronoUnit

object HealthConnectManager {

    val REQUIRED_PERMISSIONS = setOf(
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(BodyFatRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    )

    fun getSdkStatus(context: Context): Int {
        return HealthConnectClient.getSdkStatus(context)
    }

    fun isAvailable(context: Context): Boolean {
        return getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    suspend fun hasPermissions(context: Context): Boolean {
        if (!isAvailable(context)) return false
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(REQUIRED_PERMISSIONS)
    }

    suspend fun syncData(context: Context, dao: GainsDao): Result<Int> {
        if (!isAvailable(context)) {
            return Result.failure(IllegalStateException("Health Connect is not available on this device."))
        }

        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            if (!granted.containsAll(REQUIRED_PERMISSIONS)) {
                return Result.failure(SecurityException("Health Connect permissions not granted."))
            }

            val thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS)
            val timeRangeFilter = TimeRangeFilter.after(thirtyDaysAgo)
            var totalSyncedCount = 0

            // 1. Sync Weight Records
            val weightResponse = client.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = timeRangeFilter
                )
            )

            if (weightResponse.records.isNotEmpty()) {
                var weightMetric = dao.getMetricDefinitionByName("Body Weight")
                if (weightMetric == null) {
                    val id = dao.insertMetricDefinition(
                        MetricDefinition(name = "Body Weight", unit = "kg", isSystem = true, displayOrder = 1, source = "HEALTH_CONNECT")
                    )
                    weightMetric = dao.getMetricDefinitionById(id)
                }

                if (weightMetric != null) {
                    val existingExternalIds = dao.getAllMetricEntryExternalIds().toSet()
                    val newEntries = weightResponse.records.mapNotNull { record ->
                        val extId = record.metadata.id
                        if (existingExternalIds.contains(extId)) null
                        else {
                            MetricEntry(
                                metricId = weightMetric.id,
                                timestamp = record.time.toEpochMilli(),
                                value = record.weight.inKilograms.toFloat(),
                                externalId = extId
                            )
                        }
                    }
                    if (newEntries.isNotEmpty()) {
                        dao.insertMetricEntries(newEntries)
                        totalSyncedCount += newEntries.size
                    }
                }
            }

            // 2. Sync Body Fat Records
            val bodyFatResponse = client.readRecords(
                ReadRecordsRequest(
                    recordType = BodyFatRecord::class,
                    timeRangeFilter = timeRangeFilter
                )
            )

            if (bodyFatResponse.records.isNotEmpty()) {
                var bodyFatMetric = dao.getMetricDefinitionByName("Body Fat (%)")
                if (bodyFatMetric == null) {
                    val id = dao.insertMetricDefinition(
                        MetricDefinition(name = "Body Fat (%)", unit = "%", isSystem = false, displayOrder = 2, source = "HEALTH_CONNECT")
                    )
                    bodyFatMetric = dao.getMetricDefinitionById(id)
                }

                if (bodyFatMetric != null) {
                    val existingExternalIds = dao.getAllMetricEntryExternalIds().toSet()
                    val newEntries = bodyFatResponse.records.mapNotNull { record ->
                        val extId = record.metadata.id
                        if (existingExternalIds.contains(extId)) null
                        else {
                            MetricEntry(
                                metricId = bodyFatMetric.id,
                                timestamp = record.time.toEpochMilli(),
                                value = record.percentage.value.toFloat(),
                                externalId = extId
                            )
                        }
                    }
                    if (newEntries.isNotEmpty()) {
                        dao.insertMetricEntries(newEntries)
                        totalSyncedCount += newEntries.size
                    }
                }
            }

            // 3. Sync Exercise Sessions (Workouts)
            val exerciseResponse = client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = timeRangeFilter
                )
            )

            if (exerciseResponse.records.isNotEmpty()) {
                val existingActivityIds = dao.getAllExternalActivityIds().toSet()
                val newActivities = exerciseResponse.records.mapNotNull { record ->
                    val extId = record.metadata.id
                    if (existingActivityIds.contains(extId)) null
                    else {
                        val duration = ChronoUnit.SECONDS.between(record.startTime, record.endTime)
                        val title = record.title?.ifBlank { null } ?: formatExerciseType(record.exerciseType)
                        val sourceApp = record.metadata.dataOrigin.packageName

                        ExternalActivity(
                            externalId = extId,
                            title = title,
                            activityType = formatExerciseType(record.exerciseType),
                            startTime = record.startTime.toEpochMilli(),
                            endTime = record.endTime.toEpochMilli(),
                            durationSeconds = duration,
                            sourceApp = sourceApp
                        )
                    }
                }
                if (newActivities.isNotEmpty()) {
                    dao.insertExternalActivities(newActivities)
                    totalSyncedCount += newActivities.size
                }
            }

            Result.success(totalSyncedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatExerciseType(type: Int): String {
        return when (type) {
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "Running"
            ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "Walking"
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> "Cycling"
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "Swimming"
            ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> "HIIT"
            ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> "Gym / Weightlifting"
            else -> "Workout"
        }
    }
}
