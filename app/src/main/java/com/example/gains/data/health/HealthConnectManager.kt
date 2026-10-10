package com.example.gains.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseRouteResult
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
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
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        "android.permission.health.READ_EXERCISE_ROUTES",
        "android.permission.health.READ_HEALTH_DATA_HISTORY"
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

    fun isSamsungHealth(packageName: String?): Boolean {
        if (packageName == null) return false
        val lower = packageName.lowercase()
        return lower.contains("shealth") || lower.contains("samsung") || lower.contains("sec.android")
    }

    fun formatSourceApp(packageName: String?): String {
        if (packageName.isNullOrEmpty()) return "Health Connect"
        val lower = packageName.lowercase()
        return when {
            lower.contains("nike") || lower.contains("plusgps") -> "Nike Run Club"
            lower.contains("garmin") -> "Garmin Connect"
            lower.contains("shealth") || lower.contains("samsung") || lower.contains("sec.android") -> "Samsung Health"
            lower.contains("fitness") || lower.contains("google.android.apps.fitness") -> "Google Fit"
            lower.contains("fitbit") -> "Fitbit"
            lower.contains("strava") -> "Strava"
            lower.contains("mapmyrun") -> "MapMyRun"
            lower.contains("runkeeper") -> "Runkeeper"
            lower.contains("runtastic") || lower.contains("adidas") -> "Adidas Running"
            else -> packageName
        }
    }

    suspend fun syncData(context: Context, dao: GainsDao): Result<Int> {
        if (!isAvailable(context)) {
            return Result.failure(IllegalStateException("Health Connect is not available on this device."))
        }

        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            if (granted.isEmpty()) {
                return Result.failure(SecurityException("Health Connect permissions not granted."))
            }

            // Clean up existing DB activities with legacy raw package names
            try {
                val existingList = dao.getAllExternalActivitiesList()
                for (activity in existingList) {
                    val formatted = formatSourceApp(activity.sourceApp)
                    if (formatted != activity.sourceApp) {
                        dao.updateExternalActivity(activity.copy(sourceApp = formatted))
                    }
                }
            } catch (e: Exception) {
                // Ignore cleanup error if any
            }

            // Sync boundary: August 1st of current year (or previous year if before August)
            val now = java.time.LocalDate.now()
            val year = if (now.monthValue < 8) now.year - 1 else now.year
            val augustFirst = java.time.ZonedDateTime.of(year, 8, 1, 0, 0, 0, 0, java.time.ZoneId.systemDefault()).toInstant()
            val timeRangeFilter = TimeRangeFilter.after(augustFirst)
            var totalSyncedCount = 0

            // 1. Sync Weight Records
            val weightResponse = try {
                client.readRecords(
                    ReadRecordsRequest(
                        recordType = WeightRecord::class,
                        timeRangeFilter = timeRangeFilter
                    )
                )
            } catch (e: Exception) { null }

            if (weightResponse != null && weightResponse.records.isNotEmpty()) {
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
            val bodyFatResponse = try {
                client.readRecords(
                    ReadRecordsRequest(
                        recordType = BodyFatRecord::class,
                        timeRangeFilter = timeRangeFilter
                    )
                )
            } catch (e: Exception) { null }

            if (bodyFatResponse != null && bodyFatResponse.records.isNotEmpty()) {
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

            // 3. Sync Steps (Aggregated per day, prioritizing Samsung Health)
            val stepsResponse = try {
                client.readRecords(
                    ReadRecordsRequest(
                        recordType = StepsRecord::class,
                        timeRangeFilter = timeRangeFilter
                    )
                )
            } catch (e: Exception) { null }

            if (stepsResponse != null && stepsResponse.records.isNotEmpty()) {
                var stepsMetric = dao.getMetricDefinitionByName("Daily Steps")
                if (stepsMetric == null) {
                    val id = dao.insertMetricDefinition(
                        MetricDefinition(name = "Daily Steps", unit = "steps", isSystem = true, displayOrder = 3, targetValue = 10000f, source = "HEALTH_CONNECT")
                    )
                    stepsMetric = dao.getMetricDefinitionById(id)
                }

                if (stepsMetric != null) {
                    // Filter records: Prefer Samsung Health records if present; otherwise pick app package with highest step volume
                    val samsungSteps = stepsResponse.records.filter { isSamsungHealth(it.metadata.dataOrigin.packageName) }
                    val targetStepRecords = if (samsungSteps.isNotEmpty()) {
                        samsungSteps
                    } else {
                        val byPkg = stepsResponse.records.groupBy { it.metadata.dataOrigin.packageName }
                        byPkg.maxByOrNull { it.value.sumOf { r -> r.count } }?.value ?: stepsResponse.records
                    }

                    // Clean up any legacy non-aggregated interval entries
                    try {
                        dao.deleteMetricEntriesNotMatchingPrefix(stepsMetric.id, "daily_steps_%")
                    } catch (e: Exception) { /* ignore */ }

                    val zoneId = java.time.ZoneId.systemDefault()
                    val today = java.time.LocalDate.now()
                    val stepsByDate = targetStepRecords.groupBy { record ->
                        record.startTime.atZone(zoneId).toLocalDate()
                    }

                    for ((date, dayRecords) in stepsByDate) {
                        val totalSteps = dayRecords.sumOf { it.count }
                        val extId = "daily_steps_$date"
                        val dayTimestamp = if (date == today) {
                            System.currentTimeMillis()
                        } else {
                            date.atTime(23, 59, 59).atZone(zoneId).toInstant().toEpochMilli()
                        }

                        val existing = dao.getMetricEntryByExternalId(extId)
                        if (existing != null) {
                            if (existing.value != totalSteps.toFloat() || (date == today && existing.timestamp != dayTimestamp)) {
                                dao.updateMetricEntry(existing.copy(value = totalSteps.toFloat(), timestamp = dayTimestamp))
                                totalSyncedCount++
                            }
                        } else {
                            dao.insertMetricEntry(
                                MetricEntry(
                                    metricId = stepsMetric.id,
                                    timestamp = dayTimestamp,
                                    value = totalSteps.toFloat(),
                                    externalId = extId
                                )
                            )
                            totalSyncedCount++
                        }
                    }
                }
            }

            // 4. Sync Sleep Sessions (Aggregated per wake-up day, prioritizing Samsung Health)
            val sleepResponse = try {
                client.readRecords(
                    ReadRecordsRequest(
                        recordType = SleepSessionRecord::class,
                        timeRangeFilter = timeRangeFilter
                    )
                )
            } catch (e: Exception) { null }

            if (sleepResponse != null && sleepResponse.records.isNotEmpty()) {
                var sleepMetric = dao.getMetricDefinitionByName("Sleep")
                if (sleepMetric == null) {
                    val id = dao.insertMetricDefinition(
                        MetricDefinition(name = "Sleep", unit = "hrs", isSystem = true, displayOrder = 4, targetValue = 8.0f, source = "HEALTH_CONNECT")
                    )
                    sleepMetric = dao.getMetricDefinitionById(id)
                }

                if (sleepMetric != null) {
                    val samsungSleep = sleepResponse.records.filter { isSamsungHealth(it.metadata.dataOrigin.packageName) }
                    val targetSleepRecords = if (samsungSleep.isNotEmpty()) {
                        samsungSleep
                    } else {
                        val byPkg = sleepResponse.records.groupBy { it.metadata.dataOrigin.packageName }
                        byPkg.maxByOrNull { it.value.sumOf { r -> ChronoUnit.MINUTES.between(r.startTime, r.endTime) } }?.value ?: sleepResponse.records
                    }

                    // Clean up legacy non-daily sleep entries
                    try {
                        dao.deleteMetricEntriesNotMatchingPrefix(sleepMetric.id, "daily_sleep_%")
                    } catch (e: Exception) { /* ignore */ }

                    val zoneId = java.time.ZoneId.systemDefault()
                    val sleepByWakeDate = targetSleepRecords.groupBy { record ->
                        record.endTime.atZone(zoneId).toLocalDate()
                    }

                    for ((date, dayRecords) in sleepByWakeDate) {
                        val totalMinutes = dayRecords.sumOf { ChronoUnit.MINUTES.between(it.startTime, it.endTime) }
                        val hours = totalMinutes / 60.0f
                        val extId = "daily_sleep_$date"
                        val timestamp = dayRecords.maxOf { it.endTime }.toEpochMilli()

                        val existing = dao.getMetricEntryByExternalId(extId)
                        if (existing != null) {
                            if (Math.abs(existing.value - hours) > 0.01f || existing.timestamp != timestamp) {
                                dao.updateMetricEntry(existing.copy(value = hours, timestamp = timestamp))
                                totalSyncedCount++
                            }
                        } else {
                            dao.insertMetricEntry(
                                MetricEntry(
                                    metricId = sleepMetric.id,
                                    timestamp = timestamp,
                                    value = hours,
                                    externalId = extId
                                )
                            )
                            totalSyncedCount++
                        }
                    }
                }
            }

            // 5. Bulk Read Distance & Calories Records for entire time window
            val allDistances = try {
                client.readRecords(ReadRecordsRequest(recordType = DistanceRecord::class, timeRangeFilter = timeRangeFilter)).records
            } catch (e: Exception) { emptyList() }

            val allTotalCalories = try {
                client.readRecords(ReadRecordsRequest(recordType = TotalCaloriesBurnedRecord::class, timeRangeFilter = timeRangeFilter)).records
            } catch (e: Exception) { emptyList() }

            val allActiveCalories = try {
                client.readRecords(ReadRecordsRequest(recordType = ActiveCaloriesBurnedRecord::class, timeRangeFilter = timeRangeFilter)).records
            } catch (e: Exception) { emptyList() }

            // 6. Sync Exercise Sessions (Workouts)
            val exerciseResponse = try {
                client.readRecords(
                    ReadRecordsRequest(
                        recordType = ExerciseSessionRecord::class,
                        timeRangeFilter = timeRangeFilter
                    )
                )
            } catch (e: Exception) { null }

            if (exerciseResponse != null && exerciseResponse.records.isNotEmpty()) {
                for (record in exerciseResponse.records) {
                    val extId = record.metadata.id
                    val duration = ChronoUnit.SECONDS.between(record.startTime, record.endTime)
                    val rawSource = record.metadata.dataOrigin.packageName
                    val formattedSource = formatSourceApp(rawSource)
                    val title = record.title?.ifBlank { null } ?: if (formattedSource == "Nike Run Club") "Nike Run Club Run" else formatExerciseType(record.exerciseType)

                    val startBuf = record.startTime.minus(2, ChronoUnit.MINUTES)
                    val endBuf = record.endTime.plus(2, ChronoUnit.MINUTES)

                    // Match Distance records from SAME app package name first to prevent multi-app triple counting
                    val sameOriginDistances = allDistances.filter { d ->
                        d.metadata.dataOrigin.packageName == rawSource &&
                        d.startTime.isBefore(endBuf) && d.endTime.isAfter(startBuf)
                    }
                    val sessionDistances = if (sameOriginDistances.isNotEmpty()) {
                        sameOriginDistances
                    } else {
                        val fallbackMatching = allDistances.filter { d ->
                            d.startTime.isBefore(endBuf) && d.endTime.isAfter(startBuf)
                        }
                        val groupedByOrigin = fallbackMatching.groupBy { it.metadata.dataOrigin.packageName }
                        groupedByOrigin.values.maxByOrNull { list -> list.sumOf { it.distance.inMeters } } ?: emptyList()
                    }
                    val distanceMeters = sessionDistances.sumOf { it.distance.inMeters }.takeIf { it > 0 }

                    // Match Calories records from SAME app package name first
                    val sameOriginTotalCal = allTotalCalories.filter { c ->
                        c.metadata.dataOrigin.packageName == rawSource &&
                        c.startTime.isBefore(endBuf) && c.endTime.isAfter(startBuf)
                    }
                    val sameOriginActiveCal = allActiveCalories.filter { c ->
                        c.metadata.dataOrigin.packageName == rawSource &&
                        c.startTime.isBefore(endBuf) && c.endTime.isAfter(startBuf)
                    }

                    val totalCalSum = if (sameOriginTotalCal.isNotEmpty()) {
                        sameOriginTotalCal.sumOf { it.energy.inKilocalories }
                    } else {
                        val fallbackTotal = allTotalCalories.filter { c -> c.startTime.isBefore(endBuf) && c.endTime.isAfter(startBuf) }
                        val grouped = fallbackTotal.groupBy { it.metadata.dataOrigin.packageName }
                        grouped.values.maxOfOrNull { list -> list.sumOf { it.energy.inKilocalories } } ?: 0.0
                    }

                    val activeCalSum = if (sameOriginActiveCal.isNotEmpty()) {
                        sameOriginActiveCal.sumOf { it.energy.inKilocalories }
                    } else {
                        val fallbackActive = allActiveCalories.filter { c -> c.startTime.isBefore(endBuf) && c.endTime.isAfter(startBuf) }
                        val grouped = fallbackActive.groupBy { it.metadata.dataOrigin.packageName }
                        grouped.values.maxOfOrNull { list -> list.sumOf { it.energy.inKilocalories } } ?: 0.0
                    }

                    val caloriesKcal = (if (activeCalSum > 0) activeCalSum else totalCalSum).takeIf { it > 0 }

                    // Read Exercise Route location points
                    val routeJson = try {
                        val routeResult = record.exerciseRouteResult
                        if (routeResult is ExerciseRouteResult.Data) {
                            val routeList = routeResult.exerciseRoute.route
                            if (routeList.isNotEmpty()) {
                                routeList.joinToString(prefix = "[", postfix = "]") { loc ->
                                    """{"lat":${loc.latitude},"lng":${loc.longitude}}"""
                                }
                            } else null
                        } else null
                    } catch (e: Exception) {
                        null
                    }

                    val existing = dao.getExternalActivityByExtId(extId)
                    if (existing != null) {
                        val updated = existing.copy(
                            title = title,
                            distanceMeters = distanceMeters,
                            caloriesKcal = caloriesKcal,
                            routeJson = routeJson ?: existing.routeJson,
                            sourceApp = formattedSource
                        )
                        if (updated != existing) {
                            dao.updateExternalActivity(updated)
                            totalSyncedCount++
                        }
                    } else {
                        val newAct = ExternalActivity(
                            externalId = extId,
                            title = title,
                            activityType = formatExerciseType(record.exerciseType),
                            startTime = record.startTime.toEpochMilli(),
                            endTime = record.endTime.toEpochMilli(),
                            durationSeconds = duration,
                            distanceMeters = distanceMeters,
                            caloriesKcal = caloriesKcal,
                            sourceApp = formattedSource,
                            routeJson = routeJson
                        )
                        dao.insertExternalActivities(listOf(newAct))
                        totalSyncedCount++
                    }
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
