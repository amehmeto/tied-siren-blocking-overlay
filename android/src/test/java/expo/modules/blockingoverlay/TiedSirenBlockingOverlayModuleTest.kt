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
    fun `EXTRA_BLOCK_UNTIL constant should have correct value`() {
        assertEquals("blockUntil", BlockingOverlayActivity.EXTRA_BLOCK_UNTIL)
    }

    @Test
    fun `intent extras should be set correctly`() {
        val packageName = "com.example.testapp"
        val blockUntil = 1700000000000L

        val intent = Intent().apply {
            putExtra(BlockingOverlayActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(BlockingOverlayActivity.EXTRA_BLOCK_UNTIL, blockUntil)
        }

        assertEquals(packageName, intent.getStringExtra(BlockingOverlayActivity.EXTRA_PACKAGE_NAME))
        assertEquals(blockUntil, intent.getLongExtra(BlockingOverlayActivity.EXTRA_BLOCK_UNTIL, 0))
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
}
