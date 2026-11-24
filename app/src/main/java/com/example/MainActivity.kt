package com.example

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.GoogleAuthHelper
import com.example.data.local.AcademicRepository
import com.example.data.local.PreferencesRepository
import com.example.data.local.StudyNowDatabase
import com.example.data.remote.AvatarPreset
import com.example.data.remote.CloudRepository
import com.example.data.remote.UiState
import com.example.notifications.NotificationHelper
import com.example.notifications.StudyReminderScheduler
import com.example.ui.components.StudentAvatar
import com.example.ui.screens.AcademicScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HabitsScreen
import com.example.ui.screens.PomodoroScreen
import com.example.ui.screens.ProfileSheet
import com.example.ui.theme.StudyNowTheme
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.PomodoroAlertHandler
import com.example.ui.viewmodel.StudyNowViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

enum class StudyNowTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("Tổng quan", Icons.Filled.Home, Icons.Outlined.Home),
    ACADEMIC("Lịch học", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    HABITS("Thói quen", Icons.Filled.LocalFireDepartment, Icons.Outlined.LocalFireDepartment),
    POMODORO("Pomodoro", Icons.Filled.Timer, Icons.Outlined.Timer)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember(context) { PreferencesRepository(context.applicationContext) }
            val themeMode by prefs.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)

            StudyNowTheme(themeMode = themeMode) {
                AppContent(
                    themeMode = themeMode,
                    preferencesRepository = prefs
                )
            }
        }
    }
}

