package com.Margin.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.Margin.app.data.local.dao.*
import com.Margin.app.data.local.entity.*

@Database(
    entities = [
        SessionEntity::class,
        SubjectEntity::class,
        AttendanceRecordEntity::class,
        TaskEntity::class,
        TimetableEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun subjectDao(): SubjectDao
    abstract fun attendanceRecordDao(): AttendanceRecordDao
    abstract fun taskDao(): TaskDao
    abstract fun timetableDao(): TimetableDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "margin.db"
                ).build()
                .also { INSTANCE = it }
            }
        }
    }
}
