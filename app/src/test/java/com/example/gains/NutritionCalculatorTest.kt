package com.example.gains

import com.example.gains.domain.*
import org.junit.Assert.*
import org.junit.Test

class NutritionCalculatorTest {

    @Test
    fun testMifflinStJeorBmrCalculation() {
        // Male: 80kg, 180cm, 25 years -> 10(80) + 6.25(180) - 5(25) + 5 = 1805 kcal
        val bmrMale = NutritionCalculator.calculateBmr(80.0, 180.0, 25, BiologicalSex.MALE)
        assertEquals(1805f, bmrMale, 0.1f)

        // Female: 65kg, 168cm, 28 years -> 10(65) + 6.25(168) - 5(28) - 161 = 1399 kcal
        val bmrFemale = NutritionCalculator.calculateBmr(65.0, 168.0, 28, BiologicalSex.FEMALE)
        assertEquals(1399f, bmrFemale, 0.1f)
    }

    @Test
    fun testGymSessionMetBurnCalculation() {
        // 60-minute session for an 80kg individual at MET 5.0
        // (5.0 * 3.5 * 80 / 200) * 60 = 7.0 * 60 = 420 kcal
        val burn = NutritionCalculator.calculateGymSessionBurnKcal(60, 80.0)
        assertEquals(420f, burn, 0.1f)
    }

    @Test
    fun testWeeklyRatePaceCalculation() {
        val baseTdee = 2800f
        val weightKg = 80.0
        // 0.5% bodyweight loss per week = 0.4 kg/week
        // 0.4 * 7700 / 7 = 440 kcal deficit -> target: 2800 - 440 = 2360 kcal
        val paceCut = GoalPaceConfig(
            goalType = NutritionGoalType.CUT_MODERATE,
            weeklyRatePercent = -0.5f
        )
        val targetCalories = NutritionCalculator.calculateTargetCalories(baseTdee, weightKg, paceCut)
        assertEquals(2360, targetCalories)
    }

