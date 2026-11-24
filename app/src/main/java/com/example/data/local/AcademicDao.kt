package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AcademicDao {

    @Query("SELECT * FROM course_schedules WHERE userId = :userId ORDER BY dayOfWeek ASC, startHour ASC, startMinute ASC")
    fun observeCourses(userId: String): Flow<List<CourseScheduleEntity>>

    @Query("SELECT * FROM course_schedules WHERE reminderEnabled = 1")
    suspend fun getAllReminderCourses(): List<CourseScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseScheduleEntity): Long

    @Update
    suspend fun updateCourse(course: CourseScheduleEntity)

    @Query("DELETE FROM course_schedules WHERE id = :id")
    suspend fun deleteCourseById(id: Long)

    @Query("SELECT * FROM exam_schedules WHERE userId = :userId ORDER BY examTimeMillis ASC")
    fun observeExams(userId: String): Flow<List<ExamScheduleEntity>>

    @Query("SELECT * FROM exam_schedules WHERE reminderEnabled = 1")
    suspend fun getAllReminderExams(): List<ExamScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamScheduleEntity): Long

    @Update
    suspend fun updateExam(exam: ExamScheduleEntity)

    @Query("DELETE FROM exam_schedules WHERE id = :id")
    suspend fun deleteExamById(id: Long)
}
