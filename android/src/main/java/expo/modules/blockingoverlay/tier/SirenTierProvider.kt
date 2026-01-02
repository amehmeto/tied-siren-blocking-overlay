package expo.modules.blockingoverlay.tier

/**
 * Dependency injection provider for SirenTier implementations.
 * Provides access to tier implementations (app, website, keyword blocking).
 *
 * Default implementations are Noop for website and keyword tiers.
 * AppTier must be set explicitly as it requires the real implementation.
 */
object SirenTierProvider {

    /**
     * App tier implementation.
     * Must be set before use - typically set during module initialization.
     */
    var appTier: AppTier? = null

    /**
     * Website tier implementation.
     * Defaults to NoopWebsiteTier until website blocking is implemented.
     */
    var websiteTier: WebsiteTier = NoopWebsiteTier()

    /**
     * Keyword tier implementation.
     * Defaults to NoopKeywordTier until keyword blocking is implemented.
     */
    var keywordTier: KeywordTier = NoopKeywordTier()

    /**
     * Reset all tiers to defaults.
     * Useful for testing.
     */
    fun reset() {
        appTier = null
        websiteTier = NoopWebsiteTier()
        keywordTier = NoopKeywordTier()
    }
}