@Composable
fun AppContent(
    themeMode: ThemeMode,
    preferencesRepository: PreferencesRepository
) {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        val uid = user.uid
        key(uid) {
            StudyNowMainShell(
                currentUserId = uid,
                themeMode = themeMode,
                preferencesRepository = preferencesRepository
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudyNowMainShell(
    currentUserId: String,
    themeMode: ThemeMode,
    preferencesRepository: PreferencesRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: StudyNowViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val firestore = FirebaseFirestore.getInstance(databaseId)
                val roomDb = StudyNowDatabase.getInstance(app)
                val scheduler = StudyReminderScheduler(app)
                val academicRepo = AcademicRepository(roomDb.academicDao(), scheduler)
                val cloudRepo = CloudRepository(firestore)
                val alertHandler = object : PomodoroAlertHandler {
                    override fun onPomodoroCompleted(phaseLabel: String, subjectName: String?) {
                        NotificationHelper.playPomodoroCompletionAlert(app)
                        val subjectPart = if (!subjectName.isNullOrBlank()) " ($subjectName)" else ""
                        NotificationHelper.showNotification(
                            context = app,
                            channelId = NotificationHelper.CHANNEL_POMODORO,
                            notificationId = 400_002,
                            title = "Hoàn thành phiên $phaseLabel!$subjectPart",
                            body = "Tuyệt vời! Bạn vừa hoàn tất phiên tập trung trên StudyNow."
                        )
                    }
                }
                StudyNowViewModel(
                    currentUserId = currentUserId,
                    academicRepository = academicRepo,
                    cloudRepository = cloudRepo,
                    preferencesRepository = preferencesRepository,
                    scheduler = scheduler,
                    alertHandler = alertHandler
                )
            }
        }
    )

    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val habitsState by viewModel.habitsState.collectAsStateWithLifecycle()
    val quoteState by viewModel.quoteState.collectAsStateWithLifecycle()
    val tipState by viewModel.tipState.collectAsStateWithLifecycle()
    val localAvatarPath by viewModel.localAvatarPath.collectAsStateWithLifecycle()
    val pomodoroState by viewModel.pomodoroState.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.pomodoroSoundEnabled.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(StudyNowTab.DASHBOARD) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var hasNotifPermission by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
    }

    val snackbarHostState = remember { SnackbarHostState() }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
        scope.launch {
            snackbarHostState.showSnackbar(
                if (granted) "Đã bật thông báo nhắc lịch StudyNow!"
                else "Chưa cấp quyền thông báo. Bạn có thể bật lại trong Cài đặt."
            )
        }
    }

    val requestNotificationPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            hasNotifPermission = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    if (showProfileSheet) {
        BackHandler { showProfileSheet = false }
    } else if (currentTab != StudyNowTab.DASHBOARD) {
        BackHandler { currentTab = StudyNowTab.DASHBOARD }
    }

    val profile = (profileState as? UiState.Success)?.data
    val avatarPreset = AvatarPreset.fromRaw(profile?.avatarPreset)

    val orientation = LocalConfiguration.current.orientation

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp || orientation == Configuration.ORIENTATION_LANDSCAPE

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = if (currentTab == StudyNowTab.DASHBOARD) "StudyNow" else currentTab.label,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                val next = when (themeMode) {
                                    ThemeMode.DARK -> ThemeMode.LIGHT
                                    ThemeMode.LIGHT -> ThemeMode.DARK
                                    ThemeMode.SYSTEM -> ThemeMode.DARK
                                }
                                viewModel.setThemeMode(next)
                            },
                            modifier = Modifier.testTag("toggle_theme_button")
                        ) {
                            Icon(
                                imageVector = if (themeMode == ThemeMode.DARK) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = "Chuyển giao diện Sáng/Tối"
                            )
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clip(CircleShape)
                                .clickable { showProfileSheet = true }
                                .padding(4.dp)
                                .testTag("open_profile_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            StudentAvatar(
                                photoUrl = profile?.photoUrl,
                                localAvatarPath = localAvatarPath,
                                preset = avatarPreset,
                                size = 36.dp
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar {
                        StudyNowTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_bar_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        StudyNowTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}")
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (currentTab) {
                        StudyNowTab.DASHBOARD -> DashboardScreen(
                            profileState = profileState,
                            courses = courses,
                            exams = exams,
                            habitsState = habitsState,
                            quoteState = quoteState,
                            hasNotificationPermission = hasNotifPermission,
                            onRequestNotificationPermission = requestNotificationPermission,
                            onToggleHabitToday = viewModel::toggleHabitToday,
                            onOpenAcademic = { currentTab = StudyNowTab.ACADEMIC },
                            onOpenHabits = { currentTab = StudyNowTab.HABITS },
                            onOpenPomodoro = { currentTab = StudyNowTab.POMODORO },
                            onTestNotification = {
                                if (!hasNotifPermission) {
                                    requestNotificationPermission()
                                } else {
                                    viewModel.triggerTestNotification()
                                }
                            },
                            onSeedSampleAcademic = viewModel::seedSampleAcademicData,
                            onSeedStarterHabits = viewModel::seedStarterHabits,
                            onRefreshQuote = viewModel::fetchRandomQuote,
                            tipState = tipState,
                            onRefreshTip = viewModel::fetchRandomTip
                        )

                        StudyNowTab.ACADEMIC -> AcademicScreen(
                            courses = courses,
                            exams = exams,
                            onSaveCourse = viewModel::saveCourse,
                            onDeleteCourse = viewModel::deleteCourse,
                            onSaveExam = viewModel::saveExam,
                            onDeleteExam = viewModel::deleteExam,
                            onSeedSampleData = viewModel::seedSampleAcademicData
                        )

                        StudyNowTab.HABITS -> HabitsScreen(
                            habitsState = habitsState,
                            onToggleHabitToday = viewModel::toggleHabitToday,
                            onAddHabit = viewModel::addHabit,
                            onUpdateHabit = viewModel::updateHabit,
                            onDeleteHabit = viewModel::deleteHabit,
                            onSeedStarterHabits = viewModel::seedStarterHabits
                        )

                        StudyNowTab.POMODORO -> PomodoroScreen(
                            state = pomodoroState,
                            courses = courses,
                            profileState = profileState,
                            soundEnabled = soundEnabled,
                            onSelectPhase = viewModel::selectPomodoroPhase,
                            onSelectSubject = viewModel::selectPomodoroSubject,
                            onToggleRunning = viewModel::togglePomodoroRunning,
                            onReset = viewModel::resetPomodoro,
                            onToggleSound = viewModel::setPomodoroSoundEnabled
                        )
                    }
                }
            }
        }
    }

    if (showProfileSheet) {
        ProfileSheet(
            currentUserId = currentUserId,
            profileState = profileState,
            localAvatarPath = localAvatarPath,
            themeMode = themeMode,
            hasNotificationPermission = hasNotifPermission,
            onDismiss = { showProfileSheet = false },
            onSaveProfile = viewModel::updateProfile,
            onSaveLocalAvatarPath = viewModel::saveLocalAvatarPath,
            onChangeThemeMode = viewModel::setThemeMode,
            onRequestNotificationPermission = requestNotificationPermission,
            onTestNotification = {
                if (!hasNotifPermission) requestNotificationPermission()
                else viewModel.triggerTestNotification()
            },
            onSignOut = {
                showProfileSheet = false
                GoogleAuthHelper.signOut(credentialManager, scope)
            }
        )
    }
}
