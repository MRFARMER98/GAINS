package com.example.gains.ui.main.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.example.gains.WorkoutLogger
import com.example.gains.RoutinesPlanner
import com.example.gains.ExerciseLibrary
import androidx.compose.material.icons.filled.CalendarMonth
import com.example.gains.data.ExternalActivity
import com.example.gains.data.PlannedSession
import com.example.gains.data.SettingsManager
import com.example.gains.data.WorkoutLabel
import com.example.gains.data.WorkoutSessionWithLabel
import com.example.gains.theme.*
import com.example.gains.ui.components.DashboardHeaderCard
import com.example.gains.ui.components.GainsCard
import com.example.gains.ui.components.HistoryCard
import com.example.gains.ui.components.SelectWorkoutTypeDialog
import com.example.gains.ui.main.MainScreenUiState
import com.example.gains.ui.main.MainScreenViewModel
import java.text.SimpleDateFormat
import java.util.*

internal fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour in 0..11 -> "Good morning"
        hour in 12..16 -> "Good afternoon"
        hour in 17..21 -> "Good evening"
        else -> "What's up"
    }
}

internal fun calculateStreak(sessions: List<WorkoutSessionWithLabel>): String {
    if (sessions.isEmpty()) return "0 Days"
    val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val activeDays = sessions.map { sdf.format(Date(it.timestamp)) }.toSet()
    
    val cal = Calendar.getInstance()
    var streak = 0
    
    val todayStr = sdf.format(cal.time)
    if (activeDays.contains(todayStr)) {
        streak++
        cal.add(Calendar.DAY_OF_YEAR, -1)
        while (activeDays.contains(sdf.format(cal.time))) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
    } else {
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(cal.time)
        if (activeDays.contains(yesterdayStr)) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
            while (activeDays.contains(sdf.format(cal.time))) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            }
        }
    }
    return "$streak ${if (streak == 1) "Day" else "Days"}"
}

fun getShortSourceBadge(sourceApp: String?): String {
    if (sourceApp.isNullOrBlank()) return "HC"
    val lower = sourceApp.lowercase()
    return when {
        lower.contains("nike") || lower.contains("plusgps") -> "NIKE"
        lower.contains("garmin") -> "GARMIN"
        lower.contains("samsung") || lower.contains("shealth") || lower.contains("sec.android") -> "SAMSUNG"
        lower.contains("fitness") || lower.contains("google") -> "FIT"
        lower.contains("fitbit") -> "FITBIT"
        lower.contains("strava") -> "STRAVA"
        else -> sourceApp.take(10).uppercase()
    }
}

sealed class TimelineItem {
    abstract val timestamp: Long
    abstract val key: String

    data class GainsSession(val session: WorkoutSessionWithLabel) : TimelineItem() {
        override val timestamp: Long get() = session.timestamp
        override val key: String get() = "session_${session.id}"
    }

    data class External(val activity: ExternalActivity) : TimelineItem() {
        override val timestamp: Long get() = activity.startTime
        override val key: String get() = "external_${activity.id}"
    }
}

