package expo.modules.blockingoverlay.lookout

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class NoopKeywordLookoutTest {

    private val noopLookout = NoopKeywordLookout()

    @Test
    fun `startWatching should not throw`() {
        noopLookout.startWatching()
    }

    @Test
    fun `stopWatching should not throw`() {
        noopLookout.stopWatching()
    }

    @Test
    fun `setOnKeywordDetectedListener should not throw with null`() {
        noopLookout.setOnKeywordDetectedListener(null)
    }

    @Test
    fun `setOnKeywordDetectedListener should not throw with listener`() {
        noopLookout.setOnKeywordDetectedListener { _, _, _ -> }
    }

    @Test
    fun `multiple operations should not throw`() {
        noopLookout.setOnKeywordDetectedListener { _, _, _ -> }
        noopLookout.startWatching()
        noopLookout.stopWatching()
        noopLookout.setOnKeywordDetectedListener(null)
    }
}
