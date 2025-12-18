package expo.modules.blockingoverlay

import android.content.Context
import android.util.Log

/**
 * Helper for managing blocked apps in SharedPreferences.
 * Used by both BlockingCallback (native) and TiedSirenBlockingOverlayModule (JS bridge).
 */
object BlockedAppsStorage {

    private const val TAG = "BlockedAppsStorage"
    const val PREFS_NAME = "tied_siren_blocking"
    const val KEY_BLOCKED_APPS = "blocked_apps"

    fun setBlockedApps(context: Context, packageNames: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putStringSet(KEY_BLOCKED_APPS, packageNames)
            .apply()

        Log.d(TAG, "Blocked apps updated: ${packageNames.size} apps")
    }

    fun getBlockedApps(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_BLOCKED_APPS, emptySet()) ?: emptySet()
    }

    fun clearBlockedApps(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_BLOCKED_APPS)
            .apply()

        Log.d(TAG, "Blocked apps cleared")
    }
}
