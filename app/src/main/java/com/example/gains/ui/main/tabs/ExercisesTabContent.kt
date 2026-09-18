package com.example.gains.ui.main.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.navigation3.runtime.NavKey
import com.example.gains.ExerciseDetail
import com.example.gains.data.ExerciseWithSummary
import com.example.gains.theme.*
import com.example.gains.ui.components.GainsCard
import com.example.gains.ui.main.SyncState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesTabContent(
    exercises: List<ExerciseWithSummary>,
    syncState: SyncState,
    onSyncClick: () -> Unit,
    onItemClick: (NavKey) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscleGroup by remember { mutableStateOf("ALL") }

    val muscleGroups = remember { listOf("ALL", "CHEST", "BACK", "LEGS", "SHOULDERS", "BICEPS", "TRICEPS", "CORE") }

    val filteredExercises = remember(exercises, searchQuery, selectedMuscleGroup) {
        exercises.filter { ex ->
            val matchesQuery = searchQuery.isBlank() ||
                    ex.name.contains(searchQuery, ignoreCase = true) ||
                    ex.muscleGroup.contains(searchQuery, ignoreCase = true)
            val matchesMuscle = selectedMuscleGroup == "ALL" ||
                    ex.muscleGroup.contains(selectedMuscleGroup, ignoreCase = true)
            matchesQuery && matchesMuscle
        }
    }

    val loggedExercises = remember(filteredExercises) {
        filteredExercises.filter { it.sessionCount > 0 }
            .sortedByDescending { it.lastLoggedTimestamp ?: 0L }
    }

    val otherExercises = remember(filteredExercises) {
        filteredExercises.filter { it.sessionCount == 0 }
            .sortedBy { it.name }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Exercises",
                    style = HeaderBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Manage your gym catalog",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            IconButton(
                onClick = onSyncClick,
                enabled = syncState == SyncState.Idle,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp))
                    .size(40.dp)
            ) {
                if (syncState == SyncState.Syncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync Exercises",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        AnimatedVisibility(visible = syncState != SyncState.Idle) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when (syncState) {
                            SyncState.Syncing -> MaterialTheme.colorScheme.surfaceVariant
                            SyncState.Success -> AccentGreenBg
                            is SyncState.Error -> SystemRedSoftBg
                            else -> Color.Transparent
                        }
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            when (syncState) {
                                SyncState.Syncing -> MaterialTheme.colorScheme.outline
                                SyncState.Success -> AccentGreen
                                is SyncState.Error -> SystemRed
                                else -> Color.Transparent
                            }
                        ),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = when (val s = syncState) {
                        SyncState.Syncing -> "Syncing exercises from Google Sheets..."
                        SyncState.Success -> "Exercises updated successfully!"
                        is SyncState.Error -> "Sync failed: ${s.message}"
                        else -> ""
                    },
                    color = when (syncState) {
                        SyncState.Syncing -> MaterialTheme.colorScheme.onSurfaceVariant
                        SyncState.Success -> Color(0xFF1C7A43)
                        is SyncState.Error -> Color(0xFFB3261E)
                        else -> Color.Unspecified
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by name or muscle group...", color = MaterialTheme.colorScheme.secondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp)),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
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

        // Muscle Group Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(muscleGroups, key = { it }) { group ->
                val isSelected = selectedMuscleGroup == group
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                        .border(
                            BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedMuscleGroup = group }
                        .padding(horizontal = 12.dp, vertical = 8.dp) // Minimum touch padding
                ) {
                    Text(
                        text = group,
                        style = LabelCaps.copy(fontSize = 10.sp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredExercises.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotEmpty() || selectedMuscleGroup != "ALL") "No exercises match your filter." else "No exercises cataloged.\nTap the sync button to import from Google Sheets.",
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                if (loggedExercises.isNotEmpty()) {
                    item {
                        Text(
                            text = "YOUR EXERCISES",
                            style = LabelCaps,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        )
                    }

                    items(loggedExercises, key = { it.id }) { exercise ->
                        ExerciseSummaryCard(exercise = exercise, onClick = { onItemClick(ExerciseDetail(exercise.id)) })
                    }
                }

                if (otherExercises.isNotEmpty()) {
                    item {
                        Text(
                            text = "OTHER EXERCISES",
                            style = LabelCaps,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }

                    items(otherExercises, key = { it.id }) { exercise ->
                        ExerciseSummaryCard(exercise = exercise, onClick = { onItemClick(ExerciseDetail(exercise.id)) })
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseSummaryCard(
    exercise: ExerciseWithSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GainsCard(
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                val subtitle = if (exercise.sessionCount > 0) {
                    val prText = if (exercise.maxWeight > 0.0) "${exercise.maxWeight} kg" else "${exercise.maxReps} reps"
                    val sessionText = if (exercise.sessionCount == 1) "1 session" else "${exercise.sessionCount} sessions"
                    "PR: $prText  •  $sessionText"
                } else {
                    "Not logged yet"
                }
                Text(
                    text = subtitle,
                    style = BodySemiBold.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = exercise.muscleGroup.uppercase(),
                    style = LabelCaps.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
