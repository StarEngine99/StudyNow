package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "course_schedules")
data class CourseScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userId: String,
    val subjectName: String,
    val subjectCode: String = "",
    val room: String,
    val lecturer: String,
    val dayOfWeek: Int, // 1 = Monday .. 7 = Sunday
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val credits: Int = 3,
    val midtermScore: Double? = null,
    val finalScore: Double? = null,
    val colorIndex: Int = 0,
    val reminderEnabled: Boolean = true,
    val reminderMinutesBefore: Int = 15,
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val averageScore: Double?
        get() = when {
            midtermScore != null && finalScore != null -> (midtermScore * 0.4) + (finalScore * 0.6)
            finalScore != null -> finalScore
            midtermScore != null -> midtermScore
            else -> null
        }
}

@Entity(tableName = "exam_schedules")
data class ExamScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userId: String,
    val subjectName: String,
    val examRoom: String,
    val seatNumber: String = "",
    val examType: String = "Cuối kỳ",
    val examTimeMillis: Long,
    val durationMinutes: Int = 90,
    val notes: String = "",
    val reminderEnabled: Boolean = true,
    val reminderMinutesBefore: Int = 60,
    val updatedAtMillis: Long = System.currentTimeMillis()
)
