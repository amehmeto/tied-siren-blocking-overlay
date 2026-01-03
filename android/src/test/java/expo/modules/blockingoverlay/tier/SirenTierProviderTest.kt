package expo.modules.blockingoverlay.tier

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
class SirenTierProviderTest {

    @Before
    fun setUp() {
        SirenTierProvider.reset()
    }

    @Test
    fun `appTier should not be initialized by default`() {
        assertFalse(SirenTierProvider.isAppTierInitialized)
    }

    @Test
    fun `appTier should throw IllegalStateException when accessed before initialization`() {
        try {
            SirenTierProvider.appTier
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("AppTier not initialized"))
            assertTrue(e.message!!.contains("SirenTierProvider.appTier"))
        }
    }

    @Test
    fun `websiteTier should be NoopWebsiteTier by default`() {
        assertTrue(SirenTierProvider.websiteTier is NoopWebsiteTier)
    }

    @Test
    fun `keywordTier should be NoopKeywordTier by default`() {
        assertTrue(SirenTierProvider.keywordTier is NoopKeywordTier)
    }

    @Test
    fun `reset should restore defaults`() {
        // Set custom values
        SirenTierProvider.appTier = object : AppTier {
            override suspend fun block(packages: List<String>) {}
            override suspend fun unblockAll() {}
        }
        SirenTierProvider.websiteTier = object : WebsiteTier {
            override suspend fun block(domains: List<String>) {}
            override suspend fun unblockAll() {}
        }

        // Reset
        SirenTierProvider.reset()

        // Verify defaults
        assertFalse(SirenTierProvider.isAppTierInitialized)
        assertTrue(SirenTierProvider.websiteTier is NoopWebsiteTier)
        assertTrue(SirenTierProvider.keywordTier is NoopKeywordTier)
    }

    @Test
    fun `isAppTierInitialized should return true after setting appTier`() {
        assertFalse(SirenTierProvider.isAppTierInitialized)

        SirenTierProvider.appTier = object : AppTier {
            override suspend fun block(packages: List<String>) {}
            override suspend fun unblockAll() {}
        }

        assertTrue(SirenTierProvider.isAppTierInitialized)
    }

    @Test
    fun `can set custom appTier`() {
        val customTier = object : AppTier {
            override suspend fun block(packages: List<String>) {}
            override suspend fun unblockAll() {}
        }

        SirenTierProvider.appTier = customTier

        assertEquals(customTier, SirenTierProvider.appTier)
    }

    @Test
    fun `can set custom websiteTier`() {
        val customTier = object : WebsiteTier {
            override suspend fun block(domains: List<String>) {}
            override suspend fun unblockAll() {}
        }

        SirenTierProvider.websiteTier = customTier

        assertEquals(customTier, SirenTierProvider.websiteTier)
    }

    @Test
    fun `can set custom keywordTier`() {
        val customTier = object : KeywordTier {
            override suspend fun block(keywords: List<String>) {}
            override suspend fun unblockAll() {}
        }

        SirenTierProvider.keywordTier = customTier

        assertEquals(customTier, SirenTierProvider.keywordTier)
    }
}
