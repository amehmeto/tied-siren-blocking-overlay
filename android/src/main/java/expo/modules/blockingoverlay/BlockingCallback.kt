package expo.modules.blockingoverlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import expo.modules.accessibilityservice.AccessibilityService
import expo.modules.foregroundservice.ForegroundServiceCallback
import java.time.LocalTime

/**
 * Native callback that bridges foreground service lifecycle to accessibility events.
 * Instantiated via reflection by expo-foreground-service.
 *
 * Includes a watchdog mechanism that periodically verifies listener registration
 * and re-registers if Android killed/restarted the accessibility service.
 *
 * IMPORTANT: Must have public no-arg constructor for reflection.
 */
class BlockingCallback : ForegroundServiceCallback, AccessibilityService.EventListener {

    companion object {
        private const val TAG = "BlockingCallback"
        // Debounce interval to prevent rapid overlay launches (in milliseconds)
        private const val DEBOUNCE_INTERVAL_MS = 500L
        // Watchdog interval to verify listener registration (5 minutes)
        private const val WATCHDOG_INTERVAL_MS = 5L * 60L * 1000L
    }

    // Volatile for thread safety - accessed from service thread and accessibility thread
    @Volatile
    private var applicationContext: Context? = null

    // Debouncing: track last overlay launch time and package
    @Volatile
    private var lastOverlayLaunchTime: Long = 0L
    @Volatile
    private var lastOverlayPackage: String? = null

    // Watchdog runs on a dedicated background thread to avoid blocking the main thread
    private var watchdogThread: HandlerThread? = null
    private var watchdogHandler: Handler? = null

    // Broadcast receiver for accessibility service reconnection
    @Volatile
    private var serviceConnectionReceiver: BroadcastReceiver? = null
    @Volatile
    private var receiverContext: Context? = null

    private val watchdogRunnable: Runnable = object : Runnable {
        override fun run() {
            val ctx = applicationContext
            if (ctx == null) {
                // Context is null — service has stopped, let watchdog die intentionally
                Log.w(TAG, "Watchdog: context is null, stopping")
                return
            }

            checkAndRecoverListener()

            // Schedule next check
            watchdogHandler?.postDelayed(this, WATCHDOG_INTERVAL_MS)
        }
    }

    /**
     * Check if this callback is still registered as an accessibility listener
     * and re-register if it was dropped while the service is connected.
     */
    internal fun checkAndRecoverListener() {
        val isRegistered = AccessibilityService.hasListener(this)
        val listenerCount = AccessibilityService.getListenerCount()
        val isServiceConnected = AccessibilityService.isConnected

        Log.d(TAG, "Watchdog heartbeat: registered=$isRegistered, " +
            "listeners=$listenerCount, serviceConnected=$isServiceConnected")

        SentryHelper.addBreadcrumb("watchdog", "Heartbeat", mapOf(
            "registered" to isRegistered,
            "listenerCount" to listenerCount,
            "serviceConnected" to isServiceConnected
        ))

        if (!isRegistered && isServiceConnected) {
            Log.w(TAG, "Watchdog: listener not registered, re-registering")
            val registered = AccessibilityService.addEventListener(this)
            SentryHelper.addBreadcrumb("watchdog", "Re-registration", mapOf(
                "success" to registered
            ))
            if (!registered) {
                SentryHelper.captureMessage(
                    "Watchdog: listener re-registration failed", "error"
                )
            }
        } else if (!isRegistered) {
            Log.d(TAG, "Watchdog: listener not registered but service disconnected, skipping re-registration")
        }
    }

