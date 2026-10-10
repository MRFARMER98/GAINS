package com.example.gains.ui.spoke

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.example.gains.WorkoutLogger
import com.example.gains.theme.LabelCaps
import com.example.gains.ui.main.MainScreenUiState
import com.example.gains.ui.main.MainScreenViewModel
import com.example.gains.ui.main.PlannerTabContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesPlannerScreen(
    viewModel: MainScreenViewModel,
    onBackClick: () -> Unit,
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val allPlannedSessions by viewModel.allPlannedSessions.collectAsStateWithLifecycle(initialValue = emptyList())
    val allLabels by viewModel.allLabels.collectAsStateWithLifecycle(initialValue = emptyList())
    val allTemplates by viewModel.allTemplates.collectAsStateWithLifecycle(initialValue = emptyList())
    val sessions = (state as? MainScreenUiState.Success)?.sessions ?: emptyList()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "ROUTINES & PLANNER",
                        style = LabelCaps.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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
    }
}
