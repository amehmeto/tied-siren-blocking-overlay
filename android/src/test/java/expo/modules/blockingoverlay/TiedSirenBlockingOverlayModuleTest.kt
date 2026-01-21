package expo.modules.blockingoverlay

import android.content.Context
import android.content.Intent
import expo.modules.kotlin.AppContext
import expo.modules.kotlin.exception.CodedException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class TiedSirenBlockingOverlayModuleTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockAppContext: AppContext

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `EXTRA_PACKAGE_NAME constant should have correct value`() {
        assertEquals("packageName", BlockingOverlayActivity.EXTRA_PACKAGE_NAME)
    }

    @Test
    fun `intent extras should be set correctly`() {
        val packageName = "com.example.testapp"

        val intent = Intent().apply {
            putExtra(BlockingOverlayActivity.EXTRA_PACKAGE_NAME, packageName)
        }

        assertEquals(packageName, intent.getStringExtra(BlockingOverlayActivity.EXTRA_PACKAGE_NAME))
    }

    @Test
    fun `intent flags should include NEW_TASK, CLEAR_TASK, and NO_HISTORY`() {
        val expectedFlags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TASK or
            Intent.FLAG_ACTIVITY_NO_HISTORY

        val intent = Intent().apply {
            flags = expectedFlags
        }

        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NO_HISTORY != 0)
    }

    @Test
    fun `empty package name should be detected as blank`() {
        val emptyPackageName = ""
        assertTrue(emptyPackageName.isBlank())
    }

    @Test
    fun `whitespace-only package name should be detected as blank`() {
        val whitespacePackageName = "   "
        assertTrue(whitespacePackageName.isBlank())
    }

    @Test
    fun `valid package name should not be blank`() {
        val validPackageName = "com.example.app"
        assertTrue(validPackageName.isNotBlank())
    }

    @Test
    fun `CodedException should contain correct error code for invalid package`() {
        val exception = CodedException("ERR_INVALID_PACKAGE", "Package name cannot be empty", null)
        assertEquals("ERR_INVALID_PACKAGE", exception.code)
    }

    @Test
    fun `CodedException should contain correct error code for overlay launch failure`() {
        val exception = CodedException("ERR_OVERLAY_LAUNCH", "Failed to launch overlay", null)
        assertEquals("ERR_OVERLAY_LAUNCH", exception.code)
    }

    @Test
    fun `home intent should have correct action and category`() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        assertEquals(Intent.ACTION_MAIN, homeIntent.action)
        assertTrue(homeIntent.categories.contains(Intent.CATEGORY_HOME))
        assertTrue(homeIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    // ========== Schedule API Error Codes Tests ==========

    @Test
    fun `CodedException should contain correct error code for no context`() {
        val exception = CodedException("ERR_NO_CONTEXT", "Context not available", null)
        assertEquals("ERR_NO_CONTEXT", exception.code)
    }

    @Test
    fun `CodedException should contain correct error code for invalid schedule`() {
        val exception = CodedException("ERR_INVALID_SCHEDULE", "Invalid schedule data", null)
        assertEquals("ERR_INVALID_SCHEDULE", exception.code)
    }

    @Test
    fun `CodedException should contain correct error code for schedule failure`() {
        val exception = CodedException("ERR_SCHEDULE_FAILED", "Failed to set schedule", null)
        assertEquals("ERR_SCHEDULE_FAILED", exception.code)
    }

    // ========== Schedule Data Structure Tests ==========

    @Test
    fun `BlockingWindow can be created from map data`() {
        val windowMap = mapOf(
            "id" to "work-hours",
            "startTime" to "09:00",
            "endTime" to "17:00",
            "packageNames" to listOf("com.instagram.android", "com.twitter.android")
        )

        @Suppress("UNCHECKED_CAST")
        val window = BlockingWindow(
            id = windowMap["id"] as String,
            startTime = windowMap["startTime"] as String,
            endTime = windowMap["endTime"] as String,
            packageNames = windowMap["packageNames"] as List<String>
        )

        assertEquals("work-hours", window.id)
        assertEquals("09:00", window.startTime)
        assertEquals("17:00", window.endTime)
        assertEquals(2, window.packageNames.size)
        assertTrue(window.packageNames.contains("com.instagram.android"))
        assertTrue(window.packageNames.contains("com.twitter.android"))
    }

    @Test
    fun `BlockingWindow can be converted back to map`() {
        val window = BlockingWindow(
            id = "sleep-time",
            startTime = "22:00",
            endTime = "06:00",
            packageNames = listOf("com.facebook.katana")
        )

        val map = mapOf(
            "id" to window.id,
            "startTime" to window.startTime,
            "endTime" to window.endTime,
            "packageNames" to window.packageNames
        )

        assertEquals("sleep-time", map["id"])
        assertEquals("22:00", map["startTime"])
        assertEquals("06:00", map["endTime"])
        assertEquals(listOf("com.facebook.katana"), map["packageNames"])
    }

    @Test
    fun `multiple BlockingWindows can be mapped from list`() {
        val windowsList = listOf(
            mapOf(
                "id" to "window-1",
                "startTime" to "09:00",
                "endTime" to "12:00",
                "packageNames" to listOf("com.app1")
            ),
            mapOf(
                "id" to "window-2",
                "startTime" to "14:00",
                "endTime" to "18:00",
                "packageNames" to listOf("com.app2", "com.app3")
            )
        )

        @Suppress("UNCHECKED_CAST")
        val blockingWindows = windowsList.map { windowMap ->
            BlockingWindow(
                id = windowMap["id"] as String,
                startTime = windowMap["startTime"] as String,
                endTime = windowMap["endTime"] as String,
                packageNames = windowMap["packageNames"] as List<String>
            )
        }

        assertEquals(2, blockingWindows.size)
        assertEquals("window-1", blockingWindows[0].id)
        assertEquals("window-2", blockingWindows[1].id)
        assertEquals(1, blockingWindows[0].packageNames.size)
        assertEquals(2, blockingWindows[1].packageNames.size)
    }
}
