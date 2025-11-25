import TiedSirenBlockingOverlayModule from './TiedSirenBlockingOverlayModule';
export { TiedSirenBlockingOverlayModule };
export * from './TiedSirenBlockingOverlay.types';
/**
 * Displays a fullscreen blocking overlay on Android.
 *
 * @param packageName - The Android package name being blocked
 * @throws ERR_INVALID_PACKAGE - If packageName is empty or invalid
 * @throws ERR_OVERLAY_LAUNCH - If the overlay activity fails to launch
 */
export async function showOverlay(packageName) {
    return TiedSirenBlockingOverlayModule.showOverlay(packageName);
}
//# sourceMappingURL=index.js.map