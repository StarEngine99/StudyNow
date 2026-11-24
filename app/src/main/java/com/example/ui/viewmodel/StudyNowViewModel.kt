package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AcademicRepository
import com.example.data.local.CourseScheduleEntity
import com.example.data.local.ExamScheduleEntity
import com.example.data.local.PreferencesRepository
import com.example.data.remote.AvatarPreset
import com.example.data.remote.CloudRepository
import com.example.data.remote.Habit
import com.example.data.remote.HabitCategory
import com.example.data.remote.StreakCalculator
import com.example.data.remote.StudentProfile
import com.example.data.remote.UiState
import com.example.data.remote.QuoteApiService
import com.example.data.remote.QuoteResponse
import com.example.data.remote.QuoteUiModel
import com.example.data.remote.TranslateApiService
import com.example.notifications.StudyReminderScheduler
import com.example.ui.theme.ThemeMode
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PomodoroPhase(val label: String, val defaultMinutes: Int) {
    FOCUS("Tập trung", 25),
    DEEP_WORK("Chuyên sâu", 45),
    SHORT_BREAK("Nghỉ ngắn", 5),
    LONG_BREAK("Nghỉ dài", 15)
}

data class PomodoroUiState(
    val phase: PomodoroPhase = PomodoroPhase.FOCUS,
    val totalSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val completedSessionsToday: Int = 0,
    val selectedSubjectName: String? = null
) {
    val progress: Float
        get() = if (totalSeconds <= 0) 0f else 1f - (remainingSeconds.toFloat() / totalSeconds.toFloat())
}

interface PomodoroAlertHandler {
    fun onPomodoroCompleted(phaseLabel: String, subjectName: String?)
}

