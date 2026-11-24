package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_HABIT
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 1001)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "StudyNow"
        val body = intent.getStringExtra(EXTRA_BODY) ?: "Đến giờ học tập rồi!"
        val skipReschedule = intent.getBooleanExtra(EXTRA_SKIP_RESCHEDULE, false)

        val channelId = when (type) {
            TYPE_COURSE -> NotificationHelper.CHANNEL_CLASSES
            TYPE_EXAM -> NotificationHelper.CHANNEL_EXAMS
            TYPE_POMODORO -> NotificationHelper.CHANNEL_POMODORO
            else -> NotificationHelper.CHANNEL_HABITS
        }

        NotificationHelper.showNotification(
            context = context,
            channelId = channelId,
            notificationId = notificationId,
            title = title,
            body = body
        )

        if (type == TYPE_POMODORO) {
            NotificationHelper.playPomodoroCompletionAlert(context)
        }

        if (!skipReschedule) {
            val scheduler = StudyReminderScheduler(context)
            when (type) {
                TYPE_COURSE -> {
                    val day = intent.getIntExtra(EXTRA_DAY_OF_WEEK, 1)
                    val hour = intent.getIntExtra(EXTRA_HOUR, 7)
                    val minute = intent.getIntExtra(EXTRA_MINUTE, 0)
                    val minutesBefore = intent.getIntExtra(EXTRA_MINUTES_BEFORE, 15)
                    scheduler.rescheduleNextWeeklyCourse(
                        requestCode = notificationId,
                        title = title,
                        body = body,
                        isoDayOfWeek = day,
                        hour = hour,
                        minute = minute,
                        minutesBefore = minutesBefore
                    )
                }
                TYPE_HABIT -> {
                    val hour = intent.getIntExtra(EXTRA_HOUR, 20)
                    val minute = intent.getIntExtra(EXTRA_MINUTE, 0)
                    scheduler.rescheduleNextDailyHabit(
                        requestCode = notificationId,
                        title = title,
                        body = body,
                        hour = hour,
                        minute = minute
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_DAY_OF_WEEK = "extra_day_of_week"
        const val EXTRA_HOUR = "extra_hour"
        const val EXTRA_MINUTE = "extra_minute"
        const val EXTRA_MINUTES_BEFORE = "extra_minutes_before"
        const val EXTRA_SKIP_RESCHEDULE = "extra_skip_reschedule"

        const val TYPE_COURSE = "COURSE"
        const val TYPE_EXAM = "EXAM"
        const val TYPE_HABIT = "HABIT"
        const val TYPE_POMODORO = "POMODORO"
    }
}
