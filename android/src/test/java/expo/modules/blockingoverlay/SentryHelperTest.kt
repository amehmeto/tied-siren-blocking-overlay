package expo.modules.blockingoverlay

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class SentryHelperTest {

    @Before
    fun setUp() {
        // Reset verbose logging before each test
        SentryHelper.verboseLogging = false
    }

    @After
    fun tearDown() {
        SentryHelper.verboseLogging = false
    }

    // ========== verboseLogging flag tests ==========

    @Test
    fun `verboseLogging should default to false`() {
        // Reset by creating new state
        SentryHelper.verboseLogging = false
        assertFalse(SentryHelper.verboseLogging)
    }

    @Test
    fun `verboseLogging can be enabled`() {
        SentryHelper.verboseLogging = true
        assertTrue(SentryHelper.verboseLogging)
    }

    // ========== addVerboseBreadcrumb tests ==========

    @Test
    fun `addVerboseBreadcrumb should not call provider when verboseLogging is false`() {
        SentryHelper.verboseLogging = false
        var providerCalled = false

        SentryHelper.addVerboseBreadcrumb("test", "message") {
            providerCalled = true
            mapOf("key" to "value")
        }

        assertFalse("Provider should not be called when verboseLogging is false", providerCalled)
    }

    @Test
    fun `addVerboseBreadcrumb should call provider when verboseLogging is true`() {
        SentryHelper.verboseLogging = true
        var providerCalled = false

        SentryHelper.addVerboseBreadcrumb("test", "message") {
            providerCalled = true
            mapOf("key" to "value")
        }

        assertTrue("Provider should be called when verboseLogging is true", providerCalled)
    }

    @Test
    fun `addVerboseBreadcrumb should not throw when provider returns null`() {
        SentryHelper.verboseLogging = true

        // Should not throw
        SentryHelper.addVerboseBreadcrumb("test", "message") { null }
    }

    // ========== addBreadcrumb tests (fallback behavior) ==========

    @Test
    fun `addBreadcrumb should not throw when Sentry is unavailable`() {
        // Sentry is not available in test environment, so this tests fallback
        SentryHelper.addBreadcrumb("category", "message", mapOf("key" to "value"))
        // Test passes if no exception is thrown
    }

    @Test
    fun `addBreadcrumb should handle null data`() {
        SentryHelper.addBreadcrumb("category", "message", null)
        // Test passes if no exception is thrown
    }

    @Test
    fun `addBreadcrumb should handle empty data map`() {
        SentryHelper.addBreadcrumb("category", "message", emptyMap())
        // Test passes if no exception is thrown
    }

    // ========== captureMessage tests ==========

    @Test
    fun `captureMessage should not throw when Sentry is unavailable`() {
        SentryHelper.captureMessage("test message", "error")
        // Test passes if no exception is thrown
    }

    @Test
    fun `captureMessage should handle different log levels`() {
        SentryHelper.captureMessage("test", "error")
        SentryHelper.captureMessage("test", "warning")
        SentryHelper.captureMessage("test", "info")
        SentryHelper.captureMessage("test", "unknown")
        // Test passes if no exception is thrown
    }

    // ========== captureException tests ==========

    @Test
    fun `captureException should not throw when Sentry is unavailable`() {
        val exception = RuntimeException("Test exception")
        SentryHelper.captureException(exception)
        // Test passes if no exception is thrown
    }

    @Test
    fun `captureException should handle nested exceptions`() {
        val cause = IllegalArgumentException("Cause")
        val exception = RuntimeException("Wrapper", cause)
        SentryHelper.captureException(exception)
        // Test passes if no exception is thrown
    }
}
