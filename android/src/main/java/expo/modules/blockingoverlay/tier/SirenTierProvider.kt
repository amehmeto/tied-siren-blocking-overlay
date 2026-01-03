package expo.modules.blockingoverlay.tier

/**
 * Dependency injection provider for SirenTier implementations.
 * Provides access to tier implementations (app, website, keyword blocking).
 *
 * Default implementations are Noop for website and keyword tiers.
 * AppTier must be set explicitly as it requires the real implementation.
 */
object SirenTierProvider {

    private var _appTier: AppTier? = null

    /**
     * App tier implementation.
     * Must be set before use - typically set during module initialization.
     * @throws IllegalStateException if accessed before being set
     */
    var appTier: AppTier
        get() = _appTier
            ?: throw IllegalStateException(
                "AppTier not initialized. Call SirenTierProvider.appTier = <implementation> " +
                "during module initialization before accessing."
            )
        set(value) {
            _appTier = value
        }

    /**
     * Check if appTier has been initialized.
     */
    val isAppTierInitialized: Boolean
        get() = _appTier != null

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
        _appTier = null
        websiteTier = NoopWebsiteTier()
        keywordTier = NoopKeywordTier()
    }
}
