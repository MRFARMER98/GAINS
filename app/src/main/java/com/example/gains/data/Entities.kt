package com.example.gains.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val muscleGroup: String,
    val notes: String? = null
)

@Entity(tableName = "workout_labels")
data class WorkoutLabel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val colorHex: String
)

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val name: String,
    val workoutType: String = "GYM",
    val endTime: Long = 0L,
    val labelId: Int? = null
)

@Entity(
    tableName = "logged_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["exerciseId"])
    ]
)
data class LoggedSet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Long,
    val exerciseId: Int,
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val isCompleted: Boolean = false
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1, // Enforce single active profile
    val name: String = "Wouter",
    val photoUri: String? = null,
    val height: Double? = null,
    val age: Int? = null,
    val birthDateTimestamp: Long? = null,
    val currentWeight: Double? = null,
    val biologicalSex: String = "MALE",
    val activityLevel: String = "MODERATE",
    val nutritionGoalType: String = "MAINTENANCE",
    val customCaloriesTarget: Float? = null,
    val customProteinTargetG: Float? = null,
    val customCarbsTargetG: Float? = null,
    val customFatTargetG: Float? = null,
    val customFiberTargetG: Float? = null,
    val nutritionEngineMode: String = "CLASSIC_FORMULA",
    val activityAllocationStrategy: String = "REALTIME_DAILY_BURN",
    val activeCalorieMacroSplit: String = "CARBS_PRIORITY",
    val macroPreset: String = "BALANCED_SPORTS",
    val weeklyRatePercent: Float? = null,
    val targetGoalDateTimestamp: Long? = null,
    val lastCalibrationDismissedTimestamp: Long? = null
) {
    val calculatedAge: Int?
        get() {
            val dob = birthDateTimestamp ?: return age
            if (dob <= 0L) return age
            val dobCal = java.util.Calendar.getInstance().apply { timeInMillis = dob }
            val nowCal = java.util.Calendar.getInstance()
            var years = nowCal.get(java.util.Calendar.YEAR) - dobCal.get(java.util.Calendar.YEAR)
            if (nowCal.get(java.util.Calendar.DAY_OF_YEAR) < dobCal.get(java.util.Calendar.DAY_OF_YEAR)) {
                years--
            }
            return years.coerceAtLeast(0)
        }
}

@Entity(tableName = "planned_sessions")
data class PlannedSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateTimestamp: Long,
    val name: String,
    val workoutType: String = "GYM",
    val labelId: Int? = null,
    val isCompleted: Boolean = false,
    val templateId: Long? = null
)

@Entity(tableName = "workout_templates")
data class WorkoutTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val workoutType: String = "GYM",
    val labelId: Int? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "template_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplate::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["templateId"]),
        Index(value = ["exerciseId"])
    ]
)
data class TemplateSet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val templateId: Long,
    val exerciseId: Int,
    val setNumber: Int,
    val targetWeight: Double,
    val targetReps: Int
)

@Entity(tableName = "metric_definitions")
data class MetricDefinition(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val unit: String,
    val isSystem: Boolean,
    val displayOrder: Int,
    val targetValue: Float? = null,
    val targetDate: Long? = null,
    val source: String = "MANUAL"
)

@Entity(
    tableName = "metric_entries",
    foreignKeys = [
        ForeignKey(
            entity = MetricDefinition::class,
            parentColumns = ["id"],
            childColumns = ["metricId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["metricId"])
    ]
)
data class MetricEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val metricId: Long,
    val timestamp: Long,
    val value: Float,
    val externalId: String? = null
)

@Entity(
    tableName = "external_activities",
    indices = [
        Index(value = ["externalId"], unique = true)
    ]
)
data class ExternalActivity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val externalId: String,
    val title: String,
    val activityType: String,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val distanceMeters: Double? = null,
    val caloriesKcal: Double? = null,
    val sourceApp: String? = null,
    val routeJson: String? = null
)

