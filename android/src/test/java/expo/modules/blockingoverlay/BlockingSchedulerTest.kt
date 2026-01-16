package expo.modules.blockingoverlay

import android.content.Context
import android.content.SharedPreferences
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalTime
import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class BlockingSchedulerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockPrefs: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    private lateinit var scheduler: BlockingScheduler

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        BlockingScheduleStorage.invalidateCache()

        whenever(mockContext.getSharedPreferences(any(), eq(Context.MODE_PRIVATE)))
            .thenReturn(mockPrefs)
        whenever(mockPrefs.edit()).thenReturn(mockEditor)
        whenever(mockEditor.putString(any(), any())).thenReturn(mockEditor)
        whenever(mockEditor.remove(any())).thenReturn(mockEditor)

        scheduler = BlockingScheduler(mockContext)
    }

    // ========== Schedule Management Tests ==========

    @Test
    fun `setSchedule should store windows`() {
        val windows = listOf(
            BlockingWindow(
                id = "test-1",
                startTime = "09:00",
                endTime = "17:00",
                packageNames = listOf("com.example.app")
            )
        )

        scheduler.setSchedule(windows)
        val result = scheduler.getSchedule()

        assertEquals(1, result.size)
        assertEquals("test-1", result[0].id)
    }

    @Test
    fun `clearSchedule should remove all windows`() {
        val windows = listOf(
            BlockingWindow(
                id = "to-clear",
                startTime = "09:00",
                endTime = "17:00",
                packageNames = listOf("com.example.app")
            )
        )

        scheduler.setSchedule(windows)
        scheduler.clearSchedule()
        val result = scheduler.getSchedule()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `setSchedule should replace existing schedule`() {
        val oldWindows = listOf(
            BlockingWindow(
                id = "old-window",
                startTime = "08:00",
                endTime = "12:00",
                packageNames = listOf("com.old.app")
            )
        )
        val newWindows = listOf(
            BlockingWindow(
                id = "new-window",
                startTime = "14:00",
                endTime = "18:00",
                packageNames = listOf("com.new.app")
            )
        )

        scheduler.setSchedule(oldWindows)
        scheduler.setSchedule(newWindows)
        val result = scheduler.getSchedule()

        assertEquals(1, result.size)
        assertEquals("new-window", result[0].id)
    }

    // ========== Time Window Tests ==========

    @Test
    fun `getActiveBlockedPackagesAt should return packages within normal window`() {
        val windows = listOf(
            BlockingWindow(
                id = "work-hours",
                startTime = "09:00",
                endTime = "17:00",
                packageNames = listOf("com.social.app", "com.game.app")
            )
        )

        scheduler.setSchedule(windows)
        val result = scheduler.getActiveBlockedPackagesAt(LocalTime.of(12, 0))

        assertEquals(2, result.size)
        assertTrue(result.contains("com.social.app"))
        assertTrue(result.contains("com.game.app"))
    }

    @Test
    fun `getActiveBlockedPackagesAt should return empty set outside window`() {
        val windows = listOf(
            BlockingWindow(
                id = "work-hours",
                startTime = "09:00",
                endTime = "17:00",
                packageNames = listOf("com.social.app")
            )
        )

        scheduler.setSchedule(windows)
        val result = scheduler.getActiveBlockedPackagesAt(LocalTime.of(20, 0))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getActiveBlockedPackagesAt should include packages at exact start time`() {
        val windows = listOf(
            BlockingWindow(
                id = "exact-start",
                startTime = "09:00",
                endTime = "17:00",
                packageNames = listOf("com.example.app")
            )
        )

        scheduler.setSchedule(windows)
        val result = scheduler.getActiveBlockedPackagesAt(LocalTime.of(9, 0))

        assertEquals(1, result.size)
        assertTrue(result.contains("com.example.app"))
    }

    @Test
    fun `getActiveBlockedPackagesAt should include packages at exact end time - inclusive`() {
        val windows = listOf(
            BlockingWindow(
                id = "exact-end",
                startTime = "09:00",
                endTime = "17:00",
                packageNames = listOf("com.example.app")
            )
        )

        scheduler.setSchedule(windows)
        val result = scheduler.getActiveBlockedPackagesAt(LocalTime.of(17, 0))

        assertEquals(1, result.size)
        assertTrue(result.contains("com.example.app"))
    }

    @Test
    fun `getActiveBlockedPackagesAt should exclude packages after end time`() {
        val windows = listOf(
            BlockingWindow(
                id = "exact-end",
                startTime = "09:00",
                endTime = "17:00",
                packageNames = listOf("com.example.app")
            )
        )

        scheduler.setSchedule(windows)
        val result = scheduler.getActiveBlockedPackagesAt(LocalTime.of(17, 1))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getActiveBlockedPackagesAt should handle overnight window correctly`() {
        val windows = listOf(
            BlockingWindow(
                id = "overnight",
                startTime = "22:00",
                endTime = "06:00",
                packageNames = listOf("com.night.app")
            )
        )

        scheduler.setSchedule(windows)

        // Before midnight
        val beforeMidnight = scheduler.getActiveBlockedPackagesAt(LocalTime.of(23, 30))
        assertTrue(beforeMidnight.contains("com.night.app"))

        // After midnight
        val afterMidnight = scheduler.getActiveBlockedPackagesAt(LocalTime.of(3, 0))
        assertTrue(afterMidnight.contains("com.night.app"))

        // At end time (inclusive)
        val atEndTime = scheduler.getActiveBlockedPackagesAt(LocalTime.of(6, 0))
        assertTrue(atEndTime.contains("com.night.app"))

        // Outside overnight window (daytime)
        val daytime = scheduler.getActiveBlockedPackagesAt(LocalTime.of(12, 0))
        assertTrue(daytime.isEmpty())
    }

    @Test
    fun `getActiveBlockedPackagesAt should merge packages from multiple overlapping windows`() {
        val windows = listOf(
            BlockingWindow(
                id = "window-1",
                startTime = "08:00",
                endTime = "12:00",
                packageNames = listOf("com.app1", "com.app2")
            ),
            BlockingWindow(
                id = "window-2",
                startTime = "10:00",
                endTime = "14:00",
                packageNames = listOf("com.app2", "com.app3")
            )
        )

        scheduler.setSchedule(windows)
        val result = scheduler.getActiveBlockedPackagesAt(LocalTime.of(11, 0))

        assertEquals(3, result.size)
        assertTrue(result.contains("com.app1"))
        assertTrue(result.contains("com.app2"))
        assertTrue(result.contains("com.app3"))
    }

    @Test
    fun `getActiveBlockedPackagesAt should return correct packages when only one window is active`() {
        val windows = listOf(
            BlockingWindow(
                id = "morning",
                startTime = "08:00",
                endTime = "12:00",
                packageNames = listOf("com.morning.app")
            ),
            BlockingWindow(
                id = "afternoon",
                startTime = "14:00",
                endTime = "18:00",
                packageNames = listOf("com.afternoon.app")
            )
        )

        scheduler.setSchedule(windows)

        val morningResult = scheduler.getActiveBlockedPackagesAt(LocalTime.of(10, 0))
        assertEquals(1, morningResult.size)
        assertTrue(morningResult.contains("com.morning.app"))

        val afternoonResult = scheduler.getActiveBlockedPackagesAt(LocalTime.of(16, 0))
        assertEquals(1, afternoonResult.size)
        assertTrue(afternoonResult.contains("com.afternoon.app"))

        val lunchResult = scheduler.getActiveBlockedPackagesAt(LocalTime.of(13, 0))
        assertTrue(lunchResult.isEmpty())
    }

    // ========== isPackageBlocked Tests ==========

    @Test
    fun `isPackageBlocked should return true for blocked package`() {
        val windows = listOf(
            BlockingWindow(
                id = "all-day",
                startTime = "00:00",
                endTime = "23:59",
                packageNames = listOf("com.blocked.app")
            )
        )

        scheduler.setSchedule(windows)

        assertTrue(scheduler.isPackageBlocked("com.blocked.app"))
    }

    @Test
    fun `isPackageBlocked should return false for non-blocked package`() {
        val windows = listOf(
            BlockingWindow(
                id = "all-day",
                startTime = "00:00",
                endTime = "23:59",
                packageNames = listOf("com.blocked.app")
            )
        )

        scheduler.setSchedule(windows)

        assertFalse(scheduler.isPackageBlocked("com.allowed.app"))
    }

    // ========== isPackageBlockedAt Tests ==========

    @Test
    fun `isPackageBlockedAt should return true when package is in active window`() {
        val windows = listOf(
            BlockingWindow(
                id = "work-hours",
                startTime = "09:00",
                endTime = "17:00",
                packageNames = listOf("com.social.app", "com.game.app")
            )
        )

        scheduler.setSchedule(windows)

        assertTrue(scheduler.isPackageBlockedAt("com.social.app", LocalTime.of(12, 0)))
        assertTrue(scheduler.isPackageBlockedAt("com.game.app", LocalTime.of(12, 0)))
    }

    @Test
    fun `isPackageBlockedAt should return false when package is not in any window`() {
        val windows = listOf(
            BlockingWindow(
                id = "work-hours",
                startTime = "09:00",
                endTime = "17:00",
                packageNames = listOf("com.social.app")
            )
        )

        scheduler.setSchedule(windows)

        assertFalse(scheduler.isPackageBlockedAt("com.allowed.app", LocalTime.of(12, 0)))
    }

    @Test
    fun `isPackageBlockedAt should return false when time is outside window`() {
        val windows = listOf(
            BlockingWindow(
                id = "work-hours",
                startTime = "09:00",
                endTime = "17:00",
                packageNames = listOf("com.social.app")
            )
        )

        scheduler.setSchedule(windows)

        assertFalse(scheduler.isPackageBlockedAt("com.social.app", LocalTime.of(20, 0)))
    }

    @Test
    fun `isPackageBlockedAt should short-circuit and find package in first matching window`() {
        val windows = listOf(
            BlockingWindow(
                id = "window-1",
                startTime = "08:00",
                endTime = "12:00",
                packageNames = listOf("com.target.app")
            ),
            BlockingWindow(
                id = "window-2",
                startTime = "10:00",
                endTime = "14:00",
                packageNames = listOf("com.other.app")
            )
        )

        scheduler.setSchedule(windows)

        assertTrue(scheduler.isPackageBlockedAt("com.target.app", LocalTime.of(10, 0)))
    }

    // ========== Edge Cases ==========

    @Test
    fun `should handle empty schedule`() {
        scheduler.setSchedule(emptyList())
        val result = scheduler.getActiveBlockedPackagesAt(LocalTime.of(12, 0))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `should handle window with empty package list`() {
        val windows = listOf(
            BlockingWindow(
                id = "empty-packages",
                startTime = "09:00",
                endTime = "17:00",
                packageNames = emptyList()
            )
        )

        scheduler.setSchedule(windows)
        val result = scheduler.getActiveBlockedPackagesAt(LocalTime.of(12, 0))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `should handle midnight exactly in overnight window`() {
        val windows = listOf(
            BlockingWindow(
                id = "overnight",
                startTime = "22:00",
                endTime = "06:00",
                packageNames = listOf("com.night.app")
            )
        )

        scheduler.setSchedule(windows)
        val result = scheduler.getActiveBlockedPackagesAt(LocalTime.MIDNIGHT)

        assertTrue(result.contains("com.night.app"))
    }

    @Test
    fun `should handle window spanning entire day`() {
        val windows = listOf(
            BlockingWindow(
                id = "all-day",
                startTime = "00:00",
                endTime = "23:59",
                packageNames = listOf("com.always.blocked")
            )
        )

        scheduler.setSchedule(windows)

        val morning = scheduler.getActiveBlockedPackagesAt(LocalTime.of(6, 0))
        val afternoon = scheduler.getActiveBlockedPackagesAt(LocalTime.of(15, 0))
        val night = scheduler.getActiveBlockedPackagesAt(LocalTime.of(22, 0))
        val endOfDay = scheduler.getActiveBlockedPackagesAt(LocalTime.of(23, 59))

        assertTrue(morning.contains("com.always.blocked"))
        assertTrue(afternoon.contains("com.always.blocked"))
        assertTrue(night.contains("com.always.blocked"))
        assertTrue(endOfDay.contains("com.always.blocked"))
    }

    @Test
    fun `should handle same start and end time window`() {
        val windows = listOf(
            BlockingWindow(
                id = "point-block",
                startTime = "12:00",
                endTime = "12:00",
                packageNames = listOf("com.quick.app")
            )
        )

        scheduler.setSchedule(windows)

        val during = scheduler.getActiveBlockedPackagesAt(LocalTime.of(12, 0))
        val before = scheduler.getActiveBlockedPackagesAt(LocalTime.of(11, 59))
        val after = scheduler.getActiveBlockedPackagesAt(LocalTime.of(12, 1))

        assertTrue(during.contains("com.quick.app"))
        assertTrue(before.isEmpty())
        assertTrue(after.isEmpty())
    }
}
