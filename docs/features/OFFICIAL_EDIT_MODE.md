# Official Edit Mode Compile Flag

## Overview

The `official` compile flag enables editing of official game data (maps and levels) directly in the level editor. This is intended for developers working on the official game content.

## Usage

### Building with Official Edit Mode

To enable official edit mode, add the `-Pofficial=true` flag when building:

```bash
# Build desktop version with official editing enabled
./gradlew :composeApp:run -Pofficial=true

# Build Android APK with official editing enabled
./gradlew :composeApp:assembleDebug -Pofficial=true

# Build for production (official editing disabled by default)
./gradlew :composeApp:build
```

You can also add it to `gradle.properties`:

```properties
official=true
```

### Default Behavior

By default, `official` is set to `false`, which means:
- Official maps and levels are read-only in the editor
- All save, delete, and edit buttons are disabled for official content
- Users can only copy official content to create user versions

When `official=true`:
- Official maps and levels can be edited, saved, and deleted
- All editor controls work normally for official content
- A warning dialog appears on app close if official data was modified

## Warning Dialog

When official edit mode is enabled and you modify official game data, a warning dialog will appear when closing the application:

**Title:** "Official Game Data Modified"

**Message:** 
> You have made changes to official game data (maps or levels).
>
> These changes will be overwritten when you restart the game, as official content is restored from the repository on each launch.
>
> To preserve your changes, consider copying the official content to create user versions instead.

The dialog lists all modified official maps and levels.

## Implementation Details

### Generated Code

The build process generates `OfficialEditMode.kt`:

```kotlin
package de.egril.defender

object OfficialEditMode {
    const val enabled: Boolean = true  // or false
}
```

### Tracking Changes

The `OfficialDataChangeTracker` object tracks modifications to official content:

```kotlin
// Track map modification
OfficialDataChangeTracker.trackMapModified("map_tutorial")

// Track level modification
OfficialDataChangeTracker.trackLevelModified("welcome_to_defender_of_egril")

// Check if any official data has been modified
val hasChanges = OfficialDataChangeTracker.hasModifiedOfficialData()

// Get lists of modified content
val modifiedMaps = OfficialDataChangeTracker.getModifiedOfficialMaps()
val modifiedLevels = OfficialDataChangeTracker.getModifiedOfficialLevels()
```

### UI Integration

All editor UI components check `OfficialEditMode.enabled`:

```kotlin
// Enable editing if not official OR if official edit mode is enabled
enabled = !map.isOfficial || OfficialEditMode.enabled
```

This applies to:
- Map name input field (MapEditorHeader.kt)
- Save map button (MapEditorView.kt)
- Delete map button (MapListCard.kt)
- Level title/subtitle input fields (LevelInfoTab.kt)
- Save level button (LevelEditor.kt)
- Delete level button (LevelEditor.kt)

### Window Close Handler

The `WindowCloseHandler` is extended to check for official data changes:

```kotlin
// Register checker at app startup (if OfficialEditMode is enabled)
WindowCloseHandler.setOfficialDataChangedChecker { 
    OfficialDataChangeTracker.hasModifiedOfficialData()
}

// Check on window close
if (WindowCloseHandler.hasOfficialDataChanged()) {
    showOfficialDataChangedDialog = true
}
```

## Best Practices

### For Developers

1. **Use official edit mode only for development** - Don't distribute builds with this flag enabled
2. **Always test with official=false** - Ensure the game works correctly in production mode
3. **Copy official content for major changes** - Use the "Copy" button to create user versions if making significant modifications
4. **Commit changes to repository** - Official content changes should be committed to the game repository

### For Users

The official edit mode is not intended for end users. If you want to create custom content:
1. Copy official maps/levels to create user versions
2. Edit your user versions
3. Your changes will be preserved in the `gamedata/user/` directory

## File Structure

```
gamedata/
├── official/          # Official content (read-only by default)
│   ├── maps/         # Official maps
│   ├── levels/       # Official levels
│   ├── sequence.json # Official level sequence
│   └── worldmap.json # Official world map
└── user/             # User content (always editable)
    ├── maps/         # User maps
    ├── levels/       # User levels
    └── sequence.json # User level sequence
```

## Localization

The warning dialog is fully localized in:
- English (default)
- German
- Spanish
- French
- Italian

String keys:
- `official_data_changed_title`
- `official_data_changed_message`
- `official_data_modified_maps`
- `official_data_modified_levels`
- `understood`

## Testing

Tests are provided in `OfficialDataChangeTrackerTest.kt` to verify:
- Tracking of official map/level modifications
- Filtering out non-official content
- Duplicate tracking
- Clearing tracked changes

Run tests:
```bash
./gradlew :composeApp:testDebugUnitTest --tests "de.egril.defender.editor.OfficialDataChangeTrackerTest"
```