@Entity(
    tableName = "food_items",
    indices = [
        Index(value = ["barcode"]),
        Index(value = ["name"]),
        Index(value = ["brand"])
    ]
)
data class FoodItem(
    @PrimaryKey val id: String,
    val name: String,
    val brand: String? = null,
    val category: String? = null,
    val barcode: String? = null,
    val source: String = "AH",
    val isVerified: Boolean = true,
    val perUnit: String = "100g",
    val nutriScore: String? = null,
    val imageUrl: String? = null,
    val priceEur: Float? = null,
    val unitPriceEur: Float? = null,
    val unitPriceDescription: String? = null,
    val ingredientsText: String? = null,
    val allergensText: String? = null,
    val isVegetarian: Boolean? = null,
    val isVegan: Boolean? = null,
    val packageWeightGrams: Float? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bonus_deals",
    foreignKeys = [
        ForeignKey(
            entity = FoodItem::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BonusDeal(
    @PrimaryKey val foodId: String,
    val bonusPriceEur: Float? = null,
    val bonusType: String? = null,
    val validFrom: Long = 0,
    val validUntil: Long = 0
)

data class FoodWithBonus(
    @androidx.room.Embedded val foodItem: FoodItem,
    @androidx.room.Relation(
        parentColumn = "id",
        entityColumn = "foodId"
    )
    val bonusDeal: BonusDeal? = null
)

@Entity(
    tableName = "food_nutrients",
    foreignKeys = [
        ForeignKey(
            entity = FoodItem::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FoodNutrient(
    @PrimaryKey val foodId: String,
    val caloriesKcal: Float,
    val proteinG: Float,
    val carbsG: Float,
    val sugarsG: Float,
    val fatG: Float,
    val saturatedFatG: Float,
    val unsaturatedFatG: Float? = null,
    @androidx.room.ColumnInfo(defaultValue = "0") val fiberG: Float = 0f,
    @androidx.room.ColumnInfo(defaultValue = "0") val saltG: Float = 0f,
    val sodiumMg: Float? = null,
    val vitaminAUg: Float? = null,
    val vitaminB1Mg: Float? = null,
    val vitaminB2Mg: Float? = null,
    val vitaminB6Mg: Float? = null,
    val vitaminB12Ug: Float? = null,
    val vitaminCMg: Float? = null,
    val vitaminDUg: Float? = null,
    val vitaminEMg: Float? = null,
    val folicAcidUg: Float? = null,
    val calciumMg: Float? = null,
    val ironMg: Float? = null,
    val magnesiumMg: Float? = null,
    val potassiumMg: Float? = null,
    val zincMg: Float? = null,
    val phosphorusMg: Float? = null
)

@Entity(
    tableName = "food_servings",
    foreignKeys = [
        ForeignKey(
            entity = FoodItem::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["foodId"])]
)
data class FoodServing(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodId: String,
    val description: String,
    val gramWeight: Float,
    val isDefault: Boolean = false
)

@Entity(
    tableName = "logged_food_entries",
    indices = [
        Index(value = ["dateTimestamp", "mealType"]),
        Index(value = ["dateTimestamp"]),
        Index(value = ["foodId"])
    ]
)
data class LoggedFoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateTimestamp: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val mealType: String, // BREAKFAST, LUNCH, DINNER, SNACK, PRE_WORKOUT, POST_WORKOUT
    val foodId: String,
    val foodName: String,
    val brandName: String? = null,
    val servingDescription: String? = null,
    val servingQuantity: Float = 1.0f,
    val gramWeightTotal: Float = 100.0f,
    val caloriesKcal: Float = 0f,
    val proteinG: Float = 0f,
    val carbsG: Float = 0f,
    val fatG: Float = 0f,
    val fiberG: Float = 0f,
    val saltG: Float = 0f,
    val saturatedFatG: Float = 0f,
    val sugarsG: Float = 0f,
    val recipeLogGroupId: String? = null,
    val recipeName: String? = null,
    val imageUrl: String? = null
)

@Entity(tableName = "food_recipes")
data class FoodRecipe(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val servingsCount: Int = 1,
    val prepTimeMinutes: Int? = null,
    val cookTimeMinutes: Int? = null,
    val category: String? = null,
    val instructions: String? = null,
    val imageUrl: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "food_recipe_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = FoodRecipe::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FoodItem::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["recipeId"]),
        Index(value = ["foodId"])
    ]
)
data class FoodRecipeIngredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val foodId: String,
    val quantityGrams: Float,
    val servingDescription: String? = null
)

data class RecipeIngredientWithFood(
    val ingredientId: Long,
    val recipeId: Long,
    val foodId: String,
    val foodName: String,
    val brandName: String?,
    val quantityGrams: Float,
    val servingDescription: String?,
    val caloriesPer100g: Float,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val fiberPer100g: Float,
    val saltPer100g: Float,
    val saturatedFatPer100g: Float,
    val sugarsPer100g: Float
)

data class FoodRecipeWithDetails(
    val recipe: FoodRecipe,
    val ingredients: List<RecipeIngredientWithFood> = emptyList()
) {
    val totalTimeMinutes: Int
        get() = (recipe.prepTimeMinutes ?: 0) + (recipe.cookTimeMinutes ?: 0)

    val totalCaloriesKcal: Float
        get() = ingredients.sumOf { (it.caloriesPer100g * (it.quantityGrams / 100.0)).toDouble() }.toFloat()

    val totalProteinG: Float
        get() = ingredients.sumOf { (it.proteinPer100g * (it.quantityGrams / 100.0)).toDouble() }.toFloat()

    val totalCarbsG: Float
        get() = ingredients.sumOf { (it.carbsPer100g * (it.quantityGrams / 100.0)).toDouble() }.toFloat()

    val totalFatG: Float
        get() = ingredients.sumOf { (it.fatPer100g * (it.quantityGrams / 100.0)).toDouble() }.toFloat()

    val caloriesPerServing: Float
        get() = if (recipe.servingsCount > 0) totalCaloriesKcal / recipe.servingsCount else totalCaloriesKcal

    val proteinPerServing: Float
        get() = if (recipe.servingsCount > 0) totalProteinG / recipe.servingsCount else totalProteinG

    val carbsPerServing: Float
        get() = if (recipe.servingsCount > 0) totalCarbsG / recipe.servingsCount else totalCarbsG

    val fatPerServing: Float
        get() = if (recipe.servingsCount > 0) totalFatG / recipe.servingsCount else totalFatG
}


