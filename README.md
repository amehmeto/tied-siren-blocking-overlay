# tied-siren-blocking-overlay

An Expo module that displays a fullscreen blocking overlay on Android for TiedSiren.

## Features

- Launch fullscreen blocking Activity within 200ms
- Non-dismissible via back button
- "Close" button redirects to Android home screen
- Prevents user interaction with blocked apps

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

## API

### `showOverlay(packageName: string, blockUntil: number): Promise<void>`

Displays a fullscreen blocking overlay.

- `packageName`: The Android package name being blocked
- `blockUntil`: Unix timestamp (milliseconds) until which the app is blocked

### Error Codes

- `ERR_INVALID_PACKAGE`: Invalid or empty package name provided
- `ERR_OVERLAY_LAUNCH`: Failed to launch the overlay activity

## Platform Support

- Android: API 26+ (Android 8.0+)
- iOS: Not supported (Android-only feature)

## License

MIT
