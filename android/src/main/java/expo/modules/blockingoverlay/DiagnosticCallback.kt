package expo.modules.blockingoverlay

import android.content.Context
import android.util.Log
import android.widget.Toast
import expo.modules.foregroundservice.ForegroundServiceCallback

/**
 * Simple diagnostic callback to verify reflection-based DI is working.
 * Register with: setCallbackClass("expo.modules.blockingoverlay.DiagnosticCallback")
 *
 * If working, you'll see:
 * - Toast on device: "TSOB DiagnosticCallback: Service started!"
 * - Logcat: [TSOB].Diagnostic: Service started
 * - Sentry breadcrumb: [TSOB].diagnostic: DI test - service started
 */
class DiagnosticCallback : ForegroundServiceCallback {

    companion object {
        private const val TAG = "[TSOB].Diagnostic"
        const val CLASS_NAME = "expo.modules.blockingoverlay.DiagnosticCallback"
    }

    override fun onServiceStarted(context: Context) {
        Log.i(TAG, "✅ Service started - reflection DI is WORKING!")

        // Sentry breadcrumb
        SentryHelper.addBreadcrumb("diagnostic", "DI test - service started", mapOf(
            "callbackClass" to CLASS_NAME,
            "contextAvailable" to (context != null),
            "timestamp" to System.currentTimeMillis()
        ))

        // Also capture as a message so it's very visible in Sentry
        SentryHelper.captureMessage("DiagnosticCallback.onServiceStarted() called - DI working!", "info")

        // Toast for immediate visual feedback on device
        try {
            Toast.makeText(
                context.applicationContext,
                "TSOB DiagnosticCallback: Service started!",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Log.w(TAG, "Could not show toast: ${e.message}")
        }
    }

    override fun onServiceStopped() {
        Log.i(TAG, "Service stopped")
        SentryHelper.addBreadcrumb("diagnostic", "DI test - service stopped")
    }
}
