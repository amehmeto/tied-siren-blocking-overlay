package expo.modules.blockingoverlay

import android.content.Intent
import android.widget.Button
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class BlockingOverlayActivityTest {

    private lateinit var activityController: ActivityController<BlockingOverlayActivity>
    private lateinit var activity: BlockingOverlayActivity

    @Before
    fun setUp() {
        val intent = Intent().apply {
            putExtra(BlockingOverlayActivity.EXTRA_PACKAGE_NAME, "com.example.blocked")
        }
        activityController = Robolectric.buildActivity(BlockingOverlayActivity::class.java, intent)
    }

    @Test
    fun `activity should be created successfully`() {
        activity = activityController.create().get()
        assertNotNull(activity)
    }

    @Test
    fun `activity should not be finishing after creation`() {
        activity = activityController.create().start().resume().get()
        assertTrue(!activity.isFinishing)
    }

    @Test
    fun `close button should exist in layout`() {
        activity = activityController.create().get()
        val closeButton = activity.findViewById<Button>(R.id.closeButton)
        assertNotNull(closeButton)
    }

    @Test
    fun `clicking close button should navigate to home screen`() {
        activity = activityController.create().start().resume().get()
        val closeButton = activity.findViewById<Button>(R.id.closeButton)

        closeButton.performClick()

        val shadowActivity = shadowOf(activity)
        val nextIntent = shadowActivity.nextStartedActivity

        assertNotNull(nextIntent)
        assertEquals(Intent.ACTION_MAIN, nextIntent.action)
        assertTrue(nextIntent.categories.contains(Intent.CATEGORY_HOME))
    }

    @Test
    fun `clicking close button should finish the activity`() {
        activity = activityController.create().start().resume().get()
        val closeButton = activity.findViewById<Button>(R.id.closeButton)

        closeButton.performClick()

        assertTrue(activity.isFinishing)
    }

    @Test
    fun `back press should not finish the activity`() {
        activity = activityController.create().start().resume().get()

        @Suppress("DEPRECATION")
        activity.onBackPressed()

        assertTrue(!activity.isFinishing)
    }

    @Test
    fun `intent extras should be accessible in activity`() {
        val packageName = "com.example.testblocked"

        val intent = Intent().apply {
            putExtra(BlockingOverlayActivity.EXTRA_PACKAGE_NAME, packageName)
        }

        activityController = Robolectric.buildActivity(BlockingOverlayActivity::class.java, intent)
        activity = activityController.create().get()

        assertEquals(
            packageName,
            activity.intent.getStringExtra(BlockingOverlayActivity.EXTRA_PACKAGE_NAME)
        )
    }

    @Test
    fun `home intent should have NEW_TASK flag`() {
        activity = activityController.create().start().resume().get()
        val closeButton = activity.findViewById<Button>(R.id.closeButton)

        closeButton.performClick()

        val shadowActivity = shadowOf(activity)
        val nextIntent = shadowActivity.nextStartedActivity

        assertTrue(nextIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }
}
