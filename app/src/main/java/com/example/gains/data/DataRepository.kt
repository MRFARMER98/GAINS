package com.example.gains.data

import com.example.gains.data.sync.CsvParser
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface DataRepository {
    val allExercises: Flow<List<Exercise>>
    val allExercisesWithSummary: Flow<List<ExerciseWithSummary>>
    val allSessions: Flow<List<WorkoutSession>>
    val allSessionsWithLabels: Flow<List<WorkoutSessionWithLabel>>
    val allLabels: Flow<List<WorkoutLabel>>
    val userProfile: Flow<UserProfile?>
    val allPlannedSessions: Flow<List<PlannedSession>>

    suspend fun insertSession(session: WorkoutSession): Long
    suspend fun updateSession(session: WorkoutSession)
    suspend fun deleteSession(session: WorkoutSession)

    suspend fun insertPlannedSession(plannedSession: PlannedSession): Long
    suspend fun deletePlannedSessionById(id: Long)

    suspend fun insertLoggedSet(loggedSet: LoggedSet): Long
    suspend fun updateLoggedSet(loggedSet: LoggedSet)
    suspend fun deleteLoggedSet(loggedSet: LoggedSet)

    fun getLoggedSetsForSession(sessionId: Long): Flow<List<LoggedSetWithExercise>>
    fun getSessionById(sessionId: Long): Flow<WorkoutSession?>
    
    suspend fun insertExercises(exercises: List<Exercise>)
    suspend fun syncExercises(sheetUrl: String): Result<Unit>

    // Labels
    suspend fun insertLabel(label: WorkoutLabel): Long
    suspend fun deleteLabel(label: WorkoutLabel)

    // User Profile
    suspend fun updateProfile(profile: UserProfile)

    // Exercise Details & History
    fun getExerciseById(exerciseId: Int): Flow<Exercise?>
    fun getHistoryForExercise(exerciseId: Int): Flow<List<LoggedSetWithSession>>
    suspend fun getLatestCompletedSetsForExercise(exerciseId: Int): List<LoggedSet>
    suspend fun updateExerciseNotes(exerciseId: Int, notes: String?)

    // Templates
    val allTemplatesWithDetails: Flow<List<WorkoutTemplateWithDetails>>
    fun getTemplateById(id: Long): Flow<WorkoutTemplate?>
    fun getTemplateSetsForTemplate(templateId: Long): Flow<List<TemplateSetWithExercise>>
    suspend fun insertTemplate(template: WorkoutTemplate): Long
    suspend fun updateTemplate(template: WorkoutTemplate)
    suspend fun deleteTemplateById(id: Long)
    suspend fun insertTemplateSet(templateSet: TemplateSet): Long
    suspend fun updateTemplateSet(templateSet: TemplateSet)
    suspend fun deleteTemplateSet(templateSet: TemplateSet)
    suspend fun createSessionFromTemplate(templateId: Long, name: String? = null, autoLoadPrevious: Boolean = false): Long
    suspend fun createTemplateFromSession(sessionId: Long, templateName: String): Long

    // Metrics
    val allMetricsWithLatest: Flow<List<MetricWithLatestEntry>>
    fun getEntriesForMetric(metricId: Long): Flow<List<MetricEntry>>
    suspend fun insertMetricEntry(entry: MetricEntry): Long
    suspend fun updateMetricEntry(entry: MetricEntry)
    suspend fun deleteMetricEntry(entry: MetricEntry)
    suspend fun updateMetricGoal(id: Long, targetValue: Float?, targetDate: Long?)
    suspend fun updateMetricSource(id: Long, source: String)
    suspend fun ensureDefaultMetricsSeeded()

    // External Activities & Health Connect
    val allExternalActivities: Flow<List<ExternalActivity>>
    suspend fun syncHealthConnect(context: android.content.Context): Result<Int>

    // Food Tracker
    fun searchFoodItems(query: String): Flow<List<FoodItem>>
    suspend fun getFoodItemByBarcode(barcode: String): FoodItem?
    suspend fun getFoodItemById(id: String): FoodItem?
    fun getNutrientForFood(foodId: String): Flow<FoodNutrient?>
    suspend fun getNutrientForFoodSync(foodId: String): FoodNutrient?
    fun getServingsForFood(foodId: String): Flow<List<FoodServing>>
    suspend fun getServingsForFoodSync(foodId: String): List<FoodServing>
    fun getLoggedFoodEntriesForDate(dateTimestamp: Long): Flow<List<LoggedFoodEntry>>
    suspend fun insertLoggedFoodEntry(entry: LoggedFoodEntry): Long
    suspend fun deleteLoggedFoodEntryById(id: Long)
    val allRecipes: Flow<List<FoodRecipe>>
    val allRecipesWithDetails: Flow<List<FoodRecipeWithDetails>>
    suspend fun getRecipeWithDetails(recipeId: Long): FoodRecipeWithDetails?
    suspend fun saveRecipe(recipe: FoodRecipe, ingredients: List<FoodRecipeIngredient>): Long
    suspend fun deleteRecipe(recipeId: Long)
    suspend fun logRecipeAsMeal(recipeId: Long, servingsToLog: Float, mealType: String, dateTimestamp: Long): String
    suspend fun createRecipe(recipe: FoodRecipe, ingredients: List<FoodRecipeIngredient>): Long
    suspend fun ensureDefaultFoodsSeeded()
}

