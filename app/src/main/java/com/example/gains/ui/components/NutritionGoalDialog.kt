package com.example.gains.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gains.data.UserProfile
import com.example.gains.domain.*
import com.example.gains.theme.BodySemiBold
import com.example.gains.theme.HeaderBold
import com.example.gains.theme.InfraredAccent
import com.example.gains.theme.LabelCaps
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionGoalDialog(
    userProfile: UserProfile?,
    onSavePlan: (
        activityLevel: ActivityLevel,
        engineMode: NutritionEngineMode,
        allocationStrategy: ActivityAllocationStrategy,
        activeMacroSplit: ActiveCalorieMacroSplit,
        macroPreset: MacroPreset,
        goalType: NutritionGoalType,
        weeklyRatePercent: Float?,
        targetDateTimestamp: Long?,
        customCalories: Float?,
        customProteinG: Float?,
        customCarbsG: Float?,
        customFatG: Float?,
        customFiberG: Float?
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val profileSex = remember(userProfile?.biologicalSex) {
        try { BiologicalSex.valueOf(userProfile?.biologicalSex ?: "MALE") } catch (e: Exception) { BiologicalSex.MALE }
    }
    var selectedActivity by remember {
        mutableStateOf(try { ActivityLevel.valueOf(userProfile?.activityLevel ?: "MODERATE") } catch (e: Exception) { ActivityLevel.MODERATE })
    }
    var selectedEngineMode by remember {
        mutableStateOf(try { NutritionEngineMode.valueOf(userProfile?.nutritionEngineMode ?: "CLASSIC_FORMULA") } catch (e: Exception) { NutritionEngineMode.CLASSIC_FORMULA })
    }
    var selectedAllocationStrategy by remember {
        mutableStateOf(try { ActivityAllocationStrategy.valueOf(userProfile?.activityAllocationStrategy ?: "REALTIME_DAILY_BURN") } catch (e: Exception) { ActivityAllocationStrategy.REALTIME_DAILY_BURN })
    }
    var selectedActiveMacroSplit by remember {
        mutableStateOf(try { ActiveCalorieMacroSplit.valueOf(userProfile?.activeCalorieMacroSplit ?: "CARBS_PRIORITY") } catch (e: Exception) { ActiveCalorieMacroSplit.CARBS_PRIORITY })
    }
    var selectedMacroPreset by remember {
        mutableStateOf(try { MacroPreset.valueOf(userProfile?.macroPreset ?: "BALANCED_SPORTS") } catch (e: Exception) { MacroPreset.BALANCED_SPORTS })
    }
    var selectedGoal by remember {
        mutableStateOf(try { NutritionGoalType.valueOf(userProfile?.nutritionGoalType ?: "MAINTENANCE") } catch (e: Exception) { NutritionGoalType.MAINTENANCE })
    }

    var weeklyRateSlider by remember {
        mutableStateOf(userProfile?.weeklyRatePercent ?: when (selectedGoal) {
            NutritionGoalType.CUT_AGGRESSIVE -> -0.8f
            NutritionGoalType.CUT_MODERATE -> -0.5f
            NutritionGoalType.LEAN_BULK -> 0.25f
            else -> 0.0f
        })
    }

    var customCaloriesText by remember { mutableStateOf(userProfile?.customCaloriesTarget?.toInt()?.toString() ?: "2400") }
    var customProteinText by remember { mutableStateOf(userProfile?.customProteinTargetG?.toInt()?.toString() ?: "180") }
    var customCarbsText by remember { mutableStateOf(userProfile?.customCarbsTargetG?.toInt()?.toString() ?: "250") }
    var customFatText by remember { mutableStateOf(userProfile?.customFatTargetG?.toInt()?.toString() ?: "70") }
    var customFiberText by remember { mutableStateOf(userProfile?.customFiberTargetG?.toInt()?.toString() ?: "30") }

    // Live preview calculation
    val liveTargets = remember(
        userProfile, profileSex, selectedActivity, selectedEngineMode,
        selectedAllocationStrategy, selectedActiveMacroSplit, selectedMacroPreset,
        selectedGoal, weeklyRateSlider, customCaloriesText, customProteinText,
        customCarbsText, customFatText, customFiberText
    ) {
        NutritionCalculator.calculateTargets(
            weightKg = userProfile?.currentWeight,
            heightCm = userProfile?.height,
            ageYears = userProfile?.calculatedAge,
            sex = profileSex,
            activityLevel = selectedActivity,
            engineMode = selectedEngineMode,
            allocationStrategy = selectedAllocationStrategy,
            activeMacroSplit = selectedActiveMacroSplit,
            macroPreset = selectedMacroPreset,
            goalType = selectedGoal,
            weeklyRatePercent = if (selectedGoal == NutritionGoalType.MAINTENANCE) 0f else weeklyRateSlider,
            targetWeightKg = null,
            targetDateTimestamp = null,
            activeCaloriesBurnedToday = if (selectedEngineMode == NutritionEngineMode.ACTIVITY_SYNCED) 350f else 0f,
            customCalories = customCaloriesText.toFloatOrNull(),
            customProteinG = customProteinText.toFloatOrNull(),
            customCarbsG = customCarbsText.toFloatOrNull(),
            customFatG = customFatText.toFloatOrNull(),
            customFiberG = customFiberText.toFloatOrNull()
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "NUTRITION & MACRO PLAN",
                                style = LabelCaps.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                },
                bottomBar = {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        Button(
                            onClick = {
                                onSavePlan(
                                    selectedActivity,
                                    selectedEngineMode,
                                    selectedAllocationStrategy,
                                    selectedActiveMacroSplit,
                                    selectedMacroPreset,
                                    selectedGoal,
                                    if (selectedGoal == NutritionGoalType.MAINTENANCE) 0f else weeklyRateSlider,
                                    null,
                                    customCaloriesText.toFloatOrNull(),
                                    customProteinText.toFloatOrNull(),
                                    customCarbsText.toFloatOrNull(),
                                    customFatText.toFloatOrNull(),
                                    customFiberText.toFloatOrNull()
                                )
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = InfraredAccent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "APPLY NUTRITION PLAN",
                                style = LabelCaps.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Header Live Preview Card
                    GainsCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Science,
                                        contentDescription = null,
                                        tint = InfraredAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "ESTIMATED DAILY TARGET",
                                        style = LabelCaps.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Text(
                                    text = "${liveTargets.caloriesKcal} kcal",
                                    style = HeaderBold.copy(fontSize = 22.sp),
                                    color = InfraredAccent
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Macro pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MiniTargetPill("PROTEIN", "${liveTargets.proteinG.toInt()}g", Color(0xFF10B981))
                                MiniTargetPill("CARBS", "${liveTargets.carbsG.toInt()}g", Color(0xFF0EA5E9))
                                MiniTargetPill("FAT", "${liveTargets.fatG.toInt()}g", Color(0xFFF59E0B))
                                MiniTargetPill("FIBER", "${liveTargets.fiberG.toInt()}g", Color(0xFF8B5CF6))
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "BMR: ${liveTargets.bmrKcal} kcal · Base TDEE: ${liveTargets.tdeeKcal} kcal",
                                    style = LabelCaps.copy(fontSize = 9.5.sp),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                if (selectedEngineMode == NutritionEngineMode.ACTIVITY_SYNCED) {
                                    Text(
                                        text = "Activity-Synced Mode Active",
                                        style = LabelCaps.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                        color = InfraredAccent
                                    )
                                }
                            }
                        }
                    }

                    // Section 1: Engine Mode & Strategy
                    Column {
                        Text(
                            text = "CALCULATION ENGINE MODE",
                            style = LabelCaps.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            NutritionEngineMode.values().forEach { mode ->
                                val isSelected = selectedEngineMode == mode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) InfraredAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                                        .border(1.dp, if (isSelected) InfraredAccent else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                        .clickable { selectedEngineMode = mode }
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = mode.label,
                                            style = BodySemiBold.copy(fontSize = 12.sp),
                                            color = if (isSelected) InfraredAccent else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // If Activity-Synced is active: show Allocation Strategy selector
                        AnimatedVisibility(visible = selectedEngineMode == NutritionEngineMode.ACTIVITY_SYNCED) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                Text(
                                    text = "ACTIVITY ALLOCATION STRATEGY",
                                    style = LabelCaps.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ActivityAllocationStrategy.values().forEach { strategy ->
                                        val isSelected = selectedAllocationStrategy == strategy
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) InfraredAccent.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface)
                                                .border(1.dp, if (isSelected) InfraredAccent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                .clickable { selectedAllocationStrategy = strategy }
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { selectedAllocationStrategy = strategy },
                                                colors = RadioButtonDefaults.colors(selectedColor = InfraredAccent)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(strategy.label, style = BodySemiBold.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurface)
                                                Text(strategy.description, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.secondary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section 2: Physical Activity Level (PAL)
                    Column {
                        Text(
                            text = "PHYSICAL ACTIVITY LEVEL (PAL)",
                            style = LabelCaps.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ActivityLevel.values().forEach { level ->
                                val isSelected = selectedActivity == level
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) InfraredAccent.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface)
                                        .border(1.dp, if (isSelected) InfraredAccent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .clickable { selectedActivity = level }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedActivity = level },
                                        colors = RadioButtonDefaults.colors(selectedColor = InfraredAccent)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(level.label, style = BodySemiBold.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurface)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("(${level.multiplier}x)", style = LabelCaps.copy(fontSize = 9.sp), color = InfraredAccent)
                                        }
                                        Text(level.description, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            }
                        }
                    }

                    // Section 3: Goal & Pace Slider
                    Column {
                        Text(
                            text = "FITNESS GOAL",
                            style = LabelCaps.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            NutritionGoalType.values().forEach { goal ->
                                val isSelected = selectedGoal == goal
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) InfraredAccent.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface)
                                        .border(1.dp, if (isSelected) InfraredAccent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedGoal = goal
                                            when (goal) {
                                                NutritionGoalType.CUT_AGGRESSIVE -> weeklyRateSlider = -0.8f
                                                NutritionGoalType.CUT_MODERATE -> weeklyRateSlider = -0.5f
                                                NutritionGoalType.LEAN_BULK -> weeklyRateSlider = 0.25f
                                                else -> weeklyRateSlider = 0.0f
                                            }
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            selectedGoal = goal
                                            when (goal) {
                                                NutritionGoalType.CUT_AGGRESSIVE -> weeklyRateSlider = -0.8f
                                                NutritionGoalType.CUT_MODERATE -> weeklyRateSlider = -0.5f
                                                NutritionGoalType.LEAN_BULK -> weeklyRateSlider = 0.25f
                                                else -> weeklyRateSlider = 0.0f
                                            }
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = InfraredAccent)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(goal.label, style = BodySemiBold.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurface)
                                        Text(goal.description, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            }
                        }

                        // Rate of Change Slider (for Cut and Bulk)
                        if (selectedGoal != NutritionGoalType.MAINTENANCE && selectedGoal != NutritionGoalType.CUSTOM) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "WEEKLY RATE OF CHANGE: ${if (weeklyRateSlider > 0) "+" else ""}${String.format("%.2f", weeklyRateSlider)}% / WEEK",
                                style = LabelCaps.copy(fontSize = 10.sp),
                                color = InfraredAccent
                            )
                            Slider(
                                value = weeklyRateSlider,
                                onValueChange = { weeklyRateSlider = (it * 20).roundToInt() / 20f },
                                valueRange = if (selectedGoal == NutritionGoalType.LEAN_BULK) 0.1f..0.6f else -1.2f..-0.2f,
                                colors = SliderDefaults.colors(
                                    thumbColor = InfraredAccent,
                                    activeTrackColor = InfraredAccent
                                )
                            )
                        }
                    }

                    // Section 4: Macro Presets
                    Column {
                        Text(
                            text = "MACRONUTRIENT DISTRIBUTION",
                            style = LabelCaps.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            MacroPreset.values().forEach { preset ->
                                val isSelected = selectedMacroPreset == preset
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) InfraredAccent.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface)
                                        .border(1.dp, if (isSelected) InfraredAccent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .clickable { selectedMacroPreset = preset },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedMacroPreset = preset },
                                        colors = RadioButtonDefaults.colors(selectedColor = InfraredAccent)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                        Text(preset.label, style = BodySemiBold.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurface)
                                        Text(preset.description, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            }
                        }
                    }

                    // Section 5: Active Workout Calorie Macro Allocation
                    Column {
                        Text(
                            text = "ACTIVE CALORIE MACRO SPLIT",
                            style = LabelCaps.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ActiveCalorieMacroSplit.values().forEach { split ->
                                val isSelected = selectedActiveMacroSplit == split
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) InfraredAccent.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface)
                                        .border(1.dp, if (isSelected) InfraredAccent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .clickable { selectedActiveMacroSplit = split },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedActiveMacroSplit = split },
                                        colors = RadioButtonDefaults.colors(selectedColor = InfraredAccent)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                        Text(split.label, style = BodySemiBold.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurface)
                                        Text(split.description, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            }
                        }
                    }

                    // Custom Fields (If Custom is selected)
                    if (selectedMacroPreset == MacroPreset.CUSTOM || selectedGoal == NutritionGoalType.CUSTOM) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = InfraredAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CUSTOM TARGETS",
                                    style = LabelCaps.copy(fontSize = 11.sp),
                                    color = InfraredAccent
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = customCaloriesText,
                                onValueChange = { customCaloriesText = it },
                                label = { Text("Daily Calories (kcal)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customProteinText,
                                    onValueChange = { customProteinText = it },
                                    label = { Text("Protein (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                OutlinedTextField(
                                    value = customCarbsText,
                                    onValueChange = { customCarbsText = it },
                                    label = { Text("Carbs (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customFatText,
                                    onValueChange = { customFatText = it },
                                    label = { Text("Fat (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                OutlinedTextField(
                                    value = customFiberText,
                                    onValueChange = { customFiberText = it },
                                    label = { Text("Fiber (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }

                    // Science Citation Info Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BMR is computed via Mifflin-St Jeor (ADA gold standard). Macro distribution follows ISSN recommendations (1.8–2.2g/kg protein, 20–25% fat, balance in carbs). In Activity-Synced mode, calories adapt dynamically based on your logged training and Health Connect sync.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun MiniTargetPill(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = LabelCaps.copy(fontSize = 8.sp),
            color = color
        )
        Text(
            text = value,
            style = HeaderBold.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
