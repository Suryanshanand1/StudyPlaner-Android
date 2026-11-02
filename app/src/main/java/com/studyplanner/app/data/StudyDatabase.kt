package com.studyplanner.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SubjectEntity::class,
        ChapterEntity::class,
        StudyPlanEntity::class,
        FocusSessionEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class StudyDatabase : RoomDatabase() {
    abstract fun dao(): StudyDao

    companion object {
        @Volatile
        private var instance: StudyDatabase? = null

        fun get(context: Context): StudyDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StudyDatabase::class.java,
                    "study-planner.db",
                ).fallbackToDestructiveMigration(true).build().also { instance = it }
            }
        }
    }
}
