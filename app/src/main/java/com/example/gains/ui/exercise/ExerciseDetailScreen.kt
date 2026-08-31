package com.example.gains.ui.exercise

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gains.GainsApplication
import com.example.gains.data.LoggedSetWithSession
import com.example.gains.theme.BodySemiBold
import com.example.gains.theme.HeaderBold
import com.example.gains.theme.InfraredAccent
import com.example.gains.theme.LabelCaps
import com.example.gains.theme.MetricLarge
import com.example.gains.theme.PrimarySoftBg
import com.example.gains.ui.components.GainsCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exerciseId: Int,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as GainsApplication
    val viewModel: ExerciseDetailViewModel = viewModel(key = exerciseId.toString()) {
        ExerciseDetailViewModel(exerciseId, app.repository)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var allExpandedSignal by remember { mutableStateOf<Boolean?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when (val uiState = state) {
            is ExerciseDetailUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = InfraredAccent)
                }
            }
            is ExerciseDetailUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = BodySemiBold
                    )
                }
            }
            is ExerciseDetailUiState.Success -> {
                // Top Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.exercise.name,
                            style = HeaderBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PrimarySoftBg)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = uiState.exercise.muscleGroup.uppercase(),
                                style = LabelCaps.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                var selectedMetric by remember(uiState.maxWeight) {
                    mutableStateOf(if (uiState.maxWeight == 0.0) ChartMetric.MAX_REPS else ChartMetric.MAX_WEIGHT)
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // PR Metrics Grid
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "PERSONAL RECORDS & STATS",
                                style = LabelCaps,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricStatCard(
                                    label = "MAX WEIGHT",
                                    value = if (uiState.maxWeight > 0) "${uiState.maxWeight} kg" else "--",
                                    isSelected = selectedMetric == ChartMetric.MAX_WEIGHT,
                                    onClick = { selectedMetric = ChartMetric.MAX_WEIGHT },
                                    modifier = Modifier.weight(1f)
                                )
                                MetricStatCard(
                                    label = "MAX REPS",
                                    value = if (uiState.maxReps > 0) "${uiState.maxReps} reps" else "--",
                                    isSelected = selectedMetric == ChartMetric.MAX_REPS,
                                    onClick = { selectedMetric = ChartMetric.MAX_REPS },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricStatCard(
                                    label = "MAX SESSION VOL",
                                    value = if (uiState.maxSessionVolume > 0) "${uiState.maxSessionVolume.toInt()} kg" else "--",
                                    isSelected = selectedMetric == ChartMetric.MAX_VOLUME,
                                    onClick = { selectedMetric = ChartMetric.MAX_VOLUME },
                                    modifier = Modifier.weight(1f)
                                )
                                MetricStatCard(
                                    label = "REPS @ MAX WEIGHT",
                                    value = if (uiState.repsAtMaxWeight > 0) "${uiState.repsAtMaxWeight} reps" else "--",
                                    isSelected = selectedMetric == ChartMetric.REPS_AT_MAX_WEIGHT,
                                    onClick = { selectedMetric = ChartMetric.REPS_AT_MAX_WEIGHT },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Chart
                    item {
                        ExerciseProgressChart(
                            points = uiState.progressPoints,
                            selectedMetric = selectedMetric
                        )
                    }

                    // Personal Form Notes
                    item {
                        ExerciseNotesCard(
                            initialNotes = uiState.exercise.notes ?: "",
                            onSaveNotes = { viewModel.saveNotes(it) }
                        )
                    }

                    // Session History Timeline
                    item {
                        val isAllFolded = allExpandedSignal == false
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "HISTORY TIMELINE (${uiState.historyGroups.size} SESSIONS)",
                                style = LabelCaps,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            if (uiState.historyGroups.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            allExpandedSignal = if (isAllFolded) true else false
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isAllFolded) Icons.Default.UnfoldMore else Icons.Default.UnfoldLess,
                                        contentDescription = if (isAllFolded) "Unfold All" else "Fold All",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isAllFolded) "UNFOLD ALL" else "FOLD ALL",
                                        style = LabelCaps.copy(fontSize = 9.sp),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.historyGroups.isEmpty()) {
                        item {
                            GainsCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "No completed sets logged for this exercise yet.",
                                    style = BodySemiBold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(24.dp)
                                )
                            }
                        }
                    } else {
                        items(uiState.historyGroups, key = { it.sessionId }) { group ->
                            ExerciseSessionHistoryCard(
                                group = group,
                                bestSetId = uiState.bestSetId,
                                isInitiallyExpanded = true,
                                forceExpanded = allExpandedSignal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricStatCard(
    label: String,
    value: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val bgColor = if (isSelected) PrimarySoftBg else MaterialTheme.colorScheme.surface
    val borderWidth = if (isSelected) 1.5.dp else 1.dp

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .border(BorderStroke(borderWidth, borderColor), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = value,
                style = MetricLarge.copy(fontSize = 20.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = LabelCaps.copy(fontSize = 9.sp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun ExerciseNotesCard(
    initialNotes: String,
    onSaveNotes: (String) -> Unit
) {
    var notesText by remember(initialNotes) { mutableStateOf(initialNotes) }
    var isSaved by remember { mutableStateOf(false) }

    GainsCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = InfraredAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FORM & PERFORMANCE CUES",
                        style = LabelCaps,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                if (isSaved) {
                    Text(
                        text = "SAVED ✓",
                        style = LabelCaps.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = notesText,
                onValueChange = {
                    notesText = it
                    isSaved = false
                },
                placeholder = {
                    Text(
                        "Add cues or setup details (e.g. Seat #4, grip width 1.5x shoulders)...",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        onSaveNotes(notesText)
                        isSaved = true
                    },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text("SAVE CUES", style = LabelCaps.copy(fontSize = 9.sp), color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ExerciseSessionHistoryCard(
    group: ExerciseSessionHistoryGroup,
    bestSetId: Int?,
    isInitiallyExpanded: Boolean = true,
    forceExpanded: Boolean? = null
) {
    var isExpanded by rememberSaveable(group.sessionId) { mutableStateOf(isInitiallyExpanded) }

    LaunchedEffect(forceExpanded) {
        if (forceExpanded != null) {
            isExpanded = forceExpanded
        }
    }

    val sessionDate = remember(group.sessionTimestamp) { Date(group.sessionTimestamp) }
    val dayOfWeek = remember(group.sessionTimestamp) {
        SimpleDateFormat("EEEE", Locale.getDefault()).format(sessionDate)
    }
    val dateTimeStr = remember(group.sessionTimestamp) {
        SimpleDateFormat("MMM dd, yyyy  •  hh:mm a", Locale.getDefault()).format(sessionDate)
    }

    val displayTitle = remember(group.sessionName, dayOfWeek) {
        val name = group.sessionName
        if (name.contains(" Session on ") || name.startsWith("Gym Session on ") || name.startsWith("Running Session on ") || name.startsWith("Hyrox Session on ") || name.startsWith("Session on ")) {
            dayOfWeek
        } else if (name == "Gym" || name == "Running" || name == "Hyrox" || name == "Gym Session" || name == "Running Session" || name == "Hyrox Session") {
            dayOfWeek
        } else {
            name
        }
    }

    GainsCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row (Clickable to fold/expand sets)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayTitle,
                        style = BodySemiBold.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dateTimeStr,
                        style = BodySemiBold.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${group.sessionVolume.toInt()} kg vol",
                            style = LabelCaps.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Fold Sets" else "Expand Sets",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))

            // History set breakdown
            group.sets.forEach { set ->
                val isBest = set.id == bestSetId
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .background(
                            if (isBest) PrimarySoftBg else MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Set Number on far left
                    Text(
                        text = "Set ${set.setNumber}",
                        style = LabelCaps.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )

                    // Weight & Reps in exact geometric center of the card
                    val setInfoText = if (set.weight > 0.0) "${set.weight} kg  ×  ${set.reps} reps" else "${set.reps} reps"
                    Text(
                        text = setInfoText,
                        style = BodySemiBold.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    // BEST Badge on far right
                    if (isBest) {
                        Row(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = InfraredAccent,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "BEST",
                                style = LabelCaps.copy(fontSize = 8.sp),
                                color = InfraredAccent
                            )
                        }
                    }
                }
            }
        }
    }
}
}
