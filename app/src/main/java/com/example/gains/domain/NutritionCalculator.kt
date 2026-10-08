package com.example.gains.domain

import kotlin.math.abs
import kotlin.math.roundToInt

enum class BiologicalSex {
    MALE,
    FEMALE
}

enum class ActivityLevel(val multiplier: Float, val label: String, val description: String) {
    SEDENTARY(1.20f, "Sedentary", "Little or no exercise, desk job"),
    LIGHT(1.375f, "Lightly Active", "1–3 workouts / light exercise per week"),
    MODERATE(1.55f, "Moderately Active", "3–5 workouts / moderate activity per week"),
    VERY_ACTIVE(1.725f, "Very Active", "6–7 intense workouts per week"),
    EXTRA_ACTIVE(1.90f, "Extra Active", "Daily heavy training or physically demanding job")
}

enum class NutritionEngineMode(val label: String, val description: String) {
    CLASSIC_FORMULA("Classic Formula", "Mifflin-St Jeor BMR + Physical Activity Level multiplier"),
    ACTIVITY_SYNCED("Activity-Synced Dynamic", "Adapts calories from GAINS workouts, steps, & Health Connect")
}

enum class ActivityAllocationStrategy(val label: String, val description: String) {
    REALTIME_DAILY_BURN("Real-Time Daily Burn", "Base budget + actual workout calories credited as logged"),
    WEEKLY_AVERAGED("Weekly Averaged", "Smoothed average active burn added consistently each day"),
    TRAINING_REST_SPLIT("Training vs. Rest Split", "Higher calories/carbs on scheduled workout days, lower on rest days")
}

enum class ActiveCalorieMacroSplit(val label: String, val description: String) {
    CARBS_PRIORITY("Carbohydrates Priority (Recommended)", "100% of bonus calories go to carbs for glycogen resynthesis"),
    PROPORTIONAL("Proportional Macro Split", "Distributes bonus calories across Protein, Carbs, and Fat equally")
}

enum class MacroPreset(val label: String, val description: String) {
    BALANCED_SPORTS("Balanced Sports (ISSN)", "2.0g/kg protein, 25% fat, balanced carbs for lifting"),
    HIGH_CARB_PERFORMANCE("High Carb Performance", "1.8g/kg protein, 18% fat, maximum carbs to fuel high training volume"),
    LOW_CARB("Low Carb", "2.2g/kg protein, 45% fat, reduced carbohydrates"),
    CUSTOM("Custom Macros", "Manually specified caloric and macronutrient targets")
}

enum class NutritionGoalType(val defaultCalorieMultiplier: Float, val label: String, val description: String) {
    CUT_AGGRESSIVE(0.75f, "Aggressive Cut (-25%)", "Rapid fat loss sprint, elevated protein required"),
    CUT_MODERATE(0.80f, "Moderate Cut (-20%)", "Recommended sustainable fat loss preserving lean mass"),
    MAINTENANCE(1.00f, "Maintenance (0%)", "Energy balance, recomposition, optimal performance"),
    LEAN_BULK(1.10f, "Lean Bulk (+10%)", "Lean muscle hypertrophy with minimal fat gain"),
    CUSTOM(1.00f, "Custom", "Manually specified caloric and macro goals")
}

data class GoalPaceConfig(
    val goalType: NutritionGoalType = NutritionGoalType.MAINTENANCE,
    val weeklyRatePercent: Float? = null, // e.g. -0.5% for moderate cut, +0.25% for lean bulk
    val targetWeightKg: Double? = null,
    val targetDateTimestamp: Long? = null
)

data class CalibrationCheckInState(
    val recommendedCalorieAdjustment: Int,
    val observedWeeklyRateKg: Double,
    val targetWeeklyRateKg: Double,
    val weighInCount: Int,
    val loggedDaysCount: Int,
    val message: String
)

