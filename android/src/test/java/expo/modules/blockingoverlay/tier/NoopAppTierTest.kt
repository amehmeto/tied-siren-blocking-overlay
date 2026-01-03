package expo.modules.blockingoverlay.tier

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class NoopAppTierTest {

    private val noopTier = NoopAppTier()

    @Test
    fun `block should not throw with empty list`() = runBlocking {
        noopTier.block(emptyList())
    }

    @Test
    fun `block should not throw with packages`() = runBlocking {
        noopTier.block(listOf("com.facebook.katana", "com.instagram.android"))
    }

    @Test
    fun `unblockAll should not throw`() = runBlocking {
        noopTier.unblockAll()
    }

    @Test
    fun `multiple operations should not throw`() = runBlocking {
        noopTier.block(listOf("com.test.app"))
        noopTier.unblockAll()
        noopTier.block(listOf("com.app1", "com.app2"))
        noopTier.unblockAll()
    }
}
