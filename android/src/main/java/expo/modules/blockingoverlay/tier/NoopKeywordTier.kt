package expo.modules.blockingoverlay.tier

import android.util.Log

/**
 * No-op implementation of KeywordTier.
 * Placeholder until keyword blocking is implemented.
 */
class NoopKeywordTier : KeywordTier {

    companion object {
        private const val TAG = "NoopKeywordTier"
    }

    override suspend fun block(keywords: List<String>) {
        if (keywords.isNotEmpty()) {
            Log.d(TAG, "block() called with ${keywords.size} keywords (no-op)")
        }
    }

    override suspend fun unblockAll() {
        Log.d(TAG, "unblockAll() called (no-op)")
    }
}
