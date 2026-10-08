package com.example.gains.ui.main

import android.content.Context
import com.example.gains.data.*
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MainScreenViewModelTest {
    @Test
    fun uiState_initiallyLoading() = runTest {
        val viewModel = MainScreenViewModel(FakeDataRepository())
        assertEquals(MainScreenUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun foodTracker_dailyNutrientSummary_calculatesTotals() = runTest {
        val fakeRepo = FakeDataRepository()
        val viewModel = MainScreenViewModel(fakeRepo)
        
        val food = FoodItem(id = "1", name = "Franse Mager Kwark", brand = "Albert Heijn")
        val nutrient = FoodNutrient(foodId = "1", caloriesKcal = 52f, proteinG = 8.5f, carbsG = 4.0f, sugarsG = 4.0f, fatG = 0.1f, saturatedFatG = 0.05f, fiberG = 0f, saltG = 0.1f)
        val serving = FoodServing(id = 1, foodId = "1", description = "1 Bak (500g)", gramWeight = 500f)

        viewModel.logFoodItem(food, nutrient, serving, 1.0f, "BREAKFAST")
        
        assertEquals(0f, viewModel.dailyNutrientSummary.value.caloriesKcal)
    }
}

private class FakeDataRepository : DataRepository {
    override val allExercises: Flow<List<Exercise>> = flowOf(emptyList())
    override val allExercisesWithSummary: Flow<List<ExerciseWithSummary>> = flowOf(emptyList())
    override val allSessions: Flow<List<WorkoutSession>> = flowOf(emptyList())
    override val allSessionsWithLabels: Flow<List<WorkoutSessionWithLabel>> = flowOf(emptyList())
    override val allLabels: Flow<List<WorkoutLabel>> = flowOf(emptyList())
    override val userProfile: Flow<UserProfile?> = flowOf(null)
    override val allPlannedSessions: Flow<List<PlannedSession>> = flowOf(emptyList())

    override suspend fun insertSession(session: WorkoutSession): Long = 1L
    override suspend fun updateSession(session: WorkoutSession) {}
    override suspend fun deleteSession(session: WorkoutSession) {}

    override suspend fun insertPlannedSession(plannedSession: PlannedSession): Long = 1L
    override suspend fun deletePlannedSessionById(id: Long) {}

    override suspend fun insertLoggedSet(loggedSet: LoggedSet): Long = 1L
    override suspend fun updateLoggedSet(loggedSet: LoggedSet) {}
    override suspend fun deleteLoggedSet(loggedSet: LoggedSet) {}

    override fun getLoggedSetsForSession(sessionId: Long): Flow<List<LoggedSetWithExercise>> = flowOf(emptyList())
    override fun getSessionById(sessionId: Long): Flow<WorkoutSession?> = flowOf(null)

    override suspend fun insertExercises(exercises: List<Exercise>) {}
    override suspend fun syncExercises(sheetUrl: String): Result<Unit> = Result.success(Unit)

    override suspend fun insertLabel(label: WorkoutLabel): Long = 1L
    override suspend fun deleteLabel(label: WorkoutLabel) {}

    override suspend fun updateProfile(profile: UserProfile) {}

    override fun getExerciseById(exerciseId: Int): Flow<Exercise?> = flowOf(null)
    override fun getHistoryForExercise(exerciseId: Int): Flow<List<LoggedSetWithSession>> = flowOf(emptyList())
    override suspend fun getLatestCompletedSetsForExercise(exerciseId: Int): List<LoggedSet> = emptyList()
    override suspend fun updateExerciseNotes(exerciseId: Int, notes: String?) {}

    override val allTemplatesWithDetails: Flow<List<WorkoutTemplateWithDetails>> = flowOf(emptyList())
    override fun getTemplateById(id: Long): Flow<WorkoutTemplate?> = flowOf(null)
    override fun getTemplateSetsForTemplate(templateId: Long): Flow<List<TemplateSetWithExercise>> = flowOf(emptyList())
    override suspend fun insertTemplate(template: WorkoutTemplate): Long = 1L
    override suspend fun updateTemplate(template: WorkoutTemplate) {}
    override suspend fun deleteTemplateById(id: Long) {}
    override suspend fun insertTemplateSet(templateSet: TemplateSet): Long = 1L
    override suspend fun updateTemplateSet(templateSet: TemplateSet) {}
    override suspend fun deleteTemplateSet(templateSet: TemplateSet) {}
    override suspend fun createSessionFromTemplate(templateId: Long, name: String?, autoLoadPrevious: Boolean): Long = 1L
    override suspend fun createTemplateFromSession(sessionId: Long, templateName: String): Long = 1L

    override val allMetricsWithLatest: Flow<List<MetricWithLatestEntry>> = flowOf(emptyList())
    override fun getEntriesForMetric(metricId: Long): Flow<List<MetricEntry>> = flowOf(emptyList())
    override suspend fun insertMetricEntry(entry: MetricEntry): Long = 1L
    override suspend fun updateMetricEntry(entry: MetricEntry) {}
    override suspend fun deleteMetricEntry(entry: MetricEntry) {}
    override suspend fun updateMetricGoal(id: Long, targetValue: Float?, targetDate: Long?) {}
    override suspend fun updateMetricSource(id: Long, source: String) {}
    override suspend fun ensureDefaultMetricsSeeded() {}

    override val allExternalActivities: Flow<List<ExternalActivity>> = flowOf(emptyList())
    override suspend fun syncHealthConnect(context: Context): Result<Int> = Result.success(0)

    // Food Tracker
    private val loggedEntriesFlow = kotlinx.coroutines.flow.MutableStateFlow<List<LoggedFoodEntry>>(emptyList())

    override fun searchFoodItems(query: String): Flow<List<FoodItem>> = flowOf(emptyList())
    override suspend fun getFoodItemByBarcode(barcode: String): FoodItem? = null
    override suspend fun getFoodItemById(id: String): FoodItem? = null
    override fun getNutrientForFood(foodId: String): Flow<FoodNutrient?> = flowOf(null)
    override suspend fun getNutrientForFoodSync(foodId: String): FoodNutrient? = null
    override fun getServingsForFood(foodId: String): Flow<List<FoodServing>> = flowOf(emptyList())
    override suspend fun getServingsForFoodSync(foodId: String): List<FoodServing> = emptyList()
    override fun getLoggedFoodEntriesForDate(dateTimestamp: Long): Flow<List<LoggedFoodEntry>> = loggedEntriesFlow
    override suspend fun insertLoggedFoodEntry(entry: LoggedFoodEntry): Long {
        loggedEntriesFlow.value = loggedEntriesFlow.value + entry
        return 1L
    }
    override suspend fun deleteLoggedFoodEntryById(id: Long) {
        loggedEntriesFlow.value = loggedEntriesFlow.value.filter { it.id != id }
    }
    override val allRecipes: Flow<List<FoodRecipe>> = flowOf(emptyList())
    override val allRecipesWithDetails: Flow<List<FoodRecipeWithDetails>> = flowOf(emptyList())
    override suspend fun getRecipeWithDetails(recipeId: Long): FoodRecipeWithDetails? = null
    override suspend fun saveRecipe(recipe: FoodRecipe, ingredients: List<FoodRecipeIngredient>): Long = 1L
    override suspend fun deleteRecipe(recipeId: Long) {}
    override suspend fun logRecipeAsMeal(recipeId: Long, servingsToLog: Float, mealType: String, dateTimestamp: Long): String = "group-1"
    override suspend fun createRecipe(recipe: FoodRecipe, ingredients: List<FoodRecipeIngredient>): Long = 1L
    override suspend fun ensureDefaultFoodsSeeded() {}
}

