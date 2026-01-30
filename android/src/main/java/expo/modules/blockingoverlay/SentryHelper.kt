package expo.modules.blockingoverlay

import android.util.Log

/**
 * Safe wrapper for Sentry calls.
 * Sentry is provided by the host app via @sentry/react-native.
 * If Sentry is not available, calls gracefully fall back to Android Log.
 */
object SentryHelper {

    private const val TAG = "SentryHelper"

    /**
     * Enable verbose breadcrumbs (includes detailed window info on every app change).
     * Set to false in production to reduce overhead.
     */
    @Volatile
    var verboseLogging: Boolean = false

    // Cache whether Sentry is available to avoid repeated reflection
    private val sentryAvailable: Boolean by lazy {
        try {
            Class.forName("io.sentry.Sentry")
            true
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "Sentry not available - breadcrumbs will only appear in logcat")
            false
        }
    }

    /**
     * Adds a verbose breadcrumb - only logged when verboseLogging is enabled.
     * Use for high-frequency events like onAppChanged to avoid overhead.
     */
    fun addVerboseBreadcrumb(category: String, message: String, dataProvider: () -> Map<String, Any>?) {
        if (!verboseLogging) return
        addBreadcrumb(category, message, dataProvider())
    }

    /**
     * Adds a breadcrumb to Sentry for debugging.
     * Falls back to Log.d if Sentry is not available.
     */
    fun addBreadcrumb(category: String, message: String, data: Map<String, Any>? = null) {
        // Always log to logcat for adb debugging
        val dataStr = data?.entries?.joinToString(", ") { "${it.key}=${it.value}" } ?: ""
        Log.d("[TSOB].$category", if (dataStr.isNotEmpty()) "$message | $dataStr" else message)

        if (!sentryAvailable) return

        try {
            val sentryClass = Class.forName("io.sentry.Sentry")
            val breadcrumbClass = Class.forName("io.sentry.Breadcrumb")

            // Create breadcrumb: Breadcrumb()
            val breadcrumb = breadcrumbClass.getDeclaredConstructor().newInstance()

            // Set category - prefixed with [TSOB] to distinguish from TiedSiren51
            breadcrumbClass.getMethod("setCategory", String::class.java)
                .invoke(breadcrumb, "[TSOB].$category")

            // Set message - also prefixed
            breadcrumbClass.getMethod("setMessage", String::class.java)
                .invoke(breadcrumb, "[TSOB] $message")

            // Set level to INFO
            val sentryLevelClass = Class.forName("io.sentry.SentryLevel")
            val infoLevel = sentryLevelClass.getField("INFO").get(null)
            breadcrumbClass.getMethod("setLevel", sentryLevelClass)
                .invoke(breadcrumb, infoLevel)

            // Add data if provided
            if (data != null) {
                val setDataMethod = breadcrumbClass.getMethod("setData", String::class.java, Any::class.java)
                data.forEach { (key, value) ->
                    setDataMethod.invoke(breadcrumb, key, value.toString())
                }
            }

            // Add breadcrumb: Sentry.addBreadcrumb(breadcrumb)
            sentryClass.getMethod("addBreadcrumb", breadcrumbClass)
                .invoke(null, breadcrumb)

        } catch (e: Exception) {
            Log.w(TAG, "Failed to add Sentry breadcrumb: ${e.message}")
        }
    }

    /**
     * Captures an error message to Sentry.
     */
    fun captureMessage(message: String, level: String = "error") {
        Log.e("[TSOB].Error", message)

        if (!sentryAvailable) return

        try {
            val sentryClass = Class.forName("io.sentry.Sentry")
            val sentryLevelClass = Class.forName("io.sentry.SentryLevel")

            val sentryLevel = when (level.lowercase()) {
                "warning" -> sentryLevelClass.getField("WARNING").get(null)
                "info" -> sentryLevelClass.getField("INFO").get(null)
                else -> sentryLevelClass.getField("ERROR").get(null)
            }

            sentryClass.getMethod("captureMessage", String::class.java, sentryLevelClass)
                .invoke(null, "[TSOB] $message", sentryLevel)

        } catch (e: Exception) {
            Log.w(TAG, "Failed to capture Sentry message: ${e.message}")
        }
    }

    /**
     * Captures an exception to Sentry.
     */
    fun captureException(throwable: Throwable) {
        Log.e("[TSOB].Exception", "[TSOB] Exception captured", throwable)

        if (!sentryAvailable) return

        try {
            val sentryClass = Class.forName("io.sentry.Sentry")
            sentryClass.getMethod("captureException", Throwable::class.java)
                .invoke(null, throwable)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to capture Sentry exception: ${e.message}")
        }
    }
}
