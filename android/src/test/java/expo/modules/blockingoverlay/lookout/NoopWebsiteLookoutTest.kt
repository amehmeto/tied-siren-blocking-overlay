package expo.modules.blockingoverlay.lookout

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class NoopWebsiteLookoutTest {

    private val noopLookout = NoopWebsiteLookout()

    @Test
    fun `startWatching should not throw`() {
        noopLookout.startWatching()
    }

    @Test
    fun `stopWatching should not throw`() {
        noopLookout.stopWatching()
    }

    @Test
    fun `setOnWebsiteDetectedListener should not throw with null`() {
        noopLookout.setOnWebsiteDetectedListener(null)
    }

    @Test
    fun `setOnWebsiteDetectedListener should not throw with listener`() {
        noopLookout.setOnWebsiteDetectedListener { _, _ -> }
    }

    @Test
    fun `multiple operations should not throw`() {
        noopLookout.setOnWebsiteDetectedListener { _, _ -> }
        noopLookout.startWatching()
        noopLookout.stopWatching()
        noopLookout.setOnWebsiteDetectedListener(null)
    }
}
