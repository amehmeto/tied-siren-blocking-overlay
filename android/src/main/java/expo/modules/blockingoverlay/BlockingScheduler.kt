package expo.modules.blockingoverlay

import android.content.Context
import android.util.Log
import java.time.LocalTime

/**
 * Manages blocking schedules and determines which apps should be blocked at any given time.
 *
 * The JavaScript layer calculates when blocking should activate based on user sessions
 * and blocklists, while this class independently stores and enforces the schedule on
 * the native layer.
 *
 * **Thread Safety Note:**
 * This class delegates storage to [BlockingScheduleStorage] which uses a volatile cache.
 * While individual operations are thread-safe, compound operations (e.g., read-modify-write)
 * are not atomic. For this app's single-process use case, this is acceptable.
 *
 * @property context Android context for accessing storage
 */
class BlockingScheduler(private val context: Context) {

    companion object {
        private const val TAG = "BlockingScheduler"
    }

    /**
     * Replaces the current blocking schedule with a new one.
     * This is called via the setBlockingSchedule() API.
     *
     * The schedule is stored in SharedPreferences and checked in real-time
     * by BlockingCallback whenever an app change is detected.
     *
     * @param windows The new list of blocking windows to enforce
     */
    fun setSchedule(windows: List<BlockingWindow>) {
        BlockingScheduleStorage.setSchedule(context, windows)
        Log.d(TAG, "Schedule updated with ${windows.size} windows")
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
     * After clearing, no apps will be blocked.
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
            if (window.isActiveAt(time)) {
                blockedPackages.addAll(window.packageNames)
            }
        }

        Log.v(TAG, "Active blocked packages at ${time.format(BlockingWindow.TIME_FORMATTER)}: ${blockedPackages.size}")
        return blockedPackages
    }

    /**
     * Checks if a specific package is currently blocked.
     * Uses short-circuit evaluation for efficiency.
     *
     * @param packageName The package name to check
     * @return true if the package is blocked at the current time
     */
    fun isPackageBlocked(packageName: String): Boolean {
        return isPackageBlockedAt(packageName, LocalTime.now())
    }

    /**
     * Checks if a specific package is blocked at a specific time.
     * Uses short-circuit evaluation - returns as soon as a matching window is found.
     *
     * @param packageName The package name to check
     * @param time The time to check
     * @return true if the package is blocked at the specified time
     */
    fun isPackageBlockedAt(packageName: String, time: LocalTime): Boolean {
        val schedule = BlockingScheduleStorage.getSchedule(context)

        for (window in schedule) {
            if (window.isActiveAt(time) && window.packageNames.contains(packageName)) {
                return true
            }
        }
        return false
    }
}
