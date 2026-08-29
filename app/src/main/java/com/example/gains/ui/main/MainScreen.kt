package com.example.gains.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileOutputStream
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.gains.ExerciseDetail
import com.example.gains.GainsApplication
import com.example.gains.WorkoutLogger
import com.example.gains.data.Exercise
import com.example.gains.data.WorkoutLabel
import com.example.gains.data.WorkoutSessionWithLabel
import com.example.gains.theme.AccentGreen
import com.example.gains.theme.AccentGreenBg
import com.example.gains.theme.BodySemiBold
import com.example.gains.theme.HeaderBold
import com.example.gains.theme.InfraredAccent
import com.example.gains.theme.LabelCaps
import com.example.gains.theme.PrimarySoftBg
import com.example.gains.theme.SystemGrayDark
import com.example.gains.theme.SystemGrayLight
import com.example.gains.theme.SystemRed
import com.example.gains.theme.SystemRedSoftBg
import com.example.gains.ui.components.DashboardHeaderCard
import com.example.gains.ui.components.GainsCard
import com.example.gains.ui.components.HistoryCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as GainsApplication
    val viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(app.repository) }
    
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val allExercises by app.repository.allExercises.collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // Sleek Custom Navigation Bar (Monochrome with Infrared dots, no capsules)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline))
                    .padding(vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomTabItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = Icons.Default.Dashboard,
                        label = "HOME"
                    )
                    CustomTabItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = Icons.Default.FitnessCenter,
                        label = "EXERCISES"
                    )
                    CustomTabItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = Icons.Default.Settings,
                        label = "SETTINGS"
                    )
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> {
                    WorkoutTabContent(
                        state = state,
                        viewModel = viewModel,
                        onItemClick = onItemClick
                    )
                }
                1 -> {
                    ExercisesTabContent(
                        exercises = allExercises,
                        syncState = syncState,
                        onSyncClick = { viewModel.syncDatabase() },
                        onItemClick = onItemClick
                    )
                }
                else -> {
                    SettingsTabContent(
                        viewModel = viewModel,
                        settingsManager = app.settingsManager
                    )
                }
            }
        }
    }
}

@Composable
fun CustomTabItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    val activeColor = InfraredAccent
    val inactiveColor = MaterialTheme.colorScheme.secondary
    
    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) activeColor else inactiveColor,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = LabelCaps.copy(fontSize = 9.sp),
            color = if (selected) activeColor else inactiveColor
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Small active dot indicator instead of blocky Material pill
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(if (selected) activeColor else Color.Transparent)
        )
    }
}

private fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour in 0..11 -> "Good morning"
        hour in 12..16 -> "Good afternoon"
        hour in 17..21 -> "Good evening"
        else -> "What's up"
    }
}

