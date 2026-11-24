package com.example.ui.screens

import android.app.TimePickerDialog
import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.remote.Habit
import com.example.data.remote.HabitCategory
import com.example.data.remote.StreakCalculator
import com.example.data.remote.UiState
import com.example.ui.components.EmptySectionCard
import com.example.ui.components.formatTime
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StreakFlame
import com.example.ui.theme.SuccessEmerald

@Composable
fun HabitsScreen(
    habitsState: UiState<List<Habit>>,
    onToggleHabitToday: (Habit) -> Unit,
    onAddHabit: (String, HabitCategory, Int, Int, Int, Boolean) -> Unit,
    onUpdateHabit: (String, String, HabitCategory, Int, Int, Int, Boolean) -> Unit,
    onDeleteHabit: (String) -> Unit,
    onSeedStarterHabits: () -> Unit
) {
    val todayIso = remember { StreakCalculator.todayIso() }
    val recentDays = remember { StreakCalculator.recentDaysIso(7) }
    var showDialog by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }

    val orientation = LocalConfiguration.current.orientation

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideOrLandscape = maxWidth >= 600.dp || orientation == Configuration.ORIENTATION_LANDSCAPE
        val gridColumns = if (isWideOrLandscape) 2 else 1

        when (habitsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    EmptySectionCard(
                        title = "Lỗi đồng bộ thói quen",
                        subtitle = habitsState.message
                    )
                }
            }
            is UiState.Success -> {
                val habits = habitsState.data
                val doneToday = habits.count { it.isCompletedOn(todayIso) }
                val totalCurrentStreak = habits.sumOf { it.currentStreak }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Realtime Streak Header Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Chuỗi thói quen Thời gian thực",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Hôm nay: $doneToday/${habits.size} hoàn thành",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(StreakFlame.copy(alpha = 0.18f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocalFireDepartment,
                                    contentDescription = "Tổng chuỗi Streak",
                                    tint = StreakFlame,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$totalCurrentStreak",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = StreakFlame,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (habits.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.TopCenter) {
                            EmptySectionCard(
                                title = "Bắt đầu xây dựng thói quen tốt",
                                subtitle = "Thêm thói quen hằng ngày như Đọc sách, Tập thể dục, Học Tiếng Anh để tính Streak tự động.",
                                actionLabel = "Tạo 3 thói quen mẫu",
                                onAction = onSeedStarterHabits
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(gridColumns),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(habits, key = { it.id }) { habit ->
                                HabitCard(
                                    habit = habit,
                                    todayIso = todayIso,
                                    recentDays = recentDays,
                                    onToggleToday = { onToggleHabitToday(habit) },
                                    onEdit = {
                                        editingHabit = habit
                                        showDialog = true
                                    },
                                    onDelete = { onDeleteHabit(habit.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingHabit = null
                showDialog = true
            },
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("habit_add_fab")
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Thêm thói quen")
        }
    }

    if (showDialog) {
        HabitEditorDialog(
            initial = editingHabit,
            onDismiss = { showDialog = false },
            onSave = { title, cat, mins, hour, min, enabled ->
                if (editingHabit == null) {
                    onAddHabit(title, cat, mins, hour, min, enabled)
                } else {
                    onUpdateHabit(editingHabit!!.id, title, cat, mins, hour, min, enabled)
                }
                showDialog = false
            }
        )
    }
}

@Composable
private fun HabitCard(
    habit: Habit,
    todayIso: String,
    recentDays: List<Pair<String, String>>,
    onToggleToday: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isDoneToday = habit.isCompletedOn(todayIso)
    val checkColor by animateColorAsState(
        targetValue = if (isDoneToday) SuccessEmerald else MaterialTheme.colorScheme.surfaceVariant,
        label = "habitCheckColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${habit.categoryEnum().label} • ${habit.targetMinutes}p",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (habit.reminderEnabled) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                        contentDescription = null,
                        tint = if (habit.reminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (habit.reminderEnabled) formatTime(habit.reminderHour.toInt(), habit.reminderMinute.toInt()) else "Tắt nhắc",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Sửa thói quen", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Xóa thói quen", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = null,
                            tint = StreakFlame,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Chuỗi: ${habit.currentStreak} ngày • Kỷ lục: ${habit.bestStreak}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(checkColor)
                        .clickable(onClick = onToggleToday)
                        .testTag("habit_toggle_${habit.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = if (isDoneToday) "Hủy điểm danh hôm nay" else "Điểm danh hôm nay",
                        tint = if (isDoneToday) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 7-day Streak matrix
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                recentDays.forEach { (iso, dayLabel) ->
                    val completed = habit.isCompletedOn(iso)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    if (completed) SuccessEmerald
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (completed) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (iso == todayIso) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitEditorDialog(
    initial: Habit?,
    onDismiss: () -> Unit,
    onSave: (String, HabitCategory, Int, Int, Int, Boolean) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var category by remember { mutableStateOf(initial?.categoryEnum() ?: HabitCategory.STUDY) }
    var minutesText by remember { mutableStateOf((initial?.targetMinutes ?: 25L).toString()) }
    var reminderHour by remember { mutableIntStateOf(initial?.reminderHour?.toInt() ?: 20) }
    var reminderMinute by remember { mutableIntStateOf(initial?.reminderMinute?.toInt() ?: 0) }
    var reminderEnabled by remember { mutableStateOf(initial?.reminderEnabled ?: true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Thêm thói quen mới" else "Sửa thói quen") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tên thói quen *") },
                    placeholder = { Text("VD: Đọc sách 20 phút") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Nhóm thói quen", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(HabitCategory.entries) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { minutesText = it.filter(Char::isDigit) },
                    label = { Text("Mục tiêu mỗi ngày (phút)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Nhắc nhở hằng ngày", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                }

                if (reminderEnabled) {
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, h, m -> reminderHour = h; reminderMinute = m },
                                reminderHour,
                                reminderMinute,
                                true
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Giờ nhắc mỗi ngày: ${formatTime(reminderHour, reminderMinute)}")
                    }
                }

                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = title.trim()
                    if (clean.isEmpty()) {
                        errorMsg = "Vui lòng nhập tên thói quen."
                        return@Button
                    }
                    val mins = minutesText.toIntOrNull()?.coerceIn(1, 600) ?: 25
                    onSave(clean, category, mins, reminderHour, reminderMinute, reminderEnabled)
                }
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
