package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.res.Configuration
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.CourseScheduleEntity
import com.example.data.local.ExamScheduleEntity
import com.example.ui.components.EmptySectionCard
import com.example.ui.components.dayOfWeekLabel
import com.example.ui.components.daysUntilMillis
import com.example.ui.components.formatExamDateOnly
import com.example.ui.components.formatExamDateTime
import com.example.ui.components.formatTime
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SubjectPalette
import com.example.ui.theme.SuccessEmerald
import java.util.Calendar
import java.util.Locale

private enum class AcademicTab(val label: String) {
    TIMETABLE("Thời khóa biểu"),
    EXAMS("Lịch thi"),
    GRADES("Điểm & GPA")
}

@Composable
fun AcademicScreen(
    courses: List<CourseScheduleEntity>,
    exams: List<ExamScheduleEntity>,
    onSaveCourse: (CourseScheduleEntity) -> Unit,
    onDeleteCourse: (Long) -> Unit,
    onSaveExam: (ExamScheduleEntity) -> Unit,
    onDeleteExam: (Long) -> Unit,
    onSeedSampleData: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(AcademicTab.TIMETABLE) }
    var selectedDayFilter by remember { mutableIntStateOf(0) } // 0 = All week
    var editingCourse by remember { mutableStateOf<CourseScheduleEntity?>(null) }
    var showCourseDialog by remember { mutableStateOf(false) }
    var editingExam by remember { mutableStateOf<ExamScheduleEntity?>(null) }
    var showExamDialog by remember { mutableStateOf(false) }

    val orientation = LocalConfiguration.current.orientation

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideOrLandscape = maxWidth >= 600.dp || orientation == Configuration.ORIENTATION_LANDSCAPE
        val gridColumns = if (isWideOrLandscape) 2 else 1

        Column(modifier = Modifier.fillMaxSize()) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                AcademicTab.entries.forEachIndexed { idx, tab ->
                    SegmentedButton(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        shape = SegmentedButtonDefaults.itemShape(idx, AcademicTab.entries.size),
                        modifier = Modifier.testTag("academic_tab_${tab.name.lowercase()}")
                    ) {
                        Text(tab.label, maxLines = 1)
                    }
                }
            }

            when (selectedTab) {
                AcademicTab.TIMETABLE -> {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedDayFilter == 0,
                                onClick = { selectedDayFilter = 0 },
                                label = { Text("Cả tuần") }
                            )
                        }
                        items((1..7).toList()) { day ->
                            FilterChip(
                                selected = selectedDayFilter == day,
                                onClick = { selectedDayFilter = day },
                                label = { Text(dayOfWeekLabel(day, short = true)) }
                            )
                        }
                    }

                    val filtered = remember(courses, selectedDayFilter) {
                        if (selectedDayFilter == 0) courses else courses.filter { it.dayOfWeek == selectedDayFilter }
                    }

                    if (filtered.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            EmptySectionCard(
                                title = "Chưa có môn học nào",
                                subtitle = "Lưu thời khóa biểu offline với phòng học, giảng viên & nhắc trước giờ vào lớp.",
                                actionLabel = if (courses.isEmpty()) "Thêm dữ liệu mẫu" else "Thêm môn học",
                                onAction = {
                                    if (courses.isEmpty()) onSeedSampleData()
                                    else {
                                        editingCourse = null
                                        showCourseDialog = true
                                    }
                                }
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
                            items(filtered, key = { it.id }) { course ->
                                CourseCard(
                                    course = course,
                                    onEdit = {
                                        editingCourse = course
                                        showCourseDialog = true
                                    },
                                    onDelete = { onDeleteCourse(course.id) },
                                    onToggleReminder = {
                                        onSaveCourse(course.copy(reminderEnabled = !course.reminderEnabled))
                                    }
                                )
                            }
                        }
                    }
                }

                AcademicTab.EXAMS -> {
                    if (exams.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            EmptySectionCard(
                                title = "Chưa có lịch thi",
                                subtitle = "Thêm ngày thi, phòng thi, số báo danh để StudyNow tự động đếm ngược và thông báo.",
                                actionLabel = "Thêm lịch thi",
                                onAction = {
                                    editingExam = null
                                    showExamDialog = true
                                }
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
                            items(exams, key = { it.id }) { exam ->
                                ExamCard(
                                    exam = exam,
                                    onEdit = {
                                        editingExam = exam
                                        showExamDialog = true
                                    },
                                    onDelete = { onDeleteExam(exam.id) },
                                    onToggleReminder = {
                                        onSaveExam(exam.copy(reminderEnabled = !exam.reminderEnabled))
                                    }
                                )
                            }
                        }
                    }
                }

                AcademicTab.GRADES -> {
                    GradesOverview(
                        courses = courses,
                        gridColumns = gridColumns,
                        onEditGrades = { course ->
                            editingCourse = course
                            showCourseDialog = true
                        }
                    )
                }
            }
        }

        if (selectedTab != AcademicTab.GRADES) {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == AcademicTab.TIMETABLE) {
                        editingCourse = null
                        showCourseDialog = true
                    } else {
                        editingExam = null
                        showExamDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("academic_add_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Thêm mới")
            }
        }
    }

    if (showCourseDialog) {
        CourseEditorDialog(
            initial = editingCourse,
            onDismiss = { showCourseDialog = false },
            onSave = {
                onSaveCourse(it)
                showCourseDialog = false
            }
        )
    }

    if (showExamDialog) {
        ExamEditorDialog(
            initial = editingExam,
            onDismiss = { showExamDialog = false },
            onSave = {
                onSaveExam(it)
                showExamDialog = false
            }
        )
    }
}

