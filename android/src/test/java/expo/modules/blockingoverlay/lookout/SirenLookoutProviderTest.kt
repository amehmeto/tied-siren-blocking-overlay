package expo.modules.blockingoverlay.lookout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class SirenLookoutProviderTest {

    @Before
    fun setUp() {
        SirenLookoutProvider.reset()
    }

    @Test
    fun `appLookout should not be initialized by default`() {
        assertFalse(SirenLookoutProvider.isAppLookoutInitialized)
    }

    @Test
    fun `appLookout should throw IllegalStateException when accessed before initialization`() {
        try {
            SirenLookoutProvider.appLookout
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("AppLookout not initialized"))
            assertTrue(e.message!!.contains("SirenLookoutProvider.appLookout"))
        }
    }

    @Test
    fun `websiteLookout should be NoopWebsiteLookout by default`() {
        assertTrue(SirenLookoutProvider.websiteLookout is NoopWebsiteLookout)
    }

    @Test
    fun `keywordLookout should be NoopKeywordLookout by default`() {
        assertTrue(SirenLookoutProvider.keywordLookout is NoopKeywordLookout)
    }

    @Test
    fun `reset should restore defaults`() {
        // Set custom values
        SirenLookoutProvider.appLookout = object : AppLookout {
            override fun startWatching() {}
            override fun stopWatching() {}
            override fun setOnAppDetectedListener(listener: AppLookout.OnAppDetectedListener?) {}
        }
        SirenLookoutProvider.websiteLookout = object : WebsiteLookout {
            override fun startWatching() {}
            override fun stopWatching() {}
            override fun setOnWebsiteDetectedListener(listener: WebsiteLookout.OnWebsiteDetectedListener?) {}
        }

        // Reset
        SirenLookoutProvider.reset()

        // Verify defaults
        assertFalse(SirenLookoutProvider.isAppLookoutInitialized)
        assertTrue(SirenLookoutProvider.websiteLookout is NoopWebsiteLookout)
        assertTrue(SirenLookoutProvider.keywordLookout is NoopKeywordLookout)
    }

    @Test
    fun `isAppLookoutInitialized should return true after setting appLookout`() {
        assertFalse(SirenLookoutProvider.isAppLookoutInitialized)

        SirenLookoutProvider.appLookout = object : AppLookout {
            override fun startWatching() {}
            override fun stopWatching() {}
            override fun setOnAppDetectedListener(listener: AppLookout.OnAppDetectedListener?) {}
        }

        assertTrue(SirenLookoutProvider.isAppLookoutInitialized)
    }

    @Test
    fun `can set custom appLookout`() {
        val customLookout = object : AppLookout {
            override fun startWatching() {}
            override fun stopWatching() {}
            override fun setOnAppDetectedListener(listener: AppLookout.OnAppDetectedListener?) {}
        }

        SirenLookoutProvider.appLookout = customLookout

        assertEquals(customLookout, SirenLookoutProvider.appLookout)
    }

    @Test
    fun `can set custom websiteLookout`() {
        val customLookout = object : WebsiteLookout {
            override fun startWatching() {}
            override fun stopWatching() {}
            override fun setOnWebsiteDetectedListener(listener: WebsiteLookout.OnWebsiteDetectedListener?) {}
        }

        SirenLookoutProvider.websiteLookout = customLookout

        assertEquals(customLookout, SirenLookoutProvider.websiteLookout)
    }

    @Test
    fun `can set custom keywordLookout`() {
        val customLookout = object : KeywordLookout {
            override fun startWatching() {}
            override fun stopWatching() {}
            override fun setOnKeywordDetectedListener(listener: KeywordLookout.OnKeywordDetectedListener?) {}
        }

        SirenLookoutProvider.keywordLookout = customLookout

        assertEquals(customLookout, SirenLookoutProvider.keywordLookout)
    }
}