    /**
     * Handle accessibility service reconnection by checking and recovering
     * the listener registration if needed.
     */
    internal fun handleServiceReconnection() {
        Log.d(TAG, "Accessibility service reconnected broadcast received")

        val isRegistered = AccessibilityService.hasListener(this)
        if (!isRegistered) {
            Log.w(TAG, "Listener lost after service reconnect, re-registering")
            val registered = AccessibilityService.addEventListener(this)
            SentryHelper.addBreadcrumb("recovery", "Re-registered on reconnect", mapOf(
                "success" to registered
            ))
            if (!registered) {
                SentryHelper.captureMessage(
                    "Recovery: listener re-registration failed on reconnect", "error"
                )
            }
        } else {
            Log.d(TAG, "Listener still registered after service reconnect")
            SentryHelper.addBreadcrumb("recovery", "Listener intact on reconnect")
        }
    }

    // ========== ForegroundServiceCallback ==========

    override fun onServiceStarted(context: Context) {
        applicationContext = context.applicationContext
        Log.d(TAG, "Service started, registering as accessibility listener")

        val registered = AccessibilityService.addEventListener(this)
        Log.d(TAG, "Accessibility listener registration: $registered")

        // Register broadcast receiver for accessibility service reconnection
        registerServiceConnectionReceiver(context.applicationContext)

        // Start watchdog for periodic listener verification
        startWatchdog()

        // Log current schedule for debugging
        val schedule = BlockingScheduleStorage.getSchedule(context)
        Log.d(TAG, "Current schedule: ${schedule.size} windows configured")

        // Sentry breadcrumb for debugging
        val packageNames = schedule.flatMap { it.packageNames }.distinct()
        SentryHelper.addBreadcrumb("callback", "Service started", mapOf(
            "listenerRegistered" to registered,
            "windowCount" to schedule.size,
            "packageCount" to packageNames.size,
            "packages" to packageNames.joinToString(","),
            "watchdogStarted" to true
        ))

        if (!registered) {
            SentryHelper.captureMessage("AccessibilityService listener registration failed", "warning")
        }
    }

    override fun onServiceStopped() {
        Log.d(TAG, "Service stopping, unregistering accessibility listener")

        // Stop watchdog before unregistering
        stopWatchdog()

        // Unregister broadcast receiver
        unregisterServiceConnectionReceiver()

        val removed = AccessibilityService.removeEventListener(this)
        Log.d(TAG, "Accessibility listener removal: $removed")

        SentryHelper.addBreadcrumb("callback", "Service stopped", mapOf(
            "listenerRemoved" to removed
        ))

        applicationContext = null
        lastOverlayLaunchTime = 0L
        lastOverlayPackage = null
    }

    // ========== Watchdog & Recovery ==========

    private fun startWatchdog() {
        stopWatchdog()
        val thread = HandlerThread("BlockingCallback-Watchdog").apply { start() }
        watchdogThread = thread
        val handler = Handler(thread.looper)
        watchdogHandler = handler
        handler.postDelayed(watchdogRunnable, WATCHDOG_INTERVAL_MS)
        Log.d(TAG, "Watchdog started (interval=${WATCHDOG_INTERVAL_MS}ms)")
    }

    private fun stopWatchdog() {
        watchdogHandler?.removeCallbacks(watchdogRunnable)
        watchdogThread?.quitSafely()
        watchdogThread = null
        watchdogHandler = null
        Log.d(TAG, "Watchdog stopped")
    }

    private fun registerServiceConnectionReceiver(context: Context) {
        synchronized(this) {
            if (serviceConnectionReceiver != null) return

            serviceConnectionReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    handleServiceReconnection()
                }
            }

            receiverContext = context

            val filter = IntentFilter(AccessibilityService.ACTION_SERVICE_CONNECTED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    serviceConnectionReceiver, filter, Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                context.registerReceiver(serviceConnectionReceiver, filter)
            }

