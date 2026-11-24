package com.example.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StudyReminderSchedulerTest {

    @Test
    fun testComputeNextDailyTriggerMillis() {
        val triggerTime = StudyReminderScheduler.computeNextDailyTriggerMillis(10, 47)
        val now = System.currentTimeMillis()
        assertTrue("Trigger time must be in the future", triggerTime > now)
    }

    @Test
    fun testHabitRequestCodeDeterministic() {
        val id1 = "habit_123"
        val id2 = "habit_123"
        assertEquals(StudyReminderScheduler.habitRequestCode(id1), StudyReminderScheduler.habitRequestCode(id2))
    }

    @Test
    fun testSchedulerCreation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scheduler = StudyReminderScheduler(context)
        assertNotNull(scheduler)
    }
}
