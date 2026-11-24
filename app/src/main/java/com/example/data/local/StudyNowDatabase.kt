package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CourseScheduleEntity::class, ExamScheduleEntity::class],
    version = 1,
    exportSchema = false
)
abstract class StudyNowDatabase : RoomDatabase() {
    abstract fun academicDao(): AcademicDao

    companion object {
        @Volatile
        private var INSTANCE: StudyNowDatabase? = null

        fun getInstance(context: Context): StudyNowDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StudyNowDatabase::class.java,
                    "studynow_academic.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
