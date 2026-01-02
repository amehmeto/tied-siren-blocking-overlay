package expo.modules.blockingoverlay.tier

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class NoopKeywordTierTest {

    private val noopTier = NoopKeywordTier()

    @Test
    fun `block should not throw with empty list`() = runBlocking {
        noopTier.block(emptyList())
    }

    @Test
    fun `block should not throw with keywords`() = runBlocking {
        noopTier.block(listOf("gambling", "casino"))
    }

    @Test
    fun `unblockAll should not throw`() = runBlocking {
        noopTier.unblockAll()
    }

    @Test
    fun `multiple operations should not throw`() = runBlocking {
        noopTier.block(listOf("test"))
        noopTier.unblockAll()
        noopTier.block(listOf("keyword1", "keyword2"))
        noopTier.unblockAll()
    }
}
