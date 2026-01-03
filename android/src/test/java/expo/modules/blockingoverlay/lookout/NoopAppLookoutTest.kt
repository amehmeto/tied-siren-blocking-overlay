package expo.modules.blockingoverlay.lookout

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class NoopAppLookoutTest {

    private val noopLookout = NoopAppLookout()

    @Test
    fun `startWatching should not throw`() {
        noopLookout.startWatching()
    }

    @Test
    fun `stopWatching should not throw`() {
        noopLookout.stopWatching()
    }

    @Test
    fun `setOnAppDetectedListener should not throw with null`() {
        noopLookout.setOnAppDetectedListener(null)
    }

    @Test
    fun `setOnAppDetectedListener should not throw with listener`() {
        noopLookout.setOnAppDetectedListener { _, _ -> }
    }

    @Test
    fun `multiple operations should not throw`() {
        noopLookout.setOnAppDetectedListener { _, _ -> }
        noopLookout.startWatching()
        noopLookout.stopWatching()
        noopLookout.setOnAppDetectedListener(null)
    }
}
