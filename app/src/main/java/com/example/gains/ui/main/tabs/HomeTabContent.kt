package com.example.gains.ui.main.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.example.gains.RoutinesPlanner
import com.example.gains.WorkoutLogger
import com.example.gains.data.FoodNutrient
import com.example.gains.data.FoodServing
import com.example.gains.data.PlannedSession
import com.example.gains.data.WorkoutSessionWithLabel
import com.example.gains.theme.*
import com.example.gains.ui.components.AddFoodDialog
import com.example.gains.ui.components.GainsCard
import com.example.gains.ui.components.LogMetricDialog
import com.example.gains.ui.components.NutritionGoalDialog
import com.example.gains.ui.components.SelectWorkoutTypeDialog
import com.example.gains.ui.main.MainScreenUiState
import com.example.gains.ui.main.MainScreenViewModel
import com.example.gains.ui.main.WeekDayStatus
import com.example.gains.ui.main.WeeklyDashboardSummary
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeTabContent(
    viewModel: MainScreenViewModel,
    repository: com.example.gains.data.DataRepository,
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val weeklySummary by viewModel.weeklyDashboardSummary.collectAsStateWithLifecycle()
    val allPlannedSessions by viewModel.allPlannedSessions.collectAsStateWithLifecycle(initialValue = emptyList())
    val targets by viewModel.nutritionTargets.collectAsStateWithLifecycle()
    val dailyNutrientSummary by viewModel.dailyNutrientSummary.collectAsStateWithLifecycle()
    val dailyActiveBurn by viewModel.dailyActiveCaloriesBurned.collectAsStateWithLifecycle()
    val calibrationState by viewModel.weeklyCalibrationState.collectAsStateWithLifecycle()
    val allMetrics by viewModel.allMetrics.collectAsStateWithLifecycle(initialValue = emptyList())

    val sessions = (uiState as? MainScreenUiState.Success)?.sessions ?: emptyList()
    val userProfile = (uiState as? MainScreenUiState.Success)?.userProfile

    var showNutritionGoalDialog by remember { mutableStateOf(false) }
    var showWorkoutTypeDialog by remember { mutableStateOf(false) }
    var showAddFoodDialog by remember { mutableStateOf(false) }
    var showLogWeightDialog by remember { mutableStateOf(false) }

    val todaySession = remember(sessions) {
        val todayCal = Calendar.getInstance()
        val y = todayCal.get(Calendar.YEAR)
        val d = todayCal.get(Calendar.DAY_OF_YEAR)
        sessions.find { s ->
            val c = Calendar.getInstance().apply { timeInMillis = s.timestamp }
            c.get(Calendar.YEAR) == y && c.get(Calendar.DAY_OF_YEAR) == d
        }
    }

    val todayPlannedSession = remember(allPlannedSessions) {
        val todayCal = Calendar.getInstance()
        val y = todayCal.get(Calendar.YEAR)
        val d = todayCal.get(Calendar.DAY_OF_YEAR)
        allPlannedSessions.find { p ->
            val c = Calendar.getInstance().apply { timeInMillis = p.dateTimestamp }
            c.get(Calendar.YEAR) == y && c.get(Calendar.DAY_OF_YEAR) == d
        }
    }

    val streakText = remember(sessions) { calculateStreak(sessions) }
    val greeting = remember { getGreeting() }
    val todayFormattedDate = remember {
        SimpleDateFormat("EEEE, d MMM", Locale.ENGLISH).format(Date()).uppercase()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Unified Edge-to-Edge Hero Header (Athlete Greeting + Weekly Overview Strip)
        item {
            HomeHeroHeader(
                greeting = greeting,
                userName = userProfile?.name ?: "Athlete",
                streakText = streakText,
                weeklySummary = weeklySummary,
                onPlanWorkoutClick = { onItemClick(RoutinesPlanner) }
            )
        }

        // 2. Today's Focus Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY'S FOCUS",
                    style = LabelCaps.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = todayFormattedDate,
                    style = LabelCaps.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        // 3. Energy Balance Card
        item {
            EnergyBalanceCard(
                targets = targets,
                nutrientSummary = dailyNutrientSummary,
                activeBurnKcal = dailyActiveBurn,
                onEditPlanClick = { showNutritionGoalDialog = true },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 4. Today's Workout Action Card
        item {
            TodayWorkoutCard(
                completedSession = todaySession,
                plannedSession = todayPlannedSession,
                onStartWorkout = { showWorkoutTypeDialog = true },
                onStartPlanned = { planned ->
                    viewModel.startPlannedSession(planned) { sessionId ->
                        onItemClick(WorkoutLogger(sessionId))
                    }
                },
                onViewRoutines = { onItemClick(RoutinesPlanner) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 5. Quick Action Logging Strip
        item {
            QuickLogStrip(
                onLogFood = { showAddFoodDialog = true },
                onStartLift = { showWorkoutTypeDialog = true },
                onWeighIn = { showLogWeightDialog = true },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 6. Recent Highlights & Smart Coach Banner
        if (calibrationState != null) {
            item {
                WeeklyCalibrationCard(
                    calibrationState = calibrationState!!,
                    onApply = { delta -> viewModel.applyCalibrationAdjustment(delta) },
                    onDismiss = { viewModel.dismissCalibration() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        item {
            CoachInsightBanner(
                activeBurnKcal = dailyActiveBurn,
                completedSession = todaySession,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }

    // Dialogs
    if (showNutritionGoalDialog) {
        NutritionGoalDialog(
            userProfile = userProfile,
            onSavePlan = { activity, mode, alloc, split, preset, goal, rate, date, cCal, cProt, cCarb, cFat, cFib ->
                viewModel.updateNutritionPlan(
                    activity, mode, alloc, split, preset, goal, rate, date, cCal, cProt, cCarb, cFat, cFib
                )
            },
            onDismiss = { showNutritionGoalDialog = false }
        )
    }

    val allTemplates by viewModel.allTemplates.collectAsStateWithLifecycle(initialValue = emptyList())

    if (showWorkoutTypeDialog) {
        SelectWorkoutTypeDialog(
            templates = allTemplates,
            onDismiss = { showWorkoutTypeDialog = false },
            onTypeSelect = { type ->
                showWorkoutTypeDialog = false
                viewModel.startNewSession(type) { sessionId ->
                    onItemClick(WorkoutLogger(sessionId))
                }
            },
            onTemplateSelect = { templateId ->
                showWorkoutTypeDialog = false
                viewModel.createSessionFromTemplate(templateId) { sessionId ->
                    onItemClick(WorkoutLogger(sessionId))
                }
            }
        )
    }

    if (showAddFoodDialog) {
        val searchQuery by viewModel.foodSearchQuery.collectAsStateWithLifecycle()
        val searchResults by viewModel.foodSearchResults.collectAsStateWithLifecycle()
        AddFoodDialog(
            initialMealType = "SNACK",
            searchQuery = searchQuery,
            searchResults = searchResults,
            onQueryChange = { viewModel.setFoodSearchQuery(it) },
            onLogFood = { food, nut, serv, qty, meal ->
                viewModel.logFoodItem(food, nut, serv, qty, meal)
                showAddFoodDialog = false
            },
            onDismiss = { showAddFoodDialog = false },
            getNutrientForFoodSync = { foodId ->
                repository.getNutrientForFoodSync(foodId)
            },
            getServingsForFoodSync = { foodId ->
                repository.getServingsForFoodSync(foodId)
            }
        )
    }

    if (showLogWeightDialog) {
        val weightMetric = allMetrics.find { it.name.lowercase().contains("weight") }
        if (weightMetric != null) {
            LogMetricDialog(
                metricName = weightMetric.name,
                metricUnit = weightMetric.unit,
                onDismiss = { showLogWeightDialog = false },
                onSave = { value, timestamp ->
                    viewModel.logMetric(weightMetric.id, value, timestamp)
                    userProfile?.let { prof ->
                        viewModel.saveProfile(
                            name = prof.name,
                            photoUri = prof.photoUri,
                            height = prof.height,
                            age = prof.age,
                            currentWeight = value.toDouble(),
                            birthDateTimestamp = prof.birthDateTimestamp,
                            biologicalSex = prof.biologicalSex
                        )
                    }
                    showLogWeightDialog = false
                }
            )
        }
    }
}

@Composable
fun HomeHeroHeader(
    greeting: String,
    userName: String,
    streakText: String,
    weeklySummary: WeeklyDashboardSummary,
    onPlanWorkoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 16.dp)
        ) {
            // Athlete Greeting & Streak Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GAINS ATHLETE",
                        style = LabelCaps.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = InfraredAccent
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$greeting, $userName",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = InfraredAccent.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, InfraredAccent.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = InfraredAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = streakText.uppercase(),
                            style = LabelCaps.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = InfraredAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Weekly Overview Subheader
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WEEKLY OVERVIEW",
                        style = LabelCaps.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = weeklySummary.weekRangeLabel.ifBlank { "CURRENT WEEK" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = InfraredAccent.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, InfraredAccent.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "${weeklySummary.completedWorkoutsCount} OF ${weeklySummary.targetWorkoutsCount} SESSIONS",
                        style = LabelCaps.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = InfraredAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7-Day Interactive Day Strip (Mon – Sun)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                weeklySummary.days.forEach { day ->
                    WeekDayPill(day = day, modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom stats row
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${weeklySummary.totalActiveMinutes} MIN ACTIVE THIS WEEK",
                        style = LabelCaps.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "PLAN ROUTINES >",
                    style = LabelCaps.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = InfraredAccent,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 44.dp)
                        .wrapContentHeight(Alignment.CenterVertically)
                        .clickable { onPlanWorkoutClick() }
                        .padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
fun WeekDayPill(day: WeekDayStatus, modifier: Modifier = Modifier) {
    val isToday = day.isToday
    val hasWorkout = day.hasCompletedWorkout
    val isPlanned = day.hasPlannedWorkout

    val borderColor = when {
        isToday -> InfraredAccent
        hasWorkout -> PrimaryGreen.copy(alpha = 0.8f)
        isPlanned -> InfraredAccent.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    }

    val backgroundColor = when {
        isToday -> InfraredAccent.copy(alpha = 0.12f)
        hasWorkout -> PrimaryGreen.copy(alpha = 0.10f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    }

    Surface(
        modifier = modifier
            .height(68.dp),
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor,
        border = BorderStroke(if (isToday) 1.5.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Day Letter
            Text(
                text = day.dayOfWeekLetter,
                style = LabelCaps.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                color = if (isToday) InfraredAccent else MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Date Number
            Text(
                text = "${day.dayOfMonth}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = if (isToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Status Badge
            Box(
                modifier = Modifier.size(16.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    hasWorkout -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    isPlanned -> {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = "Planned",
                            tint = InfraredAccent,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    isToday -> {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(InfraredAccent)
                        )
                    }
                    else -> {
                        Text(
                            text = "—",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnergyBalanceCard(
    targets: com.example.gains.domain.NutritionTargets,
    nutrientSummary: com.example.gains.data.DailyNutrientSummary,
    activeBurnKcal: Float,
    onEditPlanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val remainingCalories = (targets.caloriesKcal - nutrientSummary.caloriesKcal).toInt()

    GainsCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onEditPlanClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = InfraredAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DAILY ENERGY BALANCE",
                        style = LabelCaps.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onEditPlanClick() }
                ) {
                    Text(
                        text = "PLAN",
                        style = LabelCaps.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = InfraredAccent
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Edit Plan",
                        tint = InfraredAccent,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Calories Equation Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Circular Ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(80.dp)
                ) {
                    val progress = if (targets.caloriesKcal > 0) {
                        (nutrientSummary.caloriesKcal / targets.caloriesKcal.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = InfraredAccent.copy(alpha = 0.15f),
                        strokeWidth = 7.dp,
                        trackColor = Color.Transparent
                    )
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = InfraredAccent,
                        strokeWidth = 7.dp,
                        strokeCap = StrokeCap.Round,
                        trackColor = Color.Transparent
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$remainingCalories",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "LEFT",
                            style = LabelCaps.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Breakdown Pills
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EquationRow(
                        label = "BASE TARGET",
                        value = "${targets.caloriesKcal} kcal",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    EquationRow(
                        label = "FOOD LOGGED",
                        value = "-${nutrientSummary.caloriesKcal.toInt()} kcal",
                        color = SystemRed
                    )
                    EquationRow(
                        label = "WORKOUT BURN",
                        value = "+${activeBurnKcal.toInt()} kcal",
                        color = PrimaryGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Macro Mini Bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MacroLinearPill(
                    label = "PROT",
                    current = nutrientSummary.proteinG,
                    target = targets.proteinG.toFloat(),
                    color = PrimaryGreen,
                    modifier = Modifier.weight(1f)
                )
                MacroLinearPill(
                    label = "CARBS",
                    current = nutrientSummary.carbsG,
                    target = targets.carbsG.toFloat(),
                    color = PrimaryCyan,
                    modifier = Modifier.weight(1f)
                )
                MacroLinearPill(
                    label = "FAT",
                    current = nutrientSummary.fatG,
                    target = targets.fatG.toFloat(),
                    color = PrimaryYellow,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun EquationRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = LabelCaps.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

@Composable
fun MacroLinearPill(
    label: String,
    current: Float,
    target: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    style = LabelCaps.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = color
                )
                Text(
                    text = "${current.toInt()}/${target.toInt()}g",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            val prog = if (target > 0) (current / target).coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(
                progress = { prog },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = color,
                trackColor = color.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun TodayWorkoutCard(
    completedSession: WorkoutSessionWithLabel?,
    plannedSession: PlannedSession?,
    onStartWorkout: () -> Unit,
    onStartPlanned: (PlannedSession) -> Unit,
    onViewRoutines: () -> Unit,
    modifier: Modifier = Modifier
) {
    GainsCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = InfraredAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TODAY'S WORKOUT",
                        style = LabelCaps.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "ROUTINES >",
                    style = LabelCaps.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = InfraredAccent,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 44.dp)
                        .wrapContentHeight(Alignment.CenterVertically)
                        .clickable { onViewRoutines() }
                        .padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                completedSession != null -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = completedSession.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Workout completed today · Active burn credited",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "DONE",
                                style = LabelCaps.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = PrimaryGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                plannedSession != null -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "SCHEDULED TODAY",
                            style = LabelCaps.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = InfraredAccent
                        )
                        Text(
                            text = plannedSession.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onStartPlanned(plannedSession) },
                            colors = ButtonDefaults.buttonColors(containerColor = InfraredAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("START SCHEDULED WORKOUT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
                else -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No workout logged or scheduled yet today.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onStartWorkout,
                            colors = ButtonDefaults.buttonColors(containerColor = InfraredAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("START EMPTY WORKOUT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickLogStrip(
    onLogFood: () -> Unit,
    onStartLift: () -> Unit,
    onWeighIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickLogItem(
            icon = Icons.Default.Restaurant,
            label = "+ FOOD",
            onClick = onLogFood,
            modifier = Modifier.weight(1f)
        )
        QuickLogItem(
            icon = Icons.Default.FitnessCenter,
            label = "+ LIFT",
            onClick = onStartLift,
            modifier = Modifier.weight(1f)
        )
        QuickLogItem(
            icon = Icons.Default.MonitorWeight,
            label = "+ WEIGHT",
            onClick = onWeighIn,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun QuickLogItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = InfraredAccent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = LabelCaps.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun CoachInsightBanner(
    activeBurnKcal: Float,
    completedSession: WorkoutSessionWithLabel?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = PrimaryYellow,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "COACH'S INSIGHT",
                    style = LabelCaps.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = PrimaryYellow
                )
                Spacer(modifier = Modifier.height(2.dp))
                val message = when {
                    activeBurnKcal > 0 -> "Workout credited +${activeBurnKcal.toInt()} kcal. Extra carbohydrates have been prioritized to restock glycogen."
                    completedSession != null -> "Great workout today! Maintain protein intake and stay hydrated for recovery."
                    else -> "Rest day budget active. Keep protein steady and allow your muscles to recover."
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun WeeklyCalibrationCard(
    calibrationState: com.example.gains.domain.CalibrationCheckInState,
    onApply: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    GainsCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Science,
                    contentDescription = null,
                    tint = InfraredAccent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "WEEKLY NUTRITION CALIBRATION",
                    style = LabelCaps.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = InfraredAccent
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = calibrationState.message,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "DISMISS",
                        style = LabelCaps.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onApply(calibrationState.recommendedCalorieAdjustment) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InfraredAccent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    val sign = if (calibrationState.recommendedCalorieAdjustment > 0) "+" else ""
                    Text(
                        text = "APPLY (${sign}${calibrationState.recommendedCalorieAdjustment} KCAL)",
                        style = LabelCaps.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    }
}