class DefaultDataRepository(private val gainsDao: GainsDao) : DataRepository {
    override val allExercises: Flow<List<Exercise>> = gainsDao.getAllExercises()
    override val allExercisesWithSummary: Flow<List<ExerciseWithSummary>> = gainsDao.getAllExercisesWithSummary()
    override val allSessions: Flow<List<WorkoutSession>> = gainsDao.getAllSessions()
    override val allSessionsWithLabels: Flow<List<WorkoutSessionWithLabel>> = gainsDao.getAllSessionsWithLabels()
    override val allLabels: Flow<List<WorkoutLabel>> = gainsDao.getAllLabels()
    override val userProfile: Flow<UserProfile?> = gainsDao.getUserProfile()
    override val allPlannedSessions: Flow<List<PlannedSession>> = gainsDao.getAllPlannedSessions()

    override suspend fun insertSession(session: WorkoutSession): Long = gainsDao.insertSession(session)
    override suspend fun updateSession(session: WorkoutSession) = gainsDao.updateSession(session)
    override suspend fun deleteSession(session: WorkoutSession) = gainsDao.deleteSession(session)

    override suspend fun insertPlannedSession(plannedSession: PlannedSession): Long = gainsDao.insertPlannedSession(plannedSession)
    override suspend fun deletePlannedSessionById(id: Long) = gainsDao.deletePlannedSessionById(id)

    override suspend fun insertLoggedSet(loggedSet: LoggedSet): Long = gainsDao.insertLoggedSet(loggedSet)
    override suspend fun updateLoggedSet(loggedSet: LoggedSet) = gainsDao.updateLoggedSet(loggedSet)
    override suspend fun deleteLoggedSet(loggedSet: LoggedSet) = gainsDao.deleteLoggedSet(loggedSet)

    override fun getLoggedSetsForSession(sessionId: Long): Flow<List<LoggedSetWithExercise>> =
        gainsDao.getLoggedSetsForSession(sessionId)

    override fun getSessionById(sessionId: Long): Flow<WorkoutSession?> =
        gainsDao.getSessionById(sessionId)

    override suspend fun insertExercises(exercises: List<Exercise>) = gainsDao.insertExercises(exercises)