data class NutritionTargets(
    val caloriesKcal: Int,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val fiberG: Float,
    val bmrKcal: Int,
    val tdeeKcal: Int,
    val baseCaloriesKcal: Int = caloriesKcal,
    val activeCaloriesKcal: Int = 0,
    val saturatedFatMaxG: Float = 22f,
    val sugarsMaxG: Float = 50f,
    val saltMaxG: Float = 6.0f,
    val engineMode: NutritionEngineMode = NutritionEngineMode.CLASSIC_FORMULA,
    val allocationStrategy: ActivityAllocationStrategy = ActivityAllocationStrategy.REALTIME_DAILY_BURN,
    val activeMacroSplit: ActiveCalorieMacroSplit = ActiveCalorieMacroSplit.CARBS_PRIORITY,
    val macroPreset: MacroPreset = MacroPreset.BALANCED_SPORTS,
    val goalType: NutritionGoalType = NutritionGoalType.MAINTENANCE,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val sex: BiologicalSex = BiologicalSex.MALE,
    val isCustom: Boolean = false
)

object NutritionCalculator {

    /**
     * Calculates Basal Metabolic Rate (BMR) using the Mifflin-St Jeor equation (1990).
     * BMR_male = 10 * weight(kg) + 6.25 * height(cm) - 5 * age + 5
     * BMR_female = 10 * weight(kg) + 6.25 * height(cm) - 5 * age - 161
     */
    fun calculateBmr(
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
        sex: BiologicalSex
    ): Float {
        val safeWeight = weightKg.coerceAtLeast(30.0)
        val safeHeight = heightCm.coerceAtLeast(100.0)
        val safeAge = ageYears.coerceIn(14, 100)

        val base = (10.0 * safeWeight) + (6.25 * safeHeight) - (5.0 * safeAge)
        return when (sex) {
            BiologicalSex.MALE -> (base + 5.0).toFloat()
            BiologicalSex.FEMALE -> (base - 161.0).toFloat()
        }
    }

    /**
     * Calculates Total Daily Energy Expenditure (TDEE) = BMR * Physical Activity Level (PAL).
     */
    fun calculateTdee(bmr: Float, activityLevel: ActivityLevel): Float {
        return bmr * activityLevel.multiplier
    }

    /**
     * Estimates calorie burn of a resistance training gym session using standard MET = 5.0.
     * Calories = (MET * 3.5 * weightKg / 200) * durationMinutes
     */
    fun calculateGymSessionBurnKcal(durationMinutes: Int, userWeightKg: Double?): Float {
        val weight = userWeightKg ?: 78.0
        val safeMinutes = durationMinutes.coerceIn(1, 240)
        // Standard MET 5.0 formula
        return ((5.0 * 3.5 * weight / 200.0) * safeMinutes).toFloat()
    }

    /**
     * Calculates target calories given a baseline TDEE and Goal Pace configuration.
     * - Rate %/week: each 1 kg body mass lost/gained ≈ 7,700 kcal. Daily delta = (weeklyRateKg * 7700) / 7.
     * - Target date: calculates needed daily deficit to hit target weight by date.
     */
    fun calculateTargetCalories(
        baseTdee: Float,
        currentWeightKg: Double,
        paceConfig: GoalPaceConfig
    ): Int {
        if (paceConfig.goalType == NutritionGoalType.CUSTOM) {
            return baseTdee.roundToInt()
        }

        // If target date is configured and in the future
        if (paceConfig.targetDateTimestamp != null && paceConfig.targetWeightKg != null) {
            val now = System.currentTimeMillis()
            val diffMs = paceConfig.targetDateTimestamp - now
            val daysRemaining = (diffMs / (1000L * 60 * 60 * 24)).coerceAtLeast(1L).toInt()
            val weightDeltaKg = currentWeightKg - paceConfig.targetWeightKg // positive if losing weight

            if (daysRemaining > 0 && abs(weightDeltaKg) > 0.1) {
                val totalCalorieDelta = weightDeltaKg * 7700.0
                val dailyDelta = totalCalorieDelta / daysRemaining
                // Clamped to maximum 25% deficit / 15% surplus for health and safety
                val minAllowed = baseTdee * 0.75f
                val maxAllowed = baseTdee * 1.15f
                val calculated = (baseTdee - dailyDelta).toFloat()
                return calculated.coerceIn(minAllowed, maxAllowed).roundToInt().coerceAtLeast(1200)
            }
        }

        // Rate of change (% of body weight per week)
        if (paceConfig.weeklyRatePercent != null && paceConfig.weeklyRatePercent != 0f) {
            val weeklyKgChange = currentWeightKg * (paceConfig.weeklyRatePercent / 100.0)
            val dailyDelta = (weeklyKgChange * 7700.0) / 7.0
            val calculated = (baseTdee + dailyDelta).toFloat()
            // Clamp between 1200 kcal floor and safe bounds
            return calculated.roundToInt().coerceAtLeast(1200)
        }

        // Default multiplier for goal type
        return (baseTdee * paceConfig.goalType.defaultCalorieMultiplier).roundToInt().coerceAtLeast(1200)
    }

