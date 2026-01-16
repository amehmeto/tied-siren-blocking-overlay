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
 */
object BlockingScheduleStorage {

    private const val TAG = "BlockingScheduleStorage"
    private const val PREFS_NAME = "tied_siren_blocking_schedule"
    private const val KEY_SCHEDULE = "blocking_schedule"

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
                put("id", window.id)
                put("startTime", window.startTime)
                put("endTime", window.endTime)
                put("packageNames", JSONArray(window.packageNames))
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
                val packageNamesArray = jsonObject.getJSONArray("packageNames")
                val packageNames = mutableListOf<String>()
                for (j in 0 until packageNamesArray.length()) {
                    packageNames.add(packageNamesArray.getString(j))
                }
                windows.add(
                    BlockingWindow(
                        id = jsonObject.getString("id"),
                        startTime = jsonObject.getString("startTime"),
                        endTime = jsonObject.getString("endTime"),
                        packageNames = packageNames
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deserialize schedule: ${e.message}")
        }
        return windows
    }
}
