package com.example.gains.ui.main.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import com.example.gains.ui.components.NutritionGoalDialog
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.navigation3.runtime.NavKey
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gains.data.DailyNutrientSummary
import com.example.gains.data.FoodItem
import com.example.gains.data.FoodNutrient
import com.example.gains.data.FoodRecipeWithDetails
import com.example.gains.data.FoodServing
import com.example.gains.data.LoggedFoodEntry
import com.example.gains.theme.*
import com.example.gains.ui.components.AddFoodDialog
import com.example.gains.ui.components.CollapsibleLoggedRecipeCard
import com.example.gains.ui.components.CreateRecipeDialog
import com.example.gains.ui.components.GainsCard
import com.example.gains.ui.components.NutrientBreakdownDialog
import com.example.gains.ui.components.RecipeCardItem
import com.example.gains.ui.components.RecipeDetailViewDialog
import com.example.gains.ui.main.MainScreenViewModel
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FoodTabContent(
    viewModel: MainScreenViewModel,
    repository: com.example.gains.data.DataRepository,
    onItemClick: (NavKey) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val selectedDateTimestamp by viewModel.selectedFoodDateTimestamp.collectAsState()
    val loggedEntries by viewModel.loggedFoodEntries.collectAsState()
    val summary by viewModel.dailyNutrientSummary.collectAsState()
    val targets by viewModel.nutritionTargets.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val userProfile = (uiState as? com.example.gains.ui.main.MainScreenUiState.Success)?.userProfile
    var showNutritionGoalDialog by remember { mutableStateOf(false) }

    val searchQuery by viewModel.foodSearchQuery.collectAsState()
    val searchResults by viewModel.foodSearchResults.collectAsState()

    val filteredRecipes by viewModel.filteredRecipes.collectAsState()

    var showAddFoodDialog by remember { mutableStateOf(false) }
    var dialogMealType by remember { mutableStateOf("BREAKFAST") }
    var isMicronutrientsExpanded by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("EEEE, d MMM", Locale.ENGLISH) }
    val isToday = remember(selectedDateTimestamp) {
        val todayCal = java.util.Calendar.getInstance()
        val selCal = java.util.Calendar.getInstance().apply { timeInMillis = selectedDateTimestamp }
        todayCal.get(java.util.Calendar.YEAR) == selCal.get(java.util.Calendar.YEAR) &&
                todayCal.get(java.util.Calendar.DAY_OF_YEAR) == selCal.get(java.util.Calendar.DAY_OF_YEAR)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // ==================== FOOD DIARY VIEW ====================
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                    // Top Date Navigation Bar
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.changeSelectedFoodDate(-1) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day", tint = MaterialTheme.colorScheme.onSurface)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isToday) "TODAY" else dateFormaterText(selectedDateTimestamp, dateFormatter),
                                    style = LabelCaps.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                    color = InfraredAccent
                                )
                                Text(
                                    text = dateFormatter.format(Date(selectedDateTimestamp)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = { viewModel.changeSelectedFoodDate(1) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    // Recipes Studio Hub Bento Card
                    item {
                        GainsCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onItemClick(com.example.gains.RecipesHub) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = InfraredAccent.copy(alpha = 0.15f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.RestaurantMenu,
                                                contentDescription = null,
                                                tint = InfraredAccent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "RECIPES & MEALS STUDIO",
                                            style = LabelCaps.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "${filteredRecipes.size} Saved • 'What's in your kitchen' Search",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Open Recipes",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Daily Macro Progress Summary
                    item {
                        GainsCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showNutritionGoalDialog = true },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Restaurant, contentDescription = null, tint = InfraredAccent, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("DAILY NUTRITION BUDGET", style = LabelCaps.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurface)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Goals", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                                    }
                                    Text(
                                        text = "${summary.caloriesKcal.toInt()} / ${targets.caloriesKcal} kcal",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (summary.caloriesKcal > targets.caloriesKcal) SystemRed else InfraredAccent
                                    )
                                }

                                if (targets.engineMode == com.example.gains.domain.NutritionEngineMode.ACTIVITY_SYNCED && targets.activeCaloriesKcal > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(InfraredAccent))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Base ${targets.baseCaloriesKcal} + Active Burn ${targets.activeCaloriesKcal} kcal",
                                            style = LabelCaps.copy(fontSize = 10.sp),
                                            color = InfraredAccent
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val targetCal = targets.caloriesKcal.toFloat()
                                val calProgress = if (targetCal > 0f) (summary.caloriesKcal / targetCal).coerceIn(0f, 1f) else 0f
                                LinearProgressIndicator(
                                    progress = { calProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (summary.caloriesKcal > targets.caloriesKcal) SystemRed else InfraredAccent,
                                    trackColor = InfraredAccent.copy(alpha = 0.15f)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Macros Circular Progress Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    MacroCircularStat("PROTEIN", summary.proteinG, targets.proteinG, "g", Color(0xFF10B981))
                                    MacroCircularStat("CARBS", summary.carbsG, targets.carbsG, "g", Color(0xFF0EA5E9))
                                    MacroCircularStat("FAT", summary.fatG, targets.fatG, "g", Color(0xFFF59E0B))
                                    MacroCircularStat("FIBER", summary.fiberG, targets.fiberG, "g", Color(0xFF8B5CF6))
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isMicronutrientsExpanded = !isMicronutrientsExpanded },
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isMicronutrientsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "Toggle Micronutrients",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isMicronutrientsExpanded) "HIDE MICRONUTRIENTS" else "VIEW MICRONUTRIENTS",
                                        style = LabelCaps.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isMicronutrientsExpanded) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("KEY HEALTH METRICS (DUTCH VOEDINGSCENTRUM)", style = LabelCaps.copy(fontSize = 10.sp), color = InfraredAccent)
                                        com.example.gains.ui.components.NutrientRow("Dietary Fiber", "${String.format("%.1f", summary.fiberG)}g", targets.fiberG, summary.fiberG)
                                        com.example.gains.ui.components.NutrientRow("Saturated Fat", "${String.format("%.1f", summary.saturatedFatG)}g (Max ${targets.saturatedFatMaxG.toInt()}g)", targets.saturatedFatMaxG, summary.saturatedFatG, isMaxConstraint = true)
                                        com.example.gains.ui.components.NutrientRow("Sugars", "${String.format("%.1f", summary.sugarsG)}g (Max ${targets.sugarsMaxG.toInt()}g)", targets.sugarsMaxG, summary.sugarsG, isMaxConstraint = true)
                                        com.example.gains.ui.components.NutrientRow("Salt", "${String.format("%.1f", summary.saltG)}g (Max ${String.format("%.1f", targets.saltMaxG)}g)", targets.saltMaxG, summary.saltG, isMaxConstraint = true)

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("VITAMINS & MINERALS (ESTIMATED)", style = LabelCaps.copy(fontSize = 10.sp), color = InfraredAccent)
                                        com.example.gains.ui.components.NutrientRow("Calcium", "450 mg", 1000f, 450f)
                                        com.example.gains.ui.components.NutrientRow("Iron", "8.2 mg", 11f, 8.2f)
                                        com.example.gains.ui.components.NutrientRow("Vitamin C", "45 mg", 75f, 45f)
                                        com.example.gains.ui.components.NutrientRow("Vitamin D", "4.0 µg", 10f, 4.0f)
                                        com.example.gains.ui.components.NutrientRow("Vitamin B12", "1.8 µg", 2.8f, 1.8f)
                                        com.example.gains.ui.components.NutrientRow("Magnesium", "220 mg", 350f, 220f)
                                    }
                                }
                            }
                        }
                    }

                    // Meal Categories List (Title-based layout, no separate cards)
                    val mealCategories = listOf(
                        Triple("BREAKFAST", Icons.Default.RestaurantMenu, "Breakfast"),
                        Triple("LUNCH", Icons.Default.LocalDining, "Lunch"),
                        Triple("DINNER", Icons.Default.Restaurant, "Dinner"),
                        Triple("SNACK", Icons.Default.Fastfood, "Snacks & Sports")
                    )

                    mealCategories.forEach { (mealKey, icon, title) ->
                        item {
                            val entriesForMeal = loggedEntries.filter { it.mealType == mealKey }
                            val mealCalories = entriesForMeal.sumOf { it.caloriesKcal.toDouble() }.toInt()

                            val recipeGroups = entriesForMeal.filter { it.recipeLogGroupId != null }.groupBy { it.recipeLogGroupId }
                            val ungroupedEntries = entriesForMeal.filter { it.recipeLogGroupId == null }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                // Title-based Meal Section Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = title.uppercase(),
                                            style = LabelCaps.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (mealCalories > 0) {
                                            Surface(
                                                color = PrimarySoftBg,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "$mealCalories kcal",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = InfraredAccent,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                dialogMealType = mealKey
                                                showAddFoodDialog = true
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Add Food", tint = InfraredAccent, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }

                                // Entries List or Minimalist Prompt
                                if (entriesForMeal.isEmpty()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                dialogMealType = mealKey
                                                showAddFoodDialog = true
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Add $title",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // Collapsible Recipe Groups
                                        recipeGroups.forEach { (_, groupEntries) ->
                                            val recipeName = groupEntries.firstOrNull()?.recipeName ?: "Custom Meal"
                                            CollapsibleLoggedRecipeCard(
                                                recipeName = recipeName,
                                                entries = groupEntries,
                                                onDeleteEntry = { id -> viewModel.deleteLoggedFoodEntry(id) },
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            )
                                        }

                                        // Clean Minimalist Logged Entries
                                        ungroupedEntries.forEach { entry ->
                                            CleanLoggedFoodRow(
                                                entry = entry,
                                                onDelete = { viewModel.deleteLoggedFoodEntry(entry.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

        // Floating Action Button for Food Diary
        FloatingActionButton(
            onClick = {
                dialogMealType = "BREAKFAST"
                showAddFoodDialog = true
            },
            containerColor = InfraredAccent,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Food")
                Spacer(modifier = Modifier.width(6.dp))
                Text("LOG FOOD", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }

    // Dialogs
    if (showAddFoodDialog) {
        AddFoodDialog(
            initialMealType = dialogMealType,
            searchQuery = searchQuery,
            searchResults = searchResults,
            onQueryChange = { viewModel.setFoodSearchQuery(it) },
            onLogFood = { food, nut, serv, qty, meal ->
                viewModel.logFoodItem(food, nut, serv, qty, meal)
            },
            onDismiss = { showAddFoodDialog = false },
            getNutrientForFoodSync = { id -> repository.getNutrientForFoodSync(id) },
            getServingsForFoodSync = { id -> repository.getServingsForFoodSync(id) }
        )
    }

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
}

@Composable
fun RowScope.MacroCircularStat(
    label: String,
    current: Float,
    target: Float,
    unit: String,
    color: Color
) {
    val progress = if (target > 0f) (current / target).coerceIn(0f, 1f) else 0f
    val percentage = if (target > 0f) ((current / target) * 100).toInt() else 0
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(52.dp)
        ) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = color.copy(alpha = 0.15f),
                strokeWidth = 5.dp,
                trackColor = Color.Transparent
            )
            if (progress > 0f) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = color,
                    strokeWidth = 5.dp,
                    trackColor = Color.Transparent,
                    strokeCap = StrokeCap.Round
                )
            }
            Text(
                text = "$percentage%",
                fontWeight = FontWeight.Bold,
                fontSize = if (percentage >= 1000) 10.sp else 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label.uppercase(),
            style = LabelCaps.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val onSurface = MaterialTheme.colorScheme.onSurface
        val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

        val macroSubtitleText = remember(current, target, unit, color, onSurface, onSurfaceVariant) {
            buildAnnotatedString {
                // Current Value (Bold Primary)
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = onSurface
                    )
                ) {
                    append("${current.toInt()}")
                }

                // Macro-accent tinted slash separator
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = color.copy(alpha = 0.9f)
                    )
                ) {
                    append(" / ")
                }

                // Target Value & Unit (Muted Secondary)
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        color = onSurfaceVariant.copy(alpha = 0.75f)
                    )
                ) {
                    append("${target.toInt()}$unit")
                }
            }
        }
        Text(
            text = macroSubtitleText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun CleanLoggedFoodRow(
    entry: LoggedFoodEntry,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    GainsCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Food Image Thumbnail (with fallback generic icon)
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(8.dp),
                color = PrimarySoftBg
            ) {
                if (!entry.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = entry.imageUrl,
                        contentDescription = entry.foodName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.RestaurantMenu,
                            contentDescription = null,
                            tint = InfraredAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.foodName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                val portionStr = cleanServingDescription(entry.servingDescription, entry.gramWeightTotal)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (entry.caloriesKcal > 0 || entry.proteinG > 0 || entry.carbsG > 0 || entry.fatG > 0) {
                        Text(
                            text = "${entry.caloriesKcal.toInt()} kcal",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = InfraredAccent
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Geen voedingswaarden",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = portionStr,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete entry",
                    tint = SystemRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun cleanServingDescription(raw: String?, gramTotal: Float): String {
    val weightStr = "${gramTotal.toInt()}g"
    if (raw.isNullOrBlank()) return weightStr
    val lower = raw.lowercase().trim()
    if (lower.contains("verpakking") || lower.contains("portie") || lower.contains("stuk") || lower.contains("stuks")) {
        return weightStr
    }
    if (raw.contains("(") && raw.contains(")")) {
        val baseName = raw.substringBefore("(").trim()
        val baseLower = baseName.lowercase()
        if (baseLower.contains("verpakking") || baseLower.contains("portie") || baseLower.contains("stuk") || baseLower.matches(Regex("""\d+\s*"""))) {
            return weightStr
        }
        return "$baseName ($weightStr)"
    }
    return if (raw.endsWith("g") || raw.endsWith("ml")) raw else weightStr
}

private fun dateFormaterText(timestamp: Long, formatter: SimpleDateFormat): String {
    return formatter.format(Date(timestamp)).uppercase()
}
