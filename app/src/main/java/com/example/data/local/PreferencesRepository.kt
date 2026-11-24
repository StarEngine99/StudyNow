package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studyNowDataStore: DataStore<Preferences> by preferencesDataStore(name = "studynow_prefs")

class PreferencesRepository(private val context: Context) {

    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val pomodoroMinutesKey = intPreferencesKey("pomodoro_focus_minutes")
    private val pomodoroSoundKey = booleanPreferencesKey("pomodoro_sound_enabled")

    val themeMode: Flow<ThemeMode> = context.studyNowDataStore.data.map { prefs ->
        ThemeMode.fromRaw(prefs[themeModeKey])
    }

    val pomodoroFocusMinutes: Flow<Int> = context.studyNowDataStore.data.map { prefs ->
        prefs[pomodoroMinutesKey] ?: 25
    }

    val pomodoroSoundEnabled: Flow<Boolean> = context.studyNowDataStore.data.map { prefs ->
        prefs[pomodoroSoundKey] ?: true
    }

    fun observeLocalAvatarPath(userId: String): Flow<String?> {
        val key = stringPreferencesKey("local_avatar_$userId")
        return context.studyNowDataStore.data.map { it[key] }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.studyNowDataStore.edit { it[themeModeKey] = mode.name }
    }

    suspend fun setPomodoroFocusMinutes(minutes: Int) {
        context.studyNowDataStore.edit { it[pomodoroMinutesKey] = minutes.coerceIn(5, 90) }
    }

    suspend fun setPomodoroSoundEnabled(enabled: Boolean) {
        context.studyNowDataStore.edit { it[pomodoroSoundKey] = enabled }
    }

    suspend fun setLocalAvatarPath(userId: String, path: String?) {
        val key = stringPreferencesKey("local_avatar_$userId")
        context.studyNowDataStore.edit { prefs ->
            if (path.isNullOrBlank()) prefs.remove(key) else prefs[key] = path
        }
    }
}
