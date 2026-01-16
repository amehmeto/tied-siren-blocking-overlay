package expo.modules.blockingoverlay

import android.content.Context
import android.content.SharedPreferences
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class BlockingScheduleStorageTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockPrefs: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        BlockingScheduleStorage.invalidateCache()

        whenever(mockContext.getSharedPreferences(any(), eq(Context.MODE_PRIVATE)))
            .thenReturn(mockPrefs)
        whenever(mockPrefs.edit()).thenReturn(mockEditor)
        whenever(mockEditor.putString(any(), any())).thenReturn(mockEditor)
        whenever(mockEditor.remove(any())).thenReturn(mockEditor)
    }

    @Test
    fun `setSchedule should persist windows to SharedPreferences`() {
        val windows = listOf(
            BlockingWindow(
                id = "window-1",
                startTime = "09:00",
                endTime = "17:00",
                packageNames = listOf("com.example.app1")
            )
        )

        BlockingScheduleStorage.setSchedule(mockContext, windows)

        verify(mockEditor).putString(eq("blocking_schedule"), any())
        verify(mockEditor).apply()
    }

    @Test
    fun `getSchedule should return empty list when no schedule exists`() {
        whenever(mockPrefs.getString("blocking_schedule", null)).thenReturn(null)

        val result = BlockingScheduleStorage.getSchedule(mockContext)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getSchedule should deserialize stored schedule correctly`() {
        val json = """[{"id":"window-1","startTime":"09:00","endTime":"17:00","packageNames":["com.example.app1","com.example.app2"]}]"""
        whenever(mockPrefs.getString("blocking_schedule", null)).thenReturn(json)

        val result = BlockingScheduleStorage.getSchedule(mockContext)

        assertEquals(1, result.size)
        assertEquals("window-1", result[0].id)
        assertEquals("09:00", result[0].startTime)
        assertEquals("17:00", result[0].endTime)
        assertEquals(2, result[0].packageNames.size)
        assertTrue(result[0].packageNames.contains("com.example.app1"))
        assertTrue(result[0].packageNames.contains("com.example.app2"))
    }

    @Test
    fun `getSchedule should handle multiple windows`() {
        val json = """[
            {"id":"window-1","startTime":"09:00","endTime":"12:00","packageNames":["com.app1"]},
            {"id":"window-2","startTime":"14:00","endTime":"18:00","packageNames":["com.app2"]}
        ]"""
        whenever(mockPrefs.getString("blocking_schedule", null)).thenReturn(json)

        val result = BlockingScheduleStorage.getSchedule(mockContext)

        assertEquals(2, result.size)
        assertEquals("window-1", result[0].id)
        assertEquals("window-2", result[1].id)
    }

    @Test
    fun `getSchedule should return empty list on malformed JSON`() {
        whenever(mockPrefs.getString("blocking_schedule", null)).thenReturn("invalid json")

        val result = BlockingScheduleStorage.getSchedule(mockContext)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `clearSchedule should remove schedule from SharedPreferences`() {
        BlockingScheduleStorage.clearSchedule(mockContext)

        verify(mockEditor).remove("blocking_schedule")
        verify(mockEditor).apply()
    }

    @Test
    fun `getSchedule should use cache after setSchedule`() {
        val windows = listOf(
            BlockingWindow(
                id = "cached-window",
                startTime = "10:00",
                endTime = "16:00",
                packageNames = listOf("com.cached.app")
            )
        )

        BlockingScheduleStorage.setSchedule(mockContext, windows)

        // Reset the mock to verify no further reads happen
        whenever(mockPrefs.getString("blocking_schedule", null)).thenReturn(null)

        val result = BlockingScheduleStorage.getSchedule(mockContext)

        assertEquals(1, result.size)
        assertEquals("cached-window", result[0].id)
    }

    @Test
    fun `invalidateCache should force reload from SharedPreferences`() {
        val json = """[{"id":"reloaded","startTime":"08:00","endTime":"20:00","packageNames":[]}]"""
        whenever(mockPrefs.getString("blocking_schedule", null)).thenReturn(json)

        // First load
        BlockingScheduleStorage.getSchedule(mockContext)

        // Invalidate
        BlockingScheduleStorage.invalidateCache()

        // Should reload from prefs
        val result = BlockingScheduleStorage.getSchedule(mockContext)

        assertEquals("reloaded", result[0].id)
    }

    @Test
    fun `clearSchedule should result in empty list on getSchedule`() {
        val windows = listOf(
            BlockingWindow(
                id = "to-clear",
                startTime = "09:00",
                endTime = "17:00",
                packageNames = listOf("com.example.app")
            )
        )

        BlockingScheduleStorage.setSchedule(mockContext, windows)
        BlockingScheduleStorage.clearSchedule(mockContext)

        val result = BlockingScheduleStorage.getSchedule(mockContext)

        assertTrue(result.isEmpty())
    }
}
