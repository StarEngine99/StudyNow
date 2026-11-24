package com.example

import android.app.Application
import com.example.notifications.NotificationHelper

class StudyNowApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
    }
}