            Log.d(TAG, "Service connection receiver registered")
        }
    }

    private fun unregisterServiceConnectionReceiver() {
        synchronized(this) {
            serviceConnectionReceiver?.let { receiver ->
                try {
                    receiverContext?.unregisterReceiver(receiver)
                    Log.d(TAG, "Service connection receiver unregistered")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to unregister receiver: ${e.message}")
                }
            }
            serviceConnectionReceiver = null
            receiverContext = null
        }
    }

    // ========== AccessibilityService.EventListener ==========

    /**
     * Called when the foreground app changes.
     * Checks if the package is blocked in any currently active time window.
     *
     * #18: Uses schedule-based blocking (checks time windows) instead of
     * simple blocked apps list.
     */
    override fun onAppChanged(packageName: String, className: String, timestamp: Long) {
        val context = applicationContext
        if (context == null) {
            Log.w(TAG, "onAppChanged called but context is null")
            SentryHelper.addBreadcrumb("callback", "onAppChanged - context null", mapOf(
                "packageName" to packageName
            ))
            return
        }

        // Check if package should be blocked based on schedule + current time
        val schedule = BlockingScheduleStorage.getSchedule(context)
        val now = LocalTime.now()
        val activeWindowCount = schedule.count { it.isActiveAt(now) }
        Log.v(TAG, "Schedule check: ${schedule.size} windows, $activeWindowCount active at $now")

        // Find which windows contain this package and their active status
        val matchingWindows = schedule.filter { it.packageNames.contains(packageName) }
        val activeMatchingWindows = matchingWindows.filter { it.isActiveAt(now) }

        val shouldBlock = activeMatchingWindows.isNotEmpty()

        // Verbose breadcrumb for debugging - only logged when SentryHelper.verboseLogging is true
        // Uses lazy data provider to avoid string building overhead in production
        SentryHelper.addVerboseBreadcrumb("callback", "onAppChanged") {
            mapOf(
                "packageName" to packageName,
                "currentTime" to now.toString(),
                "totalWindows" to schedule.size,
                "activeWindows" to activeWindowCount,
                "matchingWindows" to matchingWindows.size,
                "activeMatchingWindows" to activeMatchingWindows.size,
                "shouldBlock" to shouldBlock,
                "windowDetails" to schedule.map { w ->
                    "${w.id}:${w.startTime}-${w.endTime}:active=${w.isActiveAt(now)}:hasPackage=${w.packageNames.contains(packageName)}"
                }.joinToString("; ")
            )
        }

        if (shouldBlock) {
            // Debounce: skip if same package was blocked recently
            val currentTime = System.currentTimeMillis()
            if (packageName == lastOverlayPackage &&
                (currentTime - lastOverlayLaunchTime) < DEBOUNCE_INTERVAL_MS) {
                Log.d(TAG, "Debouncing overlay launch for: $packageName")
                SentryHelper.addBreadcrumb("callback", "Debounced", mapOf(
                    "packageName" to packageName
                ))
                return
            }

            Log.i(TAG, "BLOCKED app detected: $packageName (in active window at $now) - launching overlay")
            SentryHelper.addBreadcrumb("callback", "BLOCKING - launching overlay", mapOf(
                "packageName" to packageName,
                "time" to now.toString()
            ))
            launchOverlay(context, packageName)
        } else {
            Log.v(TAG, "App changed: $packageName (not blocked or outside active windows)")
        }
    }

    // ========== Private Helpers ==========

    private fun launchOverlay(context: Context, packageName: String) {
        try {
            val intent = Intent(context, BlockingOverlayActivity::class.java).apply {
                putExtra(BlockingOverlayActivity.EXTRA_PACKAGE_NAME, packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_NO_HISTORY
            }
            context.startActivity(intent)

            // Update debounce tracking
            lastOverlayLaunchTime = System.currentTimeMillis()
            lastOverlayPackage = packageName

            Log.d(TAG, "Overlay launched for: $packageName")
            SentryHelper.addBreadcrumb("callback", "Overlay launched successfully", mapOf(
                "packageName" to packageName
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch overlay: ${e.message}", e)
            SentryHelper.captureException(e)
            SentryHelper.addBreadcrumb("callback", "Overlay launch FAILED", mapOf(
                "packageName" to packageName,
                "error" to (e.message ?: "unknown")
            ))
        }
    }
}
