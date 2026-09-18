package com.example.gains.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.gains.GainsApplication
import com.example.gains.WorkoutLogger
import com.example.gains.theme.InfraredAccent
import com.example.gains.theme.LabelCaps
import com.example.gains.ui.main.tabs.ExercisesTabContent
import com.example.gains.ui.main.tabs.SettingsTabContent
import com.example.gains.ui.main.tabs.WorkoutTabContent

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
    val exercisesWithSummary by viewModel.exercisesWithSummary.collectAsStateWithLifecycle(initialValue = emptyList())
    val allPlannedSessions by viewModel.allPlannedSessions.collectAsStateWithLifecycle(initialValue = emptyList())
    val allLabels by viewModel.allLabels.collectAsStateWithLifecycle(initialValue = emptyList())
    val allTemplates by viewModel.allTemplates.collectAsStateWithLifecycle(initialValue = emptyList())

    val sessions = (state as? MainScreenUiState.Success)?.sessions ?: emptyList()
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // Sleek Custom Navigation Bar with 48dp Touch Bounds and Semantics
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline))
                    .padding(vertical = 6.dp)
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
                        icon = Icons.Default.CalendarMonth,
                        label = "PLANNER"
                    )
                    CustomTabItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = Icons.Default.Person,
                        label = "YOU"
                    )
                    CustomTabItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
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
                        plannedSessions = allPlannedSessions,
                        labels = allLabels,
                        onItemClick = onItemClick
                    )
                }
                1 -> {
                    ExercisesTabContent(
                        exercises = exercisesWithSummary,
                        syncState = syncState,
                        onSyncClick = { viewModel.syncDatabase() },
                        onItemClick = onItemClick
                    )
                }
                2 -> {
                    PlannerTabContent(
                        sessions = sessions,
                        plannedSessions = allPlannedSessions,
                        labels = allLabels,
                        templates = allTemplates,
                        onSchedulePlan = { date, name, type, labelId, templateId ->
                            viewModel.schedulePlannedSession(date, name, type, labelId, templateId)
                        },
                        onDeletePlan = { id -> viewModel.deletePlannedSession(id) },
                        onStartPlan = { planned ->
                            viewModel.startPlannedSession(planned) { sessionId ->
                                onItemClick(WorkoutLogger(sessionId))
                            }
                        },
                        onCreateTemplate = { name ->
                            viewModel.createNewTemplate(name) { id ->
                                onItemClick(WorkoutLogger(templateId = id, isTemplateMode = true))
                            }
                        },
                        onEditTemplate = { id ->
                            onItemClick(WorkoutLogger(templateId = id, isTemplateMode = true))
                        },
                        onDeleteTemplate = { id ->
                            viewModel.deleteTemplate(id)
                        },
                        onStartTemplate = { id ->
                            viewModel.createSessionFromTemplate(id) { sessionId ->
                                onItemClick(WorkoutLogger(sessionId))
                            }
                        },
                        onItemClick = onItemClick
                    )
                }
                3 -> {
                    YouTabContent(
                        viewModel = viewModel,
                        settingsManager = app.settingsManager,
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
            .semantics {
                this.selected = selected
                this.role = Role.Tab
            }
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) activeColor else inactiveColor,
            modifier = Modifier.size(22.dp)
        )
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Text(
            text = label,
            style = LabelCaps.copy(fontSize = 9.sp),
            color = if (selected) activeColor else inactiveColor
        )
        
        Spacer(modifier = Modifier.height(2.dp))
        
        // Active dot indicator
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(if (selected) activeColor else Color.Transparent)
        )
    }
}
