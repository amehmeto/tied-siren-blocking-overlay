package expo.modules.blockingoverlay

import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Represents a time window during which specific apps should be blocked.
 *
 * @property id Unique identifier for this blocking window
 * @property startTime Start time in HH:mm format (24-hour), inclusive
 * @property endTime End time in HH:mm format (24-hour), inclusive
 * @property packageNames List of app package names to block during this window
 */
data class BlockingWindow(
    val id: String,
    val startTime: String,
    val endTime: String,
    val packageNames: List<String>
) {
    init {
        require(id.isNotBlank()) { "id must not be blank" }
        require(isValidTimeFormat(startTime)) { "startTime must be in HH:mm format" }
        require(isValidTimeFormat(endTime)) { "endTime must be in HH:mm format" }
    }

    /**
     * Checks if this blocking window is active at the specified time.
     * Both start and end times are inclusive, so a window of 09:00-17:00
     * includes both 09:00 and 17:00.
     *
     * Handles overnight windows (e.g., 22:00 to 06:00) correctly.
     *
     * @param time The time to check
     * @return true if this window is active at the specified time
     */
    fun isActiveAt(time: LocalTime): Boolean {
        val start = LocalTime.parse(startTime, TIME_FORMATTER)
        val end = LocalTime.parse(endTime, TIME_FORMATTER)

        return if (start <= end) {
            // Normal window (e.g., 09:00 to 17:00) - both inclusive
            time >= start && time <= end
        } else {
            // Overnight window (e.g., 22:00 to 06:00) - both inclusive
            time >= start || time <= end
        }
    }

    companion object {
        internal val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        private val TIME_PATTERN = Regex("^([01]\\d|2[0-3]):([0-5]\\d)$")

        fun isValidTimeFormat(time: String): Boolean {
            return TIME_PATTERN.matches(time)
        }
    }
}
