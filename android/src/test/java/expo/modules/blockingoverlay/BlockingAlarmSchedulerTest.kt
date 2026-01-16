package expo.modules.blockingoverlay

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.junit.Assert.*
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class BlockingAlarmSchedulerTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        BlockingScheduleStorage.invalidateCache()
        BlockedAppsStorage.invalidateCache()
        BlockingScheduleStorage.clearSchedule(context)
        BlockedAppsStorage.clearBlockedApps(context)
    }

    // ========== scheduleAlarms Tests ==========

    @Test
    fun `scheduleAlarms should schedule alarms for each window`() {
        BlockingScheduleStorage.setSchedule(context, listOf(
            BlockingWindow("window-1", "09:00", "17:00", listOf("com.app1")),
            BlockingWindow("window-2", "20:00", "22:00", listOf("com.app2"))
        ))

        BlockingAlarmScheduler.scheduleAlarms(context)

        // Verify alarms were scheduled using Robolectric's shadow
        val shadowAlarmManager = shadowOf(alarmManager)
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms

        // Each window should have 2 alarms (start + end), so 4 total
        assertEquals(4, scheduledAlarms.size)
    }

    @Test
    fun `scheduleAlarms should not schedule alarms for empty schedule`() {
        BlockingScheduleStorage.setSchedule(context, emptyList())

        BlockingAlarmScheduler.scheduleAlarms(context)

        val shadowAlarmManager = shadowOf(alarmManager)
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals(0, scheduledAlarms.size)
    }

    @Test
    fun `scheduleAlarms should update blocked apps immediately`() {
        BlockingScheduleStorage.setSchedule(context, listOf(
            BlockingWindow("active", "00:00", "23:59", listOf("com.blocked"))
        ))

        BlockingAlarmScheduler.scheduleAlarms(context)

        // Should update blocked apps
        val blockedApps = BlockedAppsStorage.getBlockedApps(context)
        assertTrue(blockedApps.contains("com.blocked"))
    }

    // ========== cancelAllAlarms Tests ==========

    @Test
    fun `cancelAllAlarms should cancel alarms for all provided windows`() {
        val windows = listOf(
            BlockingWindow("window-1", "09:00", "17:00", listOf("com.app1")),
            BlockingWindow("window-2", "20:00", "22:00", listOf("com.app2"))
        )

        // First schedule the alarms
        BlockingScheduleStorage.setSchedule(context, windows)
        BlockingAlarmScheduler.scheduleAlarms(context)

        val shadowAlarmManager = shadowOf(alarmManager)
        assertEquals(4, shadowAlarmManager.scheduledAlarms.size)

        // Now cancel them
        BlockingAlarmScheduler.cancelAllAlarms(context, windows)

        // All alarms should be cancelled
        assertEquals(0, shadowAlarmManager.scheduledAlarms.size)
    }

    @Test
    fun `cancelAllAlarms should handle empty window list`() {
        BlockingAlarmScheduler.cancelAllAlarms(context, emptyList())

        // Should not throw and no alarms should be affected
        val shadowAlarmManager = shadowOf(alarmManager)
        assertEquals(0, shadowAlarmManager.scheduledAlarms.size)
    }

    // ========== rescheduleAllAlarms Tests ==========

    @Test
    fun `rescheduleAllAlarms should cancel and reschedule all alarms`() {
        BlockingScheduleStorage.setSchedule(context, listOf(
            BlockingWindow("window-1", "09:00", "17:00", listOf("com.app1"))
        ))

        BlockingAlarmScheduler.rescheduleAllAlarms(context)

        val shadowAlarmManager = shadowOf(alarmManager)
        // Should have 2 alarms (start + end)
        assertEquals(2, shadowAlarmManager.scheduledAlarms.size)
    }

    @Test
    fun `rescheduleAllAlarms should update blocked apps`() {
        BlockingScheduleStorage.setSchedule(context, listOf(
            BlockingWindow("test", "00:00", "23:59", listOf("com.test"))
        ))

        BlockingAlarmScheduler.rescheduleAllAlarms(context)

        val blockedApps = BlockedAppsStorage.getBlockedApps(context)
        assertTrue(blockedApps.contains("com.test"))
    }

    // ========== Alarm Time Tests ==========

    @Test
    fun `should schedule alarms with correct trigger times`() {
        // Schedule a window with times in the future
        val now = LocalTime.now()
        val futureStart = now.plusHours(1).format(BlockingWindow.TIME_FORMATTER)
        val futureEnd = now.plusHours(2).format(BlockingWindow.TIME_FORMATTER)

        BlockingScheduleStorage.setSchedule(context, listOf(
            BlockingWindow("future", futureStart, futureEnd, listOf("com.future"))
        ))

        BlockingAlarmScheduler.scheduleAlarms(context)

        val shadowAlarmManager = shadowOf(alarmManager)
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms

        assertEquals(2, scheduledAlarms.size)

        // Both times should be in the future
        val currentMillis = System.currentTimeMillis()
        for (alarm in scheduledAlarms) {
            assertTrue(alarm.triggerAtTime > currentMillis)
        }
    }

    @Test
    fun `should schedule overnight window alarms correctly`() {
        BlockingScheduleStorage.setSchedule(context, listOf(
            BlockingWindow("overnight", "22:00", "06:00", listOf("com.night"))
        ))

        BlockingAlarmScheduler.scheduleAlarms(context)

        val shadowAlarmManager = shadowOf(alarmManager)
        assertEquals(2, shadowAlarmManager.scheduledAlarms.size)
    }

    // ========== Edge Cases ==========

    @Test
    fun `should handle single window schedule`() {
        BlockingScheduleStorage.setSchedule(context, listOf(
            BlockingWindow("single", "12:00", "13:00", listOf("com.lunch"))
        ))

        BlockingAlarmScheduler.scheduleAlarms(context)

        val shadowAlarmManager = shadowOf(alarmManager)
        assertEquals(2, shadowAlarmManager.scheduledAlarms.size)
    }

    @Test
    fun `should handle window with empty package list`() {
        BlockingScheduleStorage.setSchedule(context, listOf(
            BlockingWindow("empty-packages", "09:00", "17:00", emptyList())
        ))

        BlockingAlarmScheduler.scheduleAlarms(context)

        // Should still schedule alarms even with empty package list
        val shadowAlarmManager = shadowOf(alarmManager)
        assertEquals(2, shadowAlarmManager.scheduledAlarms.size)
    }

    @Test
    fun `should handle window spanning entire day`() {
        BlockingScheduleStorage.setSchedule(context, listOf(
            BlockingWindow("all-day", "00:00", "23:59", listOf("com.blocked"))
        ))

        BlockingAlarmScheduler.scheduleAlarms(context)

        val shadowAlarmManager = shadowOf(alarmManager)
        assertEquals(2, shadowAlarmManager.scheduledAlarms.size)
    }

    @Test
    fun `different windows should produce different request codes`() {
        val windows = listOf(
            BlockingWindow("window-alpha", "09:00", "17:00", listOf("com.app1")),
            BlockingWindow("window-beta", "10:00", "18:00", listOf("com.app2"))
        )

        BlockingScheduleStorage.setSchedule(context, windows)
        BlockingAlarmScheduler.scheduleAlarms(context)

        val shadowAlarmManager = shadowOf(alarmManager)
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms

        // Should have 4 alarms with unique trigger times or pending intents
        assertEquals(4, scheduledAlarms.size)
    }
}
