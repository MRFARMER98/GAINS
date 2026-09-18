package com.example.gains.ui.main.tabs

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.example.gains.WorkoutLogger
import com.example.gains.data.ExternalActivity
import com.example.gains.data.PlannedSession
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
    onItemClick: (NavKey) -> Unit
) {
    var showWorkoutTypeDialog by remember { mutableStateOf(false) }

    val dayKeyFormat = remember { SimpleDateFormat("yyyyMMdd", Locale.getDefault()) }
    val todayStr = remember { dayKeyFormat.format(Date()) }
    val todayPlannedSessions = remember(plannedSessions, todayStr) {
        plannedSessions.filter { planned ->
            val cal = Calendar.getInstance().apply { timeInMillis = planned.dateTimestamp }
            dayKeyFormat.format(cal.time) == todayStr
        }
    }
    val labelsMap = remember(labels) { labels.associateBy { it.id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
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
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Today's Scheduled Routine Section
        if (todayPlannedSessions.isNotEmpty()) {
            Text(
                text = "TODAY'S SCHEDULED ROUTINE",
                style = LabelCaps,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

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
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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
                                    .background(MaterialTheme.colorScheme.background),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = workoutTypeIcon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = planned.name,
                                        style = BodySemiBold.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
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
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap for details • Play to start",
                                    style = LabelCaps.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                viewModel.startPlannedSession(planned) { sessionId ->
                                    onItemClick(WorkoutLogger(sessionId))
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Start Workout",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            TextButton(
                onClick = { showWorkoutTypeDialog = true },
                modifier = Modifier.align(Alignment.Start),
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "+ Start other workout",
                    style = LabelCaps.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            }
        } else {
            Button(
                onClick = { showWorkoutTypeDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "START WORKOUT",
                    style = LabelCaps,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // History Label
        Text(
            text = "WORKOUT HISTORY",
            style = LabelCaps,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        val externalActivities by viewModel.allExternalActivities.collectAsStateWithLifecycle(initialValue = emptyList())

        when (state) {
            MainScreenUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is MainScreenUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Error loading history: ${state.throwable.message}",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
            is MainScreenUiState.Success -> {
                val sessions = state.sessions
                if (sessions.isEmpty() && externalActivities.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
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
                    val timelineItems = remember(sessions, externalActivities) {
                        val itemsList = mutableListOf<TimelineItem>()
                        sessions.forEach { itemsList.add(TimelineItem.GainsSession(it)) }
                        externalActivities.forEach { itemsList.add(TimelineItem.External(it)) }
                        itemsList.sortedByDescending { it.timestamp }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
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
                                    ExternalActivityCard(activity = item.activity)
                                }
                            }
                        }
                    }
                }
            }
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
}

@Composable
fun ExternalActivityCard(
    activity: ExternalActivity
) {
    val date = remember(activity.startTime) { Date(activity.startTime) }
    val dayOfWeek = remember(activity.startTime) { SimpleDateFormat("EEEE", Locale.getDefault()).format(date) }
    val dateStr = remember(activity.startTime) { SimpleDateFormat("MMM dd, yyyy  •  hh:mm a", Locale.getDefault()).format(date) }
    val durationMin = activity.durationSeconds / 60

    GainsCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
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
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = activity.title,
                        style = BodySemiBold.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$dayOfWeek • $dateStr",
                        style = BodySemiBold.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = (activity.sourceApp ?: "HEALTH CONNECT").uppercase(),
                        style = LabelCaps.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${durationMin}m duration",
                    style = LabelCaps.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
