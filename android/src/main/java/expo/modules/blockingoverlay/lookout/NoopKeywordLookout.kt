package expo.modules.blockingoverlay.lookout

import android.util.Log

/**
 * No-op implementation of KeywordLookout.
 * Placeholder until keyword detection is implemented.
 */
class NoopKeywordLookout : KeywordLookout {

    companion object {
        private const val TAG = "NoopKeywordLookout"
    }

    override fun startWatching() {
        Log.d(TAG, "startWatching() called (no-op)")
    }

    override fun stopWatching() {
        Log.d(TAG, "stopWatching() called (no-op)")
    }

    override fun setOnKeywordDetectedListener(listener: KeywordLookout.OnKeywordDetectedListener?) {
        Log.d(TAG, "setOnKeywordDetectedListener() called (no-op)")
    }
}
