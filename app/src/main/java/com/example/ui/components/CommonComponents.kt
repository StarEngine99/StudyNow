package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.remote.AvatarPreset
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.MintSecondary
import com.example.ui.theme.StreakFlame
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun currentIsoDayOfWeek(): Int {
    val calDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    return when (calDay) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        else -> 7
    }
}

fun dayOfWeekLabel(isoDay: Int, short: Boolean = false): String {
    return if (short) {
        when (isoDay) {
            1 -> "T2"
            2 -> "T3"
            3 -> "T4"
            4 -> "T5"
            5 -> "T6"
            6 -> "T7"
            else -> "CN"
        }
    } else {
        when (isoDay) {
            1 -> "Thứ 2"
            2 -> "Thứ 3"
            3 -> "Thứ 4"
            4 -> "Thứ 5"
            5 -> "Thứ 6"
            6 -> "Thứ 7"
            else -> "Chủ nhật"
        }
    }
}

fun formatTime(hour: Int, minute: Int): String =
    String.format(Locale.getDefault(), "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))

fun formatExamDateTime(millis: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}

fun formatExamDateOnly(millis: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date(millis))
}

fun daysUntilMillis(targetMillis: Long): Long {
    val todayCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val targetCal = Calendar.getInstance().apply {
        timeInMillis = targetMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return (targetCal.timeInMillis - todayCal.timeInMillis) / (24L * 60L * 60L * 1000L)
}

@Composable
fun StudentAvatar(
    photoUrl: String?,
    localAvatarPath: String?,
    preset: AvatarPreset,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val hasLocalFile = !localAvatarPath.isNullOrBlank() && File(localAvatarPath).exists()
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(1.5.dp, borderColor, CircleShape)
            .background(presetColor(preset).copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        when {
            hasLocalFile -> {
                AsyncImage(
                    model = File(localAvatarPath!!),
                    contentDescription = "Ảnh đại diện sinh viên",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            !photoUrl.isNullOrBlank() -> {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "Ảnh đại diện Google",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Icon(
                    imageVector = presetIcon(preset),
                    contentDescription = "Ảnh đại diện ${preset.label}",
                    tint = presetColor(preset),
                    modifier = Modifier.size(size * 0.56f)
                )
            }
        }
    }
}

fun presetColor(preset: AvatarPreset): Color = when (preset) {
    AvatarPreset.INDIGO_OWL -> IndigoPrimary
    AvatarPreset.MINT_LEAF -> MintSecondary
    AvatarPreset.AMBER_FLAME -> StreakFlame
    AvatarPreset.SKY_COMPASS -> Color(0xFF0284C7)
}

fun presetIcon(preset: AvatarPreset) = when (preset) {
    AvatarPreset.INDIGO_OWL -> Icons.Filled.School
    AvatarPreset.MINT_LEAF -> Icons.Filled.Eco
    AvatarPreset.AMBER_FLAME -> Icons.Filled.LocalFireDepartment
    AvatarPreset.SKY_COMPASS -> Icons.Filled.Explore
}

@Composable
fun NotificationPermissionBanner(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.NotificationsOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Bật thông báo nhắc lịch",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "Cho phép StudyNow gửi nhắc nhở giờ học, lịch thi và Pomodoro.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRequestPermission,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Bật ngay", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun EmptySectionCard(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}
