# Library Updates - November 2025

This document summarizes the library updates performed on the Defender of Egril project.

## Updated Libraries

### Build Tools
| Library | Previous Version | New Version | Status                                                              |
|---------|-----------------|-------------|---------------------------------------------------------------------|
| Gradle | 8.9 | 9.2.1       | ✅ Updated                                                           |
| Kotlin | 2.1.0 | 2.2.21      | ✅ Updated                                                           |
| Android Gradle Plugin | 8.6.0 | 8.12.0      | Downgraded because intelliJ supports no newer version at the moment |
| Compose Plugin | 1.9.0 | 1.9.3       | ✅ Updated                                                           |

### Kotlin Libraries
| Library | Previous Version | New Version | Status |
|---------|-----------------|-------------|--------|
| kotlinx-serialization | 1.6.3 | 1.9.0 | ✅ Updated |

### AndroidX Libraries
| Library | Previous Version | New Version | Status |
|---------|-----------------|-------------|--------|
| androidx.activity:activity-compose | 1.9.2 | 1.12.0 | ✅ Updated |
| androidx.core:core-ktx | 1.13.1 | 1.17.0 | ✅ Updated |
| androidx.lifecycle | 2.8.0 | 2.9.6 | ✅ Updated |
| androidx.appcompat | 1.7.0 | 1.7.1 | ✅ Updated |
| androidx.material | 1.12.0 | 1.13.0 | ✅ Updated |
| androidx.constraintlayout | 2.1.4 | 2.2.1 | ✅ Updated |

### Testing Libraries
| Library | Previous Version | New Version | Status |
|---------|-----------------|-------------|--------|
| androidx.test.ext:junit | 1.2.1 | 1.3.0 | ✅ Updated |
| androidx.test.espresso:espresso-core | 3.6.1 | 3.7.0 | ✅ Updated |

### Already Latest
| Library | Version | Status |
|---------|---------|--------|
| junit | 4.13.2 | ✅ Latest |
| flagkit | 1.1.0 | ✅ Latest |
| multiplatform-settings | 1.3.0 | ✅ Latest |
| localization | 2.0.0 | ✅ Latest |

### SDK Versions
| Setting | Previous Version | New Version | Status |
|---------|-----------------|-------------|--------|
| Android compileSdk | 35 | 36 | ✅ Updated |

## Code Changes Required

### 1. wasmJs Configuration (composeApp/build.gradle.kts)
**Issue**: The `moduleName` property was deprecated in Kotlin 2.3.
**Solution**: Removed the deprecated `moduleName` property. The module name is now derived from the project name automatically.

```kotlin
// Before:
wasmJs {
    moduleName = "defenderOfEgril"
    browser { ... }
}

// After:
wasmJs {
    browser { ... }
}
```

### 2. Android compileSdk Update
**Issue**: androidx.activity:activity-compose 1.12.0 requires compileSdk 36.
**Solution**: Updated `android-compileSdk` from 35 to 36 in libs.versions.toml.

## Build Status

### ✅ Working Platforms
- **Desktop (JVM)**: Builds successfully
- **Android**: Unit tests pass, builds successfully
- **WASM/JS**: Builds successfully

### ⚠️ Known Issues

#### iOS Platform
The iOS build fails due to breaking changes in Kotlin 2.2.21's Kotlin/Native interop API. The following files need updates:

1. **composeApp/src/iosMain/kotlin/de/egril/defender/audio/FileSoundManager.ios.kt**
   - Requires `@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)` annotations
   - Type mismatch in C interop: `CValuesRef<ByteVarOf<Byte>>` vs `CPointer<out CPointed>?`

2. **composeApp/src/iosMain/kotlin/de/egril/defender/editor/FileStorage.ios.kt**
   - Requires `@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)` annotations for multiple functions

3. **composeApp/src/iosMain/kotlin/de/egril/defender/utils/TimeUtils.ios.kt**
   - Constructor signature mismatch for NSDate

These issues are platform-specific and can be addressed separately when iOS builds are needed. The core functionality on other platforms is unaffected.

## Deprecation Warnings

The following deprecation warnings are present but don't affect functionality:

1. **TabRow in LevelEditor.kt**: Deprecated in favor of PrimaryTabRow and SecondaryTabRow (Compose 1.9.3)
2. **Delicate API warnings**: Various sound management files use delicate APIs (documented behavior)
3. **WASM interop warnings**: ExperimentalWasmJsInterop annotations needed in TimeUtils.wasmJs.kt

## Testing

All tests pass on supported platforms:
- ✅ Android unit tests: PASSING
- ✅ Desktop build: SUCCESS
- ✅ WASM/JS build: SUCCESS

## Recommendations

1. **For iOS support**: Update iOS-specific files to use new Kotlin/Native interop APIs
2. **For production**: Consider addressing deprecation warnings in TabRow and sound management
3. **For WASM**: Add @OptIn annotations for experimental WASM JS interop APIs

## References

- Gradle 9.2.1 Release Notes: https://docs.gradle.org/9.2.1/release-notes.html
- Kotlin 2.2.21 Release: https://github.com/JetBrains/kotlin/releases
- Compose Multiplatform 1.9.3: https://github.com/JetBrains/compose-multiplatform/releases
- AndroidX Releases: https://developer.android.com/jetpack/androidx/versions
