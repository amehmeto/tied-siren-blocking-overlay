import TiedSirenBlockingOverlayModule from './TiedSirenBlockingOverlayModule'

export { TiedSirenBlockingOverlayModule }
export * from './TiedSirenBlockingOverlay.types'

/**
 * Displays a fullscreen blocking overlay on Android.
 *
 * @param packageName - The Android package name being blocked
 * @throws ERR_INVALID_PACKAGE - If packageName is empty or invalid
 * @throws ERR_OVERLAY_LAUNCH - If the overlay activity fails to launch
 */
export async function showOverlay(packageName: string): Promise<void> {
  return TiedSirenBlockingOverlayModule.showOverlay(packageName)
}

/**
 * Sets the list of blocked app package names in SharedPreferences.
 * These apps will trigger the blocking overlay when opened.
 *
 * @param packageNames - Array of Android package names to block
 * @throws ERR_NO_CONTEXT - If React context is not available
 */
export async function setBlockedApps(packageNames: string[]): Promise<void> {
  return TiedSirenBlockingOverlayModule.setBlockedApps(packageNames)
}

/**
 * Gets the current list of blocked app package names from SharedPreferences.
 *
 * @returns Array of currently blocked Android package names
 * @throws ERR_NO_CONTEXT - If React context is not available
 */
export async function getBlockedApps(): Promise<string[]> {
  return TiedSirenBlockingOverlayModule.getBlockedApps()
}

/**
 * Clears all blocked apps from SharedPreferences.
 *
 * @throws ERR_NO_CONTEXT - If React context is not available
 */
export async function clearBlockedApps(): Promise<void> {
  return TiedSirenBlockingOverlayModule.clearBlockedApps()
}

/**
 * Fully qualified class name for BlockingCallback.
 * Use this with setCallbackClass() in expo-foreground-service.
 */
export const BLOCKING_CALLBACK_CLASS =
  'expo.modules.blockingoverlay.BlockingCallback'
