package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
    const val CHANNEL_CLASSES = "studynow_classes"
    const val CHANNEL_EXAMS = "studynow_exams"
    const val CHANNEL_HABITS = "studynow_habits"
    const val CHANNEL_POMODORO = "studynow_pomodoro"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channels = listOf(
            NotificationChannel(
                CHANNEL_CLASSES,
                context.getString(R.string.notification_channel_classes),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Nhắc trước giờ vào lớp theo thời khóa biểu"
                enableVibration(true)
                setVibrationPattern(longArrayOf(0L, 400L, 200L, 400L))
                setSound(alarmSound, audioAttributes)
            },
            NotificationChannel(
                CHANNEL_EXAMS,
                context.getString(R.string.notification_channel_exams),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Nhắc lịch thi và phòng thi quan trọng"
                enableVibration(true)
                setVibrationPattern(longArrayOf(0L, 400L, 200L, 400L))
                setSound(alarmSound, audioAttributes)
            },
            NotificationChannel(
                CHANNEL_HABITS,
                context.getString(R.string.notification_channel_habits),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Nhắc điểm danh chuỗi thói quen hằng ngày"
                enableVibration(true)
                setVibrationPattern(longArrayOf(0L, 400L, 200L, 400L))
                setSound(alarmSound, audioAttributes)
            },
            NotificationChannel(
                CHANNEL_POMODORO,
                context.getString(R.string.notification_channel_pomodoro),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo kết thúc phiên tập trung Pomodoro"
                enableVibration(true)
                setVibrationPattern(longArrayOf(0L, 400L, 200L, 400L))
                setSound(alarmSound, audioAttributes)
            }
        )
        manager.createNotificationChannels(channels)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String
    ) {
        createChannels(context)
        if (!hasNotificationPermission(context)) return

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(alarmUri)
            .setVibrate(longArrayOf(0L, 400L, 200L, 400L))
            .setContentIntent(contentPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            playPomodoroCompletionAlert(context)
        } catch (_: SecurityException) {
            // Ignored gracefully if permission was revoked at runtime
        }
    }

    fun playPomodoroCompletionAlert(context: Context) {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = uri?.let { RingtoneManager.getRingtone(context.applicationContext, it) }
            if (ringtone != null) {
                ringtone.play()
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        if (ringtone.isPlaying) ringtone.stop()
                    } catch (_: Exception) {
                    }
                }, 5000L)
            } else {
                val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
                tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 4000)
                Handler(Looper.getMainLooper()).postDelayed({ tone.release() }, 5000L)
            }
        } catch (_: Exception) {
            // Audio playback fallback ignored on devices without audio sink
        }

        try {
            val pattern = longArrayOf(0L, 500L, 300L, 500L, 300L, 500L, 300L, 500L, 300L, 500L)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(VibratorManager::class.java)
                vm?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(5000L)
                }
            }
        } catch (_: Exception) {
            // Ignore vibration failures
        }
    }
}
