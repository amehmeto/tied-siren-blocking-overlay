/**
 * Options for displaying the blocking overlay
 */
export interface ShowOverlayOptions {
  /** The Android package name being blocked */
  packageName: string
  /** Unix timestamp (milliseconds) until which the app is blocked */
  blockUntil: number
}

/**
 * A time window during which specific apps should be blocked.
 * Used with setBlockingSchedule() to configure native blocking.
 */
export interface BlockingWindow {
  /** Unique identifier for this blocking window */
  id: string
  /** Start time in HH:mm format (24-hour), e.g., "09:00" */
  startTime: string
  /** End time in HH:mm format (24-hour), e.g., "17:00" */
  endTime: string
  /** List of Android package names to block during this window */
  packageNames: string[]
}

/**
 * Error codes thrown by the module
 */
export type BlockingOverlayErrorCode =
  | 'ERR_INVALID_PACKAGE'
  | 'ERR_OVERLAY_LAUNCH'
  | 'ERR_NO_CONTEXT'
  | 'ERR_INVALID_SCHEDULE'
  | 'ERR_SCHEDULE_FAILED'
