package expo.modules.blockingoverlay.lookout

import android.util.Log

/**
 * No-op implementation of WebsiteLookout.
 * Placeholder until website detection is implemented.
 */
class NoopWebsiteLookout : WebsiteLookout {

    companion object {
        private const val TAG = "NoopWebsiteLookout"
    }

    override fun startWatching() {
        Log.d(TAG, "startWatching() called (no-op)")
    }

    override fun stopWatching() {
        Log.d(TAG, "stopWatching() called (no-op)")
    }

    override fun setOnWebsiteDetectedListener(listener: WebsiteLookout.OnWebsiteDetectedListener?) {
        Log.d(TAG, "setOnWebsiteDetectedListener() called (no-op)")
    }
}
