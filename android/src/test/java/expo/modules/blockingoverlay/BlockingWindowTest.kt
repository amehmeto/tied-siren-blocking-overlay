package expo.modules.blockingoverlay

import org.junit.Test
import org.junit.Assert.*

class BlockingWindowTest {

    @Test
    fun `should create valid blocking window`() {
        val window = BlockingWindow(
            id = "window-1",
            startTime = "09:00",
            endTime = "17:00",
            packageNames = listOf("com.example.app1", "com.example.app2")
        )

        assertEquals("window-1", window.id)
        assertEquals("09:00", window.startTime)
        assertEquals("17:00", window.endTime)
        assertEquals(2, window.packageNames.size)
        assertTrue(window.packageNames.contains("com.example.app1"))
    }

    @Test
    fun `should accept midnight times`() {
        val window = BlockingWindow(
            id = "midnight",
            startTime = "00:00",
            endTime = "23:59",
            packageNames = listOf("com.example.app")
        )

        assertEquals("00:00", window.startTime)
        assertEquals("23:59", window.endTime)
    }

    @Test
    fun `should accept overnight window times`() {
        val window = BlockingWindow(
            id = "overnight",
            startTime = "22:00",
            endTime = "06:00",
            packageNames = listOf("com.example.app")
        )

        assertEquals("22:00", window.startTime)
        assertEquals("06:00", window.endTime)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should reject invalid start time format`() {
        BlockingWindow(
            id = "invalid",
            startTime = "9:00",
            endTime = "17:00",
            packageNames = emptyList()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should reject invalid end time format`() {
        BlockingWindow(
            id = "invalid",
            startTime = "09:00",
            endTime = "5:00",
            packageNames = emptyList()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should reject hours greater than 23`() {
        BlockingWindow(
            id = "invalid",
            startTime = "24:00",
            endTime = "17:00",
            packageNames = emptyList()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should reject minutes greater than 59`() {
        BlockingWindow(
            id = "invalid",
            startTime = "09:60",
            endTime = "17:00",
            packageNames = emptyList()
        )
    }

    @Test
    fun `should accept empty package list`() {
        val window = BlockingWindow(
            id = "empty",
            startTime = "09:00",
            endTime = "17:00",
            packageNames = emptyList()
        )

        assertTrue(window.packageNames.isEmpty())
    }

    @Test
    fun `isValidTimeFormat should return true for valid times`() {
        assertTrue(BlockingWindow.isValidTimeFormat("00:00"))
        assertTrue(BlockingWindow.isValidTimeFormat("12:30"))
        assertTrue(BlockingWindow.isValidTimeFormat("23:59"))
        assertTrue(BlockingWindow.isValidTimeFormat("09:05"))
    }

    @Test
    fun `isValidTimeFormat should return false for invalid times`() {
        assertFalse(BlockingWindow.isValidTimeFormat("9:00"))
        assertFalse(BlockingWindow.isValidTimeFormat("24:00"))
        assertFalse(BlockingWindow.isValidTimeFormat("12:60"))
        assertFalse(BlockingWindow.isValidTimeFormat("1200"))
        assertFalse(BlockingWindow.isValidTimeFormat("12-00"))
        assertFalse(BlockingWindow.isValidTimeFormat(""))
        assertFalse(BlockingWindow.isValidTimeFormat("ab:cd"))
    }

    @Test
    fun `data class should implement equals correctly`() {
        val window1 = BlockingWindow(
            id = "window-1",
            startTime = "09:00",
            endTime = "17:00",
            packageNames = listOf("com.example.app")
        )
        val window2 = BlockingWindow(
            id = "window-1",
            startTime = "09:00",
            endTime = "17:00",
            packageNames = listOf("com.example.app")
        )

        assertEquals(window1, window2)
    }

    @Test
    fun `data class should implement copy correctly`() {
        val window1 = BlockingWindow(
            id = "window-1",
            startTime = "09:00",
            endTime = "17:00",
            packageNames = listOf("com.example.app")
        )
        val window2 = window1.copy(id = "window-2")

        assertEquals("window-2", window2.id)
        assertEquals(window1.startTime, window2.startTime)
    }
}