    /**
     * Partitions macros based on calories, weight, and scientific preset.
     */
    fun partitionMacros(
        targetCalories: Int,
        userWeightKg: Double,
        preset: MacroPreset,
        goalType: NutritionGoalType,
        customProteinG: Float? = null,
        customCarbsG: Float? = null,
        customFatG: Float? = null,
        customFiberG: Float? = null
    ): QuadrupleMacros {
        if (preset == MacroPreset.CUSTOM) {
            val protein = customProteinG ?: (userWeightKg * 2.0).toFloat()
            val fat = customFatG ?: ((targetCalories * 0.25f) / 9f)
            val carbs = customCarbsG ?: (((targetCalories - (protein * 4f + fat * 9f)).coerceAtLeast(0f)) / 4f)
            val fiber = customFiberG ?: ((targetCalories / 1000f) * 14f).coerceAtLeast(30f)
            return QuadrupleMacros(protein, carbs, fat, fiber)
        }

        val (proteinMultiplier, fatPercent) = when (preset) {
            MacroPreset.BALANCED_SPORTS -> {
                val prot = if (goalType == NutritionGoalType.CUT_AGGRESSIVE || goalType == NutritionGoalType.CUT_MODERATE) 2.2f else 2.0f
                Pair(prot, 0.25f)
            }
            MacroPreset.HIGH_CARB_PERFORMANCE -> {
                Pair(1.8f, 0.18f)
            }
            MacroPreset.LOW_CARB -> {
                Pair(2.2f, 0.45f)
            }
            MacroPreset.CUSTOM -> Pair(2.0f, 0.25f)
        }

        val proteinG = (userWeightKg * proteinMultiplier).toFloat().coerceAtLeast(60f)
        val proteinKcal = proteinG * 4f

        val fatKcalTarget = (targetCalories * fatPercent).coerceAtLeast((userWeightKg * 0.6 * 9.0).toFloat())
        val fatG = fatKcalTarget / 9f

        val remainingKcal = (targetCalories - proteinKcal - fatKcalTarget).coerceAtLeast(0f)
        val carbsG = remainingKcal / 4f

        val fiberG = ((targetCalories / 1000f) * 14f).coerceAtLeast(30f)

        return QuadrupleMacros(
            proteinG = ((proteinG * 10).roundToInt() / 10f),
            carbsG = ((carbsG * 10).roundToInt() / 10f),
            fatG = ((fatG * 10).roundToInt() / 10f),
            fiberG = ((fiberG * 10).roundToInt() / 10f)
        )
    }

