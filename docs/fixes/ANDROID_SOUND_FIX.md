# Android Sound Fix

## Issue
The sound system was not working on Android. Despite sound files being present and the sound system being implemented, no audio was heard on Android devices.

## Root Cause
The Android sound implementation requires a two-step initialization process:

1. **Common initialization**: `GlobalSoundManager.initialize()` creates the `FileSoundManager` instance
2. **Platform-specific initialization**: `initializeAndroidAudio(context)` creates the Android `SoundPool`

The second step was never being called, so the `SoundPool` remained `null` and all sound playback silently failed.

## Solution
Added the missing `initializeAndroidAudio(context)` call to `MainActivity.onCreate()`:

```kotlin
// Initialize Android audio system for sound playback
initializeAndroidAudio(this)
```

This call is placed right after `AndroidContextProvider.initialize(this)` to ensure the application context is available.

## Implementation Details

### Modified Files
- `composeApp/src/androidMain/kotlin/de/egril/defender/MainActivity.kt`
  - Added import: `import de.egril.defender.audio.initializeAndroidAudio`
  - Added initialization call in `onCreate()` method

### How It Works
1. When the app starts, `MainActivity.onCreate()` is called
2. Context provider is initialized first
3. Audio system is initialized with `initializeAndroidAudio(this)`
   - Creates a `SoundPool` with 10 max streams
   - Configures audio attributes for game sounds
   - Stores the application context for loading sound files
4. Later, when `GlobalSoundManager.initialize()` is called from `App.kt`, the `FileSoundManager` is ready to use the initialized `SoundPool`

### Platform Comparison
- **Desktop**: Uses javax.sound.sampled API, no special initialization needed
- **iOS**: Uses AVAudioSession, initialized in `initializeAudioSystem()`
- **Android**: Uses SoundPool, requires context-based initialization in MainActivity

## Testing
- Build verification: ✓ Successful compilation
- Security check: ✓ No vulnerabilities detected
- Manual testing: Requires Android device/emulator to verify audio playback

## Future Considerations
The fix follows Android best practices:
- Audio attributes are set appropriately for game sounds (USAGE_GAME, CONTENT_TYPE_SONIFICATION)
- Application context is used (not activity context) to avoid memory leaks
- SoundPool is properly configured with sufficient streams for simultaneous sounds
