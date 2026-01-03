package expo.modules.blockingoverlay.lookout

/**
 * Interface for app-specific detection.
 * Implementations detect when blocked apps are launched.
 */
interface AppLookout {
    /**
     * Callback invoked when a blocked app is detected.
     */
    fun interface OnAppDetectedListener {
        fun onAppDetected(packageName: String, timestamp: Long)
    }

    /**
     * Start watching for app launches.
     */
    fun startWatching()

    /**
     * Stop watching for app launches.
     */
    fun stopWatching()

    /**
     * Set the listener for app detection events.
     */
    fun setOnAppDetectedListener(listener: OnAppDetectedListener?)
}
