package expo.modules.blockingoverlay

import android.content.Context
import android.util.Log
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Manages blocking schedules and determines which apps should be blocked at any given time.
 *
 * The JavaScript layer calculates when blocking should activate based on user sessions
 * and blocklists, while this class independently stores and enforces the schedule on
 * the native layer.
 *
 * @property context Android context for accessing storage
 */
class BlockingScheduler(private val context: Context) {

    companion object {
        private const val TAG = "BlockingScheduler"
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
    }

    /**
     * Replaces the current blocking schedule with a new one.
     * This is called via the setBlockingSchedule() API.
     *
     * @param windows The new list of blocking windows to enforce
     */
    fun setSchedule(windows: List<BlockingWindow>) {
        BlockingScheduleStorage.setSchedule(context, windows)
        Log.d(TAG, "Schedule replaced with ${windows.size} windows")
    }

    /**
     * Retrieves the current blocking schedule.
     *
     * @return The list of all blocking windows
     */
    fun getSchedule(): List<BlockingWindow> {
        return BlockingScheduleStorage.getSchedule(context)
    }

    /**
     * Clears all blocking windows from the schedule.
     */
    fun clearSchedule() {
        BlockingScheduleStorage.clearSchedule(context)
        Log.d(TAG, "Schedule cleared")
    }

    /**
     * Gets the set of package names that should be blocked at the current time.
     *
     * @return Set of package names to block right now
     */
    fun getActiveBlockedPackages(): Set<String> {
        return getActiveBlockedPackagesAt(LocalTime.now())
    }

    /**
     * Gets the set of package names that should be blocked at a specific time.
     * This is useful for testing and for scheduling future blocks.
     *
     * @param time The time to check for active blocking windows
     * @return Set of package names to block at the specified time
     */
    fun getActiveBlockedPackagesAt(time: LocalTime): Set<String> {
        val schedule = BlockingScheduleStorage.getSchedule(context)
        val blockedPackages = mutableSetOf<String>()

        for (window in schedule) {
            if (isTimeInWindow(time, window)) {
                blockedPackages.addAll(window.packageNames)
            }
        }

        Log.v(TAG, "Active blocked packages at ${time.format(TIME_FORMATTER)}: ${blockedPackages.size}")
        return blockedPackages
    }

    /**
     * Checks if the specified time falls within the given blocking window.
     * Handles overnight windows (e.g., 23:00 to 02:00) correctly.
     *
     * @param time The time to check
     * @param window The blocking window to check against
     * @return true if the time is within the window
     */
    private fun isTimeInWindow(time: LocalTime, window: BlockingWindow): Boolean {
        val startTime = LocalTime.parse(window.startTime, TIME_FORMATTER)
        val endTime = LocalTime.parse(window.endTime, TIME_FORMATTER)

        return if (startTime <= endTime) {
            // Normal window (e.g., 09:00 to 17:00)
            time >= startTime && time < endTime
        } else {
            // Overnight window (e.g., 23:00 to 02:00)
            time >= startTime || time < endTime
        }
    }

    /**
     * Checks if a specific package is currently blocked.
     *
     * @param packageName The package name to check
     * @return true if the package is blocked at the current time
     */
    fun isPackageBlocked(packageName: String): Boolean {
        return getActiveBlockedPackages().contains(packageName)
    }
}
