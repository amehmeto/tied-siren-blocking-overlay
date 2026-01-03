package expo.modules.blockingoverlay.tier

/**
 * Interface for app-specific blocking.
 * Implementations handle the native mechanism to block app access.
 */
interface AppTier {
    /**
     * Block the specified app packages.
     * @param packages List of Android package names to block
     */
    suspend fun block(packages: List<String>)

    /**
     * Unblock all currently blocked apps.
     */
    suspend fun unblockAll()
}
