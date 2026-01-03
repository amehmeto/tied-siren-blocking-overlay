package expo.modules.blockingoverlay.tier

import android.util.Log

/**
 * No-op implementation of WebsiteTier.
 * Placeholder until website blocking is implemented.
 */
class NoopWebsiteTier : WebsiteTier {

    companion object {
        private const val TAG = "NoopWebsiteTier"
    }

    override suspend fun block(domains: List<String>) {
        Log.d(TAG, "block() called with ${domains.size} domains (no-op)")
    }

    override suspend fun unblockAll() {
        Log.d(TAG, "unblockAll() called (no-op)")
    }
}
