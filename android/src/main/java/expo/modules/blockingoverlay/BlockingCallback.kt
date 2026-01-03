package expo.modules.blockingoverlay

import android.content.Context
import android.content.Intent
import android.util.Log
import expo.modules.accessibilityservice.AccessibilityService
import expo.modules.foregroundservice.ForegroundServiceCallback

/**
 * Native callback that bridges foreground service lifecycle to accessibility events.
 * Instantiated via reflection by expo-foreground-service.
 *
 * IMPORTANT: Must have public no-arg constructor for reflection.
 */
class BlockingCallback : ForegroundServiceCallback, AccessibilityService.EventListener {

    companion object {
        private const val TAG = "BlockingCallback"
        // Debounce interval to prevent rapid overlay launches (in milliseconds)
        private const val DEBOUNCE_INTERVAL_MS = 500L
    }

    // Volatile for thread safety - accessed from service thread and accessibility thread
    @Volatile
    private var applicationContext: Context? = null

    // Debouncing: track last overlay launch time and package
    @Volatile
    private var lastOverlayLaunchTime: Long = 0L
    @Volatile
    private var lastOverlayPackage: String? = null

    // ========== ForegroundServiceCallback ==========

    override fun onServiceStarted(context: Context) {
        applicationContext = context.applicationContext
        Log.d(TAG, "Service started, registering as accessibility listener")

        val registered = AccessibilityService.addEventListener(this)
        Log.d(TAG, "Accessibility listener registration: $registered")

        // Log current blocked apps for debugging
        val blockedApps = BlockedAppsStorage.getBlockedApps(context)
        Log.d(TAG, "Currently blocked apps: $blockedApps")
    }

    override fun onServiceStopped() {
        Log.d(TAG, "Service stopping, unregistering accessibility listener")

        val removed = AccessibilityService.removeEventListener(this)
        Log.d(TAG, "Accessibility listener removal: $removed")

        applicationContext = null
        lastOverlayLaunchTime = 0L
        lastOverlayPackage = null
    }

    // ========== AccessibilityService.EventListener ==========

    override fun onAppChanged(packageName: String, className: String, timestamp: Long) {
        val context = applicationContext
        if (context == null) {
            Log.w(TAG, "onAppChanged called but context is null")
            return
        }

        val blockedApps = BlockedAppsStorage.getBlockedApps(context)

        if (blockedApps.contains(packageName)) {
            // Debounce: skip if same package was blocked recently
            val now = System.currentTimeMillis()
            if (packageName == lastOverlayPackage &&
                (now - lastOverlayLaunchTime) < DEBOUNCE_INTERVAL_MS) {
                Log.d(TAG, "Debouncing overlay launch for: $packageName")
                return
            }

            Log.i(TAG, "BLOCKED app detected: $packageName - launching overlay")
            launchOverlay(context, packageName)
        } else {
            Log.v(TAG, "App changed: $packageName (not blocked)")
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
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch overlay: ${e.message}", e)
        }
    }
}