private fun calculateStreak(sessions: List<WorkoutSessionWithLabel>): String {
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

@Composable
fun WorkoutTabContent(
    state: MainScreenUiState,
    viewModel: MainScreenViewModel,
    onItemClick: (NavKey) -> Unit
) {
    var showWorkoutTypeDialog by remember { mutableStateOf(false) }

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

        // Start Workout Button
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

        Spacer(modifier = Modifier.height(28.dp))

        // History Label
        Text(
            text = "WORKOUT HISTORY",
            style = LabelCaps,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // History List
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
                if (sessions.isEmpty()) {
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
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(sessions, key = { it.id }) { session ->
                            HistoryCard(
                                session = session,
                                onClick = { onItemClick(WorkoutLogger(session.id)) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showWorkoutTypeDialog) {
        SelectWorkoutTypeDialog(
            onDismiss = { showWorkoutTypeDialog = false },
            onTypeSelect = { workoutType ->
                showWorkoutTypeDialog = false
                viewModel.startNewSession(workoutType) { sessionId ->
                    onItemClick(WorkoutLogger(sessionId))
                }
            }
        )
    }
}

@Composable
fun ConfirmDeleteDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DELETE WORKOUT?",
                    style = LabelCaps,
                    color = SystemRed,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = "Are you sure you want to permanently delete this workout session? All logged sets will be removed.",
                    style = BodySemiBold.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", style = LabelCaps, color = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SystemRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("DELETE", style = LabelCaps, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun SelectWorkoutTypeDialog(
    onDismiss: () -> Unit,
    onTypeSelect: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SELECT WORKOUT TYPE",
                    style = LabelCaps,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Gym Option
                WorkoutTypeOptionRow(
                    title = "Gym Workout",
                    description = "Strength & bodybuilding tracking",
                    icon = Icons.Default.FitnessCenter,
                    onClick = { onTypeSelect("GYM") }
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Run Option
                WorkoutTypeOptionRow(
                    title = "Running Session",
                    description = "Cardio, pace, & distance",
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    onClick = { onTypeSelect("RUN") }
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Hyrox Option
                WorkoutTypeOptionRow(
                    title = "Hyrox Challenge",
                    description = "Functional fitness racing",
                    icon = Icons.Default.FlashOn,
                    onClick = { onTypeSelect("HYROX") }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = onDismiss) {
                    Text("CANCEL", style = LabelCaps, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
fun WorkoutTypeOptionRow(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(InfraredAccent.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = InfraredAccent,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = BodySemiBold.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = description,
                style = BodySemiBold.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesTabContent(
    exercises: List<Exercise>,
    syncState: SyncState,
    onSyncClick: () -> Unit,
    onItemClick: (NavKey) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredExercises = remember(exercises, searchQuery) {
        if (searchQuery.isBlank()) {
            exercises
        } else {
            exercises.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.muscleGroup.contains(searchQuery, ignoreCase = true)
            }
        }
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
                    .background(PrimarySoftBg, RoundedCornerShape(10.dp))
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
                            SyncState.Syncing -> SystemGrayLight
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
                        SyncState.Syncing -> SystemGrayDark
                        SyncState.Success -> Color(0xFF1C7A43)
                        is SyncState.Error -> Color(0xFFB3261E)
                        else -> Color.Black
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

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredExercises.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotEmpty()) "No exercises match your search." else "No exercises cataloged.\nTap the sync button to import from Google Sheets.",
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
                items(filteredExercises, key = { it.id }) { exercise ->
                    GainsCard(
                        modifier = Modifier.clickable { onItemClick(ExerciseDetail(exercise.id)) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = exercise.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .background(PrimarySoftBg, RoundedCornerShape(6.dp))
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
            }
        }
    }
}

@Composable
fun SettingsTabContent(
    viewModel: MainScreenViewModel,
    settingsManager: com.example.gains.data.SettingsManager,
    modifier: Modifier = Modifier
) {
    val themeMode by settingsManager.themeMode.collectAsStateWithLifecycle()
    var showCreateLabelDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    val labels by viewModel.allLabels.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val profile = (uiState as? MainScreenUiState.Success)?.userProfile
    val currentName = profile?.name ?: "Wouter"
    val currentPhotoUri = profile?.photoUri
    val currentHeight = profile?.height
    val currentAge = profile?.age
    val currentWeight = profile?.currentWeight

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = HeaderBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Configure app preferences",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Card 0: Profile
        Text(
            text = "PROFILE",
            style = LabelCaps,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        GainsCard(modifier = Modifier.clickable { showEditProfileDialog = true }) {
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val settingsBitmap = remember(currentPhotoUri) {
                            if (currentPhotoUri != null) {
                                try {
                                    BitmapFactory.decodeFile(currentPhotoUri)?.asImageBitmap()
                                } catch (e: Exception) {
                                    null
                                }
                            } else {
                                null
                            }
                        }
                        if (settingsBitmap != null) {
                            Image(
                                bitmap = settingsBitmap,
                                contentDescription = "Profile Picture",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = currentName.firstOrNull()?.toString() ?: "U",
                                style = BodySemiBold.copy(fontSize = 16.sp, fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Name",
                            style = BodySemiBold.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentName,
                            style = BodySemiBold.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Card 1: Theme Preferences
        Text(
            text = "DISPLAY PREFERENCES",
            style = LabelCaps,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        GainsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "App Theme",
                    style = BodySemiBold.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose light, dark, or follow system default",
                    style = BodySemiBold.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.secondary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("SYSTEM", "LIGHT", "DARK").forEach { mode ->
                        val isSelected = themeMode == mode
                        Button(
                            onClick = { settingsManager.setThemeMode(mode) },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else SystemGrayLight,
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = mode,
                                style = LabelCaps.copy(fontSize = 9.sp),
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Card 2: Custom Labels Manager
        Text(
            text = "WORKOUT LABELS",
            style = LabelCaps,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        GainsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Custom Tags",
                        style = BodySemiBold.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = { showCreateLabelDialog = true }) {
                        Text("+ CREATE LABEL", style = LabelCaps, color = MaterialTheme.colorScheme.primary)
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                if (labels.isEmpty()) {
                    Text(
                        text = "No custom labels created yet.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        labels.forEach { label ->
                            val color = try {
                                Color(android.graphics.Color.parseColor(label.colorHex))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = label.name,
                                        style = BodySemiBold.copy(fontSize = 14.sp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteLabel(label) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Label",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Card 3: Account & Sync Placeholders
        Text(
            text = "ACCOUNT & SYNC (COMING SOON)",
            style = LabelCaps,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        GainsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsPlaceholderRow(
                    title = "Measurement Units",
                    value = "Metric (kg, km)"
                )
                Spacer(modifier = Modifier.height(12.dp))
                SettingsPlaceholderRow(
                    title = "Google Sheets Sync",
                    value = "Published Sheet Link"
                )
                Spacer(modifier = Modifier.height(12.dp))
                SettingsPlaceholderRow(
                    title = "Local Database Export",
                    value = "Backup data (.json)"
                )
            }
        }
    }

    if (showCreateLabelDialog) {
        CreateLabelDialog(
            onDismiss = { showCreateLabelDialog = false },
            onCreateClick = { labelName, colorHex ->
                viewModel.createLabel(labelName, colorHex)
                showCreateLabelDialog = false
            }
        )
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            initialName = currentName,
            initialPhotoUri = currentPhotoUri,
            initialHeight = currentHeight,
            initialAge = currentAge,
            initialWeight = currentWeight,
            onDismiss = { showEditProfileDialog = false },
            onSaveClick = { newName, photo, h, a, w ->
                viewModel.saveProfile(newName, photo, h, a, w)
                showEditProfileDialog = false
            }
        )
    }
}

@Composable
fun EditProfileDialog(
    initialName: String,
    initialPhotoUri: String?,
    initialHeight: Double?,
    initialAge: Int?,
    initialWeight: Double?,
    onDismiss: () -> Unit,
    onSaveClick: (String, String?, Double?, Int?, Double?) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var photoUri by remember { mutableStateOf(initialPhotoUri) }
    var height by remember { mutableStateOf(initialHeight?.toString() ?: "") }
    var age by remember { mutableStateOf(initialAge?.toString() ?: "") }
    var weight by remember { mutableStateOf(initialWeight?.toString() ?: "") }
    
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val localPath = copyUriToInternalStorage(context, uri)
            if (localPath != null) {
                photoUri = localPath
            }
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "EDIT PROFILE",
                    style = LabelCaps,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
                
                // Clickable Avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap = remember(photoUri) {
                        if (photoUri != null) {
                            try {
                                BitmapFactory.decodeFile(photoUri)?.asImageBitmap()
                            } catch (e: Exception) {
                                null
                            }
                        } else {
                            null
                        }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "PHOTO",
                                style = LabelCaps.copy(fontSize = 8.sp),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name", color = MaterialTheme.colorScheme.secondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = height,
                        onValueChange = { height = it },
                        label = { Text("Height (cm)", color = MaterialTheme.colorScheme.secondary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
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
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Weight (kg)", color = MaterialTheme.colorScheme.secondary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
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
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Age", color = MaterialTheme.colorScheme.secondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", style = LabelCaps, color = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSaveClick(
                                    name,
                                    photoUri,
                                    height.toDoubleOrNull(),
                                    age.toIntOrNull(),
                                    weight.toDoubleOrNull()
                                )
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("SAVE", style = LabelCaps, color = Color.White)
                    }
                }
            }
        }
    }
}

private fun copyUriToInternalStorage(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        context.filesDir.listFiles { _, name -> name.startsWith("profile_picture_") }?.forEach {
            it.delete()
        }
        val newFile = File(context.filesDir, "profile_picture_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(newFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        newFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun CreateLabelDialog(
    onDismiss: () -> Unit,
    onCreateClick: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val colors = listOf("#FF5E3A", "#10B981", "#0EA5E9", "#8B5CF6", "#F59E0B", "#F43F5E")
    var selectedColor by remember { mutableStateOf(colors.first()) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "CREATE WORKOUT LABEL",
                    style = LabelCaps,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Label name (e.g. Leg Day)", color = MaterialTheme.colorScheme.secondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "CHOOSE COLOR",
                    style = LabelCaps,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colors.forEach { hex ->
                         val isSelected = selectedColor == hex
                         val color = Color(android.graphics.Color.parseColor(hex))
                         Box(
                             modifier = Modifier
                                 .size(36.dp)
                                 .clip(CircleShape)
                                 .background(color)
                                 .border(
                                     width = if (isSelected) 3.dp else 0.dp,
                                     color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                     shape = CircleShape
                                 )
                                 .clickable { selectedColor = hex }
                         )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", style = LabelCaps, color = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onCreateClick(name, selectedColor)
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("SAVE", style = LabelCaps, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsPlaceholderRow(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = BodySemiBold.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = LabelCaps.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
