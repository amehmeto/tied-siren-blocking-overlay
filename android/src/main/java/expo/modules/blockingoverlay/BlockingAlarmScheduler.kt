package expo.modules.blockingoverlay

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Schedules AlarmManager alarms for blocking window start/end times.
 *
 * Uses [AlarmManager.setExactAndAllowWhileIdle] for precise timing even in Doze mode.
 * Each blocking window gets two alarms: one for start time and one for end time.
 *
 * Request codes for PendingIntents are generated based on window ID hash to ensure
 * uniqueness and allow cancellation of specific alarms.
 *
 * **Alarm Persistence:**
 * Alarms are cleared when the device reboots. The [BlockingReceiver] handles
 * ACTION_BOOT_COMPLETED to reschedule alarms after reboot.
 */
object BlockingAlarmScheduler {

    private const val TAG = "BlockingAlarmScheduler"

    // Request code offset to differentiate start vs end alarms
    private const val START_ALARM_OFFSET = 0
    private const val END_ALARM_OFFSET = 1_000_000

    /**
     * Schedules alarms for all blocking windows in the current schedule.
     * Cancels any existing alarms before scheduling new ones.
     *
     * @param context Android context for AlarmManager access
     */
    fun scheduleAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val schedule = BlockingScheduleStorage.getSchedule(context)

        Log.d(TAG, "Scheduling alarms for ${schedule.size} windows")

        for (window in schedule) {
            scheduleWindowAlarms(context, alarmManager, window)
        }

        // Also update blocked apps immediately for current time
        updateBlockedAppsNow(context)
    }

    /**
     * Cancels all existing alarms and reschedules based on current schedule.
     * Called after device boot to restore alarms.
     *
     * @param context Android context for AlarmManager access
     */
    fun rescheduleAllAlarms(context: Context) {
        val schedule = BlockingScheduleStorage.getSchedule(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        Log.i(TAG, "Rescheduling all alarms (${schedule.size} windows)")

        // Cancel existing alarms first
        for (window in schedule) {
            cancelWindowAlarms(context, alarmManager, window)
        }

        // Schedule new alarms
        scheduleAlarms(context)
    }

    /**
     * Cancels all alarms for the given schedule and clears the blocked apps.
     *
     * @param context Android context for AlarmManager access
     * @param windows List of blocking windows to cancel alarms for
     */
    fun cancelAllAlarms(context: Context, windows: List<BlockingWindow>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        Log.d(TAG, "Canceling all alarms for ${windows.size} windows")

        for (window in windows) {
            cancelWindowAlarms(context, alarmManager, window)
        }
    }

    /**
     * Schedules start and end alarms for a single blocking window.
     */
    private fun scheduleWindowAlarms(
        context: Context,
        alarmManager: AlarmManager,
        window: BlockingWindow
    ) {
        val startTime = LocalTime.parse(window.startTime, BlockingWindow.TIME_FORMATTER)
        val endTime = LocalTime.parse(window.endTime, BlockingWindow.TIME_FORMATTER)

        val startAlarmTime = getNextAlarmTime(startTime)
        val endAlarmTime = getNextAlarmTime(endTime)

        val startIntent = createAlarmIntent(context, BlockingReceiver.ACTION_WINDOW_START, window.id)
        val endIntent = createAlarmIntent(context, BlockingReceiver.ACTION_WINDOW_END, window.id)

        val startPendingIntent = createPendingIntent(context, getStartRequestCode(window.id), startIntent)
        val endPendingIntent = createPendingIntent(context, getEndRequestCode(window.id), endIntent)

        val startMillis = startAlarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = endAlarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                startMillis,
                startPendingIntent
            )
            Log.d(TAG, "Scheduled start alarm for window ${window.id} at $startAlarmTime")

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                endMillis,
                endPendingIntent
            )
            Log.d(TAG, "Scheduled end alarm for window ${window.id} at $endAlarmTime")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to schedule exact alarm (missing SCHEDULE_EXACT_ALARM permission): ${e.message}")
        }
    }

    /**
     * Cancels both start and end alarms for a specific window.
     */
    private fun cancelWindowAlarms(
        context: Context,
        alarmManager: AlarmManager,
        window: BlockingWindow
    ) {
        val startIntent = createAlarmIntent(context, BlockingReceiver.ACTION_WINDOW_START, window.id)
        val endIntent = createAlarmIntent(context, BlockingReceiver.ACTION_WINDOW_END, window.id)

        val startPendingIntent = createPendingIntent(context, getStartRequestCode(window.id), startIntent)
        val endPendingIntent = createPendingIntent(context, getEndRequestCode(window.id), endIntent)

        alarmManager.cancel(startPendingIntent)
        alarmManager.cancel(endPendingIntent)

        Log.d(TAG, "Canceled alarms for window ${window.id}")
    }

    /**
     * Creates an Intent for the BlockingReceiver with the specified action.
     */
    private fun createAlarmIntent(context: Context, action: String, windowId: String): Intent {
        return Intent(context, BlockingReceiver::class.java).apply {
            this.action = action
            putExtra(BlockingReceiver.EXTRA_WINDOW_ID, windowId)
        }
    }

    /**
     * Creates a PendingIntent for the alarm.
     */
    private fun createPendingIntent(context: Context, requestCode: Int, intent: Intent): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    /**
     * Generates a unique request code for start alarms based on window ID.
     */
    private fun getStartRequestCode(windowId: String): Int {
        return (windowId.hashCode() and 0x7FFFFFFF) % END_ALARM_OFFSET + START_ALARM_OFFSET
    }

    /**
     * Generates a unique request code for end alarms based on window ID.
     */
    private fun getEndRequestCode(windowId: String): Int {
        return (windowId.hashCode() and 0x7FFFFFFF) % END_ALARM_OFFSET + END_ALARM_OFFSET
    }

    /**
     * Calculates the next occurrence of a given time.
     * If the time has already passed today, returns tomorrow's occurrence.
     */
    private fun getNextAlarmTime(time: LocalTime): LocalDateTime {
        val now = LocalDateTime.now()
        val todayAtTime = LocalDate.now().atTime(time)

        return if (todayAtTime.isAfter(now)) {
            todayAtTime
        } else {
            todayAtTime.plusDays(1)
        }
    }

    /**
     * Updates blocked apps immediately based on current time.
     * Called after scheduling to ensure blocking is applied right away if within a window.
     */
    private fun updateBlockedAppsNow(context: Context) {
        val scheduler = BlockingScheduler(context)
        val activePackages = scheduler.getActiveBlockedPackages()
        BlockedAppsStorage.setBlockedApps(context, activePackages)
        Log.d(TAG, "Updated blocked apps: ${activePackages.size} packages currently active")
    }
}
