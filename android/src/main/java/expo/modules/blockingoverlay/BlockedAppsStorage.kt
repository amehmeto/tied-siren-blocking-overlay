package expo.modules.blockingoverlay

import android.content.Context
import android.util.Log

/**
 * Helper for managing blocked apps in SharedPreferences.
 * Used by both BlockingCallback (native) and TiedSirenBlockingOverlayModule (JS bridge).
 *
 * Includes in-memory caching to reduce SharedPreferences reads during frequent
 * accessibility event checks.
 *
 * **Cache Behavior Note:**
 * The cache is updated when [setBlockedApps] or [clearBlockedApps] is called from this process.
 * If SharedPreferences is modified externally (e.g., another process or direct file edit),
 * the cache will not reflect those changes until [invalidateCache] is called explicitly.
 * For this app's use case (single-process blocking), this is acceptable.
 */
object BlockedAppsStorage {

    private const val TAG = "BlockedAppsStorage"
    const val PREFS_NAME = "tied_siren_blocking"
    const val KEY_BLOCKED_APPS = "blocked_apps"

    // In-memory cache to reduce SharedPreferences reads
    // Volatile for thread safety since this is accessed from multiple threads
    @Volatile
    private var cachedBlockedApps: Set<String>? = null

    fun setBlockedApps(context: Context, packageNames: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putStringSet(KEY_BLOCKED_APPS, packageNames)
            .apply()

        // Update cache with a defensive copy
        cachedBlockedApps = HashSet(packageNames)

        Log.d(TAG, "Blocked apps updated: ${packageNames.size} apps")
    }

    fun getBlockedApps(context: Context): Set<String> {
        // Return cached value if available
        cachedBlockedApps?.let {
            Log.v(TAG, "Returning ${it.size} blocked apps from cache")
            return it
        }

        Log.d(TAG, "Cache miss - reading blocked apps from SharedPreferences")
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Create a defensive copy to avoid mutation issues with SharedPreferences' internal set
        val apps = HashSet(prefs.getStringSet(KEY_BLOCKED_APPS, emptySet()) ?: emptySet())

        // Update cache
        cachedBlockedApps = apps
        Log.d(TAG, "Loaded ${apps.size} blocked apps from disk")

        return apps
    }

    fun clearBlockedApps(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_BLOCKED_APPS)
            .apply()

        // Clear cache
        cachedBlockedApps = emptySet()

        Log.d(TAG, "Blocked apps cleared")
    }

    /**
     * Invalidates the in-memory cache, forcing the next read to fetch from SharedPreferences.
     * Useful if blocked apps may have been modified externally.
     */
    fun invalidateCache() {
        cachedBlockedApps = null
        Log.d(TAG, "Cache invalidated")
    }
}
