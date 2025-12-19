# Web/Wasm Implementation Guide

## Overview

Defender of Egril now supports running in web browsers via Kotlin/Wasm compilation target. This allows the game to run directly in modern browsers without requiring any installation.

## Architecture

### Platform-Specific Implementations

#### 1. Main Entry Point
- **File**: `composeApp/src/wasmJsMain/kotlin/com/defenderofegril/main.kt`
- Uses `ComposeViewport` to render the Compose UI in the browser
- Attaches to the document body element

#### 2. File Storage (Save Games)
- **File**: `composeApp/src/wasmJsMain/kotlin/com/defenderofegril/editor/FileStorage.wasmJs.kt`
- Uses browser `localStorage` API for persistence
- Save games are stored with prefix `defender-of-egril:`
- Compatible with the common `FileStorage` interface
- **Virtual Directories**: Since localStorage has no directory concept, directories are virtual. The `fileExists()` method checks both for file keys and directory prefixes (any key starting with `path + "/"`).

#### 3. Time Utilities
- **File**: `composeApp/src/wasmJsMain/kotlin/com/defenderofegril/utils/TimeUtils.wasmJs.kt`
- Uses JavaScript `Date` API via `@JsFun` external declarations
- Provides `currentTimeMillis()` and `formatTimestamp()` functions
- Format: "MMM dd, yyyy HH:mm" (e.g., "Jan 15, 2024 14:30")

#### 4. Mouse Wheel Zoom
- **File**: `composeApp/src/wasmJsMain/kotlin/com/defenderofegril/ui/MouseWheelZoom.wasmJs.kt`
- Implements mouse wheel zoom similar to desktop version
- Uses Compose UI's pointer event system

#### 5. Platform Utils
- **File**: `composeApp/src/wasmJsMain/kotlin/com/defenderofegril/ui/PlatformUtils.wasmJs.kt`
- Level editor is disabled on web (`isEditorAvailable() = false`)

### Build Configuration

The wasmJs target is configured in `composeApp/build.gradle.kts`:

```kotlin
wasmJs {
    moduleName = "defenderOfEgril"
    browser {
        commonWebpackConfig {
            outputFileName = "defenderOfEgril.js"
        }
    }
    binaries.executable()
}
```

## Building and Running

### Development Mode

Start the development server with hot reload:
```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

Access at: http://localhost:8080

### Production Build

Create an optimized production bundle:
```bash
./gradlew :composeApp:wasmJsBrowserProductionWebpack
```

Output location: `composeApp/build/kotlin-webpack/wasmJs/productionExecutable/`

Files generated:
- `defenderOfEgril.js` (~527 KB) - JavaScript bundle
- `*.wasm` (~8 MB) - WebAssembly module including Skiko runtime
- `index.html` - Entry point HTML file

## Browser Requirements

- Chrome 119 or later
- Firefox 120 or later
- Safari 17 or later
- Edge 119 or later

These browsers support the required WebAssembly features:
- WebAssembly GC (Garbage Collection)
- WebAssembly Exception Handling
- WebAssembly SIMD (for Skiko graphics)

## Features

### Supported ✅
- Full game play functionality
- All 5 levels
- Tower placement and upgrades
- Enemy waves and combat
- Save/Load games (using localStorage)
- Mouse wheel zoom
- Drag to pan
- World map progression
- Cheat codes

### Not Supported ❌
- Level editor (desktop-only feature)

## Technical Details

### Platform Abstractions

The implementation uses Kotlin Multiplatform's expect/actual mechanism for platform-specific code:

1. **Time Operations**: Created `TimeUtils` with expect/actual for all platforms
   - Replaces JVM-specific `System.currentTimeMillis()`
   - Replaces `SimpleDateFormat` with platform-specific formatting

2. **Storage**: Uses `FileStorage` interface with web-specific localStorage implementation

3. **UI Input**: Mouse wheel events work the same as desktop via Compose UI's event system

### JavaScript Interop

External JavaScript functions are declared using `@JsFun`:

```kotlin
@JsFun("() => Date.now()")
external fun jsDateNow(): Double
```

This allows calling JavaScript APIs from Kotlin/Wasm while maintaining type safety.

## Deployment

### Static Hosting

The production build can be deployed to any static web hosting service:

1. Build production bundle
2. Copy files from `composeApp/build/kotlin-webpack/wasmJs/productionExecutable/`
3. Upload to hosting (GitHub Pages, Netlify, Vercel, etc.)

### Content Delivery

Recommended headers for optimal performance:
```
Content-Type: application/wasm (for .wasm files)
Content-Encoding: gzip or brotli (compress all files)
Cache-Control: public, max-age=31536000 (for versioned assets)
```

## Performance Considerations

- Initial load: ~8-9 MB download (WASM + JS)
- Subsequent loads: Cached by browser
- Runtime performance: Near-native speed with WebAssembly
- Save games: No server needed, stored locally

## Debugging

### Development Tools

1. Enable browser DevTools
2. Source maps are included in development builds
3. Console logs work as expected
4. Network tab shows WASM module loading

### Common Issues

**Game doesn't load:**
- Check browser console for errors
- Verify WebAssembly support in browser
- Clear browser cache and reload

**Save games not working:**
- Check localStorage is enabled
- Check available storage quota
- Use DevTools > Application > Local Storage to inspect

**Slow performance:**
- Use production build (much smaller and faster)
- Check browser hardware acceleration is enabled
- Close unnecessary tabs to free up memory

## Future Improvements

Potential enhancements:
- Progressive Web App (PWA) support for offline play
- Service Worker for caching
- Cloud save sync (requires backend)
- Mobile touch controls optimization
- Reduced bundle size through code splitting