class StudyNowViewModel(
    private val currentUserId: String,
    private val academicRepository: AcademicRepository,
    private val cloudRepository: CloudRepository,
    private val preferencesRepository: PreferencesRepository,
    private val scheduler: StudyReminderScheduler,
    private val alertHandler: PomodoroAlertHandler
) : ViewModel() {

    val courses: StateFlow<List<CourseScheduleEntity>> = academicRepository.observeCourses(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val exams: StateFlow<List<ExamScheduleEntity>> = academicRepository.observeExams(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val profileState: StateFlow<UiState<StudentProfile?>> = cloudRepository.observeProfile(currentUserId)
        .map<StudentProfile?, UiState<StudentProfile?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing profile", rootError)
            emit(UiState.Error(rootError.message ?: "Không thể tải hồ sơ sinh viên"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val habitsState: StateFlow<UiState<List<Habit>>> = cloudRepository.observeHabits(currentUserId)
        .map<List<Habit>, UiState<List<Habit>>> { habits ->
            habits.forEach { scheduler.scheduleHabitReminder(it) }
            UiState.Success(habits)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing habits", rootError)
            emit(UiState.Error(rootError.message ?: "Không thể đồng bộ thói quen"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val quoteApiService = QuoteApiService.create()
    private val translateApiService = TranslateApiService.create()

    private val _quoteState = MutableStateFlow<UiState<QuoteUiModel>>(UiState.Loading)
    val quoteState: StateFlow<UiState<QuoteUiModel>> = _quoteState.asStateFlow()

    private val _tipState = MutableStateFlow<UiState<QuoteUiModel>>(UiState.Loading)
    val tipState: StateFlow<UiState<QuoteUiModel>> = _tipState.asStateFlow()

    init {
        fetchRandomQuote()
        fetchRandomTip()
    }

    fun fetchRandomQuote() {
        viewModelScope.launch {
            _quoteState.value = UiState.Loading
            try {
                val quote = quoteApiService.getRandomQuote()
                val english = quote.quote ?: ""
                val translation = runCatching {
                    translateApiService.translate(english).responseData?.translatedText
                }.getOrNull() ?: "Kiến thức là chìa khóa mở ra cánh cửa tương lai."

                _quoteState.value = UiState.Success(
                    QuoteUiModel(quote.id, english, quote.author, translation)
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _quoteState.value = UiState.Error(e.localizedMessage ?: "Không thể tải câu nói truyền cảm hứng")
            }
        }
    }

    fun fetchRandomTip() {
        viewModelScope.launch {
            _tipState.value = UiState.Loading
            try {
                val tip = quoteApiService.getRandomQuote()
                val english = tip.quote ?: ""
                val translation = runCatching {
                    translateApiService.translate(english).responseData?.translatedText
                }.getOrNull() ?: "Hãy kiên trì theo đuổi mục tiêu học tập mỗi ngày."

                _tipState.value = UiState.Success(
                    QuoteUiModel(tip.id, english, tip.author, translation)
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _tipState.value = UiState.Error(e.localizedMessage ?: "Không thể tải lời khuyên")
            }
        }
    }

    val localAvatarPath: StateFlow<String?> = preferencesRepository.observeLocalAvatarPath(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    val pomodoroSoundEnabled: StateFlow<Boolean> = preferencesRepository.pomodoroSoundEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = true
        )

    private val _pomodoroState = MutableStateFlow(PomodoroUiState())
    val pomodoroState: StateFlow<PomodoroUiState> = _pomodoroState.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            Firebase.auth.currentUser?.let { user ->
                cloudRepository.ensureProfileExists(user)
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setPomodoroSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setPomodoroSoundEnabled(enabled)
        }
    }

    fun saveLocalAvatarPath(path: String?) {
        viewModelScope.launch {
            preferencesRepository.setLocalAvatarPath(currentUserId, path)
            _events.emit("Đã cập nhật ảnh đại diện")
        }
    }

    fun saveCourse(course: CourseScheduleEntity) {
        viewModelScope.launch {
            academicRepository.saveCourse(course.copy(userId = currentUserId))
            val msg = if (course.reminderEnabled) {
                "Đã lưu môn ${course.subjectName} & hẹn thông báo trước ${course.reminderMinutesBefore} phút"
            } else {
                "Đã lưu môn ${course.subjectName}"
            }
            _events.emit(msg)
        }
    }

    fun deleteCourse(courseId: Long) {
        viewModelScope.launch {
            academicRepository.deleteCourse(courseId)
            _events.emit("Đã xóa môn học")
        }
    }

    fun saveExam(exam: ExamScheduleEntity) {
        viewModelScope.launch {
            academicRepository.saveExam(exam.copy(userId = currentUserId))
            val msg = if (exam.reminderEnabled) {
                "Đã lưu lịch thi ${exam.subjectName} & đặt nhắc nhở"
            } else {
                "Đã lưu lịch thi ${exam.subjectName}"
            }
            _events.emit(msg)
        }
    }

    fun deleteExam(examId: Long) {
        viewModelScope.launch {
            academicRepository.deleteExam(examId)
            _events.emit("Đã xóa lịch thi")
        }
    }

    fun seedSampleAcademicData() {
        viewModelScope.launch {
            academicRepository.seedSampleAcademicData(currentUserId)
            _events.emit("Đã thêm thời khóa biểu & lịch thi mẫu")
        }
    }

    fun addHabit(
        title: String,
        category: HabitCategory,
        targetMinutes: Int,
        reminderHour: Int,
        reminderMinute: Int,
        reminderEnabled: Boolean
    ) {
        viewModelScope.launch {
            cloudRepository.addHabit(
                title = title,
                category = category,
                targetMinutes = targetMinutes,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                reminderEnabled = reminderEnabled
            ).onSuccess { habit ->
                scheduler.scheduleHabitReminder(habit)
                _events.emit("Đã tạo thói quen \"${habit.title}\"")
            }.onFailure { err ->
                _events.emit(err.localizedMessage ?: "Không thể thêm thói quen")
            }
        }
    }

    fun updateHabit(
        habitId: String,
        title: String,
        category: HabitCategory,
        targetMinutes: Int,
        reminderHour: Int,
        reminderMinute: Int,
        reminderEnabled: Boolean
    ) {
        viewModelScope.launch {
            cloudRepository.updateHabit(
                habitId = habitId,
                title = title,
                category = category,
                targetMinutes = targetMinutes,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                reminderEnabled = reminderEnabled
            ).onSuccess {
                _events.emit("Đã cập nhật thói quen")
            }.onFailure { err ->
                _events.emit(err.localizedMessage ?: "Không thể cập nhật thói quen")
            }
        }
    }

    fun toggleHabitToday(habit: Habit) {
        viewModelScope.launch {
            val today = StreakCalculator.todayIso()
            cloudRepository.toggleHabitToday(habit, today)
                .onFailure { err ->
                    _events.emit(err.localizedMessage ?: "Lỗi điểm danh thói quen")
                }
        }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            scheduler.cancelHabitReminder(habitId)
            cloudRepository.deleteHabit(habitId)
                .onSuccess { _events.emit("Đã xóa thói quen") }
                .onFailure { err -> _events.emit(err.localizedMessage ?: "Không thể xóa thói quen") }
        }
    }

    fun seedStarterHabits() {
        viewModelScope.launch {
            cloudRepository.seedStarterHabits()
                .onSuccess { list ->
                    list.forEach { scheduler.scheduleHabitReminder(it) }
                    _events.emit("Đã thêm 3 thói quen sinh viên mẫu")
                }
                .onFailure { err ->
                    _events.emit(err.localizedMessage ?: "Không thể tạo thói quen mẫu")
                }
        }
    }

    fun updateProfile(
        displayName: String,
        schoolName: String,
        className: String,
        studentCode: String,
        avatarPreset: AvatarPreset
    ) {
        viewModelScope.launch {
            cloudRepository.updateProfile(
                displayName = displayName,
                schoolName = schoolName,
                className = className,
                studentCode = studentCode,
                avatarPreset = avatarPreset
            ).onSuccess {
                _events.emit("Đã đồng bộ hồ sơ sinh viên")
            }.onFailure { err ->
                _events.emit(err.localizedMessage ?: "Không thể cập nhật hồ sơ")
            }
        }
    }

    fun selectPomodoroPhase(phase: PomodoroPhase) {
        pausePomodoro()
        val secs = phase.defaultMinutes * 60
        _pomodoroState.update {
            it.copy(
                phase = phase,
                totalSeconds = secs,
                remainingSeconds = secs,
                isRunning = false
            )
        }
    }

    fun selectPomodoroSubject(subjectName: String?) {
        _pomodoroState.update { it.copy(selectedSubjectName = subjectName) }
    }

    fun togglePomodoroRunning() {
        if (_pomodoroState.value.isRunning) {
            pausePomodoro()
        } else {
            startPomodoro()
        }
    }

    private fun startPomodoro() {
        val current = _pomodoroState.value
        val remaining = if (current.remainingSeconds <= 0) current.totalSeconds else current.remainingSeconds
        val endTimeMillis = System.currentTimeMillis() + remaining * 1000L
        scheduler.schedulePomodoroAlarm(
            endTimeMillis = endTimeMillis,
            subjectLabel = current.selectedSubjectName,
            phaseTitle = current.phase.label
        )
        _pomodoroState.update { it.copy(remainingSeconds = remaining, isRunning = true) }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_pomodoroState.value.isRunning && _pomodoroState.value.remainingSeconds > 0) {
                delay(1000L)
                val next = (_pomodoroState.value.remainingSeconds - 1).coerceAtLeast(0)
                _pomodoroState.update { it.copy(remainingSeconds = next) }
                if (next == 0) {
                    onPomodoroFinished()
                    break
                }
            }
        }
    }

    fun pausePomodoro() {
        timerJob?.cancel()
        timerJob = null
        scheduler.cancelPomodoroAlarm()
        _pomodoroState.update { it.copy(isRunning = false) }
    }

    fun resetPomodoro() {
        pausePomodoro()
        _pomodoroState.update {
            it.copy(remainingSeconds = it.totalSeconds, isRunning = false)
        }
    }

    private fun onPomodoroFinished() {
        timerJob?.cancel()
        timerJob = null
        scheduler.cancelPomodoroAlarm()
        val finished = _pomodoroState.value
        val isFocusPhase = finished.phase == PomodoroPhase.FOCUS || finished.phase == PomodoroPhase.DEEP_WORK
        val minutesCompleted = finished.totalSeconds / 60

        _pomodoroState.update {
            it.copy(
                isRunning = false,
                completedSessionsToday = if (isFocusPhase) it.completedSessionsToday + 1 else it.completedSessionsToday
            )
        }

        if (pomodoroSoundEnabled.value) {
            alertHandler.onPomodoroCompleted(finished.phase.label, finished.selectedSubjectName)
        }

        if (isFocusPhase) {
            viewModelScope.launch {
                cloudRepository.recordPomodoroSession(minutesCompleted)
                _events.emit("Hoàn thành $minutesCompleted phút tập trung! Đã lưu thống kê.")
            }
        } else {
            viewModelScope.launch {
                _events.emit("Hết giờ ${finished.phase.label}. Sẵn sàng quay lại học nhé!")
            }
        }
    }

    fun triggerTestNotification() {
        scheduler.scheduleTestNotificationIn5Seconds()
        viewModelScope.launch {
            _events.emit("Thông báo thử sẽ xuất hiện sau 5 giây!")
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    private companion object {
        const val TAG = "StudyNowVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
