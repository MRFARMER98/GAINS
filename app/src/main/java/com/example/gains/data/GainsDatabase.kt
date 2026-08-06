package com.example.gains.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Exercise::class, WorkoutSession::class, LoggedSet::class], version = 3, exportSchema = false)
abstract class GainsDatabase : RoomDatabase() {
    abstract fun gainsDao(): GainsDao

    companion object {
        @Volatile
        private var INSTANCE: GainsDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): GainsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GainsDatabase::class.java,
                    "gains_database"
                )
                .addCallback(GainsDatabaseCallback())
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
            db.execSQL("INSERT INTO exercises (name, muscleGroup) VALUES ('Bench Press', 'Chest')")
            db.execSQL("INSERT INTO exercises (name, muscleGroup) VALUES ('Squat', 'Legs')")
            db.execSQL("INSERT INTO exercises (name, muscleGroup) VALUES ('Deadlift', 'Back')")
            db.execSQL("INSERT INTO exercises (name, muscleGroup) VALUES ('Overhead Press', 'Shoulders')")
            db.execSQL("INSERT INTO exercises (name, muscleGroup) VALUES ('Bicep Curl', 'Arms')")
            db.execSQL("INSERT INTO exercises (name, muscleGroup) VALUES ('Lat Pulldown', 'Back')")
            db.execSQL("INSERT INTO exercises (name, muscleGroup) VALUES ('Tricep Pushdown', 'Arms')")
            db.execSQL("INSERT INTO exercises (name, muscleGroup) VALUES ('Leg Press', 'Legs')")
            db.execSQL("INSERT INTO exercises (name, muscleGroup) VALUES ('Dumbbell Lateral Raise', 'Shoulders')")
            db.execSQL("INSERT INTO exercises (name, muscleGroup) VALUES ('Incline Dumbbell Press', 'Chest')")
        }
    }
}
