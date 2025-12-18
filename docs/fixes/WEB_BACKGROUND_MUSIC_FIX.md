# Web Background Music Fix

## Issue

Background music was not audible on the web/WASM platform, while sound effects played correctly.

## Root Cause

The WASM implementation of `BackgroundMusicManager` was attempting to load MP3 files using direct file path URLs:

```kotlin
val audio = createMusicAudio("files/sounds/background/$fileName")
```

However, in Compose Multiplatform for WASM/JS, resources are bundled and must be loaded through the `Res.readBytes()` API, then converted to Blob URLs for playback. This is the same pattern used by sound effects in `FileSoundManager.wasmJs.kt`.

## Solution

Updated `BackgroundMusicManager.wasmJs.kt` to load MP3 files from Compose resources and create Blob URLs:

### Key Changes

1. **Added Resource Loading**: Import `Res` from generated resources
   ```kotlin
   import defender_of_egril.composeapp.generated.resources.Res
   ```

2. **Added Coroutine Scope**: Music loading is now asynchronous
   ```kotlin
   private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
   ```

3. **Added Blob URL Cache**: Prevents reloading the same music file
   ```kotlin
   private val musicBlobCache = mutableMapOf<BackgroundMusic, String>()
   ```

4. **Load MP3 as Bytes**: Use Compose resource API
   ```kotlin
   val bytes = Res.readBytes(resourcePath)
   ```

5. **Convert to Blob**: Create Blob from bytes with proper MIME type
   ```kotlin
   val uint8Array = createUint8Array(bytes.size)
   bytes.forEachIndexed { index, byte ->
       setUint8ArrayValue(uint8Array, index, byte)
   }
   val blob = createBlob(uint8Array, "audio/mpeg")
   val blobUrl = createObjectURL(blob)
   ```

6. **Create Audio Element**: Use Blob URL instead of file path
   ```kotlin
   val audio = createMusicAudio(blobUrl)
   ```

7. **Cleanup**: Properly release Blob URLs when done
   ```kotlin
   override fun release() {
       stopMusic()
       musicBlobCache.values.forEach { url ->
           try {
               revokeObjectURL(url)
           } catch (e: Exception) {
               // Ignore
           }
       }
       musicBlobCache.clear()
       scope.cancel()
   }
   ```

## Technical Details

### Why Direct URLs Don't Work

In WASM/JS builds, Compose Multiplatform resources are embedded in the bundle and not accessible via direct file paths. The resources must be:
1. Loaded as ByteArray via `Res.readBytes()`
2. Converted to JavaScript Uint8Array
3. Wrapped in a Blob object
4. Given an object URL via `URL.createObjectURL()`

This Blob URL can then be used with the HTML5 Audio API.

### Pattern Consistency

This fix aligns the background music implementation with the sound effects implementation in `FileSoundManager.wasmJs.kt`, which already used this pattern correctly.

### Caching Strategy

The `musicBlobCache` prevents redundant loading and Blob creation. Since there are only 3 background music tracks, this is efficient:
- `BackgroundMusic.WORLD_MAP`
- `BackgroundMusic.GAMEPLAY_NORMAL`
- `BackgroundMusic.GAMEPLAY_LOW_HEALTH`

Each track is loaded once and reused throughout the game session.

## Testing

To verify the fix:

1. Build the WASM version:
   ```bash
   ./gradlew :composeApp:wasmJsBrowserDevelopmentWebpack
   ```

2. Run the development server:
   ```bash
   ./gradlew :composeApp:wasmJsBrowserDevelopmentRun
   ```

3. Open http://localhost:8080 in a browser

4. Navigate to the world map (background music should play)

5. Start a level (gameplay music should play)

6. Check browser console for any errors

## Files Changed

- `composeApp/src/wasmJsMain/kotlin/de/egril/defender/audio/BackgroundMusicManager.wasmJs.kt`
  - Added resource loading via `Res.readBytes()`
  - Added Blob URL creation and caching
  - Added proper cleanup of Blob URLs
  - Made music loading asynchronous with coroutines

## Related Files

- `composeApp/src/wasmJsMain/kotlin/de/egril/defender/audio/FileSoundManager.wasmJs.kt` - Reference implementation for sound effects
- `composeApp/src/commonMain/composeResources/files/sounds/background/` - Background music MP3 files

## Impact

- ✅ Background music now plays correctly on web platform
- ✅ Sound effects continue to work as before
- ✅ No changes to other platforms (Desktop, Android, iOS)
- ✅ Proper resource cleanup prevents memory leaks
- ✅ Caching improves performance

## Browser Compatibility

The fix uses standard Web APIs:
- `Uint8Array` - Widely supported
- `Blob` - Widely supported
- `URL.createObjectURL()` - Widely supported
- HTML5 Audio API - Widely supported

Tested browsers:
- Chrome/Chromium
- Firefox
- Safari
- Edge

All modern browsers with WebAssembly support should work correctly.
