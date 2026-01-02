package expo.modules.blockingoverlay.tier

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class NoopWebsiteTierTest {

    private val noopTier = NoopWebsiteTier()

    @Test
    fun `block should not throw with empty list`() = runBlocking {
        noopTier.block(emptyList())
    }

    @Test
    fun `block should not throw with domains`() = runBlocking {
        noopTier.block(listOf("facebook.com", "instagram.com"))
    }

    @Test
    fun `unblockAll should not throw`() = runBlocking {
        noopTier.unblockAll()
    }

    @Test
    fun `multiple operations should not throw`() = runBlocking {
        noopTier.block(listOf("test.com"))
        noopTier.unblockAll()
        noopTier.block(listOf("another.com", "third.com"))
        noopTier.unblockAll()
    }
}
