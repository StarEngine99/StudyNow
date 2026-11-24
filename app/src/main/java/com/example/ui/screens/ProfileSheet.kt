package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.remote.AvatarPreset
import com.example.data.remote.StudentProfile
import com.example.data.remote.UiState
import com.example.notifications.StudyReminderScheduler
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.ThemeMode
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSheet(
    currentUserId: String,
    profileState: UiState<StudentProfile?>,
    localAvatarPath: String?,
    themeMode: ThemeMode,
    hasNotificationPermission: Boolean,
    onDismiss: () -> Unit,
    onSaveProfile: (String, String, String, String, AvatarPreset) -> Unit,
    onSaveLocalAvatarPath: (String?) -> Unit,
    onChangeThemeMode: (ThemeMode) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onTestNotification: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val profile = (profileState as? UiState.Success)?.data
    val scheduler = remember(context) { StudyReminderScheduler(context) }

    var displayName by remember(profile?.displayName) { mutableStateOf(profile?.displayName ?: "") }
    var schoolName by remember(profile?.schoolName) { mutableStateOf(profile?.schoolName ?: "") }
    var className by remember(profile?.className) { mutableStateOf(profile?.className ?: "") }
    var studentCode by remember(profile?.studentCode) { mutableStateOf(profile?.studentCode ?: "") }
    var selectedPreset by remember(profile?.avatarPreset) {
        mutableStateOf(AvatarPreset.fromRaw(profile?.avatarPreset))
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val dest = File(context.filesDir, "avatar_$currentUserId.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                dest.absolutePath
            }.onSuccess { path ->
                onSaveLocalAvatarPath(path)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Hồ sơ sinh viên & Cài đặt",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StudentAvatar(
                    photoUrl = profile?.photoUrl,
                    localAvatarPath = localAvatarPath,
                    preset = selectedPreset,
                    size = 68.dp
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = profile?.email ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Chọn ảnh")
                        }
                        if (!localAvatarPath.isNullOrBlank()) {
                            TextButton(onClick = { onSaveLocalAvatarPath(null) }) {
                                Text("Dùng mặc định")
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AvatarPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = selectedPreset == preset,
                        onClick = { selectedPreset = preset },
                        label = { Text(preset.label) }
                    )
                }
            }

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Họ và tên sinh viên") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = schoolName,
                onValueChange = { schoolName = it },
                label = { Text("Trường Đại học / Cao đẳng") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it },
                    label = { Text("Lớp / Khoa") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = studentCode,
                    onValueChange = { studentCode = it },
                    label = { Text("MSSV") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = {
                    if (displayName.trim().isNotEmpty()) {
                        onSaveProfile(displayName, schoolName, className, studentCode, selectedPreset)
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_profile_button")
            ) {
                Text("Lưu & Đồng bộ hồ sơ", fontWeight = FontWeight.SemiBold)
            }

            HorizontalDivider()

            Text("Giao diện (Light / Dark Mode)", style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { idx, mode ->
                    SegmentedButton(
                        selected = themeMode == mode,
                        onClick = { onChangeThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(idx, ThemeMode.entries.size),
                        modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}")
                    ) {
                        Text(mode.label)
                    }
                }
            }

            HorizontalDivider()

            Text("Thông báo & Báo thức đúng giờ", style = MaterialTheme.typography.titleMedium)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Quyền thông báo", style = MaterialTheme.typography.bodyLarge)
                        if (hasNotificationPermission) {
                            Text("Đã bật", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        } else {
                            Text(
                                text = "Cấp quyền",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onRequestNotificationPermission() }
                            )
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val exactOk = scheduler.canScheduleExactAlarms()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Báo thức chính xác từng giây", style = MaterialTheme.typography.bodyLarge)
                            if (exactOk) {
                                Text("Sẵn sàng", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                            } else {
                                Text(
                                    text = "Mở cài đặt",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            if (!hasNotificationPermission) onRequestNotificationPermission()
                            else onTestNotification()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gửi thông báo thử sau 5 giây")
                    }
                }
            }

            OutlinedButton(
                onClick = onSignOut,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Đăng xuất tài khoản")
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}
