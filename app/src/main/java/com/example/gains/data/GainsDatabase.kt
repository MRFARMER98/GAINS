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

@Database(entities = [Exercise::class, WorkoutSession::class, LoggedSet::class, WorkoutLabel::class, UserProfile::class, PlannedSession::class], version = 9, exportSchema = false)
abstract class GainsDatabase : RoomDatabase() {
    abstract fun gainsDao(): GainsDao

    companion object {
        @Volatile
        private var INSTANCE: GainsDatabase? = null

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

        fun getDatabase(context: Context, scope: CoroutineScope): GainsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GainsDatabase::class.java,
                    "gains_database"
                )
                .addCallback(GainsDatabaseCallback())
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class GainsDatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Empty callback so new installations rely fully on user syncing exercises
        }
    }
}
