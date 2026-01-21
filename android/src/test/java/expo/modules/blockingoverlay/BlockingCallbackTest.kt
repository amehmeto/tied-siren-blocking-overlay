package expo.modules.blockingoverlay

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import expo.modules.accessibilityservice.AccessibilityService
import expo.modules.foregroundservice.ForegroundServiceCallback
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Modifier

/**
 * Tests for BlockingCallback with schedule-based blocking (#18).
 *
 * The callback now checks BlockingScheduleStorage for time-based windows
 * instead of BlockedAppsStorage for a simple list.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class BlockingCallbackTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockApplicationContext: Context

    @Mock
    private lateinit var mockSchedulePrefs: SharedPreferences

    @Mock
    private lateinit var mockScheduleEditor: SharedPreferences.Editor

    companion object {
        // SharedPreferences name used by BlockingScheduleStorage
        private const val SCHEDULE_PREFS_NAME = "tied_siren_blocking_schedule"
        private const val KEY_SCHEDULE = "blocking_schedule"

        /**
         * Creates a JSON schedule string with an always-active window (00:00-23:59).
         */
        fun createAlwaysActiveSchedule(packageNames: List<String>): String {
            val jsonArray = JSONArray()
            val jsonObject = JSONObject().apply {
                put("id", "test-window")
                put("startTime", "00:00")
                put("endTime", "23:59")
                put("packageNames", JSONArray(packageNames))
            }
            jsonArray.put(jsonObject)
            return jsonArray.toString()
        }

        /**
         * Creates a JSON schedule string with a never-active window.
         * Uses a 1-minute window in the past.
         */
        fun createNeverActiveSchedule(packageNames: List<String>): String {
            val jsonArray = JSONArray()
            val jsonObject = JSONObject().apply {
                put("id", "test-window")
                put("startTime", "00:00")
                put("endTime", "00:01") // 1 minute window at midnight
                put("packageNames", JSONArray(packageNames))
            }
            jsonArray.put(jsonObject)
            return jsonArray.toString()
        }
    }

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        `when`(mockContext.applicationContext).thenReturn(mockApplicationContext)

        // Mock SharedPreferences for BlockingScheduleStorage
        `when`(mockApplicationContext.getSharedPreferences(eq(SCHEDULE_PREFS_NAME), eq(Context.MODE_PRIVATE)))
            .thenReturn(mockSchedulePrefs)
        `when`(mockSchedulePrefs.edit()).thenReturn(mockScheduleEditor)

        // Invalidate cache before each test to ensure fresh reads
        BlockingScheduleStorage.invalidateCache()
    }

    // ========== Interface Tests ==========

    @Test
    fun `has public no-arg constructor for reflection`() {
        val clazz = BlockingCallback::class.java
        val constructor = clazz.getDeclaredConstructor()

        assertNotNull("BlockingCallback must have a no-arg constructor", constructor)
        assertTrue(
            "Constructor must be public for reflection",
            Modifier.isPublic(constructor.modifiers)
        )
    }

    @Test
    fun `can be instantiated via reflection`() {
        val clazz = BlockingCallback::class.java
        val constructor = clazz.getDeclaredConstructor()

        val instance = constructor.newInstance()

        assertNotNull("Instance should be created", instance)
        assertTrue("Instance should be BlockingCallback", instance is BlockingCallback)
    }

    @Test
    fun `implements ForegroundServiceCallback interface`() {
        val callback = BlockingCallback()

        assertTrue(
            "BlockingCallback must implement ForegroundServiceCallback",
            callback is ForegroundServiceCallback
        )
    }

    @Test
    fun `implements AccessibilityService EventListener interface`() {
        val callback = BlockingCallback()

        assertTrue(
            "BlockingCallback must implement AccessibilityService.EventListener",
            callback is AccessibilityService.EventListener
        )
    }

    @Test
    fun `ForegroundServiceCallback interface has correct methods`() {
        val callback = BlockingCallback()
        val fscInterface = ForegroundServiceCallback::class.java

        val onServiceStarted = fscInterface.getMethod("onServiceStarted", Context::class.java)
        val onServiceStopped = fscInterface.getMethod("onServiceStopped")

        assertNotNull("onServiceStarted method should exist", onServiceStarted)
        assertNotNull("onServiceStopped method should exist", onServiceStopped)
    }

    @Test
    fun `EventListener interface has correct methods`() {
        val callback = BlockingCallback()
        val elInterface = AccessibilityService.EventListener::class.java

        val onAppChanged = elInterface.getMethod(
            "onAppChanged",
            String::class.java,
            String::class.java,
            Long::class.javaPrimitiveType
        )

        assertNotNull("onAppChanged method should exist", onAppChanged)
    }

    // ========== Context Lifecycle Tests ==========

    @Test
    fun `onAppChanged does nothing when context is null`() {
        val callback = BlockingCallback()

        // Should not throw - context is null by default
        callback.onAppChanged("com.test.app", "TestActivity", System.currentTimeMillis())

        // If we get here without exception, test passes
        assertTrue(true)
    }

    @Test
    fun `onServiceStopped clears context`() {
        val callback = BlockingCallback()

        // Setup schedule with always-active window
        val scheduleJson = createAlwaysActiveSchedule(listOf("com.test.app"))
        `when`(mockSchedulePrefs.getString(KEY_SCHEDULE, null)).thenReturn(scheduleJson)

        // Start then stop service
        callback.onServiceStarted(mockContext)
        callback.onServiceStopped()

        // After stop, onAppChanged should do nothing (context is null)
        callback.onAppChanged("com.test.app", "MainActivity", System.currentTimeMillis())

        // startActivity should not be called after stop
        verify(mockApplicationContext, never()).startActivity(any())
    }

    // ========== Schedule-Based Blocking Tests ==========

    @Test
    fun `onAppChanged with blocked app in active window should launch overlay`() {
        val callback = BlockingCallback()
        val blockedPackage = "com.facebook.katana"

        // Setup schedule with always-active window containing the blocked package
        val scheduleJson = createAlwaysActiveSchedule(listOf(blockedPackage))
        `when`(mockSchedulePrefs.getString(KEY_SCHEDULE, null)).thenReturn(scheduleJson)

        // Start service to set context
        callback.onServiceStarted(mockContext)

        // Trigger onAppChanged with blocked app
        callback.onAppChanged(blockedPackage, "MainActivity", System.currentTimeMillis())

        // Verify startActivity was called
        verify(mockApplicationContext).startActivity(any())
    }

    @Test
    fun `onAppChanged with non-blocked app should not launch overlay`() {
        val callback = BlockingCallback()

        // Setup schedule blocking only Facebook
        val scheduleJson = createAlwaysActiveSchedule(listOf("com.facebook.katana"))
        `when`(mockSchedulePrefs.getString(KEY_SCHEDULE, null)).thenReturn(scheduleJson)

        // Start service
        callback.onServiceStarted(mockContext)

        // Trigger onAppChanged with non-blocked app (Instagram)
        callback.onAppChanged("com.instagram.android", "MainActivity", System.currentTimeMillis())

        // Verify startActivity was NOT called
        verify(mockApplicationContext, never()).startActivity(any())
    }

    @Test
    fun `onAppChanged with empty schedule should not launch overlay`() {
        val callback = BlockingCallback()

        // Setup empty schedule
        `when`(mockSchedulePrefs.getString(KEY_SCHEDULE, null)).thenReturn("[]")

        // Start service
        callback.onServiceStarted(mockContext)

        // Trigger onAppChanged
        callback.onAppChanged("com.facebook.katana", "MainActivity", System.currentTimeMillis())

        // Verify startActivity was NOT called
        verify(mockApplicationContext, never()).startActivity(any())
    }

    @Test
    fun `onAppChanged with null schedule should not launch overlay`() {
        val callback = BlockingCallback()

        // Setup null schedule (no schedule set)
        `when`(mockSchedulePrefs.getString(KEY_SCHEDULE, null)).thenReturn(null)

        // Start service
        callback.onServiceStarted(mockContext)

        // Trigger onAppChanged
        callback.onAppChanged("com.facebook.katana", "MainActivity", System.currentTimeMillis())

        // Verify startActivity was NOT called
        verify(mockApplicationContext, never()).startActivity(any())
    }

    // ========== Intent Configuration Tests ==========

    @Test
    fun `overlay intent should have correct flags`() {
        val callback = BlockingCallback()
        val intentCaptor = argumentCaptor<Intent>()
        val blockedPackage = "com.test.app"

        val scheduleJson = createAlwaysActiveSchedule(listOf(blockedPackage))
        `when`(mockSchedulePrefs.getString(KEY_SCHEDULE, null)).thenReturn(scheduleJson)

        callback.onServiceStarted(mockContext)
        callback.onAppChanged(blockedPackage, "MainActivity", System.currentTimeMillis())

        verify(mockApplicationContext).startActivity(intentCaptor.capture())

        val intent = intentCaptor.firstValue
        val expectedFlags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                Intent.FLAG_ACTIVITY_NO_HISTORY

        assertEquals(
            "Intent should have correct flags",
            expectedFlags,
            intent.flags
        )
    }

    @Test
    fun `overlay intent should include package name extra`() {
        val callback = BlockingCallback()
        val intentCaptor = argumentCaptor<Intent>()
        val testPackage = "com.test.blocked.app"

        val scheduleJson = createAlwaysActiveSchedule(listOf(testPackage))
        `when`(mockSchedulePrefs.getString(KEY_SCHEDULE, null)).thenReturn(scheduleJson)

        callback.onServiceStarted(mockContext)
        callback.onAppChanged(testPackage, "MainActivity", System.currentTimeMillis())

        verify(mockApplicationContext).startActivity(intentCaptor.capture())

        val intent = intentCaptor.firstValue
        assertEquals(
            "Intent should contain package name extra",
            testPackage,
            intent.getStringExtra(BlockingOverlayActivity.EXTRA_PACKAGE_NAME)
        )
    }

    // ========== Debouncing Tests ==========

    @Test
    fun `rapid calls for same package should be debounced`() {
        val callback = BlockingCallback()
        val testPackage = "com.debounce.test"

        val scheduleJson = createAlwaysActiveSchedule(listOf(testPackage))
        `when`(mockSchedulePrefs.getString(KEY_SCHEDULE, null)).thenReturn(scheduleJson)

        callback.onServiceStarted(mockContext)

        // First call should launch overlay
        callback.onAppChanged(testPackage, "MainActivity", System.currentTimeMillis())

        // Rapid second call (within 500ms debounce window) should be ignored
        callback.onAppChanged(testPackage, "MainActivity", System.currentTimeMillis())

        // Rapid third call should also be ignored
        callback.onAppChanged(testPackage, "MainActivity", System.currentTimeMillis())

        // Verify startActivity was called only ONCE despite 3 calls
        verify(mockApplicationContext, org.mockito.Mockito.times(1)).startActivity(any())
    }

    @Test
    fun `different packages should not be debounced`() {
        val callback = BlockingCallback()
        val package1 = "com.first.app"
        val package2 = "com.second.app"

        val scheduleJson = createAlwaysActiveSchedule(listOf(package1, package2))
        `when`(mockSchedulePrefs.getString(KEY_SCHEDULE, null)).thenReturn(scheduleJson)

        callback.onServiceStarted(mockContext)

        // First package
        callback.onAppChanged(package1, "MainActivity", System.currentTimeMillis())

        // Different package should NOT be debounced
        callback.onAppChanged(package2, "MainActivity", System.currentTimeMillis())

        // Verify startActivity was called TWICE (once per package)
        verify(mockApplicationContext, org.mockito.Mockito.times(2)).startActivity(any())
    }

    // ========== Multiple Windows Tests ==========

    @Test
    fun `onAppChanged should check all windows for blocked package`() {
        val callback = BlockingCallback()

        // Create schedule with two windows, package blocked in second window
        val jsonArray = JSONArray()
        jsonArray.put(JSONObject().apply {
            put("id", "window-1")
            put("startTime", "00:00")
            put("endTime", "23:59")
            put("packageNames", JSONArray(listOf("com.other.app")))
        })
        jsonArray.put(JSONObject().apply {
            put("id", "window-2")
            put("startTime", "00:00")
            put("endTime", "23:59")
            put("packageNames", JSONArray(listOf("com.facebook.katana")))
        })

        `when`(mockSchedulePrefs.getString(KEY_SCHEDULE, null)).thenReturn(jsonArray.toString())

        callback.onServiceStarted(mockContext)
        callback.onAppChanged("com.facebook.katana", "MainActivity", System.currentTimeMillis())

        // Should find package in second window and block
        verify(mockApplicationContext).startActivity(any())
    }
}
