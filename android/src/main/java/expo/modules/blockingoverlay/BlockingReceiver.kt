package expo.modules.blockingoverlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.time.LocalTime

/**
 * BroadcastReceiver that handles blocking schedule alarm callbacks.
 *
 * This receiver is triggered by:
 * - AlarmManager alarms at blocking window start/end times
 * - Device reboot (ACTION_BOOT_COMPLETED) to reschedule alarms
 *
 * On each alarm callback, it recalculates the active blocked packages and updates
 * [BlockedAppsStorage], which the accessibility service checks during app detection.
 */
class BlockingReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BlockingReceiver"

        const val ACTION_WINDOW_START = "expo.modules.blockingoverlay.ACTION_WINDOW_START"
        const val ACTION_WINDOW_END = "expo.modules.blockingoverlay.ACTION_WINDOW_END"
        const val EXTRA_WINDOW_ID = "window_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received intent: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> handleBootCompleted(context)
            ACTION_WINDOW_START -> handleWindowStart(context, intent)
            ACTION_WINDOW_END -> handleWindowEnd(context, intent)
            else -> Log.w(TAG, "Unknown action: ${intent.action}")
        }
    }

    /**
     * Handles device boot by rescheduling all alarms.
     * Alarms are cleared on device shutdown, so we need to reschedule them.
     */
    private fun handleBootCompleted(context: Context) {
        Log.i(TAG, "Device booted - rescheduling blocking alarms")

        // Also update blocked apps immediately based on current time
        updateBlockedApps(context)

        // Reschedule all alarms
        BlockingAlarmScheduler.rescheduleAllAlarms(context)
    }

    /**
     * Handles a blocking window start event.
     * Updates the active blocked packages to include apps from this window.
     */
    private fun handleWindowStart(context: Context, intent: Intent) {
        val windowId = intent.getStringExtra(EXTRA_WINDOW_ID)
        Log.i(TAG, "Window started: $windowId")

        updateBlockedApps(context)
    }

    /**
     * Handles a blocking window end event.
     * Recalculates active blocked packages (may still include apps from overlapping windows).
     */
    private fun handleWindowEnd(context: Context, intent: Intent) {
        val windowId = intent.getStringExtra(EXTRA_WINDOW_ID)
        Log.i(TAG, "Window ended: $windowId")

        updateBlockedApps(context)
    }

    /**
     * Recalculates and updates the set of blocked packages based on current time.
     * Uses [BlockingScheduler] to determine which windows are active and merges
     * their package lists.
     */
    private fun updateBlockedApps(context: Context) {
        val scheduler = BlockingScheduler(context)
        val currentTime = LocalTime.now()
        val activePackages = scheduler.getActiveBlockedPackagesAt(currentTime)

        Log.d(TAG, "Updating blocked apps at $currentTime: ${activePackages.size} packages")
        BlockedAppsStorage.setBlockedApps(context, activePackages)
    }
}
