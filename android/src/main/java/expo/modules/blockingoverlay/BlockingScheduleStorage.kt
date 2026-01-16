package expo.modules.blockingoverlay

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * Storage for blocking schedule using SharedPreferences with JSON serialization.
 * Used by BlockingScheduler to persist blocking windows across app restarts.
 *
 * Includes in-memory caching to reduce SharedPreferences reads during frequent
 * accessibility event checks.
 *
 * **Cache Behavior Note:**
 * The cache is updated when [setSchedule] or [clearSchedule] is called from this process.
 * If SharedPreferences is modified externally (e.g., another process or direct file edit),
 * the cache will not reflect those changes until [invalidateCache] is called explicitly.
 *
 * **Thread Safety Note:**
 * The cache uses @Volatile for visibility across threads. Individual read/write operations
 * are thread-safe, but compound operations (read-then-write) are not atomic. For this app's
 * single-process use case, this is acceptable.
 */
object BlockingScheduleStorage {

    private const val TAG = "BlockingScheduleStorage"
    private const val PREFS_NAME = "tied_siren_blocking_schedule"
    private const val KEY_SCHEDULE = "blocking_schedule"

    // JSON keys for serialization
    private const val JSON_KEY_ID = "id"
    private const val JSON_KEY_START_TIME = "startTime"
    private const val JSON_KEY_END_TIME = "endTime"
    private const val JSON_KEY_PACKAGE_NAMES = "packageNames"

    @Volatile
    private var cachedSchedule: List<BlockingWindow>? = null

    fun setSchedule(context: Context, windows: List<BlockingWindow>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = serializeWindows(windows)
        prefs.edit()
            .putString(KEY_SCHEDULE, jsonString)
            .apply()

        cachedSchedule = ArrayList(windows)

        Log.d(TAG, "Schedule updated: ${windows.size} windows")
    }

    fun getSchedule(context: Context): List<BlockingWindow> {
        cachedSchedule?.let {
            Log.v(TAG, "Returning ${it.size} windows from cache")
            return it
        }

        Log.d(TAG, "Cache miss - reading schedule from SharedPreferences")
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_SCHEDULE, null)

        val windows = if (jsonString != null) {
            deserializeWindows(jsonString)
        } else {
            emptyList()
        }

        cachedSchedule = windows
        Log.d(TAG, "Loaded ${windows.size} windows from disk")

        return windows
    }

    fun clearSchedule(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_SCHEDULE)
            .apply()

        cachedSchedule = emptyList()

        Log.d(TAG, "Schedule cleared")
    }

    fun invalidateCache() {
        cachedSchedule = null
        Log.d(TAG, "Cache invalidated")
    }

    private fun serializeWindows(windows: List<BlockingWindow>): String {
        val jsonArray = JSONArray()
        for (window in windows) {
            val jsonObject = JSONObject().apply {
                put(JSON_KEY_ID, window.id)
                put(JSON_KEY_START_TIME, window.startTime)
                put(JSON_KEY_END_TIME, window.endTime)
                put(JSON_KEY_PACKAGE_NAMES, JSONArray(window.packageNames))
            }
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }

    private fun deserializeWindows(jsonString: String): List<BlockingWindow> {
        val windows = mutableListOf<BlockingWindow>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val packageNamesArray = jsonObject.getJSONArray(JSON_KEY_PACKAGE_NAMES)
                val packageNames = mutableListOf<String>()
                for (j in 0 until packageNamesArray.length()) {
                    packageNames.add(packageNamesArray.getString(j))
                }
                windows.add(
                    BlockingWindow(
                        id = jsonObject.getString(JSON_KEY_ID),
                        startTime = jsonObject.getString(JSON_KEY_START_TIME),
                        endTime = jsonObject.getString(JSON_KEY_END_TIME),
                        packageNames = packageNames
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to deserialize schedule, returning empty list: ${e.message}")
        }
        return windows
    }
}
