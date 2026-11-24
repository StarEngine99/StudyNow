package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.CourseScheduleEntity
import com.example.data.local.ExamScheduleEntity
import com.example.data.remote.Habit
import java.util.Calendar
import java.util.Locale

class StudyReminderScheduler(private val context: Context) {

    private val alarmManager: AlarmManager?
        get() = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        val manager = alarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun scheduleCourseReminder(course: CourseScheduleEntity) {
        val requestCode = courseRequestCode(course.id)
        if (!course.reminderEnabled || course.id <= 0L) {
            cancelCourseReminder(course.id)
            return
        }
        val triggerAt = computeNextWeeklyTriggerMillis(
            isoDayOfWeek = course.dayOfWeek,
            hour = course.startHour,
            minute = course.startMinute,
            minutesBefore = course.reminderMinutesBefore
        )
        val timeText = String.format(Locale.getDefault(), "%02d:%02d", course.startHour, course.startMinute)
        val title = "Sắp vào lớp: ${course.subjectName}"
        val body = "Bắt đầu lúc $timeText tại phòng ${course.room} (nhắc trước ${course.reminderMinutesBefore} phút)."

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(ReminderBroadcastReceiver.EXTRA_TYPE, ReminderBroadcastReceiver.TYPE_COURSE)
            putExtra(ReminderBroadcastReceiver.EXTRA_NOTIFICATION_ID, requestCode)
            putExtra(ReminderBroadcastReceiver.EXTRA_TITLE, title)
            putExtra(ReminderBroadcastReceiver.EXTRA_BODY, body)
            putExtra(ReminderBroadcastReceiver.EXTRA_DAY_OF_WEEK, course.dayOfWeek)
            putExtra(ReminderBroadcastReceiver.EXTRA_HOUR, course.startHour)
            putExtra(ReminderBroadcastReceiver.EXTRA_MINUTE, course.startMinute)
            putExtra(ReminderBroadcastReceiver.EXTRA_MINUTES_BEFORE, course.reminderMinutesBefore)
        }
        scheduleAlarm(requestCode, triggerAt, intent)
    }

    fun cancelCourseReminder(courseId: Long) {
        cancelAlarm(courseRequestCode(courseId))
    }

    fun scheduleExamReminder(exam: ExamScheduleEntity) {
        val requestCode = examRequestCode(exam.id)
        if (!exam.reminderEnabled || exam.id <= 0L) {
            cancelExamReminder(exam.id)
            return
        }
        val triggerAt = exam.examTimeMillis - (exam.reminderMinutesBefore * 60_000L)
        if (triggerAt <= System.currentTimeMillis() + 3_000L) {
            cancelExamReminder(exam.id)
            return
        }
        val title = "Nhắc lịch thi: ${exam.subjectName}"
        val seatInfo = if (exam.seatNumber.isNotBlank()) " • SBD/Ghế: ${exam.seatNumber}" else ""
        val body = "Thi ${exam.examType} tại phòng ${exam.examRoom}$seatInfo. Chúc bạn làm bài thật tốt!"

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(ReminderBroadcastReceiver.EXTRA_TYPE, ReminderBroadcastReceiver.TYPE_EXAM)
            putExtra(ReminderBroadcastReceiver.EXTRA_NOTIFICATION_ID, requestCode)
            putExtra(ReminderBroadcastReceiver.EXTRA_TITLE, title)
            putExtra(ReminderBroadcastReceiver.EXTRA_BODY, body)
        }
        scheduleAlarm(requestCode, triggerAt, intent)
    }

    fun cancelExamReminder(examId: Long) {
        cancelAlarm(examRequestCode(examId))
    }

    fun scheduleHabitReminder(habit: Habit) {
        if (habit.id.isBlank()) return
        val requestCode = habitRequestCode(habit.id)
        if (!habit.reminderEnabled) {
            cancelHabitReminder(habit.id)
            return
        }
        val hour = habit.reminderHour.toInt().coerceIn(0, 23)
        val minute = habit.reminderMinute.toInt().coerceIn(0, 59)
        val triggerAt = computeNextDailyTriggerMillis(hour, minute)
        val title = "Giờ thực hiện thói quen: ${habit.title}"
        val body = "Chuỗi hiện tại ${habit.currentStreak} ngày! Hoàn thành ${habit.targetMinutes} phút hôm nay để giữ vững phong độ."

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(ReminderBroadcastReceiver.EXTRA_TYPE, ReminderBroadcastReceiver.TYPE_HABIT)
            putExtra(ReminderBroadcastReceiver.EXTRA_NOTIFICATION_ID, requestCode)
            putExtra(ReminderBroadcastReceiver.EXTRA_TITLE, title)
            putExtra(ReminderBroadcastReceiver.EXTRA_BODY, body)
            putExtra(ReminderBroadcastReceiver.EXTRA_HOUR, hour)
            putExtra(ReminderBroadcastReceiver.EXTRA_MINUTE, minute)
        }
        scheduleAlarm(requestCode, triggerAt, intent)
    }

    fun cancelHabitReminder(habitId: String) {
        if (habitId.isBlank()) return
        cancelAlarm(habitRequestCode(habitId))
    }

    fun schedulePomodoroAlarm(endTimeMillis: Long, subjectLabel: String?, phaseTitle: String) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(ReminderBroadcastReceiver.EXTRA_TYPE, ReminderBroadcastReceiver.TYPE_POMODORO)
            putExtra(ReminderBroadcastReceiver.EXTRA_NOTIFICATION_ID, POMODORO_REQUEST_CODE)
            putExtra(ReminderBroadcastReceiver.EXTRA_TITLE, "Hoàn thành phiên $phaseTitle!")
            val subjectPart = if (!subjectLabel.isNullOrBlank()) " Môn: $subjectLabel." else ""
            putExtra(ReminderBroadcastReceiver.EXTRA_BODY, "Đã hết thời gian đếm ngược.$subjectPart Hãy nghỉ ngơi hoặc chuyển phiên tiếp theo.")
        }
        scheduleAlarm(POMODORO_REQUEST_CODE, endTimeMillis, intent)
    }

    fun cancelPomodoroAlarm() {
        cancelAlarm(POMODORO_REQUEST_CODE)
    }

    fun scheduleTestNotificationIn5Seconds() {
        val triggerAt = System.currentTimeMillis() + 5_000L
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(ReminderBroadcastReceiver.EXTRA_TYPE, ReminderBroadcastReceiver.TYPE_HABIT)
            putExtra(ReminderBroadcastReceiver.EXTRA_NOTIFICATION_ID, TEST_REQUEST_CODE)
            putExtra(ReminderBroadcastReceiver.EXTRA_TITLE, "StudyNow • Thông báo hoạt động!")
            putExtra(ReminderBroadcastReceiver.EXTRA_BODY, "Hệ thống nhắc lịch học, lịch thi và chuỗi thói quen đã sẵn sàng đúng giờ.")
            putExtra(ReminderBroadcastReceiver.EXTRA_SKIP_RESCHEDULE, true)
        }
        scheduleAlarm(TEST_REQUEST_CODE, triggerAt, intent)
    }

    fun rescheduleNextWeeklyCourse(
        requestCode: Int,
        title: String,
        body: String,
        isoDayOfWeek: Int,
        hour: Int,
        minute: Int,
        minutesBefore: Int
    ) {
        val nextTrigger = computeNextWeeklyTriggerMillis(isoDayOfWeek, hour, minute, minutesBefore, minOffsetMillis = 60_000L)
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(ReminderBroadcastReceiver.EXTRA_TYPE, ReminderBroadcastReceiver.TYPE_COURSE)
            putExtra(ReminderBroadcastReceiver.EXTRA_NOTIFICATION_ID, requestCode)
            putExtra(ReminderBroadcastReceiver.EXTRA_TITLE, title)
            putExtra(ReminderBroadcastReceiver.EXTRA_BODY, body)
            putExtra(ReminderBroadcastReceiver.EXTRA_DAY_OF_WEEK, isoDayOfWeek)
            putExtra(ReminderBroadcastReceiver.EXTRA_HOUR, hour)
            putExtra(ReminderBroadcastReceiver.EXTRA_MINUTE, minute)
            putExtra(ReminderBroadcastReceiver.EXTRA_MINUTES_BEFORE, minutesBefore)
        }
        scheduleAlarm(requestCode, nextTrigger, intent)
    }

    fun rescheduleNextDailyHabit(
        requestCode: Int,
        title: String,
        body: String,
        hour: Int,
        minute: Int
    ) {
        val nextTrigger = computeNextDailyTriggerMillis(hour, minute, minOffsetMillis = 60_000L)
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(ReminderBroadcastReceiver.EXTRA_TYPE, ReminderBroadcastReceiver.TYPE_HABIT)
            putExtra(ReminderBroadcastReceiver.EXTRA_NOTIFICATION_ID, requestCode)
            putExtra(ReminderBroadcastReceiver.EXTRA_TITLE, title)
            putExtra(ReminderBroadcastReceiver.EXTRA_BODY, body)
            putExtra(ReminderBroadcastReceiver.EXTRA_HOUR, hour)
            putExtra(ReminderBroadcastReceiver.EXTRA_MINUTE, minute)
        }
        scheduleAlarm(requestCode, nextTrigger, intent)
    }

    private fun scheduleAlarm(requestCode: Int, triggerAtMillis: Long, intent: Intent) {
        val manager = alarmManager ?: return
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (canScheduleExactAlarms()) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun cancelAlarm(requestCode: Int) {
        val manager = alarmManager ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            manager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    companion object {
        private const val COURSE_BASE = 100_000
        private const val EXAM_BASE = 200_000
        private const val HABIT_BASE = 300_000
        private const val POMODORO_REQUEST_CODE = 400_001
        private const val TEST_REQUEST_CODE = 400_999

        fun courseRequestCode(courseId: Long): Int = COURSE_BASE + (courseId % 50_000).toInt()
        fun examRequestCode(examId: Long): Int = EXAM_BASE + (examId % 50_000).toInt()
        fun habitRequestCode(habitId: String): Int = HABIT_BASE + (habitId.hashCode() and 0x7FFF)

        fun computeNextWeeklyTriggerMillis(
            isoDayOfWeek: Int,
            hour: Int,
            minute: Int,
            minutesBefore: Int,
            minOffsetMillis: Long = 3_000L
        ): Long {
            val calendarDay = when (isoDayOfWeek) {
                1 -> Calendar.MONDAY
                2 -> Calendar.TUESDAY
                3 -> Calendar.WEDNESDAY
                4 -> Calendar.THURSDAY
                5 -> Calendar.FRIDAY
                6 -> Calendar.SATURDAY
                else -> Calendar.SUNDAY
            }
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, calendarDay)
                set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
                set(Calendar.MINUTE, minute.coerceIn(0, 59))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MINUTE, -minutesBefore)
            }
            while (cal.timeInMillis <= now + minOffsetMillis) {
                cal.add(Calendar.WEEK_OF_YEAR, 1)
            }
            return cal.timeInMillis
        }

        fun computeNextDailyTriggerMillis(
            hour: Int,
            minute: Int,
            minOffsetMillis: Long = 3_000L
        ): Long {
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
                set(Calendar.MINUTE, minute.coerceIn(0, 59))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            while (cal.timeInMillis <= now + minOffsetMillis) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return cal.timeInMillis
        }
    }
}
