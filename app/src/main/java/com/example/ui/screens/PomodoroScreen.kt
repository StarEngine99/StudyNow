package com.example.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CourseScheduleEntity
import com.example.data.remote.StudentProfile
import com.example.data.remote.UiState
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.viewmodel.PomodoroPhase
import com.example.ui.viewmodel.PomodoroUiState
import java.util.Locale

@Composable
fun PomodoroScreen(
    state: PomodoroUiState,
    courses: List<CourseScheduleEntity>,
    profileState: UiState<StudentProfile?>,
    soundEnabled: Boolean,
    onSelectPhase: (PomodoroPhase) -> Unit,
    onSelectSubject: (String?) -> Unit,
    onToggleRunning: () -> Unit,
    onReset: () -> Unit,
    onToggleSound: (Boolean) -> Unit
) {
    val orientation = LocalConfiguration.current.orientation
    val profile = (profileState as? UiState.Success)?.data
    val totalMinutesAllTime = profile?.pomodoroMinutesTotal ?: 0L
    val totalSessionsAllTime = profile?.pomodoroSessionsCount ?: 0L

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp || orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    PomodoroRing(state = state)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    PomodoroControlsPanel(
                        state = state,
                        courses = courses,
                        soundEnabled = soundEnabled,
                        totalMinutesAllTime = totalMinutesAllTime,
                        totalSessionsAllTime = totalSessionsAllTime,
                        onSelectPhase = onSelectPhase,
                        onSelectSubject = onSelectSubject,
                        onToggleRunning = onToggleRunning,
                        onReset = onReset,
                        onToggleSound = onToggleSound
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PomodoroPhaseChips(state.phase, onSelectPhase)
                PomodoroRing(state = state)
                PomodoroControlsPanel(
                    state = state,
                    courses = courses,
                    soundEnabled = soundEnabled,
                    totalMinutesAllTime = totalMinutesAllTime,
                    totalSessionsAllTime = totalSessionsAllTime,
                    onSelectPhase = onSelectPhase,
                    onSelectSubject = onSelectSubject,
                    onToggleRunning = onToggleRunning,
                    onReset = onReset,
                    onToggleSound = onToggleSound,
                    showPhaseChips = false
                )
            }
        }
    }
}

@Composable
private fun PomodoroPhaseChips(
    activePhase: PomodoroPhase,
    onSelectPhase: (PomodoroPhase) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(PomodoroPhase.entries) { phase ->
            FilterChip(
                selected = activePhase == phase,
                onClick = { onSelectPhase(phase) },
                label = { Text("${phase.label} (${phase.defaultMinutes}p)") }
            )
        }
    }
}

@Composable
private fun PomodoroRing(state: PomodoroUiState) {
    val animatedProgress by animateFloatAsState(
        targetValue = state.progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 450),
        label = "pomodoroRing"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val activeColor = when (state.phase) {
        PomodoroPhase.FOCUS, PomodoroPhase.DEEP_WORK -> MaterialTheme.colorScheme.primary
        PomodoroPhase.SHORT_BREAK, PomodoroPhase.LONG_BREAK -> MaterialTheme.colorScheme.secondary
    }

    val mins = state.remainingSeconds / 60
    val secs = state.remainingSeconds % 60
    val formatted = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

    Box(
        modifier = Modifier.size(250.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 18.dp.toPx()
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = activeColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = activeColor.copy(alpha = 0.14f)
            ) {
                Text(
                    text = state.phase.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = activeColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatted,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.testTag("pomodoro_timer_text")
            )
            if (!state.selectedSubjectName.isNullOrBlank()) {
                Text(
                    text = state.selectedSubjectName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PomodoroControlsPanel(
    state: PomodoroUiState,
    courses: List<CourseScheduleEntity>,
    soundEnabled: Boolean,
    totalMinutesAllTime: Long,
    totalSessionsAllTime: Long,
    onSelectPhase: (PomodoroPhase) -> Unit,
    onSelectSubject: (String?) -> Unit,
    onToggleRunning: () -> Unit,
    onReset: () -> Unit,
    onToggleSound: (Boolean) -> Unit,
    showPhaseChips: Boolean = true
) {
    if (showPhaseChips) {
        PomodoroPhaseChips(state.phase, onSelectPhase)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedIconButton(
            onClick = onReset,
            modifier = Modifier
                .size(54.dp)
                .testTag("pomodoro_reset_button")
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = "Đặt lại đồng hồ")
        }

        Spacer(modifier = Modifier.width(18.dp))

        Button(
            onClick = onToggleRunning,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .height(58.dp)
                .width(180.dp)
                .testTag("pomodoro_start_pause_button")
        ) {
            Icon(
                imageVector = if (state.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (state.isRunning) "Tạm dừng" else "Bắt đầu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (courses.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Gắn môn học đang ôn tập",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = state.selectedSubjectName == null,
                        onClick = { onSelectSubject(null) },
                        label = { Text("Tự học") }
                    )
                }
                items(courses.distinctBy { it.subjectName }) { course ->
                    FilterChip(
                        selected = state.selectedSubjectName == course.subjectName,
                        onClick = { onSelectSubject(course.subjectName) },
                        label = { Text(course.subjectName) }
                    )
                }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (soundEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Âm thanh & rung khi hết giờ", style = MaterialTheme.typography.titleMedium)
                        Text("Thông báo nền tự động hoạt động kể cả khi tắt màn hình", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(checked = soundEnabled, onCheckedChange = onToggleSound)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatPill(label = "Phiên hôm nay", value = "${state.completedSessionsToday}")
                StatPill(label = "Tổng phiên", value = "$totalSessionsAllTime")
                StatPill(label = "Tổng phút học", value = "$totalMinutesAllTime")
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontFamily = JetBrainsMonoFamily,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
