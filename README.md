# tied-siren-blocking-overlay

An Expo module that displays a fullscreen blocking overlay on Android for TiedSiren.

## Features

- Launch fullscreen blocking Activity within 200ms
- Non-dismissible via back button
- "Close" button redirects to Android home screen
- Prevents user interaction with blocked apps
- Graceful error handling without app crashes

## Installation

```bash
npm install @anthropic/tied-siren-blocking-overlay
```

## Usage

```typescript
import { showOverlay } from '@anthropic/tied-siren-blocking-overlay'

// Show blocking overlay for a package
await showOverlay('com.facebook.katana', Date.now() + 3600000)
```

### Error Handling

```typescript
import { showOverlay } from '@anthropic/tied-siren-blocking-overlay'

try {
  await showOverlay('com.facebook.katana', Date.now() + 3600000)
} catch (error) {
  if (error.code === 'ERR_INVALID_PACKAGE') {
    console.error('Invalid package name provided')
  } else if (error.code === 'ERR_OVERLAY_LAUNCH') {
    console.error('Failed to launch overlay:', error.message)
  }
}
```

## API

### `showOverlay(packageName: string, blockUntil: number): Promise<void>`

Displays a fullscreen blocking overlay.

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `packageName` | `string` | The Android package name being blocked (e.g., `com.facebook.katana`) |
| `blockUntil` | `number` | Unix timestamp (milliseconds) until which the app is blocked |

**Returns:** `Promise<void>` - Resolves when the overlay is launched successfully.

**Throws:** See Error Codes below.

## Error Codes

| Code | Description | Cause |
|------|-------------|-------|
| `ERR_INVALID_PACKAGE` | Invalid or empty package name provided | The `packageName` parameter is empty, null, or contains only whitespace |
| `ERR_OVERLAY_LAUNCH` | Failed to launch the overlay activity | React context unavailable, or Android system denied the activity start |

All errors are logged to Android Logcat with tag `TiedSirenBlockingOverlay` for debugging.

### Viewing Logcat Errors

```bash
adb logcat -s TiedSirenBlockingOverlay:*
```

## Platform Support

| Platform | Supported | Notes |
|----------|-----------|-------|
| Android | ✅ API 26+ | Android 8.0 (Oreo) and above |
| iOS | ❌ | Android-only feature |
| Web | ❌ | Android-only feature |

## Technical Details

### Activity Configuration

The `BlockingOverlayActivity` is configured with:

- `launchMode="singleTask"` - Ensures only one overlay instance exists
- `excludeFromRecents="true"` - Hides from recent apps
- `taskAffinity=""` - Runs in its own task

### Intent Flags

The overlay is launched with:

- `FLAG_ACTIVITY_NEW_TASK` - Starts in a new task
- `FLAG_ACTIVITY_CLEAR_TASK` - Clears any existing task
- `FLAG_ACTIVITY_NO_HISTORY` - Not kept in activity stack

## License

MIT
