package com.example.data.remote

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class HabitCategory(val label: String) {
    STUDY("Học tập"),
    HEALTH("Sức khỏe"),
    LANGUAGE("Ngoại ngữ"),
    MINDSET("Tư duy");

    companion object {
        fun fromRaw(raw: String?): HabitCategory =
            entries.firstOrNull { it.name == raw } ?: STUDY
    }
}

enum class AvatarPreset(val label: String) {
    INDIGO_OWL("Cú Tri Thức"),
    MINT_LEAF("Lá Xanh"),
    AMBER_FLAME("Ngọn Lửa"),
    SKY_COMPASS("La Bàn");

    companion object {
        fun fromRaw(raw: String?): AvatarPreset =
            entries.firstOrNull { it.name == raw } ?: INDIGO_OWL
    }
}

@Keep
@IgnoreExtraProperties
data class StudentProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val schoolName: String = "",
    val className: String = "",
    val studentCode: String = "",
    val avatarPreset: String = AvatarPreset.INDIGO_OWL.name,
    val photoUrl: String? = null,
    val pomodoroMinutesTotal: Long = 0L,
    val pomodoroSessionsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Habit(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = HabitCategory.STUDY.name,
    val targetMinutes: Long = 25L,
    val completedDates: List<String> = emptyList(),
    val currentStreak: Long = 0L,
    val bestStreak: Long = 0L,
    val reminderHour: Long = 20L,
    val reminderMinute: Long = 0L,
    val reminderEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun categoryEnum(): HabitCategory = HabitCategory.fromRaw(category)

    fun isCompletedOn(isoDate: String): Boolean = completedDates.contains(isoDate)
}

object StreakCalculator {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun todayIso(): String = synchronized(dateFormat) {
        dateFormat.format(Calendar.getInstance().time)
    }

    fun recentDaysIso(days: Int = 7): List<Pair<String, String>> {
        val dayLabels = arrayOf("CN", "T2", "T3", "T4", "T5", "T6", "T7")
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -(days - 1))
        val result = mutableListOf<Pair<String, String>>()
        synchronized(dateFormat) {
            for (i in 0 until days) {
                val iso = dateFormat.format(cal.time)
                val label = dayLabels[cal.get(Calendar.DAY_OF_WEEK) - 1]
                result.add(iso to label)
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return result
    }

    fun calculateStreaks(
        completedDates: List<String>,
        previousBest: Long = 0L
    ): Pair<Long, Long> {
        val uniqueSorted = completedDates.distinct().sorted().takeLast(365)
        if (uniqueSorted.isEmpty()) return 0L to previousBest

        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayIso = synchronized(dateFormat) { dateFormat.format(todayCal.time) }
        val yesterdayCal = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayIso = synchronized(dateFormat) { dateFormat.format(yesterdayCal.time) }

        val dateSet = uniqueSorted.toHashSet()
        var currentStreak = 0L
        val cursor = when {
            dateSet.contains(todayIso) -> todayCal.clone() as Calendar
            dateSet.contains(yesterdayIso) -> yesterdayCal.clone() as Calendar
            else -> null
        }

        if (cursor != null) {
            synchronized(dateFormat) {
                while (dateSet.contains(dateFormat.format(cursor.time))) {
                    currentStreak++
                    cursor.add(Calendar.DAY_OF_YEAR, -1)
                }
            }
        }

        var maxRun = 0L
        var run = 0L
        var prevEpochDay: Long? = null
        synchronized(dateFormat) {
            for (iso in uniqueSorted) {
                val parsed = runCatching { dateFormat.parse(iso) }.getOrNull() ?: continue
                val c = Calendar.getInstance().apply {
                    time = parsed
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val epochDay = c.timeInMillis / (24L * 60L * 60L * 1000L)
                run = if (prevEpochDay != null && epochDay - prevEpochDay == 1L) run + 1L else 1L
                if (run > maxRun) maxRun = run
                prevEpochDay = epochDay
            }
        }
        val bestStreak = maxOf(previousBest, currentStreak, maxRun)
        return currentStreak to bestStreak
    }
}
