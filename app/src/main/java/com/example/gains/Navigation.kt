package com.example.gains

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.gains.ui.exercise.ExerciseDetailScreen
import com.example.gains.ui.main.MainScreen
import com.example.gains.ui.workout.WorkoutLoggerScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Main)
  val context = LocalContext.current
  val app = context.applicationContext as GainsApplication

  val sharedViewModel: com.example.gains.ui.main.MainScreenViewModel =
    androidx.lifecycle.viewmodel.compose.viewModel {
      com.example.gains.ui.main.MainScreenViewModel(app.repository)
    }

  fun navigateTo(navKey: NavKey) {
    if (backStack.lastOrNull() != navKey) {
      backStack.add(navKey)
    }
  }

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          MainScreen(
            onItemClick = { navKey -> navigateTo(navKey) },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<WorkoutLogger> { key ->
          WorkoutLoggerScreen(
            sessionId = key.sessionId,
            templateId = key.templateId,
            isTemplateMode = key.isTemplateMode,
            isPlannedMode = key.isPlannedMode,
            plannedId = key.plannedId,
            onBackClick = { backStack.removeLastOrNull() },
            onItemClick = { navKey -> navigateTo(navKey) },
            repository = app.repository,
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<ExerciseDetail> { key ->
          ExerciseDetailScreen(
            exerciseId = key.exerciseId,
            onBackClick = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<MetricHistory> { key ->
          com.example.gains.ui.main.MetricHistoryScreen(
            metricId = key.metricId,
            viewModel = sharedViewModel,
            onBackClick = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<ExternalRunDetail> { key ->
          com.example.gains.ui.main.ExternalRunDetailScreen(
            activityId = key.activityId,
            viewModel = sharedViewModel,
            onBackClick = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<RoutinesPlanner> {
          com.example.gains.ui.spoke.RoutinesPlannerScreen(
            viewModel = sharedViewModel,
            onBackClick = { backStack.removeLastOrNull() },
            onItemClick = { navKey -> navigateTo(navKey) },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<ExerciseLibrary> {
          com.example.gains.ui.spoke.ExerciseLibraryScreen(
            viewModel = sharedViewModel,
            onBackClick = { backStack.removeLastOrNull() },
            onItemClick = { navKey -> navigateTo(navKey) },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<RecipesHub> {
          com.example.gains.ui.spoke.RecipesHubScreen(
            viewModel = sharedViewModel,
            repository = app.repository,
            onBackClick = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<AppSettings> {
          com.example.gains.ui.spoke.AppSettingsScreen(
            viewModel = sharedViewModel,
            settingsManager = app.settingsManager,
            onBackClick = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }
      },
  )
}