    /**
     * Applies active workout / step calories to an existing baseline target.
     * Default: Carbs Priority (100% of extra calories allocated to carbs for glycogen resynthesis).
     */
    fun applyActiveCalories(
        baseTargets: NutritionTargets,
        activeBurnKcal: Float,
        splitStrategy: ActiveCalorieMacroSplit
    ): NutritionTargets {
        if (activeBurnKcal <= 0f) return baseTargets

        val totalCalories = baseTargets.baseCaloriesKcal + activeBurnKcal.roundToInt()

        return when (splitStrategy) {
            ActiveCalorieMacroSplit.CARBS_PRIORITY -> {
                val extraCarbsG = activeBurnKcal / 4f
                baseTargets.copy(
                    caloriesKcal = totalCalories,
                    carbsG = ((baseTargets.carbsG + extraCarbsG) * 10).roundToInt() / 10f,
                    activeCaloriesKcal = activeBurnKcal.roundToInt()
                )
            }
            ActiveCalorieMacroSplit.PROPORTIONAL -> {
                val baseTotal = baseTargets.caloriesKcal.toFloat().coerceAtLeast(1000f)
                val proteinRatio = (baseTargets.proteinG * 4f) / baseTotal
                val fatRatio = (baseTargets.fatG * 9f) / baseTotal
                val carbsRatio = (baseTargets.carbsG * 4f) / baseTotal

                val extraProtG = (activeBurnKcal * proteinRatio) / 4f
                val extraFatG = (activeBurnKcal * fatRatio) / 9f
                val extraCarbG = (activeBurnKcal * carbsRatio) / 4f

                baseTargets.copy(
                    caloriesKcal = totalCalories,
                    proteinG = ((baseTargets.proteinG + extraProtG) * 10).roundToInt() / 10f,
                    fatG = ((baseTargets.fatG + extraFatG) * 10).roundToInt() / 10f,
                    carbsG = ((baseTargets.carbsG + extraCarbG) * 10).roundToInt() / 10f,
                    activeCaloriesKcal = activeBurnKcal.roundToInt()
                )
            }
        }
    }

    /**
     * Evaluates scale weight readings and logged days to compute a non-intrusive weekly calibration recommendation.
     * Only returns a state if minimum thresholds (>= 3 weigh-ins, >= 4 logged days) are met.
     */
    fun evaluateWeeklyCalibration(
        currentTdeeKcal: Int,
        observedSevenDayWeightDeltaKg: Double,
        targetWeeklyWeightDeltaKg: Double,
        weighInCount: Int,
        loggedDaysCount: Int
    ): CalibrationCheckInState? {
        if (weighInCount < 3 || loggedDaysCount < 4) {
            return null // Not enough data to make an accurate, noise-free recommendation
        }

        // Expected calorie imbalance: 1 kg difference = ~7700 kcal / 7 = 1100 kcal/day
        val rateMismatchKg = observedSevenDayWeightDeltaKg - targetWeeklyWeightDeltaKg
        // For instance, if user is cutting (target -0.5 kg) but observed is -0.1 kg:
        // rateMismatch = -0.1 - (-0.5) = +0.4 kg (losing too slowly)
        val suggestedDailyAdjustment = -(rateMismatchKg * 7700.0 / 7.0).roundToInt()

        // Gentle clamping: don't suggest more than ±150 kcal adjustment per week to avoid whipsawing
        val clampedAdjustment = suggestedDailyAdjustment.coerceIn(-150, 150)

        // Only surface if adjustment is meaningful (at least ±50 kcal)
        if (abs(clampedAdjustment) < 50) {
            return null
        }

        val message = if (clampedAdjustment < 0) {
            "Your weight loss has averaged ${String.format("%.2f", abs(observedSevenDayWeightDeltaKg))} kg/week (Goal: ${String.format("%.2f", abs(targetWeeklyWeightDeltaKg))} kg/week). Would you like to calibrate your daily budget by $clampedAdjustment kcal?"
        } else {
            "Weight trend indicates your expenditure is higher than estimated. Would you like to increase your daily budget by +$clampedAdjustment kcal?"
        }

        return CalibrationCheckInState(
            recommendedCalorieAdjustment = clampedAdjustment,
            observedWeeklyRateKg = observedSevenDayWeightDeltaKg,
            targetWeeklyRateKg = targetWeeklyWeightDeltaKg,
            weighInCount = weighInCount,
            loggedDaysCount = loggedDaysCount,
            message = message
        )
    }

