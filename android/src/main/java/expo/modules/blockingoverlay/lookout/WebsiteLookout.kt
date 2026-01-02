package expo.modules.blockingoverlay.lookout

/**
 * Interface for website-specific detection.
 * Implementations detect when blocked websites are accessed.
 */
interface WebsiteLookout {
    /**
     * Callback invoked when a blocked website is detected.
     */
    fun interface OnWebsiteDetectedListener {
        fun onWebsiteDetected(domain: String, timestamp: Long)
    }

    /**
     * Start watching for website access.
     */
    fun startWatching()

    /**
     * Stop watching for website access.
     */
    fun stopWatching()

    /**
     * Set the listener for website detection events.
     */
    fun setOnWebsiteDetectedListener(listener: OnWebsiteDetectedListener?)
}
