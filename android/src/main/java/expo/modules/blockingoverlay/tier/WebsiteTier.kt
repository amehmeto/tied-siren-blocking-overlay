package expo.modules.blockingoverlay.tier

/**
 * Interface for website-specific blocking.
 * Implementations handle the native mechanism to block website access.
 */
interface WebsiteTier {
    /**
     * Block the specified website domains.
     * @param domains List of domains to block (e.g., "facebook.com")
     */
    suspend fun block(domains: List<String>)

    /**
     * Unblock all currently blocked websites.
     */
    suspend fun unblockAll()
}
