package expo.modules.blockingoverlay.tier

/**
 * Interface for keyword-specific blocking.
 * Implementations handle the native mechanism to block content containing keywords.
 */
interface KeywordTier {
    /**
     * Block content containing the specified keywords.
     * @param keywords List of keywords to block
     */
    suspend fun block(keywords: List<String>)

    /**
     * Unblock all currently blocked keywords.
     */
    suspend fun unblockAll()
}
