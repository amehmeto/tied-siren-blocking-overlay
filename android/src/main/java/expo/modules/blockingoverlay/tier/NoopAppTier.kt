package expo.modules.blockingoverlay.tier

import android.util.Log

/**
 * No-op implementation of AppTier.
 * Placeholder for testing or when app blocking is not needed.
 */
class NoopAppTier : AppTier {

    companion object {
        private const val TAG = "NoopAppTier"
    }

    override suspend fun block(packages: List<String>) {
        Log.d(TAG, "block() called with ${packages.size} packages (no-op)")
    }

    override suspend fun unblockAll() {
        Log.d(TAG, "unblockAll() called (no-op)")
    }
}
