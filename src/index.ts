import type { BlockingWindow } from './TiedSirenBlockingOverlay.types'
import TiedSirenBlockingOverlayModule from './TiedSirenBlockingOverlayModule'

export { TiedSirenBlockingOverlayModule }
export * from './TiedSirenBlockingOverlay.types'

// ============================================================
// Schedule-based blocking API (#9, #18)
// ============================================================

/**
 * Sets the blocking schedule for native enforcement.
 * Once set, the native layer handles all blocking autonomously
 * based on the schedule and current time.
 *
 * @param windows - Array of time windows with their blocked packages
 * @throws ERR_NO_CONTEXT - If React context is not available
 * @throws ERR_INVALID_SCHEDULE - If schedule data is malformed
 * @throws ERR_SCHEDULE_FAILED - If storing the schedule fails
 *
 * @example
 * ```typescript
 * await setBlockingSchedule([
 *   {
 *     id: 'work-hours',
 *     startTime: '09:00',
 *     endTime: '17:00',
 *     packageNames: ['com.instagram.android', 'com.twitter.android']
 *   },
 *   {
 *     id: 'sleep-time',
 *     startTime: '22:00',
 *     endTime: '06:00', // Overnight window
 *     packageNames: ['com.facebook.katana']
 *   }
 * ])
 * ```
 */
export async function setBlockingSchedule(
  windows: BlockingWindow[],
): Promise<void> {
  return TiedSirenBlockingOverlayModule.setBlockingSchedule(windows)
}

/**
 * Gets the current blocking schedule from native storage.
 *
 * @returns Array of currently configured blocking windows
 * @throws ERR_NO_CONTEXT - If React context is not available
 */
export async function getBlockingSchedule(): Promise<BlockingWindow[]> {
  return TiedSirenBlockingOverlayModule.getBlockingSchedule()
}

/**
 * Clears the blocking schedule from native storage.
 * After calling this, no apps will be blocked.
 *
 * @throws ERR_NO_CONTEXT - If React context is not available
 */
export async function clearBlockingSchedule(): Promise<void> {
  return TiedSirenBlockingOverlayModule.clearBlockingSchedule()
}

// ============================================================
// Direct blocking API (legacy/manual control)
// ============================================================

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