    override suspend fun syncExercises(sheetUrl: String): Result<Unit> {
        val client = HttpClient(OkHttp)
        return try {
            val response = client.get(sheetUrl)
            val csvText = response.bodyAsText()
            val parsedExercises = CsvParser.parseExercises(csvText)
            
            if (parsedExercises.isEmpty()) {
                return Result.failure(Exception("No exercises found in Sheet CSV"))
            }

            // Sync with existing database records
            val existing = gainsDao.getAllExercisesList()
            val existingByName = existing.associateBy { it.name.lowercase() }

            for (sheetExercise in parsedExercises) {
                val match = existingByName[sheetExercise.name.lowercase()]
                if (match != null) {
                    if (match.muscleGroup != sheetExercise.muscleGroup) {
                        gainsDao.updateExercise(match.copy(muscleGroup = sheetExercise.muscleGroup))
                    }
                } else {
                    gainsDao.insertExercise(sheetExercise)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            client.close()
        }
    }

    override suspend fun insertLabel(label: WorkoutLabel): Long = gainsDao.insertLabel(label)

    override suspend fun deleteLabel(label: WorkoutLabel) {
        gainsDao.clearSessionLabelId(label.id)
        gainsDao.deleteLabel(label)
    }

    override suspend fun updateProfile(profile: UserProfile) {
        gainsDao.insertOrUpdateProfile(profile)
    }

    override fun getExerciseById(exerciseId: Int): Flow<Exercise?> =
        gainsDao.getExerciseById(exerciseId)

    override fun getHistoryForExercise(exerciseId: Int): Flow<List<LoggedSetWithSession>> =
        gainsDao.getHistoryForExercise(exerciseId)

    override suspend fun updateExerciseNotes(exerciseId: Int, notes: String?) =
        gainsDao.updateExerciseNotes(exerciseId, notes)

    // Templates
    override val allTemplatesWithDetails: Flow<List<WorkoutTemplateWithDetails>> =
        gainsDao.getAllTemplatesWithDetails()

    override fun getTemplateById(id: Long): Flow<WorkoutTemplate?> =
        gainsDao.getTemplateById(id)

    override fun getTemplateSetsForTemplate(templateId: Long): Flow<List<TemplateSetWithExercise>> =
        gainsDao.getTemplateSetsForTemplate(templateId)

    override suspend fun insertTemplate(template: WorkoutTemplate): Long =
        gainsDao.insertTemplate(template)

    override suspend fun updateTemplate(template: WorkoutTemplate) =
        gainsDao.updateTemplate(template)

    override suspend fun deleteTemplateById(id: Long) =
        gainsDao.deleteTemplateById(id)

    override suspend fun insertTemplateSet(templateSet: TemplateSet): Long =
        gainsDao.insertTemplateSet(templateSet)

    override suspend fun getLatestCompletedSetsForExercise(exerciseId: Int): List<LoggedSet> =
        gainsDao.getLatestCompletedSetsForExercise(exerciseId)

    override suspend fun updateTemplateSet(templateSet: TemplateSet) =
        gainsDao.updateTemplateSet(templateSet)

    override suspend fun deleteTemplateSet(templateSet: TemplateSet) =
        gainsDao.deleteTemplateSet(templateSet)

    override suspend fun createSessionFromTemplate(templateId: Long, name: String?, autoLoadPrevious: Boolean): Long {
        val template = gainsDao.getTemplateByIdSync(templateId) ?: return 0L
        val sessionName = name ?: template.name
        val newSession = WorkoutSession(
            timestamp = System.currentTimeMillis(),
            name = sessionName,
            workoutType = template.workoutType,
            labelId = template.labelId
        )
        val sessionId = gainsDao.insertSession(newSession)
        val templateSets = gainsDao.getTemplateSetsList(templateId)

        if (autoLoadPrevious) {
            val exerciseGrouped = templateSets.groupBy { it.exerciseId }
            exerciseGrouped.forEach { (exId, tSets) ->
                val prevSets = gainsDao.getLatestCompletedSetsForExercise(exId)
                if (prevSets.isNotEmpty()) {
                    prevSets.forEachIndexed { index, ps ->
                        gainsDao.insertLoggedSet(
                            LoggedSet(
                                sessionId = sessionId,
                                exerciseId = exId,
                                setNumber = index + 1,
                                weight = ps.weight,
                                reps = ps.reps,
                                isCompleted = false
                            )
                        )
                    }
                } else {
                    tSets.forEach { ts ->
                        gainsDao.insertLoggedSet(
                            LoggedSet(
                                sessionId = sessionId,
                                exerciseId = ts.exerciseId,
                                setNumber = ts.setNumber,
                                weight = ts.targetWeight,
                                reps = ts.targetReps,
                                isCompleted = false
                            )
                        )
                    }
                }
            }
        } else {
            templateSets.forEach { ts ->
                gainsDao.insertLoggedSet(
                    LoggedSet(
                        sessionId = sessionId,
                        exerciseId = ts.exerciseId,
                        setNumber = ts.setNumber,
                        weight = ts.targetWeight,
                        reps = ts.targetReps,
                        isCompleted = false
                    )
                )
            }
        }
        return sessionId
    }

    override suspend fun createTemplateFromSession(sessionId: Long, templateName: String): Long {
        val sets = gainsDao.getLoggedSetsForSession(sessionId) // Flow, let's get logged sets
        val newTemplate = WorkoutTemplate(
            name = templateName,
            createdAt = System.currentTimeMillis()
        )
        val templateId = gainsDao.insertTemplate(newTemplate)
        return templateId
    }

    override val allMetricsWithLatest: Flow<List<MetricWithLatestEntry>> =
        gainsDao.getMetricsWithLatestEntries()

    override fun getEntriesForMetric(metricId: Long): Flow<List<MetricEntry>> =
        gainsDao.getEntriesForMetric(metricId)

    override suspend fun insertMetricEntry(entry: MetricEntry): Long =
        gainsDao.insertMetricEntry(entry)

    override suspend fun updateMetricEntry(entry: MetricEntry) =
        gainsDao.updateMetricEntry(entry)

    override suspend fun deleteMetricEntry(entry: MetricEntry) =
        gainsDao.deleteMetricEntry(entry)

    override suspend fun updateMetricGoal(id: Long, targetValue: Float?, targetDate: Long?) =
        gainsDao.updateMetricGoal(id, targetValue, targetDate)

    override suspend fun updateMetricSource(id: Long, source: String) =
        gainsDao.updateMetricSource(id, source)

    override suspend fun ensureDefaultMetricsSeeded() {
        val all = gainsDao.getAllMetricDefinitions().firstOrNull() ?: emptyList()
        val existingNames = all.map { it.name.lowercase() }.toSet()
        val country = java.util.Locale.getDefault().country
        val weightUnit = if (country == "US" || country == "LR" || country == "MM") "lbs" else "kg"

        if (!existingNames.contains("body weight")) {
            gainsDao.insertMetricDefinition(
                MetricDefinition(name = "Body Weight", unit = weightUnit, isSystem = true, displayOrder = 1)
            )
        }
        if (!existingNames.contains("body fat (%)")) {
            gainsDao.insertMetricDefinition(
                MetricDefinition(name = "Body Fat (%)", unit = "%", isSystem = false, displayOrder = 2, source = "HEALTH_CONNECT")
            )
        }
        if (!existingNames.contains("daily steps")) {
            gainsDao.insertMetricDefinition(
                MetricDefinition(name = "Daily Steps", unit = "steps", isSystem = true, displayOrder = 3, targetValue = 10000f, source = "HEALTH_CONNECT")
            )
        }
        if (!existingNames.contains("sleep")) {
            gainsDao.insertMetricDefinition(
                MetricDefinition(name = "Sleep", unit = "hrs", isSystem = true, displayOrder = 4, targetValue = 8.0f, source = "HEALTH_CONNECT")
            )
        }
    }

    override val allExternalActivities: Flow<List<ExternalActivity>> =
        gainsDao.getAllExternalActivities()

    override suspend fun syncHealthConnect(context: android.content.Context): Result<Int> {
        return com.example.gains.data.health.HealthConnectManager.syncData(context, gainsDao)
    }

    // Food Tracker
    override fun searchFoodItems(query: String): Flow<List<FoodItem>> =
        gainsDao.searchFoodItems(query)

    override suspend fun getFoodItemByBarcode(barcode: String): FoodItem? = withContext(Dispatchers.IO) {
        gainsDao.getFoodItemByBarcode(barcode)
    }

    override suspend fun getFoodItemById(id: String): FoodItem? = withContext(Dispatchers.IO) {
        gainsDao.getFoodItemById(id)
    }

    override fun getNutrientForFood(foodId: String): Flow<FoodNutrient?> =
        gainsDao.getNutrientForFood(foodId)

    override suspend fun getNutrientForFoodSync(foodId: String): FoodNutrient? = withContext(Dispatchers.IO) {
        gainsDao.getNutrientForFoodSync(foodId)
    }

    override fun getServingsForFood(foodId: String): Flow<List<FoodServing>> =
        gainsDao.getServingsForFood(foodId)

    override suspend fun getServingsForFoodSync(foodId: String): List<FoodServing> = withContext(Dispatchers.IO) {
        gainsDao.getServingsForFoodSync(foodId)
    }

    override fun getLoggedFoodEntriesForDate(dateTimestamp: Long): Flow<List<LoggedFoodEntry>> =
        gainsDao.getLoggedFoodEntriesForDate(dateTimestamp)

    override suspend fun insertLoggedFoodEntry(entry: LoggedFoodEntry): Long = withContext(Dispatchers.IO) {
        gainsDao.insertLoggedFoodEntry(entry)
    }

    override suspend fun deleteLoggedFoodEntryById(id: Long) = withContext(Dispatchers.IO) {
        gainsDao.deleteLoggedFoodEntryById(id)
    }

    override val allRecipes: Flow<List<FoodRecipe>> =
        gainsDao.getAllRecipes()

    override val allRecipesWithDetails: Flow<List<FoodRecipeWithDetails>> =
        gainsDao.getAllRecipes().map { recipes ->
            withContext(Dispatchers.IO) {
                recipes.map { recipe ->
                    val ingredients = gainsDao.getRecipeIngredientsWithFood(recipe.id)
                    FoodRecipeWithDetails(recipe = recipe, ingredients = ingredients)
                }
            }
        }

    override suspend fun getRecipeWithDetails(recipeId: Long): FoodRecipeWithDetails? = withContext(Dispatchers.IO) {
        val recipe = gainsDao.getRecipeByIdSync(recipeId) ?: return@withContext null
        val ingredients = gainsDao.getRecipeIngredientsWithFood(recipeId)
        FoodRecipeWithDetails(recipe = recipe, ingredients = ingredients)
    }

    override suspend fun saveRecipe(recipe: FoodRecipe, ingredients: List<FoodRecipeIngredient>): Long = withContext(Dispatchers.IO) {
        gainsDao.saveRecipeTransactional(recipe, ingredients)
    }

    override suspend fun deleteRecipe(recipeId: Long) = withContext(Dispatchers.IO) {
        gainsDao.deleteRecipeIngredientsForRecipe(recipeId)
        gainsDao.deleteRecipeById(recipeId)
    }

    override suspend fun logRecipeAsMeal(
        recipeId: Long, 
        servingsToLog: Float, 
        mealType: String, 
        dateTimestamp: Long
    ): String = withContext(Dispatchers.IO) {
        val recipeDetails = getRecipeWithDetails(recipeId) ?: return@withContext ""
        val groupId = java.util.UUID.randomUUID().toString()
        val baseServings = recipeDetails.recipe.servingsCount.coerceAtLeast(1)
        val safeServingsToLog = servingsToLog.coerceAtLeast(0.1f)
        val multiplier = (safeServingsToLog / baseServings.toFloat())

        recipeDetails.ingredients.forEach { ing ->
            val scaledGramWeight = ing.quantityGrams * multiplier
            val factor = scaledGramWeight / 100.0f

            val dynamicServingDesc = when {
                ing.servingDescription != null && multiplier != 1.0f -> {
                    val formattedMultiplier = if (multiplier % 1.0f == 0.0f) "${multiplier.toInt()}" else String.format(java.util.Locale.US, "%.1f", multiplier)
                    "${formattedMultiplier}x ${ing.servingDescription}"
                }
                ing.servingDescription != null -> ing.servingDescription
                else -> "${scaledGramWeight.toInt()}g"
            }

            val entry = LoggedFoodEntry(
                dateTimestamp = dateTimestamp,
                timestamp = System.currentTimeMillis(),
                mealType = mealType,
                foodId = ing.foodId,
                foodName = ing.foodName,
                brandName = ing.brandName,
                servingDescription = dynamicServingDesc,
                servingQuantity = multiplier,
                gramWeightTotal = scaledGramWeight,
                caloriesKcal = ing.caloriesPer100g * factor,
                proteinG = ing.proteinPer100g * factor,
                carbsG = ing.carbsPer100g * factor,
                fatG = ing.fatPer100g * factor,
                fiberG = ing.fiberPer100g * factor,
                saltG = ing.saltPer100g * factor,
                saturatedFatG = ing.saturatedFatPer100g * factor,
                sugarsG = ing.sugarsPer100g * factor,
                recipeLogGroupId = groupId,
                recipeName = recipeDetails.recipe.name
            )
            gainsDao.insertLoggedFoodEntry(entry)
        }
        groupId
    }

    override suspend fun createRecipe(recipe: FoodRecipe, ingredients: List<FoodRecipeIngredient>): Long = withContext(Dispatchers.IO) {
        saveRecipe(recipe, ingredients)
    }

    override suspend fun ensureDefaultFoodsSeeded() = withContext(Dispatchers.IO) {
        if (gainsDao.getFoodCount() == 0) {
            val defaultFoods = listOf(
                FoodItem(id = "ah-kwark-1", name = "Franse Mager Kwark", brand = "Albert Heijn", category = "Zuivel", barcode = "8718906501234", nutriScore = "A"),
                FoodItem(id = "ah-brood-1", name = "Volkoren Volkorenbrood", brand = "Albert Heijn", category = "Brood", barcode = "8718907123456", nutriScore = "A"),
                FoodItem(id = "ah-gehakt-1", name = "Mager Rundergehakt", brand = "Albert Heijn", category = "Vlees", barcode = "8718908234567", nutriScore = "B"),
                FoodItem(id = "ah-pindakaas-1", name = "100% Pindakaas", brand = "Albert Heijn", category = "Beleg", barcode = "8718909345678", nutriScore = "B"),
                FoodItem(id = "ah-melk-1", name = "Halfvolle Melk", brand = "Albert Heijn", category = "Zuivel", barcode = "8718901456789", nutriScore = "B"),
                FoodItem(id = "nevo-banaan", name = "Banaan (Vers)", brand = null, category = "Fruit", source = "NEVO", nutriScore = "A"),
                FoodItem(id = "nevo-kipfilet", name = "Kipfilet (Rauw)", brand = null, category = "Vlees", source = "NEVO", nutriScore = "A"),
                FoodItem(id = "nevo-havermout", name = "Havermout", brand = null, category = "Graan", source = "NEVO", nutriScore = "A"),
                FoodItem(id = "nevo-ei", name = "Gekookt Ei", brand = null, category = "Eieren", source = "NEVO", nutriScore = "A"),
                FoodItem(id = "nevo-broccoli", name = "Broccoli (Gekookt)", brand = null, category = "Groente", source = "NEVO", nutriScore = "A"),
                FoodItem(id = "ah-protein-1", name = "Whey Protein Vanilla", brand = "Albert Heijn", category = "Sportvoeding", barcode = "8718902567890", nutriScore = "A")
            )
            gainsDao.insertFoodItems(defaultFoods)

            val defaultNutrients = listOf(
                FoodNutrient(foodId = "ah-kwark-1", caloriesKcal = 52f, proteinG = 8.5f, carbsG = 4.0f, sugarsG = 4.0f, fatG = 0.1f, saturatedFatG = 0.05f, fiberG = 0f, saltG = 0.1f, calciumMg = 120f, vitaminB12Ug = 0.4f),
                FoodNutrient(foodId = "ah-brood-1", caloriesKcal = 220f, proteinG = 9.5f, carbsG = 38.0f, sugarsG = 2.5f, fatG = 2.0f, saturatedFatG = 0.4f, fiberG = 6.5f, saltG = 0.9f, ironMg = 2.0f),
                FoodNutrient(foodId = "ah-gehakt-1", caloriesKcal = 175f, proteinG = 20.5f, carbsG = 0f, sugarsG = 0f, fatG = 10.0f, saturatedFatG = 4.2f, fiberG = 0f, saltG = 0.2f, ironMg = 2.2f, zincMg = 4.5f),
                FoodNutrient(foodId = "ah-pindakaas-1", caloriesKcal = 625f, proteinG = 26.0f, carbsG = 11.0f, sugarsG = 4.5f, fatG = 52.0f, saturatedFatG = 8.5f, fiberG = 7.5f, saltG = 0.05f, vitaminEMg = 11f, magnesiumMg = 160f),
                FoodNutrient(foodId = "ah-melk-1", caloriesKcal = 46f, proteinG = 3.4f, carbsG = 4.7f, sugarsG = 4.7f, fatG = 1.5f, saturatedFatG = 1.0f, fiberG = 0f, saltG = 0.12f, calciumMg = 120f, vitaminB2Mg = 0.18f),
                FoodNutrient(foodId = "nevo-banaan", caloriesKcal = 89f, proteinG = 1.1f, carbsG = 20.2f, sugarsG = 12.2f, fatG = 0.3f, saturatedFatG = 0.1f, fiberG = 2.6f, saltG = 0.01f, vitaminCMg = 8.7f, potassiumMg = 358f),
                FoodNutrient(foodId = "nevo-kipfilet", caloriesKcal = 110f, proteinG = 23.5f, carbsG = 0f, sugarsG = 0f, fatG = 1.2f, saturatedFatG = 0.3f, fiberG = 0f, saltG = 0.15f, vitaminB6Mg = 0.6f, phosphorusMg = 210f),
                FoodNutrient(foodId = "nevo-havermout", caloriesKcal = 370f, proteinG = 13.0f, carbsG = 59.0f, sugarsG = 1.0f, fatG = 7.0f, saturatedFatG = 1.2f, fiberG = 10.0f, saltG = 0.02f, magnesiumMg = 130f, ironMg = 4.3f, vitaminB1Mg = 0.5f),
                FoodNutrient(foodId = "nevo-ei", caloriesKcal = 154f, proteinG = 12.6f, carbsG = 0.8f, sugarsG = 0.8f, fatG = 11.2f, saturatedFatG = 3.3f, fiberG = 0f, saltG = 0.32f, vitaminAUg = 190f, vitaminDUg = 1.8f, vitaminB12Ug = 1.1f),
                FoodNutrient(foodId = "nevo-broccoli", caloriesKcal = 35f, proteinG = 3.6f, carbsG = 2.4f, sugarsG = 1.4f, fatG = 0.6f, saturatedFatG = 0.1f, fiberG = 3.3f, saltG = 0.05f, vitaminCMg = 65f, folicAcidUg = 90f),
                FoodNutrient(foodId = "ah-protein-1", caloriesKcal = 380f, proteinG = 78.0f, carbsG = 5.0f, sugarsG = 3.0f, fatG = 5.0f, saturatedFatG = 2.5f, fiberG = 1.0f, saltG = 0.4f, calciumMg = 350f)
            )
            gainsDao.insertFoodNutrients(defaultNutrients)

            val defaultServings = listOf(
                FoodServing(foodId = "ah-kwark-1", description = "1 Bak (500g)", gramWeight = 500f, isDefault = false),
                FoodServing(foodId = "ah-kwark-1", description = "1 Portie (250g)", gramWeight = 250f, isDefault = true),
                FoodServing(foodId = "ah-brood-1", description = "1 Snede (35g)", gramWeight = 35f, isDefault = true),
                FoodServing(foodId = "ah-gehakt-1", description = "1 Portie (125g)", gramWeight = 125f, isDefault = true),
                FoodServing(foodId = "ah-gehakt-1", description = "1 Pak (500g)", gramWeight = 500f, isDefault = false),
                FoodServing(foodId = "ah-pindakaas-1", description = "1 Eetlepel (15g)", gramWeight = 15f, isDefault = true),
                FoodServing(foodId = "ah-melk-1", description = "1 Glas (200ml)", gramWeight = 200f, isDefault = true),
                FoodServing(foodId = "nevo-banaan", description = "1 Banaan (120g)", gramWeight = 120f, isDefault = true),
                FoodServing(foodId = "nevo-kipfilet", description = "1 Stuk (150g)", gramWeight = 150f, isDefault = true),
                FoodServing(foodId = "nevo-havermout", description = "1 Kom (40g)", gramWeight = 40f, isDefault = true),
                FoodServing(foodId = "nevo-ei", description = "1 Ei (55g)", gramWeight = 55f, isDefault = true),
                FoodServing(foodId = "nevo-broccoli", description = "1 Portie (150g)", gramWeight = 150f, isDefault = true),
                FoodServing(foodId = "ah-protein-1", description = "1 Scoop (30g)", gramWeight = 30f, isDefault = true)
            )
            gainsDao.insertFoodServings(defaultServings)
        }
    }
}