@Composable
fun WorkoutTabContent(
    state: MainScreenUiState,
    viewModel: MainScreenViewModel,
    plannedSessions: List<PlannedSession> = emptyList(),
    labels: List<WorkoutLabel> = emptyList(),
    settingsManager: SettingsManager? = null,
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    var showWorkoutTypeDialog by remember { mutableStateOf(false) }
    var selectedActivityForDetail by remember { mutableStateOf<ExternalActivity?>(null) }

    val dayKeyFormat = remember { SimpleDateFormat("yyyyMMdd", Locale.getDefault()) }
    val todayStr = remember { dayKeyFormat.format(Date()) }
    val todayPlannedSessions = remember(plannedSessions, todayStr) {
        plannedSessions.filter { planned ->
            val cal = Calendar.getInstance().apply { timeInMillis = planned.dateTimestamp }
            dayKeyFormat.format(cal.time) == todayStr
        }
    }
    val labelsMap = remember(labels) { labels.associateBy { it.id } }

    val disabledSourceApps by (settingsManager?.disabledSourceApps?.collectAsStateWithLifecycle(initialValue = emptySet())
        ?: remember { mutableStateOf(emptySet()) })

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            val workoutsCount = when (state) {
                is MainScreenUiState.Success -> state.sessions.size
                else -> 0
            }
            val streak = when (state) {
                is MainScreenUiState.Success -> calculateStreak(state.sessions)
                else -> "0 Days"
            }

            val profile = (state as? MainScreenUiState.Success)?.userProfile
            val userName = profile?.name ?: "Wouter"
            val photoUri = profile?.photoUri

            // Top Header Card
            DashboardHeaderCard(
                greeting = getGreeting(),
                userName = userName,
                motivationQuote = "Let's fuck shit up today",
                photoUri = photoUri,
                workoutsCount = workoutsCount,
                streak = streak,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
            )

            // Hub Drill-Down Cards (Routines & Exercise Library)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GainsCard(
                    modifier = Modifier.weight(1f),
                    onClick = { onItemClick(RoutinesPlanner) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = InfraredAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "ROUTINES",
                                style = LabelCaps.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Templates & Schedule",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                GainsCard(
                    modifier = Modifier.weight(1f),
                    onClick = { onItemClick(ExerciseLibrary) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = PrimaryCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "EXERCISES",
                                style = LabelCaps.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "100+ Exercise Library",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Today's Scheduled Routine Section
            if (todayPlannedSessions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "TODAY'S SCHEDULED ROUTINE",
                    style = LabelCaps,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                todayPlannedSessions.forEach { planned ->
                    val labelObj = labelsMap[planned.labelId]
                    val tagColor = try {
                        Color(android.graphics.Color.parseColor(labelObj?.colorHex))
                    } catch (e: Exception) {
                        InfraredAccent
                    }

                    val workoutTypeIcon = when (planned.workoutType) {
                        "RUN" -> Icons.AutoMirrored.Filled.DirectionsRun
                        "HYROX" -> Icons.Default.FlashOn
                        else -> Icons.Default.FitnessCenter
                    }

                    GainsCard(
                        onClick = {
                            val tId = planned.templateId
                            if (tId != null && tId > 0) {
                                onItemClick(WorkoutLogger(templateId = tId, isTemplateMode = true, isPlannedMode = true, plannedId = planned.id))
                            } else {
                                viewModel.startPlannedSession(planned) { sessionId ->
                                    onItemClick(WorkoutLogger(sessionId))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.background),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = workoutTypeIcon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = planned.name,
                                            style = BodySemiBold.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (labelObj != null) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(tagColor.copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = labelObj.name.uppercase(),
                                                    style = LabelCaps.copy(fontSize = 8.sp),
                                                    color = tagColor
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Tap for details • Play to start",
                                        style = LabelCaps.copy(fontSize = 9.sp),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    val tId = planned.templateId
                                    if (tId != null && tId > 0) {
                                        onItemClick(WorkoutLogger(templateId = tId, isTemplateMode = true, isPlannedMode = true, plannedId = planned.id))
                                    } else {
                                        viewModel.startPlannedSession(planned) { sessionId ->
                                            onItemClick(WorkoutLogger(sessionId))
                                        }
                                    }
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start Workout",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // History Label
            Text(
                text = "WORKOUT HISTORY",
                style = LabelCaps,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            val externalActivities by viewModel.allExternalActivities.collectAsStateWithLifecycle(initialValue = emptyList())

            when (state) {
                MainScreenUiState.Loading -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(bottom = 20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is MainScreenUiState.Error -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(bottom = 20.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Error loading history: ${state.throwable.message}",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is MainScreenUiState.Success -> {
                    val sessions = state.sessions
                    val timelineItems = remember(sessions, externalActivities, disabledSourceApps) {
                        val itemsList = mutableListOf<TimelineItem>()
                        sessions.forEach { itemsList.add(TimelineItem.GainsSession(it)) }
                        externalActivities.forEach { act ->
                            val appName = act.sourceApp ?: "Health Connect"
                            if (!disabledSourceApps.contains(appName)) {
                                itemsList.add(TimelineItem.External(act))
                            }
                        }
                        itemsList.sortedByDescending { it.timestamp }
                    }

                    if (timelineItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(bottom = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No workouts logged yet.\nTime to make some GAINS!",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 84.dp)
                        ) {
                            items(timelineItems, key = { it.key }) { item ->
                                when (item) {
                                    is TimelineItem.GainsSession -> {
                                        HistoryCard(
                                            session = item.session,
                                            onClick = { onItemClick(WorkoutLogger(item.session.id)) }
                                        )
                                    }
                                    is TimelineItem.External -> {
                                        ExternalActivityCard(
                                            activity = item.activity,
                                            onClick = { onItemClick(com.example.gains.ExternalRunDetail(item.activity.id)) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Circular Action Button (Bottom-Center FAB)
        FloatingActionButton(
            onClick = { showWorkoutTypeDialog = true },
            shape = CircleShape,
            containerColor = InfraredAccent,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 2.dp
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Start Workout",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }

    val allTemplates by viewModel.allTemplates.collectAsStateWithLifecycle(initialValue = emptyList())

    if (showWorkoutTypeDialog) {
        SelectWorkoutTypeDialog(
            templates = allTemplates,
            onDismiss = { showWorkoutTypeDialog = false },
            onTypeSelect = { workoutType ->
                showWorkoutTypeDialog = false
                viewModel.startNewSession(workoutType) { sessionId ->
                    onItemClick(WorkoutLogger(sessionId))
                }
            },
            onTemplateSelect = { templateId ->
                showWorkoutTypeDialog = false
                viewModel.createSessionFromTemplate(templateId) { sessionId ->
                    onItemClick(WorkoutLogger(sessionId))
                }
            }
        )
    }

    if (selectedActivityForDetail != null) {
        ExternalActivityDetailDialog(
            activity = selectedActivityForDetail!!,
            onDismiss = { selectedActivityForDetail = null }
        )
    }
}

data class RoutePoint(val lat: Double, val lng: Double)

fun parseRouteJson(json: String?): List<RoutePoint> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val points = mutableListOf<RoutePoint>()
        val regex = """\{"lat":\s*([0-9.-]+),\s*"lng":\s*([0-9.-]+)\}""".toRegex()
        regex.findAll(json).forEach { match ->
            val lat = match.groupValues[1].toDoubleOrNull()
            val lng = match.groupValues[2].toDoubleOrNull()
            if (lat != null && lng != null) {
                points.add(RoutePoint(lat, lng))
            }
        }
        points
    } catch (e: Exception) {
        emptyList()
    }
}

@Composable
fun RouteMapThumbnail(
    points: List<RoutePoint>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) return

    val minLat = points.minOf { it.lat }
    val maxLat = points.maxOf { it.lat }
    val minLng = points.minOf { it.lng }
    val maxLng = points.maxOf { it.lng }

    val latRange = (maxLat - minLat).coerceAtLeast(0.0001)
    val lngRange = (maxLng - minLng).coerceAtLeast(0.0001)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF16161A))
    ) {
        val padding = 20.dp.toPx()
        val width = size.width - (padding * 2)
        val height = size.height - (padding * 2)

        val scaleX = width / lngRange
        val scaleY = height / latRange
        val scale = minOf(scaleX, scaleY)

        val offsetX = padding + (width - (lngRange * scale)) / 2.0
        val offsetY = padding + (height - (latRange * scale)) / 2.0

        val canvasPoints = points.map { pt ->
            val x = (offsetX + (pt.lng - minLng) * scale).toFloat()
            val y = (offsetY + (maxLat - pt.lat) * scale).toFloat()
            Offset(x, y)
        }

        // Tactical Map Grid Background Lines
        val gridStep = 18.dp.toPx()
        var gx = 0f
        while (gx < size.width) {
            drawLine(
                color = Color.White.copy(alpha = 0.035f),
                start = Offset(gx, 0f),
                end = Offset(gx, size.height),
                strokeWidth = 1.dp.toPx()
            )
            gx += gridStep
        }
        var gy = 0f
        while (gy < size.height) {
            drawLine(
                color = Color.White.copy(alpha = 0.035f),
                start = Offset(0f, gy),
                end = Offset(size.width, gy),
                strokeWidth = 1.dp.toPx()
            )
            gy += gridStep
        }

        // Draw Route Path
        val path = Path()
        path.moveTo(canvasPoints.first().x, canvasPoints.first().y)
        for (i in 1 until canvasPoints.size) {
            path.lineTo(canvasPoints[i].x, canvasPoints[i].y)
        }

        // Outer Glow Layer
        drawPath(
            path = path,
            color = accentColor.copy(alpha = 0.35f),
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Core Polyline Track
        drawPath(
            path = path,
            color = accentColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Start Point (Green)
        drawCircle(
            color = Color(0xFF34C759),
            radius = 5.dp.toPx(),
            center = canvasPoints.first()
        )
        drawCircle(
            color = Color.White,
            radius = 2.dp.toPx(),
            center = canvasPoints.first()
        )

        // Finish Point (Red/Orange)
        drawCircle(
            color = Color(0xFFFF3B30),
            radius = 5.dp.toPx(),
            center = canvasPoints.last()
        )
        drawCircle(
            color = Color.White,
            radius = 2.dp.toPx(),
            center = canvasPoints.last()
        )
    }
}

@Composable
fun ExternalActivityCard(
    activity: ExternalActivity,
    onClick: () -> Unit = {}
) {
    val date = remember(activity.startTime) { Date(activity.startTime) }
    val dayOfWeek = remember(activity.startTime) { SimpleDateFormat("EEEE", Locale.getDefault()).format(date) }
    val dateStr = remember(activity.startTime) { SimpleDateFormat("MMM dd, yyyy  •  hh:mm a", Locale.getDefault()).format(date) }
    val isNikeRunClub = remember(activity.sourceApp) {
        val app = activity.sourceApp?.lowercase() ?: ""
        app.contains("nike") || app.contains("plusgps")
    }

    val shortBadge = remember(activity.sourceApp) { getShortSourceBadge(activity.sourceApp) }

    val durationFormatted = remember(activity.durationSeconds) {
        val m = activity.durationSeconds / 60
        val s = activity.durationSeconds % 60
        if (s > 0) "${m}m ${s}s" else "${m}m"
    }

    val distanceKmStr = remember(activity.distanceMeters) {
        activity.distanceMeters?.let { meters ->
            String.format(Locale.getDefault(), "%.2f km", meters / 1000.0)
        }
    }

    val paceStr = remember(activity.distanceMeters, activity.durationSeconds) {
        if (activity.distanceMeters != null && activity.distanceMeters > 0 && activity.durationSeconds > 0) {
            val km = activity.distanceMeters / 1000.0
            val totalSecPerKm = (activity.durationSeconds / km).toInt()
            val pMin = totalSecPerKm / 60
            val pSec = totalSecPerKm % 60
            String.format(Locale.getDefault(), "%d'%02d\" /km", pMin, pSec)
        } else null
    }

    val caloriesStr = remember(activity.caloriesKcal) {
        activity.caloriesKcal?.let { kcal ->
            "${kcal.toInt()} kcal"
        }
    }

    val brandAccentColor = if (isNikeRunClub) Color(0xFFC1F807) else MaterialTheme.colorScheme.primary

    GainsCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isNikeRunClub) Color(0xFF1E2405) else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when (activity.activityType.lowercase()) {
                            "running", "walking" -> Icons.AutoMirrored.Filled.DirectionsRun
                            "cycling" -> Icons.Default.FlashOn
                            else -> Icons.Default.FitnessCenter
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isNikeRunClub) Color(0xFFC1F807) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = activity.title,
                            style = BodySemiBold.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$dayOfWeek • $dateStr",
                            style = BodySemiBold.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Source App Compact Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isNikeRunClub) Color(0xFF1E2405) else MaterialTheme.colorScheme.primaryContainer
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = shortBadge,
                        style = LabelCaps.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = if (isNikeRunClub) Color(0xFFC1F807) else MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Simple Clean Metrics Row (Distance & Time Only)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                    .padding(vertical = 10.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (distanceKmStr != null) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "DISTANCE",
                            style = LabelCaps.copy(fontSize = 8.sp),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = distanceKmStr,
                            style = HeaderBold.copy(fontSize = 15.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "TIME",
                        style = LabelCaps.copy(fontSize = 8.sp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = durationFormatted,
                        style = HeaderBold.copy(fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun ExternalActivityDetailDialog(
    activity: ExternalActivity,
    onDismiss: () -> Unit
) {
    val date = remember(activity.startTime) { Date(activity.startTime) }
    val dayOfWeek = remember(activity.startTime) { SimpleDateFormat("EEEE", Locale.getDefault()).format(date) }
    val dateStr = remember(activity.startTime) { SimpleDateFormat("MMM dd, yyyy  •  hh:mm a", Locale.getDefault()).format(date) }
    val isNikeRunClub = remember(activity.sourceApp) {
        val app = activity.sourceApp?.lowercase() ?: ""
        app.contains("nike") || app.contains("plusgps")
    }

    val durationFormatted = remember(activity.durationSeconds) {
        val m = activity.durationSeconds / 60
        val s = activity.durationSeconds % 60
        if (s > 0) "${m}m ${s}s" else "${m}m"
    }

    val distanceKmStr = remember(activity.distanceMeters) {
        activity.distanceMeters?.let { meters ->
            String.format(Locale.getDefault(), "%.2f km", meters / 1000.0)
        }
    }

    val paceStr = remember(activity.distanceMeters, activity.durationSeconds) {
        if (activity.distanceMeters != null && activity.distanceMeters > 0 && activity.durationSeconds > 0) {
            val km = activity.distanceMeters / 1000.0
            val totalSecPerKm = (activity.durationSeconds / km).toInt()
            val pMin = totalSecPerKm / 60
            val pSec = totalSecPerKm % 60
            String.format(Locale.getDefault(), "%d'%02d\" /km", pMin, pSec)
        } else null
    }

    val speedStr = remember(activity.distanceMeters, activity.durationSeconds) {
        if (activity.distanceMeters != null && activity.distanceMeters > 0 && activity.durationSeconds > 0) {
            val km = activity.distanceMeters / 1000.0
            val hours = activity.durationSeconds / 3600.0
            val kmh = km / hours
            String.format(Locale.getDefault(), "%.1f km/h", kmh)
        } else null
    }

    val caloriesStr = remember(activity.caloriesKcal) {
        activity.caloriesKcal?.let { kcal ->
            "${kcal.toInt()} kcal"
        }
    }

    val routePoints = remember(activity.routeJson) { parseRouteJson(activity.routeJson) }
    val brandAccentColor = if (isNikeRunClub) Color(0xFFC1F807) else MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", style = LabelCaps, color = MaterialTheme.colorScheme.primary)
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activity.title,
                    style = HeaderBold.copy(fontSize = 18.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isNikeRunClub) Color(0xFF1E2405) else MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = (activity.sourceApp ?: "HEALTH CONNECT").uppercase(),
                        style = LabelCaps.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = if (isNikeRunClub) Color(0xFFC1F807) else MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "$dayOfWeek • $dateStr",
                    style = BodySemiBold.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.secondary
                )

                // Grid of Telemetry
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("DISTANCE", style = LabelCaps.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.secondary)
                            Text(distanceKmStr ?: "--", style = HeaderBold.copy(fontSize = 16.sp), color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("DURATION", style = LabelCaps.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.secondary)
                            Text(durationFormatted, style = HeaderBold.copy(fontSize = 16.sp), color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("AVG PACE", style = LabelCaps.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.secondary)
                            Text(paceStr ?: "--", style = HeaderBold.copy(fontSize = 16.sp), color = MaterialTheme.colorScheme.onSurface)
                        }
                        if (speedStr != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("AVG SPEED", style = LabelCaps.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.secondary)
                                Text(speedStr, style = HeaderBold.copy(fontSize = 16.sp), color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("CALORIES", style = LabelCaps.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.secondary)
                            Text(caloriesStr ?: "--", style = HeaderBold.copy(fontSize = 16.sp), color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                // GPS Route Map
                if (routePoints.size >= 2) {
                    Text("GPS ROUTE MAP", style = LabelCaps, color = MaterialTheme.colorScheme.secondary)
                    RouteMapThumbnail(
                        points = routePoints,
                        accentColor = brandAccentColor,
                        modifier = Modifier.height(180.dp)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}