    @Test
    fun testTargetDatePaceCalculation() {
        val baseTdee = 2800f
        val currentWeight = 85.0
        val targetWeight = 82.0 // Lose 3kg
        // 30 days in the future
        val targetDateMs = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000L)
        val pace = GoalPaceConfig(
            goalType = NutritionGoalType.CUT_MODERATE,
            targetWeightKg = targetWeight,
            targetDateTimestamp = targetDateMs
        )
        val targetCalories = NutritionCalculator.calculateTargetCalories(baseTdee, currentWeight, pace)
        // 3kg * 7700 = 23,100 kcal / 30 days = 770 kcal daily deficit -> 2800 - 770 = 2030 kcal
        // Clamped by max 25% deficit = 2100 kcal
        assertEquals(2100, targetCalories)
    }

    @Test
    fun testMacroPresets() {
        val weight = 80.0
        val calories = 2600

        // Balanced Sports (2.0g/kg protein, 25% fat)
        val balanced = NutritionCalculator.partitionMacros(calories, weight, MacroPreset.BALANCED_SPORTS, NutritionGoalType.MAINTENANCE)
        assertEquals(160f, balanced.proteinG, 0.1f) // 80 * 2.0 = 160g
        assertEquals(72.2f, balanced.fatG, 0.5f)    // 2600 * 0.25 / 9 = 72.2g
        assertTrue(balanced.carbsG > 300f)

        // High Carb Performance (1.8g/kg protein, 18% fat)
        val highCarb = NutritionCalculator.partitionMacros(calories, weight, MacroPreset.HIGH_CARB_PERFORMANCE, NutritionGoalType.MAINTENANCE)
        assertEquals(144f, highCarb.proteinG, 0.1f) // 80 * 1.8 = 144g
        assertEquals(52.0f, highCarb.fatG, 0.5f)    // 2600 * 0.18 / 9 = 52g
        assertTrue(highCarb.carbsG > balanced.carbsG)

        // Low Carb (2.2g/kg protein, 45% fat)
        val lowCarb = NutritionCalculator.partitionMacros(calories, weight, MacroPreset.LOW_CARB, NutritionGoalType.MAINTENANCE)
        assertEquals(176f, lowCarb.proteinG, 0.1f)
        assertEquals(130.0f, lowCarb.fatG, 0.5f)
        assertTrue(lowCarb.carbsG < 200f)
    }

    @Test
    fun testActiveWorkoutCaloriesAllocationCarbsPriority() {
        val base = NutritionCalculator.calculateTargets(
            weightKg = 80.0,
            heightCm = 180.0,
            ageYears = 25,
            engineMode = NutritionEngineMode.ACTIVITY_SYNCED,
            allocationStrategy = ActivityAllocationStrategy.REALTIME_DAILY_BURN,
            activeMacroSplit = ActiveCalorieMacroSplit.CARBS_PRIORITY,
            activeCaloriesBurnedToday = 400f
        )

        // 400 kcal burned -> Carbs Priority allocates 400 / 4 = 100g extra carbs
        // Base carbs without active calories:
        val baseNoActive = NutritionCalculator.calculateTargets(
            weightKg = 80.0,
            heightCm = 180.0,
            ageYears = 25,
            engineMode = NutritionEngineMode.ACTIVITY_SYNCED,
            allocationStrategy = ActivityAllocationStrategy.REALTIME_DAILY_BURN,
            activeMacroSplit = ActiveCalorieMacroSplit.CARBS_PRIORITY,
            activeCaloriesBurnedToday = 0f
        )

        assertEquals(baseNoActive.baseCaloriesKcal + 400, base.caloriesKcal)
        assertEquals(baseNoActive.proteinG, base.proteinG, 0.1f) // Protein unchanged
        assertEquals(baseNoActive.fatG, base.fatG, 0.1f)         // Fat unchanged
        assertEquals(baseNoActive.carbsG + 100f, base.carbsG, 0.1f) // Extra 100g carbs
    }

    @Test
    fun testActiveWorkoutCaloriesAllocationProportional() {
        val base = NutritionCalculator.calculateTargets(
            weightKg = 80.0,
            heightCm = 180.0,
            ageYears = 25,
            engineMode = NutritionEngineMode.ACTIVITY_SYNCED,
            allocationStrategy = ActivityAllocationStrategy.REALTIME_DAILY_BURN,
            activeMacroSplit = ActiveCalorieMacroSplit.PROPORTIONAL,
            activeCaloriesBurnedToday = 400f
        )

        val baseNoActive = NutritionCalculator.calculateTargets(
            weightKg = 80.0,
            heightCm = 180.0,
            ageYears = 25,
            engineMode = NutritionEngineMode.ACTIVITY_SYNCED,
            allocationStrategy = ActivityAllocationStrategy.REALTIME_DAILY_BURN,
            activeMacroSplit = ActiveCalorieMacroSplit.PROPORTIONAL,
            activeCaloriesBurnedToday = 0f
        )

        assertEquals(baseNoActive.baseCaloriesKcal + 400, base.caloriesKcal)
        assertTrue(base.proteinG > baseNoActive.proteinG) // Protein increased proportionally
        assertTrue(base.fatG > baseNoActive.fatG)         // Fat increased proportionally
        assertTrue(base.carbsG > baseNoActive.carbsG)     // Carbs increased proportionally
    }

    @Test
    fun testDataGatedWeeklyCalibration() {
        // Less than 3 weigh-ins -> Must return null (prevent annoying prompts)
        val tooFewWeighIns = NutritionCalculator.evaluateWeeklyCalibration(
            currentTdeeKcal = 2600,
            observedSevenDayWeightDeltaKg = -0.1,
            targetWeeklyWeightDeltaKg = -0.5,
            weighInCount = 2,
            loggedDaysCount = 6
        )
        assertNull(tooFewWeighIns)

        // Less than 4 logged days -> Must return null
        val tooFewLogs = NutritionCalculator.evaluateWeeklyCalibration(
            currentTdeeKcal = 2600,
            observedSevenDayWeightDeltaKg = -0.1,
            targetWeeklyWeightDeltaKg = -0.5,
            weighInCount = 4,
            loggedDaysCount = 3
        )
        assertNull(tooFewLogs)

        // Valid data: 4 weigh-ins, 6 logged days, losing too slowly (-0.1 kg vs goal -0.5 kg)
        // Rate mismatch = +0.4 kg -> suggests calorie cut (clamped to -150 max)
        val validCheckIn = NutritionCalculator.evaluateWeeklyCalibration(
            currentTdeeKcal = 2600,
            observedSevenDayWeightDeltaKg = -0.1,
            targetWeeklyWeightDeltaKg = -0.5,
            weighInCount = 4,
            loggedDaysCount = 6
        )
        assertNotNull(validCheckIn)
        assertTrue(validCheckIn!!.recommendedCalorieAdjustment <= -50)
        assertTrue(validCheckIn.recommendedCalorieAdjustment >= -150)
    }
}
