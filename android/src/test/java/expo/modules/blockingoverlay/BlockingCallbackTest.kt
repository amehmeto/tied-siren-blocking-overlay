package expo.modules.blockingoverlay

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import expo.modules.accessibilityservice.AccessibilityService
import expo.modules.foregroundservice.ForegroundServiceCallback
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class BlockingCallbackTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockApplicationContext: Context

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        `when`(mockContext.applicationContext).thenReturn(mockApplicationContext)
        `when`(mockApplicationContext.getSharedPreferences(eq(BlockedAppsStorage.PREFS_NAME), eq(Context.MODE_PRIVATE)))
            .thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
    }

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

        // Verify interface methods exist
        val onServiceStarted = fscInterface.getMethod("onServiceStarted", Context::class.java)
        val onServiceStopped = fscInterface.getMethod("onServiceStopped")

        assertNotNull("onServiceStarted method should exist", onServiceStarted)
        assertNotNull("onServiceStopped method should exist", onServiceStopped)
    }

    @Test
    fun `EventListener interface has correct methods`() {
        val callback = BlockingCallback()
        val elInterface = AccessibilityService.EventListener::class.java

        // Verify interface methods exist
        val onAppChanged = elInterface.getMethod(
            "onAppChanged",
            String::class.java,
            String::class.java,
            Long::class.javaPrimitiveType
        )

        assertNotNull("onAppChanged method should exist", onAppChanged)
    }

    @Test
    fun `onAppChanged does nothing when context is null`() {
        val callback = BlockingCallback()

        // Should not throw - context is null by default
        callback.onAppChanged("com.test.app", "TestActivity", System.currentTimeMillis())

        // If we get here without exception, test passes
        assertTrue(true)
    }

    @Test
    fun `onAppChanged with blocked app should attempt to launch overlay`() {
        val callback = BlockingCallback()
        val blockedApps = setOf("com.facebook.katana")

        `when`(mockSharedPreferences.getStringSet(BlockedAppsStorage.KEY_BLOCKED_APPS, emptySet()))
            .thenReturn(blockedApps)

        // Start service to set context
        callback.onServiceStarted(mockContext)

        // Trigger onAppChanged with blocked app
        callback.onAppChanged("com.facebook.katana", "MainActivity", System.currentTimeMillis())

        // Verify startActivity was called on the application context
        verify(mockApplicationContext).startActivity(any())
    }

    @Test
    fun `onAppChanged with non-blocked app should not launch overlay`() {
        val callback = BlockingCallback()
        val blockedApps = setOf("com.facebook.katana")

        `when`(mockSharedPreferences.getStringSet(BlockedAppsStorage.KEY_BLOCKED_APPS, emptySet()))
            .thenReturn(blockedApps)

        // Start service to set context
        callback.onServiceStarted(mockContext)

        // Trigger onAppChanged with non-blocked app
        callback.onAppChanged("com.instagram.android", "MainActivity", System.currentTimeMillis())

        // Verify startActivity was NOT called
        verify(mockApplicationContext, never()).startActivity(any())
    }

    @Test
    fun `onServiceStopped clears context`() {
        val callback = BlockingCallback()

        `when`(mockSharedPreferences.getStringSet(BlockedAppsStorage.KEY_BLOCKED_APPS, emptySet()))
            .thenReturn(setOf("com.test.app"))

        // Start then stop service
        callback.onServiceStarted(mockContext)
        callback.onServiceStopped()

        // After stop, onAppChanged should do nothing (context is null)
        callback.onAppChanged("com.test.app", "MainActivity", System.currentTimeMillis())

        // startActivity should not be called after stop
        verify(mockApplicationContext, never()).startActivity(any())
    }

    @Test
    fun `overlay intent should have correct flags`() {
        val callback = BlockingCallback()
        val intentCaptor = argumentCaptor<Intent>()

        `when`(mockSharedPreferences.getStringSet(BlockedAppsStorage.KEY_BLOCKED_APPS, emptySet()))
            .thenReturn(setOf("com.test.app"))

        callback.onServiceStarted(mockContext)
        callback.onAppChanged("com.test.app", "MainActivity", System.currentTimeMillis())

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

        `when`(mockSharedPreferences.getStringSet(BlockedAppsStorage.KEY_BLOCKED_APPS, emptySet()))
            .thenReturn(setOf(testPackage))

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

    @Test
    fun `rapid calls for same package should be debounced`() {
        val callback = BlockingCallback()
        val testPackage = "com.debounce.test"

        `when`(mockSharedPreferences.getStringSet(BlockedAppsStorage.KEY_BLOCKED_APPS, emptySet()))
            .thenReturn(setOf(testPackage))

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

        `when`(mockSharedPreferences.getStringSet(BlockedAppsStorage.KEY_BLOCKED_APPS, emptySet()))
            .thenReturn(setOf(package1, package2))

        callback.onServiceStarted(mockContext)

        // First package
        callback.onAppChanged(package1, "MainActivity", System.currentTimeMillis())

        // Different package should NOT be debounced
        callback.onAppChanged(package2, "MainActivity", System.currentTimeMillis())

        // Verify startActivity was called TWICE (once per package)
        verify(mockApplicationContext, org.mockito.Mockito.times(2)).startActivity(any())
    }
}
