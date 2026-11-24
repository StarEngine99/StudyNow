package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontStyle
import com.example.data.remote.QuoteUiModel
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.CourseScheduleEntity
import com.example.data.local.ExamScheduleEntity
import com.example.data.remote.Habit
import com.example.data.remote.StreakCalculator
import com.example.data.remote.StudentProfile
import com.example.data.remote.UiState
import com.example.ui.components.EmptySectionCard
import com.example.ui.components.NotificationPermissionBanner
import com.example.ui.components.currentIsoDayOfWeek
import com.example.ui.components.dayOfWeekLabel
import com.example.ui.components.daysUntilMillis
import com.example.ui.components.formatExamDateTime
import com.example.ui.components.formatTime
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StreakFlame
import com.example.ui.theme.SubjectPalette
import com.example.ui.theme.SuccessEmerald

@Composable
fun DashboardScreen(
    profileState: UiState<StudentProfile?>,
    courses: List<CourseScheduleEntity>,
    exams: List<ExamScheduleEntity>,
    habitsState: UiState<List<Habit>>,
    quoteState: UiState<QuoteUiModel>,
    tipState: UiState<QuoteUiModel>,
    hasNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onToggleHabitToday: (Habit) -> Unit,
    onOpenAcademic: () -> Unit,
    onOpenHabits: () -> Unit,
    onOpenPomodoro: () -> Unit,
    onTestNotification: () -> Unit,
    onSeedSampleAcademic: () -> Unit,
    onSeedStarterHabits: () -> Unit,
    onRefreshQuote: () -> Unit,
    onRefreshTip: () -> Unit
) {
    val todayIso = remember { StreakCalculator.todayIso() }
    val todayDay = remember { currentIsoDayOfWeek() }
    val todayCourses = remember(courses, todayDay) {
        courses.filter { it.dayOfWeek == todayDay }
    }
    val upcomingExams = remember(exams) {
        val now = System.currentTimeMillis()
        exams.filter { it.examTimeMillis >= now - 3_600_000L }.take(2)
    }

    val habits = (habitsState as? UiState.Success)?.data ?: emptyList()
    val completedToday = habits.count { it.isCompletedOn(todayIso) }
    val bestActiveStreak = habits.maxOfOrNull { it.currentStreak } ?: 0L
    val profile = (profileState as? UiState.Success)?.data

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!hasNotificationPermission) {
            item {
                NotificationPermissionBanner(onRequestPermission = onRequestNotificationPermission)
            }
        }

        // Hero Greeting Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Xin chào, ${profile?.displayName?.ifBlank { null } ?: "Sinh viên"}!",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val schoolMeta = listOfNotNull(
                            profile?.schoolName?.takeIf { it.isNotBlank() },
                            profile?.className?.takeIf { it.isNotBlank() }
                        ).joinToString(" • ")
                        Text(
                            text = if (schoolMeta.isNotBlank()) schoolMeta else "${dayOfWeekLabel(todayDay)} • Chúc bạn một ngày học hiệu quả!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DashboardStatChip(
                                icon = Icons.Filled.Schedule,
                                value = "${todayCourses.size}",
                                label = "Môn hôm nay",
                                modifier = Modifier.weight(1f)
                            )
                            DashboardStatChip(
                                icon = Icons.Filled.LocalFireDepartment,
                                value = "$bestActiveStreak ngày",
                                label = "Chuỗi cao nhất",
                                modifier = Modifier.weight(1f)
                            )
                            DashboardStatChip(
                                icon = Icons.Filled.EventNote,
                                value = "${upcomingExams.size}",
                                label = "Kỳ thi sắp tới",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Quote Card 1: Góc cảm hứng học tập
        item {
            QuoteCard(
                title = "Góc cảm hứng học tập",
                state = quoteState,
                onRefresh = onRefreshQuote
            )
        }

        // Quote Card 2: Lời khuyên thành công
        item {
            QuoteCard(
                title = "Lời khuyên thành công",
                state = tipState,
                onRefresh = onRefreshTip
            )
        }

        // Quick Actions Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onOpenPomodoro,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("quick_pomodoro_button")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Học Pomodoro", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FilledTonalButton(
                    onClick = onTestNotification,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("quick_test_notification_button")
                ) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Thử thông báo", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // Today's Classes
        item {
            SectionHeader(
                title = "Lịch học hôm nay (${dayOfWeekLabel(todayDay)})",
                actionLabel = "Xem tất cả",
                onAction = onOpenAcademic
            )
        }

        if (todayCourses.isEmpty()) {
            item {
                EmptySectionCard(
                    title = "Hôm nay chưa có lịch học",
                    subtitle = if (courses.isEmpty()) {
                        "Bạn có thể thêm môn học hoặc nạp thời khóa biểu mẫu để dùng thử."
                    } else {
                        "Bạn có ${courses.size} môn học trong tuần. Kiểm tra lịch các ngày khác nhé."
                    },
                    actionLabel = if (courses.isEmpty()) "Thêm dữ liệu mẫu" else "Mở thời khóa biểu",
                    onAction = if (courses.isEmpty()) onSeedSampleAcademic else onOpenAcademic
                )
            }
        } else {
            items(todayCourses, key = { "dash_course_${it.id}" }) { course ->
                val accent = SubjectPalette[course.colorIndex.mod(SubjectPalette.size)]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .height(46.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(accent)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = course.subjectName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Phòng ${course.room} • ${course.lecturer}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${formatTime(course.startHour, course.startMinute)}\n${formatTime(course.endHour, course.endMinute)}",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelMedium,
                            color = accent
                        )
                    }
                }
            }
        }

        // Upcoming Exams
        if (upcomingExams.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Lịch thi sắp tới",
                    actionLabel = "Chi tiết",
                    onAction = onOpenAcademic
                )
            }
            items(upcomingExams, key = { "dash_exam_${it.id}" }) { exam ->
                val daysLeft = daysUntilMillis(exam.examTimeMillis)
                val badgeText = when {
                    daysLeft <= 0L -> "Hôm nay"
                    daysLeft == 1L -> "Ngày mai"
                    else -> "Còn $daysLeft ngày"
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${exam.subjectName} (${exam.examType})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${formatExamDateTime(exam.examTimeMillis)} • Phòng ${exam.examRoom}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.tertiary
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        // Daily Habits Quick Check-in
        item {
            SectionHeader(
                title = "Thói quen hôm nay ($completedToday/${habits.size})",
                actionLabel = "Quản lý",
                onAction = onOpenHabits
            )
        }

        if (habits.isNotEmpty()) {
            item {
                val ratio = if (habits.isEmpty()) 0f else completedToday.toFloat() / habits.size.toFloat()
                LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = SuccessEmerald,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            items(habits.take(4), key = { "dash_habit_${it.id}" }) { habit ->
                val done = habit.isCompletedOn(todayIso)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleHabitToday(habit) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (done) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = if (done) "Đã hoàn thành" else "Chưa hoàn thành",
                            tint = if (done) SuccessEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = habit.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${habit.categoryEnum().label} • ${habit.targetMinutes} phút",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(StreakFlame.copy(alpha = 0.14f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = null,
                                tint = StreakFlame,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${habit.currentStreak}",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelLarge,
                                color = StreakFlame
                            )
                        }
                    }
                }
            }
        } else if (habitsState is UiState.Success) {
            item {
                EmptySectionCard(
                    title = "Chưa có thói quen nào",
                    subtitle = "Tạo bộ 3 thói quen sinh viên (Đọc sách, Tập thể dục, Học Tiếng Anh) ngay!",
                    actionLabel = "Tạo 3 thói quen mẫu",
                    onAction = onSeedStarterHabits
                )
            }
        }
    }
}

@Composable
private fun DashboardStatChip(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.16f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = actionLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onAction)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun QuoteCard(
    title: String,
    state: UiState<QuoteUiModel>,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.EventNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(
                    onClick = onRefresh,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Tải mới", style = MaterialTheme.typography.labelMedium)
                }
            }

            when (state) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
                is UiState.Success -> {
                    val quote = state.data
                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "🇬🇧 “${quote.quote ?: ""}”",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic
                        )
                        Text(
                            text = "🇻🇳 “${quote.translation ?: ""}”",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "— ${quote.author ?: "Unknown"} —",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = {
                                    uriHandler.openUri("https://dummyjson.com/quotes/${quote.id ?: 1}")
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("🔗 Xem nguồn API", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                is UiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
