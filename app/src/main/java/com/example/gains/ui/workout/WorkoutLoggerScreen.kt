package com.example.gains.ui.workout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gains.data.DataRepository
import com.example.gains.data.WorkoutLabel
import com.example.gains.theme.HeaderBold
import com.example.gains.theme.InfraredAccent
import com.example.gains.theme.LabelCaps
import com.example.gains.theme.BodySemiBold
import com.example.gains.theme.AccentGreen
import com.example.gains.theme.AccentGreenBg
import com.example.gains.theme.SystemRed
import com.example.gains.ui.components.AddExerciseDialog
import com.example.gains.ui.components.ExerciseCard
import kotlinx.coroutines.delay

import com.example.gains.ExerciseDetail
import androidx.navigation3.runtime.NavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutLoggerScreen(
    sessionId: Long,
    onBackClick: () -> Unit,
    repository: DataRepository,
    onItemClick: ((NavKey) -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: WorkoutLoggerViewModel = viewModel(key = sessionId.toString()) { WorkoutLoggerViewModel(sessionId, repository) }
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val loggedSets by viewModel.loggedSets.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val allLabels by viewModel.allLabels.collectAsStateWithLifecycle()
    
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showLabelPickerDialog by remember { mutableStateOf(false) }
    var isEditingFinished by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

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
    val isReadOnly = isFinished && !isEditingFinished

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displayTitle,
                                style = HeaderBold.copy(fontSize = 18.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            // Colored Label Badge or + TAG button in transparent toolbar
                            val activeLabel = allLabels.find { it.id == session?.labelId }
                            if (activeLabel != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                val labelColor = try {
                                    Color(android.graphics.Color.parseColor(activeLabel.colorHex))
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(labelColor.copy(alpha = 0.15f))
                                        .clickable(enabled = !isReadOnly) { showLabelPickerDialog = true }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = activeLabel.name.uppercase(),
                                        style = LabelCaps.copy(fontSize = 8.sp),
                                        color = labelColor
                                    )
                                }
                            } else if (!isReadOnly) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(4.dp))
                                        .clickable { showLabelPickerDialog = true }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "+ TAG",
                                        style = LabelCaps.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
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
                            if (isEditingFinished) {
                                IconButton(
                                    onClick = { isEditingFinished = false },
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Save Changes",
                                        tint = InfraredAccent
                                    )
                                }
                            } else {
                                Box(modifier = Modifier.padding(end = 8.dp)) {
                                    IconButton(onClick = { showMenu = true }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Options",
                                            tint = InfraredAccent
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Edit Workout", style = BodySemiBold.copy(fontSize = 14.sp)) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = null,
                                                    tint = InfraredAccent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            onClick = {
                                                showMenu = false
                                                isEditingFinished = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete Workout", style = BodySemiBold.copy(fontSize = 14.sp), color = MaterialTheme.colorScheme.error) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            onClick = {
                                                showMenu = false
                                                showDeleteConfirmDialog = true
                                            }
                                        )
                                    }
                                }
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
            // Show FAB only for active GYM workouts (disable adding exercises once finished unless editing)
            if ((session?.workoutType == "GYM" || session?.workoutType == null) && !isReadOnly) {
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
                        // Muscle group category header with sets & reps count (e.g. CHEST  •  3 SETS  •  24 REPS)
                        item(key = "header_$muscleGroup") {
                            val totalSets = setsForMuscle.size
                            val totalReps = setsForMuscle.sumOf { it.reps }
                            val setsText = if (totalSets == 1) "1 SET" else "$totalSets SETS"
                            val repsText = if (totalReps == 1) "1 REP" else "$totalReps REPS"
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            ) {
                                Text(
                                    text = muscleGroup.uppercase(),
                                    style = LabelCaps,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "  •  $setsText  •  $repsText",
                                    style = LabelCaps.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                                )
                            }
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
                                    isReadOnly = isReadOnly,
                                    onHeaderClick = { onItemClick?.invoke(ExerciseDetail(exerciseId)) }
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

        if (showLabelPickerDialog) {
            SelectSessionLabelDialog(
                labels = allLabels,
                currentLabelId = session?.labelId,
                onDismiss = { showLabelPickerDialog = false },
                onLabelSelected = { labelId ->
                    viewModel.assignLabelToSession(labelId)
                    showLabelPickerDialog = false
                }
            )
        }

        if (showDeleteConfirmDialog) {
            ConfirmDeleteDialog(
                onDismiss = { showDeleteConfirmDialog = false },
                onConfirm = {
                    viewModel.deleteSession()
                    showDeleteConfirmDialog = false
                    onBackClick()
                }
            )
        }
    }
}

@Composable
fun SelectSessionLabelDialog(
    labels: List<WorkoutLabel>,
    currentLabelId: Int?,
    onDismiss: () -> Unit,
    onLabelSelected: (Int?) -> Unit
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
                    .padding(20.dp)
            ) {
                Text(
                    text = "ASSIGN LABEL",
                    style = LabelCaps,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    // "None" option to clear label
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (currentLabelId == null) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else MaterialTheme.colorScheme.background
                                )
                                .clickable { onLabelSelected(null) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.outline)
                              )
                              Spacer(modifier = Modifier.width(10.dp))
                              Text(
                                  text = "None (Clear Tag)",
                                  style = BodySemiBold.copy(fontSize = 14.sp),
                                  color = MaterialTheme.colorScheme.onSurface
                              )
                        }
                    }
                    
                    items(labels, key = { it.id }) { label ->
                        val isSelected = currentLabelId == label.id
                        val color = try {
                            Color(android.graphics.Color.parseColor(label.colorHex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) color.copy(alpha = 0.1f)
                                    else MaterialTheme.colorScheme.background
                                )
                                .clickable { onLabelSelected(label.id) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = label.name,
                                style = BodySemiBold.copy(
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("CANCEL", style = LabelCaps, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
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
