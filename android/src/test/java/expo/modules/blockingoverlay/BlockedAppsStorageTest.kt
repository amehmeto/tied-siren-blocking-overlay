package expo.modules.blockingoverlay

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class BlockedAppsStorageTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        `when`(mockContext.getSharedPreferences(eq(BlockedAppsStorage.PREFS_NAME), eq(Context.MODE_PRIVATE)))
            .thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putStringSet(any(), any())).thenReturn(mockEditor)
        `when`(mockEditor.remove(any())).thenReturn(mockEditor)
    }

    @Test
    fun `PREFS_NAME constant should have correct value`() {
        assertEquals("tied_siren_blocking", BlockedAppsStorage.PREFS_NAME)
    }

    @Test
    fun `KEY_BLOCKED_APPS constant should have correct value`() {
        assertEquals("blocked_apps", BlockedAppsStorage.KEY_BLOCKED_APPS)
    }

    @Test
    fun `setBlockedApps should store apps in SharedPreferences`() {
        val apps = setOf("com.app1", "com.app2")

        BlockedAppsStorage.setBlockedApps(mockContext, apps)

        verify(mockEditor).putStringSet(BlockedAppsStorage.KEY_BLOCKED_APPS, apps)
        verify(mockEditor).apply()
    }

    @Test
    fun `getBlockedApps should return stored apps`() {
        val storedApps = setOf("com.facebook.katana", "com.instagram.android")
        `when`(mockSharedPreferences.getStringSet(BlockedAppsStorage.KEY_BLOCKED_APPS, emptySet()))
            .thenReturn(storedApps)

        val result = BlockedAppsStorage.getBlockedApps(mockContext)

        assertEquals(storedApps, result)
    }

    @Test
    fun `getBlockedApps should return empty set when nothing stored`() {
        `when`(mockSharedPreferences.getStringSet(BlockedAppsStorage.KEY_BLOCKED_APPS, emptySet()))
            .thenReturn(emptySet())

        val result = BlockedAppsStorage.getBlockedApps(mockContext)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getBlockedApps should return empty set when null is returned`() {
        `when`(mockSharedPreferences.getStringSet(BlockedAppsStorage.KEY_BLOCKED_APPS, emptySet()))
            .thenReturn(null)

        val result = BlockedAppsStorage.getBlockedApps(mockContext)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `clearBlockedApps should remove blocked apps from SharedPreferences`() {
        BlockedAppsStorage.clearBlockedApps(mockContext)

        verify(mockEditor).remove(BlockedAppsStorage.KEY_BLOCKED_APPS)
        verify(mockEditor).apply()
    }

    @Test
    fun `setBlockedApps with empty set should store empty set`() {
        val emptyApps = emptySet<String>()

        BlockedAppsStorage.setBlockedApps(mockContext, emptyApps)

        verify(mockEditor).putStringSet(BlockedAppsStorage.KEY_BLOCKED_APPS, emptyApps)
        verify(mockEditor).apply()
    }
}
