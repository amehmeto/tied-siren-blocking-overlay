package expo.modules.blockingoverlay.lookout

/**
 * Dependency injection provider for SirenLookout implementations.
 * Provides access to lookout implementations (app, website, keyword detection).
 *
 * Default implementations are Noop for website and keyword lookouts.
 * AppLookout must be set explicitly as it requires the real implementation.
 */
object SirenLookoutProvider {

    /**
     * App lookout implementation.
     * Must be set before use - typically set during module initialization.
     */
    var appLookout: AppLookout? = null

    /**
     * Website lookout implementation.
     * Defaults to NoopWebsiteLookout until website detection is implemented.
     */
    var websiteLookout: WebsiteLookout = NoopWebsiteLookout()

    /**
     * Keyword lookout implementation.
     * Defaults to NoopKeywordLookout until keyword detection is implemented.
     */
    var keywordLookout: KeywordLookout = NoopKeywordLookout()

    /**
     * Reset all lookouts to defaults.
     * Useful for testing.
     */
    fun reset() {
        appLookout = null
        websiteLookout = NoopWebsiteLookout()
        keywordLookout = NoopKeywordLookout()
    }
}
