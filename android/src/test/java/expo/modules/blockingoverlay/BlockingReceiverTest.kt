package expo.modules.blockingoverlay

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class BlockingReceiverTest {

    private lateinit var context: Context
    private lateinit var receiver: BlockingReceiver

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        BlockingScheduleStorage.invalidateCache()
        BlockedAppsStorage.invalidateCache()
        BlockingScheduleStorage.clearSchedule(context)
        BlockedAppsStorage.clearBlockedApps(context)

        receiver = BlockingReceiver()
    }

    // ========== Intent Action Tests ==========

    @Test
    fun `should handle ACTION_WINDOW_START intent`() {
        val intent = Intent(BlockingReceiver.ACTION_WINDOW_START).apply {
            putExtra(BlockingReceiver.EXTRA_WINDOW_ID, "test-window-1")
        }

        // Should not throw
        receiver.onReceive(context, intent)
    }

    @Test
    fun `should handle ACTION_WINDOW_END intent`() {
        val intent = Intent(BlockingReceiver.ACTION_WINDOW_END).apply {
            putExtra(BlockingReceiver.EXTRA_WINDOW_ID, "test-window-1")
        }

        // Should not throw
        receiver.onReceive(context, intent)
    }

    @Test
    fun `should handle ACTION_BOOT_COMPLETED intent`() {
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        // Should not throw
        receiver.onReceive(context, intent)
    }

    @Test
    fun `should handle unknown action gracefully`() {
        val intent = Intent("com.unknown.action")

        // Should not throw
        receiver.onReceive(context, intent)
    }

    // ========== Blocked Apps Update Tests ==========

    @Test
    fun `ACTION_WINDOW_START should trigger blocked apps update`() {
        // Set up a schedule with an active window (all day)
        val scheduler = BlockingScheduler(context)
        scheduler.setSchedule(listOf(
            BlockingWindow(
                id = "work-hours",
                startTime = "00:00",
                endTime = "23:59",
                packageNames = listOf("com.blocked.app")
            )
        ))

        val intent = Intent(BlockingReceiver.ACTION_WINDOW_START).apply {
            putExtra(BlockingReceiver.EXTRA_WINDOW_ID, "work-hours")
        }

        receiver.onReceive(context, intent)

        // Verify that blocked apps storage was updated
        val blockedApps = BlockedAppsStorage.getBlockedApps(context)
        assertTrue(blockedApps.contains("com.blocked.app"))
    }

    @Test
    fun `ACTION_WINDOW_END should trigger blocked apps update`() {
        // Set up an empty schedule (no active windows)
        val scheduler = BlockingScheduler(context)
        scheduler.clearSchedule()

        val intent = Intent(BlockingReceiver.ACTION_WINDOW_END).apply {
            putExtra(BlockingReceiver.EXTRA_WINDOW_ID, "work-hours")
        }

        receiver.onReceive(context, intent)

        // Verify that blocked apps storage is empty
        val blockedApps = BlockedAppsStorage.getBlockedApps(context)
        assertTrue(blockedApps.isEmpty())
    }

    @Test
    fun `ACTION_BOOT_COMPLETED should update blocked apps based on current schedule`() {
        // Set up a schedule with an active window (all day)
        val scheduler = BlockingScheduler(context)
        scheduler.setSchedule(listOf(
            BlockingWindow(
                id = "test",
                startTime = "00:00",
                endTime = "23:59",
                packageNames = listOf("com.always.blocked")
            )
        ))

        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        receiver.onReceive(context, intent)

        // Verify blocked apps were updated correctly
        val blockedApps = BlockedAppsStorage.getBlockedApps(context)
        assertTrue(blockedApps.contains("com.always.blocked"))
    }

    @Test
    fun `ACTION_BOOT_COMPLETED should clear blocked apps when no schedule`() {
        // Ensure no schedule exists
        val scheduler = BlockingScheduler(context)
        scheduler.clearSchedule()

        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        receiver.onReceive(context, intent)

        // Verify blocked apps are empty
        val blockedApps = BlockedAppsStorage.getBlockedApps(context)
        assertTrue(blockedApps.isEmpty())
    }

    // ========== Constants Tests ==========

    @Test
    fun `ACTION_WINDOW_START constant should match expected value`() {
        assertEquals(
            "expo.modules.blockingoverlay.ACTION_WINDOW_START",
            BlockingReceiver.ACTION_WINDOW_START
        )
    }

    @Test
    fun `ACTION_WINDOW_END constant should match expected value`() {
        assertEquals(
            "expo.modules.blockingoverlay.ACTION_WINDOW_END",
            BlockingReceiver.ACTION_WINDOW_END
        )
    }

    @Test
    fun `EXTRA_WINDOW_ID constant should match expected value`() {
        assertEquals("window_id", BlockingReceiver.EXTRA_WINDOW_ID)
    }
}
