/**
 * Options for displaying the blocking overlay
 */
export interface ShowOverlayOptions {
    /** The Android package name being blocked */
    packageName: string;
    /** Unix timestamp (milliseconds) until which the app is blocked */
    blockUntil: number;
}
/**
 * Error codes thrown by the module
 */
export type BlockingOverlayErrorCode = 'ERR_INVALID_PACKAGE' | 'ERR_OVERLAY_LAUNCH';
//# sourceMappingURL=TiedSirenBlockingOverlay.types.d.ts.map