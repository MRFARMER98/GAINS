package com.example.gains.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gains.data.DataRepository
import com.example.gains.theme.HeaderBold
import com.example.gains.theme.InfraredAccent
import com.example.gains.theme.LabelCaps
import com.example.gains.theme.AccentGreen
import com.example.gains.theme.AccentGreenBg
import com.example.gains.ui.components.AddExerciseDialog
import com.example.gains.ui.components.ExerciseCard
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutLoggerScreen(
    sessionId: Long,
    onBackClick: () -> Unit,
    repository: DataRepository,
    modifier: Modifier = Modifier,
    viewModel: WorkoutLoggerViewModel = viewModel(key = sessionId.toString()) { WorkoutLoggerViewModel(sessionId, repository) }
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val loggedSets by viewModel.loggedSets.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    var showAddExerciseDialog by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    val isExpanded = remember {
        derivedStateOf {
            !lazyListState.isScrollInProgress || lazyListState.firstVisibleItemIndex == 0
        }
    }.value

    // Live session timer logic (handles both active counting and completed frozen duration)
    var elapsedTime by remember { mutableStateOf("00:00:00") }
    LaunchedEffect(session) {
        val currentSession = session
        if (currentSession != null) {
            if (currentSession.endTime > 0L) {
                // Workout is finished: display fixed static duration
                val diff = currentSession.endTime - currentSession.timestamp
                val secs = (diff / 1000) % 60
                val mins = (diff / (1000 * 60)) % 60
                val hours = (diff / (1000 * 60 * 60)) % 24
                elapsedTime = String.format("%02d:%02d:%02d", hours, mins, secs)
            } else {
                // Workout is active: update live duration every second
                while (true) {
                    val diff = System.currentTimeMillis() - currentSession.timestamp
                    val secs = (diff / 1000) % 60
                    val mins = (diff / (1000 * 60)) % 60
                    val hours = (diff / (1000 * 60 * 60)) % 24
                    elapsedTime = String.format("%02d:%02d:%02d", hours, mins, secs)
                    delay(1000)
                }
            }
        }
    }

    val displayTitle = when (session?.workoutType) {
        "RUN" -> "Running Session"
        "HYROX" -> "Hyrox Challenge"
        else -> "Gym Workout"
    }

    val isFinished = session?.endTime != null && session?.endTime!! > 0L

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = displayTitle,
                            style = HeaderBold.copy(fontSize = 18.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isFinished) "TOTAL DURATION: $elapsedTime" else "DURATION: $elapsedTime",
                            style = LabelCaps.copy(fontSize = 9.sp),
                            color = if (isFinished) MaterialTheme.colorScheme.secondary else InfraredAccent
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    if (session != null) {
                        if (!isFinished) {
                            TextButton(onClick = { viewModel.finishSession() }) {
                                Text("FINISH", style = LabelCaps, color = InfraredAccent)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .background(AccentGreenBg, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("COMPLETED", style = LabelCaps.copy(fontSize = 9.sp), color = AccentGreen)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent // Clean seamless transparent toolbar
                )
            )
        },
        floatingActionButton = {
            // Show FAB only for active GYM workouts (disable adding exercises once finished)
            if ((session?.workoutType == "GYM" || session?.workoutType == null) && !isFinished) {
                ExtendedFloatingActionButton(
                    onClick = { showAddExerciseDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                    text = { Text("ADD EXERCISE", style = LabelCaps, color = Color.White) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    expanded = isExpanded
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val workoutType = session?.workoutType ?: "GYM"
            
            if (workoutType != "GYM") {
                // RUN or HYROX placeholder (since user requested no specifics yet)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (workoutType == "RUN") "🏃 Running Session" else "⚡ Hyrox Workout",
                            style = HeaderBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isFinished) "Telemetry logged successfully." else "Specific telemetry tracking details coming soon.",
                            color = MaterialTheme.colorScheme.secondary,
                            style = LabelCaps
                        )
                    }
                }
            } else if (loggedSets.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Log your first exercise!",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tap the '+' button to begin.",
                            color = MaterialTheme.colorScheme.outline,
                            style = LabelCaps
                        )
                    }
                }
            } else {
                // Group logged sets by muscle group/category for gym workouts
                val groupedByMuscle = loggedSets.groupBy { it.exerciseMuscleGroup }

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    groupedByMuscle.forEach { (muscleGroup, setsForMuscle) ->
                        // Muscle group category header
                        item(key = "header_$muscleGroup") {
                            Text(
                                text = muscleGroup.uppercase(),
                                style = LabelCaps,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        // Group sets under this muscle group by exerciseId
                        val exercisesGrouped = setsForMuscle.groupBy { it.exerciseId }
                        exercisesGrouped.forEach { (exerciseId, sets) ->
                            val firstSet = sets.first()
                            item(key = exerciseId) {
                                ExerciseCard(
                                    exerciseName = firstSet.exerciseName,
                                    muscleGroup = firstSet.exerciseMuscleGroup,
                                    sets = sets,
                                    onAddSetClick = { viewModel.addSet(exerciseId) },
                                    onWeightChange = { setId, weight -> viewModel.updateSetWeight(setId, weight) },
                                    onRepsChange = { setId, reps -> viewModel.updateSetReps(setId, reps) },
                                    onToggleComplete = { setId -> viewModel.toggleSetCompleted(setId) },
                                    onDeleteSet = { setId -> viewModel.deleteSet(setId) },
                                    isReadOnly = isFinished
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showAddExerciseDialog) {
            AddExerciseDialog(
                exercises = exercises,
                onDismiss = { showAddExerciseDialog = false },
                onExerciseSelect = { exercise ->
                    viewModel.addSet(exercise.id)
                    showAddExerciseDialog = false
                }
            )
        }
    }
}
