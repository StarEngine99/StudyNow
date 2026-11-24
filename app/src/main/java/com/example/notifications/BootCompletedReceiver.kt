package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.StudyNowDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = StudyNowDatabase.getInstance(context).academicDao()
                val scheduler = StudyReminderScheduler(context)
                dao.getAllReminderCourses().forEach { scheduler.scheduleCourseReminder(it) }
                dao.getAllReminderExams().forEach { scheduler.scheduleExamReminder(it) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
