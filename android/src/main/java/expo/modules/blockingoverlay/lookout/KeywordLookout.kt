package expo.modules.blockingoverlay.lookout

/**
 * Interface for keyword-specific detection.
 * Implementations detect when blocked keywords appear in content.
 */
interface KeywordLookout {
    /**
     * Callback invoked when a blocked keyword is detected.
     */
    fun interface OnKeywordDetectedListener {
        fun onKeywordDetected(keyword: String, context: String, timestamp: Long)
    }

    /**
     * Start watching for keyword matches.
     */
    fun startWatching()

    /**
     * Stop watching for keyword matches.
     */
    fun stopWatching()

    /**
     * Set the listener for keyword detection events.
     */
    fun setOnKeywordDetectedListener(listener: OnKeywordDetectedListener?)
}
