package expo.modules.blockingoverlay

import android.content.Context
import android.content.Intent
import android.util.Log
import expo.modules.accessibilityservice.MyAccessibilityService
import expo.modules.foregroundservice.ForegroundServiceCallback

/**
 * Native callback that bridges foreground service lifecycle to accessibility events.
 * Instantiated via reflection by expo-foreground-service.
 *
 * IMPORTANT: Must have public no-arg constructor for reflection.
 */
class BlockingCallback : ForegroundServiceCallback, MyAccessibilityService.EventListener {

    companion object {
        private const val TAG = "BlockingCallback"
    }

    private var applicationContext: Context? = null

    // ========== ForegroundServiceCallback ==========

    override fun onServiceStarted(context: Context) {
        applicationContext = context.applicationContext
        Log.d(TAG, "Service started, registering as accessibility listener")

        val registered = MyAccessibilityService.addEventListener(this)
        Log.d(TAG, "Accessibility listener registration: $registered")

        // Log current blocked apps for debugging
        val blockedApps = BlockedAppsStorage.getBlockedApps(context)
        Log.d(TAG, "Currently blocked apps: $blockedApps")
    }

    override fun onServiceStopped() {
        Log.d(TAG, "Service stopping, unregistering accessibility listener")

        val removed = MyAccessibilityService.removeEventListener(this)
        Log.d(TAG, "Accessibility listener removal: $removed")

        applicationContext = null
    }

    // ========== MyAccessibilityService.EventListener ==========

    override fun onAppChanged(packageName: String, className: String, timestamp: Long) {
        val context = applicationContext
        if (context == null) {
            Log.w(TAG, "onAppChanged called but context is null")
            return
        }

        val blockedApps = BlockedAppsStorage.getBlockedApps(context)

        if (blockedApps.contains(packageName)) {
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
            Log.d(TAG, "Overlay launched for: $packageName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch overlay: ${e.message}", e)
        }
    }
}
