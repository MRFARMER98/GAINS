package com.example.gains.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Exercise::class, WorkoutSession::class, LoggedSet::class, WorkoutLabel::class, 
        UserProfile::class, PlannedSession::class, WorkoutTemplate::class, TemplateSet::class, 
        MetricDefinition::class, MetricEntry::class, ExternalActivity::class,
        FoodItem::class, BonusDeal::class, FoodNutrient::class, FoodServing::class, LoggedFoodEntry::class, FoodRecipe::class, FoodRecipeIngredient::class
    ], 
    version = 22, 
    exportSchema = false
)
abstract class GainsDatabase : RoomDatabase() {
    abstract fun gainsDao(): GainsDao

    companion object {
        @Volatile
        private var INSTANCE: GainsDatabase? = null

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try { db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `nutritionEngineMode` TEXT NOT NULL DEFAULT 'CLASSIC_FORMULA'") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `activityAllocationStrategy` TEXT NOT NULL DEFAULT 'REALTIME_DAILY_BURN'") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `activeCalorieMacroSplit` TEXT NOT NULL DEFAULT 'CARBS_PRIORITY'") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `macroPreset` TEXT NOT NULL DEFAULT 'BALANCED_SPORTS'") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `weeklyRatePercent` REAL DEFAULT NULL") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `targetGoalDateTimestamp` INTEGER DEFAULT NULL") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `lastCalibrationDismissedTimestamp` INTEGER DEFAULT NULL") } catch (e: Exception) {}
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try { db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `biologicalSex` TEXT NOT NULL DEFAULT 'MALE'") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `activityLevel` TEXT NOT NULL DEFAULT 'MODERATE'") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `nutritionGoalType` TEXT NOT NULL DEFAULT 'MAINTENANCE'") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `customCaloriesTarget` REAL DEFAULT NULL") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `customProteinTargetG` REAL DEFAULT NULL") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `customCarbsTargetG` REAL DEFAULT NULL") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `customFatTargetG` REAL DEFAULT NULL") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `customFiberTargetG` REAL DEFAULT NULL") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `nutritionEngineMode` TEXT NOT NULL DEFAULT 'CLASSIC_FORMULA'") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `activityAllocationStrategy` TEXT NOT NULL DEFAULT 'REALTIME_DAILY_BURN'") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `activeCalorieMacroSplit` TEXT NOT NULL DEFAULT 'CARBS_PRIORITY'") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `macroPreset` TEXT NOT NULL DEFAULT 'BALANCED_SPORTS'") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `weeklyRatePercent` REAL DEFAULT NULL") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `targetGoalDateTimestamp` INTEGER DEFAULT NULL") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `lastCalibrationDismissedTimestamp` INTEGER DEFAULT NULL") } catch (e: Exception) {}
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try { db.execSQL("ALTER TABLE `logged_food_entries` ADD COLUMN `imageUrl` TEXT DEFAULT NULL") } catch (e: Exception) {}
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try { db.execSQL("ALTER TABLE `logged_food_entries` ADD COLUMN `recipeLogGroupId` TEXT DEFAULT NULL") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE `logged_food_entries` ADD COLUMN `recipeName` TEXT DEFAULT NULL") } catch (e: Exception) {}
                
                try { db.execSQL("ALTER TABLE `food_recipes` ADD COLUMN `prepTimeMinutes` INTEGER DEFAULT NULL") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE `food_recipes` ADD COLUMN `cookTimeMinutes` INTEGER DEFAULT NULL") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE `food_recipes` ADD COLUMN `category` TEXT DEFAULT NULL") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE `food_recipes` ADD COLUMN `instructions` TEXT DEFAULT NULL") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE `food_recipes` ADD COLUMN `imageUrl` TEXT DEFAULT NULL") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE `food_recipes` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
                
                try { db.execSQL("ALTER TABLE `food_recipe_ingredients` ADD COLUMN `servingDescription` TEXT DEFAULT NULL") } catch (e: Exception) {}
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try { db.execSQL("ALTER TABLE `food_items` DROP COLUMN `isBonus`") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE `food_items` DROP COLUMN `bonusPriceEur`") } catch (e: Exception) {}

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `bonus_deals` (
                        `foodId` TEXT NOT NULL, 
                        `bonusPriceEur` REAL, 
                        `bonusType` TEXT, 
                        `validFrom` INTEGER NOT NULL, 
                        `validUntil` INTEGER NOT NULL, 
                        PRIMARY KEY(`foodId`), 
                        FOREIGN KEY(`foodId`) REFERENCES `food_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `food_items` ADD COLUMN `priceEur` REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE `food_items` ADD COLUMN `unitPriceEur` REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE `food_items` ADD COLUMN `unitPriceDescription` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `food_items` ADD COLUMN `isBonus` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `food_items` ADD COLUMN `bonusPriceEur` REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE `food_items` ADD COLUMN `ingredientsText` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `food_items` ADD COLUMN `allergensText` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `food_items` ADD COLUMN `isVegetarian` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `food_items` ADD COLUMN `isVegan` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `food_items` ADD COLUMN `packageWeightGrams` REAL DEFAULT NULL")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `food_items` (
                        `id` TEXT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `brand` TEXT, 
                        `category` TEXT, 
                        `barcode` TEXT, 
                        `source` TEXT NOT NULL DEFAULT 'AH', 
                        `isVerified` INTEGER NOT NULL DEFAULT 1, 
                        `perUnit` TEXT NOT NULL DEFAULT '100g', 
                        `nutriScore` TEXT, 
                        `imageUrl` TEXT, 
                        `updatedAt` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_items_barcode` ON `food_items` (`barcode`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_items_name` ON `food_items` (`name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_items_brand` ON `food_items` (`brand`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `food_nutrients` (
                        `foodId` TEXT NOT NULL, 
                        `caloriesKcal` REAL NOT NULL, 
                        `proteinG` REAL NOT NULL, 
                        `carbsG` REAL NOT NULL, 
                        `sugarsG` REAL NOT NULL, 
                        `fatG` REAL NOT NULL, 
                        `saturatedFatG` REAL NOT NULL, 
                        `unsaturatedFatG` REAL, 
                        `fiberG` REAL NOT NULL DEFAULT 0, 
                        `saltG` REAL NOT NULL DEFAULT 0, 
                        `sodiumMg` REAL, 
                        `vitaminAUg` REAL, `vitaminB1Mg` REAL, `vitaminB2Mg` REAL, `vitaminB6Mg` REAL, `vitaminB12Ug` REAL, `vitaminCMg` REAL, `vitaminDUg` REAL, `vitaminEMg` REAL, `folicAcidUg` REAL, 
                        `calciumMg` REAL, `ironMg` REAL, `magnesiumMg` REAL, `potassiumMg` REAL, `zincMg` REAL, `phosphorusMg` REAL, 
                        PRIMARY KEY(`foodId`), 
                        FOREIGN KEY(`foodId`) REFERENCES `food_items`(`id`) ON DELETE CASCADE
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `food_servings` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `foodId` TEXT NOT NULL, 
                        `description` TEXT NOT NULL, 
                        `gramWeight` REAL NOT NULL, 
                        `isDefault` INTEGER NOT NULL DEFAULT 0, 
                        FOREIGN KEY(`foodId`) REFERENCES `food_items`(`id`) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_servings_foodId` ON `food_servings` (`foodId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `logged_food_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `dateTimestamp` INTEGER NOT NULL, 
                        `timestamp` INTEGER NOT NULL, 
                        `mealType` TEXT NOT NULL, 
                        `foodId` TEXT NOT NULL, 
                        `foodName` TEXT NOT NULL, 
                        `brandName` TEXT, 
                        `servingDescription` TEXT, 
                        `servingQuantity` REAL NOT NULL DEFAULT 1.0, 
                        `gramWeightTotal` REAL NOT NULL DEFAULT 100.0, 
                        `caloriesKcal` REAL NOT NULL DEFAULT 0, 
                        `proteinG` REAL NOT NULL DEFAULT 0, 
                        `carbsG` REAL NOT NULL DEFAULT 0, 
                        `fatG` REAL NOT NULL DEFAULT 0, 
                        `fiberG` REAL NOT NULL DEFAULT 0, 
                        `saltG` REAL NOT NULL DEFAULT 0, 
                        `saturatedFatG` REAL NOT NULL DEFAULT 0, 
                        `sugarsG` REAL NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_logged_food_entries_dateTimestamp_mealType` ON `logged_food_entries` (`dateTimestamp`, `mealType`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_logged_food_entries_dateTimestamp` ON `logged_food_entries` (`dateTimestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_logged_food_entries_foodId` ON `logged_food_entries` (`foodId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `food_recipes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `servingsCount` INTEGER NOT NULL DEFAULT 1, 
                        `notes` TEXT, 
                        `createdAt` INTEGER NOT NULL
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `food_recipe_ingredients` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `recipeId` INTEGER NOT NULL, 
                        `foodId` TEXT NOT NULL, 
                        `quantityGrams` REAL NOT NULL, 
                        FOREIGN KEY(`recipeId`) REFERENCES `food_recipes`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`foodId`) REFERENCES `food_items`(`id`) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_recipe_ingredients_recipeId` ON `food_recipe_ingredients` (`recipeId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_recipe_ingredients_foodId` ON `food_recipe_ingredients` (`foodId`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create workout_labels table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `workout_labels` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `colorHex` TEXT NOT NULL
                    )
                """)
                // Add labelId column to workout_sessions
                db.execSQL("ALTER TABLE `workout_sessions` ADD COLUMN `labelId` INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create user_profile table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `user_profile` (
                        `id` INTEGER NOT NULL, 
                        `name` TEXT NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """)
                // Seed default profile so existing users aren't left with an empty profile
                db.execSQL("INSERT OR IGNORE INTO user_profile (id, name) VALUES (1, 'Wouter')")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `photoUri` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `height` REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `age` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `currentWeight` REAL DEFAULT NULL")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Delete seeded exercises only if they haven't been used in any logged sets
                db.execSQL("""
                    DELETE FROM exercises 
                    WHERE name IN ('Bench Press', 'Squat', 'Deadlift', 'Overhead Press', 'Bicep Curl', 'Lat Pulldown', 'Tricep Pushdown', 'Leg Press', 'Dumbbell Lateral Raise', 'Incline Dumbbell Press')
                      AND id NOT IN (SELECT DISTINCT exerciseId FROM logged_sets)
                """)
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `exercises` ADD COLUMN `notes` TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `planned_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `dateTimestamp` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `workoutType` TEXT NOT NULL,
                        `labelId` INTEGER DEFAULT NULL,
                        `isCompleted` INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `planned_sessions` ADD COLUMN `templateId` INTEGER DEFAULT NULL")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `workout_templates` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `workoutType` TEXT NOT NULL DEFAULT 'GYM',
                        `labelId` INTEGER DEFAULT NULL,
                        `notes` TEXT DEFAULT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `template_sets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `templateId` INTEGER NOT NULL,
                        `exerciseId` INTEGER NOT NULL,
                        `setNumber` INTEGER NOT NULL,
                        `targetWeight` REAL NOT NULL,
                        `targetReps` INTEGER NOT NULL,
                        FOREIGN KEY(`templateId`) REFERENCES `workout_templates`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_template_sets_templateId` ON `template_sets` (`templateId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_template_sets_exerciseId` ON `template_sets` (`exerciseId`)")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `metric_definitions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `unit` TEXT NOT NULL,
                        `isSystem` INTEGER NOT NULL,
                        `displayOrder` INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `metric_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `metricId` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `value` REAL NOT NULL,
                        FOREIGN KEY(`metricId`) REFERENCES `metric_definitions`(`id`) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_metric_entries_metricId` ON `metric_entries` (`metricId`)")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `metric_definitions` ADD COLUMN `targetValue` REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE `metric_definitions` ADD COLUMN `targetDate` INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `external_activities` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `externalId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `activityType` TEXT NOT NULL,
                        `startTime` INTEGER NOT NULL,
                        `endTime` INTEGER NOT NULL,
                        `durationSeconds` INTEGER NOT NULL,
                        `distanceMeters` REAL,
                        `caloriesKcal` REAL,
                        `sourceApp` TEXT
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_external_activities_externalId` ON `external_activities` (`externalId`)")
                db.execSQL("ALTER TABLE `metric_definitions` ADD COLUMN `source` TEXT NOT NULL DEFAULT 'MANUAL'")
                db.execSQL("ALTER TABLE `metric_entries` ADD COLUMN `externalId` TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `birthDateTimestamp` INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `external_activities` ADD COLUMN `routeJson` TEXT DEFAULT NULL")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try { db.execSQL("ALTER TABLE `workout_sessions` ADD COLUMN `workoutType` TEXT NOT NULL DEFAULT 'GYM'") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE `workout_sessions` ADD COLUMN `endTime` INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try { db.execSQL("ALTER TABLE `logged_sets` ADD COLUMN `isCompleted` INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): GainsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GainsDatabase::class.java,
                    "gains_database"
                )
                .createFromAsset("databases/gains_database.db")
                .addCallback(GainsDatabaseCallback(context.applicationContext))
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }



    private class GainsDatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS `room_table_modification_log` (`table_id` INTEGER PRIMARY KEY, `invalidated` INTEGER NOT NULL)")
                
                val tempFile = java.io.File(context.cacheDir, "temp_sync_assets.db")
                context.assets.open("databases/gains_database.db").use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (tempFile.exists() && tempFile.length() > 0) {
                    db.execSQL("ATTACH DATABASE '${tempFile.absolutePath}' AS asset_db")
                    
                    // Sync full food catalog metadata (names, brands, categories, images, prices, nutrients) from asset_db
                    db.execSQL("""
                        UPDATE food_items 
                        SET 
                            name = (SELECT a.name FROM asset_db.food_items a WHERE a.id = food_items.id),
                            brand = (SELECT a.brand FROM asset_db.food_items a WHERE a.id = food_items.id),
                            category = (SELECT a.category FROM asset_db.food_items a WHERE a.id = food_items.id),
                            barcode = (SELECT a.barcode FROM asset_db.food_items a WHERE a.id = food_items.id),
                            source = (SELECT a.source FROM asset_db.food_items a WHERE a.id = food_items.id),
                            perUnit = (SELECT a.perUnit FROM asset_db.food_items a WHERE a.id = food_items.id),
                            nutriScore = (SELECT a.nutriScore FROM asset_db.food_items a WHERE a.id = food_items.id),
                            imageUrl = (SELECT a.imageUrl FROM asset_db.food_items a WHERE a.id = food_items.id),
                            priceEur = (SELECT a.priceEur FROM asset_db.food_items a WHERE a.id = food_items.id),
                            unitPriceEur = (SELECT a.unitPriceEur FROM asset_db.food_items a WHERE a.id = food_items.id),
                            unitPriceDescription = (SELECT a.unitPriceDescription FROM asset_db.food_items a WHERE a.id = food_items.id),
                            ingredientsText = (SELECT a.ingredientsText FROM asset_db.food_items a WHERE a.id = food_items.id),
                            allergensText = (SELECT a.allergensText FROM asset_db.food_items a WHERE a.id = food_items.id),
                            isVegetarian = (SELECT a.isVegetarian FROM asset_db.food_items a WHERE a.id = food_items.id),
                            isVegan = (SELECT a.isVegan FROM asset_db.food_items a WHERE a.id = food_items.id),
                            isVerified = (SELECT a.isVerified FROM asset_db.food_items a WHERE a.id = food_items.id),
                            packageWeightGrams = (SELECT a.packageWeightGrams FROM asset_db.food_items a WHERE a.id = food_items.id),
                            updatedAt = (SELECT a.updatedAt FROM asset_db.food_items a WHERE a.id = food_items.id)
                        WHERE EXISTS (SELECT 1 FROM asset_db.food_items a WHERE a.id = food_items.id)
                    """.trimIndent())
                    
                    // Insert any newly added food items/nutrients/servings/recipes/bonus deals
                    db.execSQL("INSERT OR IGNORE INTO food_items SELECT * FROM asset_db.food_items")
                    db.execSQL("INSERT OR REPLACE INTO food_nutrients SELECT * FROM asset_db.food_nutrients")
                    db.execSQL("INSERT OR REPLACE INTO food_servings SELECT * FROM asset_db.food_servings")
                    db.execSQL("INSERT OR REPLACE INTO bonus_deals SELECT * FROM asset_db.bonus_deals")
                    db.execSQL("INSERT OR IGNORE INTO food_recipes SELECT * FROM asset_db.food_recipes")
                    db.execSQL("INSERT OR IGNORE INTO food_recipe_ingredients SELECT * FROM asset_db.food_recipe_ingredients")
                    
                    db.execSQL("DETACH DATABASE asset_db")
                    tempFile.delete()
                }
            } catch (e: Exception) {
                android.util.Log.e("GainsDatabase", "Error syncing catalog from asset db: ${e.message}", e)
            }
        }
    }
}
