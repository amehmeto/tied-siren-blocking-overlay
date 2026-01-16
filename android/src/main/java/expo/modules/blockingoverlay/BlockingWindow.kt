package expo.modules.blockingoverlay

/**
 * Represents a time window during which specific apps should be blocked.
 *
 * @property id Unique identifier for this blocking window
 * @property startTime Start time in HH:mm format (24-hour)
 * @property endTime End time in HH:mm format (24-hour)
 * @property packageNames List of app package names to block during this window
 */
data class BlockingWindow(
    val id: String,
    val startTime: String,
    val endTime: String,
    val packageNames: List<String>
) {
    init {
        require(isValidTimeFormat(startTime)) { "startTime must be in HH:mm format" }
        require(isValidTimeFormat(endTime)) { "endTime must be in HH:mm format" }
    }

    companion object {
        private val TIME_PATTERN = Regex("^([01]\\d|2[0-3]):([0-5]\\d)$")

        fun isValidTimeFormat(time: String): Boolean {
            return TIME_PATTERN.matches(time)
        }
    }
}
