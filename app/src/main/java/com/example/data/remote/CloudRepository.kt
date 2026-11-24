package com.example.data.remote

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CloudRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Cần đăng nhập Google trước khi truy cập dữ liệu.")
    }

    fun observeProfile(userId: String): Flow<StudentProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "User mismatch" }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(StudentProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureProfileExists(user: FirebaseUser): Result<Unit> {
        val uid = user.uid
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeName = user.displayName?.trim()?.take(80)?.ifBlank { null } ?: "Sinh viên StudyNow"
                val safeEmail = user.email?.trim()?.take(120) ?: ""
                val safePhoto = user.photoUrl?.toString()?.take(500)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "schoolName" to "",
                    "className" to "",
                    "studentCode" to "",
                    "avatarPreset" to AvatarPreset.INDIGO_OWL.name,
                    "photoUrl" to safePhoto,
                    "pomodoroMinutesTotal" to 0L,
                    "pomodoroSessionsCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        displayName: String,
        schoolName: String,
        className: String,
        studentCode: String,
        avatarPreset: AvatarPreset
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Chưa đăng nhập"))
        val path = "users/$uid"
        return try {
            val cleanName = displayName.trim().take(80)
            require(cleanName.isNotEmpty()) { "Tên hiển thị không được để trống" }
            val docRef = db.collection("users").document(uid)
            docRef.update(
                mapOf(
                    "displayName" to cleanName,
                    "schoolName" to schoolName.trim().take(120),
                    "className" to className.trim().take(80),
                    "studentCode" to studentCode.trim().take(30),
                    "avatarPreset" to avatarPreset.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordPomodoroSession(minutes: Int): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Chưa đăng nhập"))
        val path = "users/$uid"
        return try {
            val safeMinutes = minutes.coerceIn(1, 180).toLong()
            db.collection("users").document(uid).update(
                mapOf(
                    "pomodoroMinutesTotal" to FieldValue.increment(safeMinutes),
                    "pomodoroSessionsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeHabits(userId: String): Flow<List<Habit>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "User mismatch" }
        val path = "users/$uid/habits"
        emitAll(
            db.collection("users").document(uid).collection("habits")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Habit::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.currentStreak }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addHabit(
        title: String,
        category: HabitCategory,
        targetMinutes: Int,
        reminderHour: Int,
        reminderMinute: Int,
        reminderEnabled: Boolean
    ): Result<Habit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Chưa đăng nhập"))
        var path = "users/$uid/habits"
        return try {
            val cleanTitle = title.trim().take(100)
            require(cleanTitle.isNotEmpty()) { "Tên thói quen không được để trống" }
            val safeMinutes = targetMinutes.coerceIn(1, 600).toLong()
            val safeHour = reminderHour.coerceIn(0, 23).toLong()
            val safeMinute = reminderMinute.coerceIn(0, 59).toLong()

            val docRef = db.collection("users").document(uid).collection("habits").document()
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "title" to cleanTitle,
                "category" to category.name,
                "targetMinutes" to safeMinutes,
                "completedDates" to emptyList<String>(),
                "currentStreak" to 0L,
                "bestStreak" to 0L,
                "reminderHour" to safeHour,
                "reminderMinute" to safeMinute,
                "reminderEnabled" to reminderEnabled,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(
                Habit(
                    id = docRef.id,
                    userId = uid,
                    title = cleanTitle,
                    category = category.name,
                    targetMinutes = safeMinutes,
                    completedDates = emptyList(),
                    currentStreak = 0L,
                    bestStreak = 0L,
                    reminderHour = safeHour,
                    reminderMinute = safeMinute,
                    reminderEnabled = reminderEnabled
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateHabit(
        habitId: String,
        title: String,
        category: HabitCategory,
        targetMinutes: Int,
        reminderHour: Int,
        reminderMinute: Int,
        reminderEnabled: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Chưa đăng nhập"))
        val path = "users/$uid/habits/$habitId"
        return try {
            require(habitId.isNotBlank()) { "Habit ID không hợp lệ" }
            val cleanTitle = title.trim().take(100)
            require(cleanTitle.isNotEmpty()) { "Tên thói quen không được để trống" }
            db.collection("users").document(uid).collection("habits").document(habitId)
                .update(
                    mapOf(
                        "title" to cleanTitle,
                        "category" to category.name,
                        "targetMinutes" to targetMinutes.coerceIn(1, 600).toLong(),
                        "reminderHour" to reminderHour.coerceIn(0, 23).toLong(),
                        "reminderMinute" to reminderMinute.coerceIn(0, 59).toLong(),
                        "reminderEnabled" to reminderEnabled,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleHabitToday(habit: Habit, todayIso: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Chưa đăng nhập"))
        val path = "users/$uid/habits/${habit.id}"
        return try {
            require(habit.id.isNotBlank()) { "Habit ID không hợp lệ" }
            val updatedDates = if (habit.completedDates.contains(todayIso)) {
                habit.completedDates.filterNot { it == todayIso }
            } else {
                (habit.completedDates + todayIso).distinct().sorted().takeLast(365)
            }
            val (newStreak, newBest) = StreakCalculator.calculateStreaks(updatedDates, habit.bestStreak)
            db.collection("users").document(uid).collection("habits").document(habit.id)
                .update(
                    mapOf(
                        "completedDates" to updatedDates,
                        "currentStreak" to newStreak,
                        "bestStreak" to newBest,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteHabit(habitId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Chưa đăng nhập"))
        val path = "users/$uid/habits/$habitId"
        return try {
            require(habitId.isNotBlank()) { "Habit ID không hợp lệ" }
            db.collection("users").document(uid).collection("habits").document(habitId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun seedStarterHabits(): Result<List<Habit>> {
        val created = mutableListOf<Habit>()
        val starters = listOf(
            Triple("Đọc sách chuyên ngành", HabitCategory.STUDY, 20) to (20 to 0),
            Triple("Tập thể dục / Chạy bộ", HabitCategory.HEALTH, 30) to (7 to 0),
            Triple("Luyện nghe & từ vựng Tiếng Anh", HabitCategory.LANGUAGE, 25) to (21 to 0),
        )
        for ((meta, time) in starters) {
            val res = addHabit(
                title = meta.first,
                category = meta.second,
                targetMinutes = meta.third,
                reminderHour = time.first,
                reminderMinute = time.second,
                reminderEnabled = true
            )
            res.onSuccess { created.add(it) }
                .onFailure { return Result.failure(it) }
        }
        return Result.success(created)
    }
}
