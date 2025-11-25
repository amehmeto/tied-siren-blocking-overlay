import TiedSirenBlockingOverlayModule from './TiedSirenBlockingOverlayModule'

export { TiedSirenBlockingOverlayModule }
export * from './TiedSirenBlockingOverlay.types'

/**
 * Displays a fullscreen blocking overlay on Android.
 *
 * @param packageName - The Android package name being blocked
 * @param blockUntil - Unix timestamp (milliseconds) until which the app is blocked
 * @throws ERR_INVALID_PACKAGE - If packageName is empty or invalid
 * @throws ERR_OVERLAY_LAUNCH - If the overlay activity fails to launch
 */
export async function showOverlay(packageName: string, blockUntil: number): Promise<void> {
  return TiedSirenBlockingOverlayModule.showOverlay(packageName, blockUntil)
}
