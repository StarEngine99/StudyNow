package com.example.data.local

import com.example.notifications.StudyReminderScheduler
import kotlinx.coroutines.flow.Flow

class AcademicRepository(
    private val dao: AcademicDao,
    private val scheduler: StudyReminderScheduler
) {
    fun observeCourses(userId: String): Flow<List<CourseScheduleEntity>> = dao.observeCourses(userId)

    fun observeExams(userId: String): Flow<List<ExamScheduleEntity>> = dao.observeExams(userId)

    suspend fun saveCourse(course: CourseScheduleEntity): Long {
        return if (course.id == 0L) {
            val newId = dao.insertCourse(course.copy(updatedAtMillis = System.currentTimeMillis()))
            val saved = course.copy(id = newId)
            scheduler.scheduleCourseReminder(saved)
            newId
        } else {
            val updated = course.copy(updatedAtMillis = System.currentTimeMillis())
            dao.updateCourse(updated)
            scheduler.scheduleCourseReminder(updated)
            updated.id
        }
    }

    suspend fun deleteCourse(courseId: Long) {
        scheduler.cancelCourseReminder(courseId)
        dao.deleteCourseById(courseId)
    }

    suspend fun saveExam(exam: ExamScheduleEntity): Long {
        return if (exam.id == 0L) {
            val newId = dao.insertExam(exam.copy(updatedAtMillis = System.currentTimeMillis()))
            val saved = exam.copy(id = newId)
            scheduler.scheduleExamReminder(saved)
            newId
        } else {
            val updated = exam.copy(updatedAtMillis = System.currentTimeMillis())
            dao.updateExam(updated)
            scheduler.scheduleExamReminder(updated)
            updated.id
        }
    }

    suspend fun deleteExam(examId: Long) {
        scheduler.cancelExamReminder(examId)
        dao.deleteExamById(examId)
    }

    suspend fun seedSampleAcademicData(userId: String) {
        val now = System.currentTimeMillis()
        val dayMillis = 24L * 60L * 60L * 1000L
        val courses = listOf(
            CourseScheduleEntity(
                userId = userId,
                subjectName = "Lập trình Di động",
                subjectCode = "COMP301",
                room = "B1-304",
                lecturer = "TS. Nguyễn Văn Minh",
                dayOfWeek = 1,
                startHour = 7,
                startMinute = 30,
                endHour = 10,
                endMinute = 0,
                credits = 3,
                midtermScore = 8.5,
                finalScore = 9.0,
                colorIndex = 0,
                reminderEnabled = true,
                reminderMinutesBefore = 15
            ),
            CourseScheduleEntity(
                userId = userId,
                subjectName = "Cơ sở dữ liệu phân tán",
                subjectCode = "COMP305",
                room = "A2-201",
                lecturer = "ThS. Trần Thị Lan",
                dayOfWeek = 3,
                startHour = 13,
                startMinute = 30,
                endHour = 16,
                endMinute = 0,
                credits = 3,
                midtermScore = 8.0,
                finalScore = null,
                colorIndex = 1,
                reminderEnabled = true,
                reminderMinutesBefore = 15
            ),
            CourseScheduleEntity(
                userId = userId,
                subjectName = "Tiếng Anh học thuật",
                subjectCode = "ENG204",
                room = "C1-105",
                lecturer = "Cô Phạm Hoàng Yến",
                dayOfWeek = 5,
                startHour = 9,
                startMinute = 0,
                endHour = 11,
                endMinute = 15,
                credits = 2,
                midtermScore = 9.0,
                finalScore = 8.5,
                colorIndex = 2,
                reminderEnabled = true,
                reminderMinutesBefore = 30
            )
        )
        courses.forEach { saveCourse(it) }

        val exams = listOf(
            ExamScheduleEntity(
                userId = userId,
                subjectName = "Lập trình Di động",
                examRoom = "Hội trường H2",
                seatNumber = "B14",
                examType = "Cuối kỳ",
                examTimeMillis = now + 5L * dayMillis,
                durationMinutes = 90,
                notes = "Mang thẻ sinh viên và máy tính xách tay",
                reminderEnabled = true,
                reminderMinutesBefore = 60
            ),
            ExamScheduleEntity(
                userId = userId,
                subjectName = "Cơ sở dữ liệu phân tán",
                examRoom = "Phòng máy PM02",
                seatNumber = "08",
                examType = "Giữa kỳ",
                examTimeMillis = now + 2L * dayMillis,
                durationMinutes = 60,
                notes = "Ôn tập chuẩn hóa BCNF và Transaction",
                reminderEnabled = true,
                reminderMinutesBefore = 60
            )
        )
        exams.forEach { saveExam(it) }
    }
}
