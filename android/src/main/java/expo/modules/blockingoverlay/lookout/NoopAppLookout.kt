package expo.modules.blockingoverlay.lookout

import android.util.Log

/**
 * No-op implementation of AppLookout.
 * Placeholder for testing or when app detection is not needed.
 */
class NoopAppLookout : AppLookout {

    companion object {
        private const val TAG = "NoopAppLookout"
    }

    override fun startWatching() {
        Log.d(TAG, "startWatching() called (no-op)")
    }

    override fun stopWatching() {
        Log.d(TAG, "stopWatching() called (no-op)")
    }

    override fun setOnAppDetectedListener(listener: AppLookout.OnAppDetectedListener?) {
        Log.d(TAG, "setOnAppDetectedListener() called (no-op)")
    }
}
