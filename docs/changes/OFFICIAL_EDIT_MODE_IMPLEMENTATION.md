# Official Edit Mode Implementation Summary

## Overview

This document summarizes the implementation of the `official` compile flag for editing official maps and levels in Defender of Egril.

## Problem Statement

The game needed a compile flag called "official" that:
1. When set, allows editing of official game data (maps and levels) directly in the editor
2. Shows a warning dialog on game close if official data has been modified
3. Warns that changes will be overwritten on next game start

## Solution

### 1. Compile Flag (build.gradle.kts)

Added gradle property `official` (default: false):

```kotlin
// Official editing flag - can be set via gradle.properties or command line: -Pofficial=true
val official: Boolean = project.findProperty("official")?.toString()?.toBoolean() ?: false
```

### 2. Generated Code (OfficialEditMode.kt)

Created gradle task that generates compile-time constant:

```kotlin
val generateOfficialEditModeConstant by tasks.registering {
    // Generates OfficialEditMode.kt with enabled flag
}
```

Generated file:
```kotlin
object OfficialEditMode {
    const val enabled: Boolean = false  // or true based on property
}
```

### 3. Change Tracking (OfficialDataChangeTracker.kt)

New singleton to track modifications:

```kotlin
object OfficialDataChangeTracker {
    fun trackMapModified(mapId: String)
    fun trackLevelModified(levelId: String)
    fun hasModifiedOfficialData(): Boolean
    fun getModifiedOfficialMaps(): List<String>
    fun getModifiedOfficialLevels(): List<String>
    fun clearTracking()
}
```

Integrated into `EditorStorage.saveMap()` and `EditorStorage.saveLevel()` to automatically track changes when official content is saved.

### 4. UI Changes

Updated all editor UI components to check the flag:

**Before:**
```kotlin
enabled = !map.isOfficial  // Always disabled for official content
```

**After:**
```kotlin
enabled = !map.isOfficial || OfficialEditMode.enabled  // Can edit if flag is set
```

Files modified:
- `MapEditorHeader.kt` - Map name input
- `MapEditorView.kt` - Save map button
- `MapListCard.kt` - Delete map button
- `LevelEditor.kt` - Save/delete level buttons
- `LevelInfoTab.kt` - Title/subtitle inputs

### 5. Warning System

#### WindowCloseHandler Extension

Extended to check for official data changes:

```kotlin
object WindowCloseHandler {
    private var officialDataChangedChecker: (() -> Boolean)? = null
    
    fun setOfficialDataChangedChecker(checker: (() -> Boolean)?)
    fun hasOfficialDataChanged(): Boolean
}
```

#### App.kt Integration

Registered checker on app startup:

```kotlin
LaunchedEffect(Unit) {
    if (OfficialEditMode.enabled) {
        WindowCloseHandler.setOfficialDataChangedChecker { 
            OfficialDataChangeTracker.hasModifiedOfficialData()
        }
    }
}
```

#### Desktop main.kt

Added dialog to warn on close:

```kotlin
Window(
    onCloseRequest = {
        // Check for official data changes first
        if (OfficialEditMode.enabled && WindowCloseHandler.hasOfficialDataChanged()) {
            showOfficialDataChangedDialog = true
        } else if (WindowCloseHandler.hasUnsavedChanges()) {
            showUnsavedChangesDialog = true
        } else {
            exitApplication()
        }
    }
)

// Display OfficialDataChangedDialog with modified content list
```

#### OfficialDataChangedDialog.kt

New composable dialog that:
- Shows warning message
- Lists modified maps
- Lists modified levels
- Provides "Understood" button to acknowledge and continue closing

### 6. Localization

Added strings in all 5 supported languages:

- `official_data_changed_title`: "Official Game Data Modified"
- `official_data_changed_message`: Detailed warning about overwrite on restart
- `official_data_modified_maps`: "Modified maps: %1$s"
- `official_data_modified_levels`: "Modified levels: %1$s"
- `understood`: Acknowledgment button text

Languages: English, German, Spanish, French, Italian

### 7. Testing

Created comprehensive unit tests (`OfficialDataChangeTrackerTest.kt`):

- ✅ Initially no modifications tracked
- ✅ Track official map modification
- ✅ Track official level modification
- ✅ Track multiple official modifications
- ✅ Non-official content not tracked
- ✅ Clear tracking removes all modifications
- ✅ Duplicate modifications only tracked once