    /**
     * Master method to calculate full nutrition targets.
     */
    fun calculateTargets(
        weightKg: Double?,
        heightCm: Double?,
        ageYears: Int?,
        sex: BiologicalSex = BiologicalSex.MALE,
        activityLevel: ActivityLevel = ActivityLevel.MODERATE,
        engineMode: NutritionEngineMode = NutritionEngineMode.CLASSIC_FORMULA,
        allocationStrategy: ActivityAllocationStrategy = ActivityAllocationStrategy.REALTIME_DAILY_BURN,
        activeMacroSplit: ActiveCalorieMacroSplit = ActiveCalorieMacroSplit.CARBS_PRIORITY,
        macroPreset: MacroPreset = MacroPreset.BALANCED_SPORTS,
        goalType: NutritionGoalType = NutritionGoalType.MAINTENANCE,
        weeklyRatePercent: Float? = null,
        targetWeightKg: Double? = null,
        targetDateTimestamp: Long? = null,
        activeCaloriesBurnedToday: Float = 0f,
        customCalories: Float? = null,
        customProteinG: Float? = null,
        customCarbsG: Float? = null,
        customFatG: Float? = null,
        customFiberG: Float? = null
    ): NutritionTargets {
        val effectiveWeight = weightKg ?: 78.0
        val effectiveHeight = heightCm ?: 180.0
        val effectiveAge = ageYears ?: 25

        val bmr = calculateBmr(effectiveWeight, effectiveHeight, effectiveAge, sex)
        // In Activity-Synced mode, base TDEE uses Sedentary (1.20) as baseline anchor so workouts aren't double-counted
        val effectivePAL = if (engineMode == NutritionEngineMode.ACTIVITY_SYNCED && allocationStrategy == ActivityAllocationStrategy.REALTIME_DAILY_BURN) {
            ActivityLevel.SEDENTARY
        } else {
            activityLevel
        }
        val tdee = calculateTdee(bmr, effectivePAL)

        val paceConfig = GoalPaceConfig(
            goalType = goalType,
            weeklyRatePercent = weeklyRatePercent,
            targetWeightKg = targetWeightKg,
            targetDateTimestamp = targetDateTimestamp
        )

        val baseCalories = if (goalType == NutritionGoalType.CUSTOM && customCalories != null) {
            customCalories.roundToInt()
        } else {
            calculateTargetCalories(tdee, effectiveWeight, paceConfig)
        }

        val baseMacros = partitionMacros(
            targetCalories = baseCalories,
            userWeightKg = effectiveWeight,
            preset = macroPreset,
            goalType = goalType,
            customProteinG = customProteinG,
            customCarbsG = customCarbsG,
            customFatG = customFatG,
            customFiberG = customFiberG
        )

        val initialTargets = NutritionTargets(
            caloriesKcal = baseCalories,
            proteinG = baseMacros.proteinG,
            carbsG = baseMacros.carbsG,
            fatG = baseMacros.fatG,
            fiberG = baseMacros.fiberG,
            bmrKcal = bmr.roundToInt(),
            tdeeKcal = tdee.roundToInt(),
            baseCaloriesKcal = baseCalories,
            activeCaloriesKcal = 0,
            engineMode = engineMode,
            allocationStrategy = allocationStrategy,
            activeMacroSplit = activeMacroSplit,
            macroPreset = macroPreset,
            goalType = goalType,
            activityLevel = activityLevel,
            sex = sex,
            isCustom = (macroPreset == MacroPreset.CUSTOM || goalType == NutritionGoalType.CUSTOM)
        )

        // Apply active calories if in Activity-Synced Real-Time mode
        return if (engineMode == NutritionEngineMode.ACTIVITY_SYNCED &&
            allocationStrategy == ActivityAllocationStrategy.REALTIME_DAILY_BURN &&
            activeCaloriesBurnedToday > 0f
        ) {
            applyActiveCalories(initialTargets, activeCaloriesBurnedToday, activeMacroSplit)
        } else {
            initialTargets
        }
    }
}

data class QuadrupleMacros(
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val fiberG: Float
)