@Composable
private fun CourseCard(
    course: CourseScheduleEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleReminder: () -> Unit
) {
    val accent = SubjectPalette[course.colorIndex.mod(SubjectPalette.size)]
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accent.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "${dayOfWeekLabel(course.dayOfWeek)} • ${formatTime(course.startHour, course.startMinute)} - ${formatTime(course.endHour, course.endMinute)}",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onToggleReminder) {
                    Icon(
                        imageVector = if (course.reminderEnabled) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                        contentDescription = "Bật/tắt nhắc lịch học",
                        tint = if (course.reminderEnabled) accent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = course.subjectName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (course.subjectCode.isNotBlank()) {
                Text(
                    text = "${course.subjectCode} • ${course.credits} tín chỉ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Phòng ${course.room}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(14.dp))
                Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
                Text(course.lecturer, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (course.reminderEnabled) "Nhắc trước ${course.reminderMinutesBefore} phút" else "Đang tắt thông báo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Sửa môn học", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Xóa môn học", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamCard(
    exam: ExamScheduleEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleReminder: () -> Unit
) {
    val daysLeft = daysUntilMillis(exam.examTimeMillis)
    val badgeText = when {
        daysLeft < 0 -> "Đã qua"
        daysLeft == 0L -> "Hôm nay!"
        daysLeft == 1L -> "Ngày mai"
        else -> "Còn $daysLeft ngày"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = "${exam.examType} • $badgeText",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onToggleReminder) {
                    Icon(
                        imageVector = if (exam.reminderEnabled) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                        contentDescription = "Bật/tắt nhắc lịch thi",
                        tint = if (exam.reminderEnabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = exam.subjectName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = formatExamDateTime(exam.examTimeMillis) + " (${exam.durationMinutes} phút)",
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            val seatText = if (exam.seatNumber.isNotBlank()) " • SBD/Ghế: ${exam.seatNumber}" else ""
            Text(
                text = "Phòng thi: ${exam.examRoom}$seatText",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (exam.notes.isNotBlank()) {
                Text(
                    text = exam.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Sửa lịch thi", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Xóa lịch thi", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun GradesOverview(
    courses: List<CourseScheduleEntity>,
    gridColumns: Int,
    onEditGrades: (CourseScheduleEntity) -> Unit
) {
    val scoredCourses = courses.filter { it.averageScore != null }
    val totalCredits = scoredCourses.sumOf { it.credits }
    val weightedSum10 = scoredCourses.sumOf { (it.averageScore ?: 0.0) * it.credits }
    val gpa10 = if (totalCredits > 0) weightedSum10 / totalCredits else 0.0
    val gpa4 = (gpa10 / 10.0) * 4.0

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
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
                        text = "Điểm trung bình tích lũy (GPA)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Tính theo ${scoredCourses.size} môn ($totalCredits tín chỉ)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.US, "%.2f / 10", gpa10),
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format(Locale.US, "Hệ 4: %.2f", gpa4),
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        if (courses.isEmpty()) {
            Box(modifier = Modifier.padding(16.dp)) {
                EmptySectionCard(
                    title = "Chưa có môn học để tính điểm",
                    subtitle = "Hãy thêm môn học ở tab Thời khóa biểu và nhập điểm giữa kỳ / cuối kỳ."
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(courses, key = { "grade_${it.id}" }) { course ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditGrades(course) },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Grade,
                                contentDescription = null,
                                tint = SuccessEmerald,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(course.subjectName, style = MaterialTheme.typography.titleMedium)
                                val gk = course.midtermScore?.let { String.format(Locale.US, "%.1f", it) } ?: "--"
                                val ck = course.finalScore?.let { String.format(Locale.US, "%.1f", it) } ?: "--"
                                Text(
                                    text = "${course.credits} TC • GK: $gk • CK: $ck",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = course.averageScore?.let { String.format(Locale.US, "%.1f", it) } ?: "--",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseEditorDialog(
    initial: CourseScheduleEntity?,
    onDismiss: () -> Unit,
    onSave: (CourseScheduleEntity) -> Unit
) {
    val context = LocalContext.current
    var subjectName by remember { mutableStateOf(initial?.subjectName ?: "") }
    var subjectCode by remember { mutableStateOf(initial?.subjectCode ?: "") }
    var room by remember { mutableStateOf(initial?.room ?: "") }
    var lecturer by remember { mutableStateOf(initial?.lecturer ?: "") }
    var dayOfWeek by remember { mutableIntStateOf(initial?.dayOfWeek ?: 1) }
    var startHour by remember { mutableIntStateOf(initial?.startHour ?: 7) }
    var startMinute by remember { mutableIntStateOf(initial?.startMinute ?: 30) }
    var endHour by remember { mutableIntStateOf(initial?.endHour ?: 10) }
    var endMinute by remember { mutableIntStateOf(initial?.endMinute ?: 0) }
    var creditsText by remember { mutableStateOf((initial?.credits ?: 3).toString()) }
    var midtermText by remember { mutableStateOf(initial?.midtermScore?.toString() ?: "") }
    var finalText by remember { mutableStateOf(initial?.finalScore?.toString() ?: "") }
    var colorIndex by remember { mutableIntStateOf(initial?.colorIndex ?: 0) }
    var reminderEnabled by remember { mutableStateOf(initial?.reminderEnabled ?: true) }
    var reminderMinutes by remember { mutableIntStateOf(initial?.reminderMinutesBefore ?: 15) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Thêm môn học" else "Cập nhật môn học") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { subjectName = it },
                    label = { Text("Tên môn học *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = subjectCode,
                        onValueChange = { subjectCode = it },
                        label = { Text("Mã môn") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = creditsText,
                        onValueChange = { creditsText = it.filter(Char::isDigit) },
                        label = { Text("Tín chỉ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.7f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text("Phòng học *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lecturer,
                        onValueChange = { lecturer = it },
                        label = { Text("Giảng viên") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Thứ trong tuần", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items((1..7).toList()) { d ->
                        FilterChip(
                            selected = dayOfWeek == d,
                            onClick = { dayOfWeek = d },
                            label = { Text(dayOfWeekLabel(d, short = true)) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, h, m -> startHour = h; startMinute = m },
                                startHour,
                                startMinute,
                                true
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Bắt đầu: ${formatTime(startHour, startMinute)}")
                    }
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, h, m -> endHour = h; endMinute = m },
                                endHour,
                                endMinute,
                                true
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Kết thúc: ${formatTime(endHour, endMinute)}")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = midtermText,
                        onValueChange = { midtermText = it },
                        label = { Text("Điểm GK (40%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = finalText,
                        onValueChange = { finalText = it },
                        label = { Text("Điểm CK (60%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SubjectPalette.forEachIndexed { idx, color ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { colorIndex = idx }
                                .padding(4.dp)
                        ) {
                            if (colorIndex == idx) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Thông báo trước giờ học", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                }

                if (reminderEnabled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10, 15, 30, 60).forEach { mins ->
                            FilterChip(
                                selected = reminderMinutes == mins,
                                onClick = { reminderMinutes = mins },
                                label = { Text("${mins}p") }
                            )
                        }
                    }
                }

                if (validationError != null) {
                    Text(validationError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = subjectName.trim()
                    val cleanRoom = room.trim()
                    if (cleanName.isEmpty() || cleanRoom.isEmpty()) {
                        validationError = "Vui lòng nhập tên môn học và phòng học."
                        return@Button
                    }
                    onSave(
                        CourseScheduleEntity(
                            id = initial?.id ?: 0L,
                            userId = initial?.userId ?: "",
                            subjectName = cleanName,
                            subjectCode = subjectCode.trim(),
                            room = cleanRoom,
                            lecturer = lecturer.trim().ifBlank { "Đang cập nhật" },
                            dayOfWeek = dayOfWeek,
                            startHour = startHour,
                            startMinute = startMinute,
                            endHour = endHour,
                            endMinute = endMinute,
                            credits = creditsText.toIntOrNull()?.coerceIn(1, 12) ?: 3,
                            midtermScore = midtermText.replace(',', '.').toDoubleOrNull()?.coerceIn(0.0, 10.0),
                            finalScore = finalText.replace(',', '.').toDoubleOrNull()?.coerceIn(0.0, 10.0),
                            colorIndex = colorIndex,
                            reminderEnabled = reminderEnabled,
                            reminderMinutesBefore = reminderMinutes
                        )
                    )
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

@Composable
private fun ExamEditorDialog(
    initial: ExamScheduleEntity?,
    onDismiss: () -> Unit,
    onSave: (ExamScheduleEntity) -> Unit
) {
    val context = LocalContext.current
    var subjectName by remember { mutableStateOf(initial?.subjectName ?: "") }
    var examRoom by remember { mutableStateOf(initial?.examRoom ?: "") }
    var seatNumber by remember { mutableStateOf(initial?.seatNumber ?: "") }
    var examType by remember { mutableStateOf(initial?.examType ?: "Cuối kỳ") }
    var examTimeMillis by remember {
        mutableLongStateOf(initial?.examTimeMillis ?: (System.currentTimeMillis() + 3L * 24 * 3600 * 1000L))
    }
    var durationText by remember { mutableStateOf((initial?.durationMinutes ?: 90).toString()) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var reminderEnabled by remember { mutableStateOf(initial?.reminderEnabled ?: true) }
    var reminderMinutes by remember { mutableIntStateOf(initial?.reminderMinutesBefore ?: 60) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val cal = remember(examTimeMillis) {
        Calendar.getInstance().apply { timeInMillis = examTimeMillis }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Thêm lịch thi" else "Cập nhật lịch thi") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { subjectName = it },
                    label = { Text("Môn thi *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = examRoom,
                        onValueChange = { examRoom = it },
                        label = { Text("Phòng thi *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = seatNumber,
                        onValueChange = { seatNumber = it },
                        label = { Text("SBD / Ghế") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Giữa kỳ", "Cuối kỳ", "Thực hành").forEach { type ->
                        FilterChip(
                            selected = examType == type,
                            onClick = { examType = type },
                            label = { Text(type) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val c = Calendar.getInstance().apply {
                                        timeInMillis = examTimeMillis
                                        set(y, m, d)
                                    }
                                    examTimeMillis = c.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ngày: ${formatExamDateOnly(examTimeMillis)}")
                    }
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, h, min ->
                                    val c = Calendar.getInstance().apply {
                                        timeInMillis = examTimeMillis
                                        set(Calendar.HOUR_OF_DAY, h)
                                        set(Calendar.MINUTE, min)
                                        set(Calendar.SECOND, 0)
                                    }
                                    examTimeMillis = c.timeInMillis
                                },
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Text("Giờ: ${formatTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))}")
                    }
                }

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter(Char::isDigit) },
                    label = { Text("Thời lượng làm bài (phút)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Ghi chú chuẩn bị") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Thông báo nhắc lịch thi", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                }

                if (reminderEnabled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(30 to "30p", 60 to "1 giờ", 120 to "2 giờ", 1440 to "1 ngày").forEach { (mins, label) ->
                            FilterChip(
                                selected = reminderMinutes == mins,
                                onClick = { reminderMinutes = mins },
                                label = { Text(label) }
                            )
                        }
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
                    val cleanName = subjectName.trim()
                    val cleanRoom = examRoom.trim()
                    if (cleanName.isEmpty() || cleanRoom.isEmpty()) {
                        errorMsg = "Vui lòng nhập môn thi và phòng thi."
                        return@Button
                    }
                    onSave(
                        ExamScheduleEntity(
                            id = initial?.id ?: 0L,
                            userId = initial?.userId ?: "",
                            subjectName = cleanName,
                            examRoom = cleanRoom,
                            seatNumber = seatNumber.trim(),
                            examType = examType,
                            examTimeMillis = examTimeMillis,
                            durationMinutes = durationText.toIntOrNull()?.coerceIn(15, 300) ?: 90,
                            notes = notes.trim(),
                            reminderEnabled = reminderEnabled,
                            reminderMinutesBefore = reminderMinutes
                        )
                    )
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