All tests pass successfully.

### 8. Documentation

Created detailed documentation (`docs/features/OFFICIAL_EDIT_MODE.md`) covering:
- Usage instructions
- Default behavior vs. enabled behavior
- Warning dialog details
- Implementation details
- Best practices
- File structure
- Localization
- Testing

## Usage

### Enable for Development

```bash
# Run desktop version with official editing
./gradlew :composeApp:run -Pofficial=true

# Build with official editing
./gradlew :composeApp:build -Pofficial=true
```

### Or add to gradle.properties

```properties
official=true
```

### Production Build (default)

```bash
# Official editing disabled by default
./gradlew :composeApp:build
```

## Behavior Comparison

### Default (official=false)
- ✅ Official content is read-only
- ✅ Edit/save/delete buttons disabled for official content
- ✅ Users must copy to create user versions
- ❌ No warning on app close

### Enabled (official=true)
- ✅ Official content is editable
- ✅ All editor controls work for official content
- ✅ Changes are tracked
- ✅ Warning dialog on app close if modified
- ⚠️ Changes overwritten on next app start

## Benefits

1. **Minimal Changes**: Follows existing patterns (similar to `WithImpressum`)
2. **Type Safety**: Compile-time constant, no runtime overhead
3. **Safe Default**: Official editing disabled by default
4. **User Protection**: Clear warning about consequences
5. **Developer Friendly**: Simple flag to enable during development
6. **Fully Localized**: Supports all game languages
7. **Well Tested**: Comprehensive unit tests included
8. **Well Documented**: Detailed documentation for users and developers

## Files Changed

### Core Implementation (17 files)
1. `composeApp/build.gradle.kts` - Added flag and generation task
2. `composeApp/src/commonMain/kotlin/de/egril/defender/editor/OfficialDataChangeTracker.kt` - New tracker
3. `composeApp/src/commonMain/kotlin/de/egril/defender/editor/EditorStorage.kt` - Integrated tracking
4. `composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/OfficialDataChangedDialog.kt` - New dialog
5. `composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/map/MapEditorHeader.kt` - UI update
6. `composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/map/MapEditorView.kt` - UI update
7. `composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/map/MapListCard.kt` - UI update
8. `composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/level/LevelEditor.kt` - UI update
9. `composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/level/LevelInfoTab.kt` - UI update
10. `composeApp/src/commonMain/kotlin/de/egril/defender/utils/WindowCloseHandler.kt` - Extended checker
11. `composeApp/src/commonMain/kotlin/de/egril/defender/App.kt` - Register checker
12. `composeApp/src/desktopMain/kotlin/de/egril/defender/main.kt` - Show dialog

### Localization (5 files)
13. `composeApp/src/commonMain/composeResources/values/strings.xml` - English
14. `composeApp/src/commonMain/composeResources/values-de/strings.xml` - German
15. `composeApp/src/commonMain/composeResources/values-es/strings.xml` - Spanish
16. `composeApp/src/commonMain/composeResources/values-fr/strings.xml` - French
17. `composeApp/src/commonMain/composeResources/values-it/strings.xml` - Italian

### Testing & Documentation (2 files)
18. `composeApp/src/commonTest/kotlin/de/egril/defender/editor/OfficialDataChangeTrackerTest.kt` - Tests
19. `docs/features/OFFICIAL_EDIT_MODE.md` - Documentation

### Generated (1 file)
20. `composeApp/build/generated/source/buildConfig/commonMain/kotlin/de/egril/defender/OfficialEditMode.kt` - Auto-generated

**Total: 20 files**

## Quality Assurance

- ✅ Code compiles successfully
- ✅ All tests pass
- ✅ No runtime errors
- ✅ Follows existing code patterns
- ✅ Minimal invasive changes
- ✅ Fully localized
- ✅ Comprehensively documented
- ✅ Safe by default

## Future Considerations

1. Could add web/WASM support for close warning (currently desktop only)
2. Could persist warning across sessions (show only once per session)
3. Could add option to automatically copy official content before editing
4. Could add visual indicators in editor when in official edit mode

## Conclusion

The implementation successfully meets all requirements from the issue:
- ✅ Compile flag "official" added
- ✅ Allows editing of official game data when enabled
- ✅ Warning dialog on close when official data modified
- ✅ Warning explains data will be overwritten on restart

The solution is production-ready, well-tested, and follows the project's coding standards and patterns.
