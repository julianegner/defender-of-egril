# Removal of Map/Level Generation Fallback Code

## Overview
This change removes old unused code that was used to generate maps and levels as a fallback when repository data files were missing. The application now always uses files from the repository and shows an error dialog if critical data is missing.

## Changes Made

### 1. Removed Generation Code

#### EditorStorage.kt
- **Removed** `initializeDefaultMapsAndLevels()` function (517 lines)
  - This function generated default maps and levels if no repository files existed
  - Generated 5 default maps (30x8, 35x9, 40x10, 45x11, 50x12)
  - Generated tutorial map and 9 levels
  - Created spiral, plains, and dance maps using MapGenerator
  - All this functionality is now obsolete as we always use repository files

#### Level.kt
- **Removed** `PathAndIslands` data class
- **Removed** `generateCurvedPathWithIslands()` function
  - Generated procedural paths with islands
  - Only used by the removed initialization code

#### MapGenerator.kt
- **Deleted** entire file (430 lines)
  - `createSpiralMap()` - generated spiral map
  - `createPlainsMap()` - generated plains map
  - `createDanceMap()` - generated dance map with circular paths
  - All helper functions for path generation
  - This was only used as fallback, not needed anymore

### 2. Added Repository Data Validation

#### EditorModels.kt
- **Added** `MissingRepositoryDataException` class
  - Exception thrown when critical repository data is missing
  - Contains list of missing data categories

#### EditorStorage.kt
- **Updated** `init` block:
  - Checks for existing user data first
  - If no user data, attempts to load from repository
  - Throws exception if repository files are missing
  - Validates all data categories after loading
  - Ensures sequence is not empty

- **Added** `validateRepositoryData()` function:
  - Checks that maps exist and are not empty
  - Checks that levels exist and are not empty
  - Checks that sequence exists and is not empty
  - Checks that worldmap data exists
  - Returns list of missing categories

### 3. Added Error Dialog

#### MissingRepositoryDataDialog.kt
- **Created** new dialog component
  - Shows when critical repository data is missing
  - Displays list of missing categories (maps, levels, sequence, worldmap)
  - Provides user-friendly error message
  - Suggests reinstalling the game or restoring data from Settings

#### App.kt
- **Updated** to catch `MissingRepositoryDataException`
  - Wraps GameViewModel creation in try-catch
  - Shows `MissingRepositoryDataDialog` if exception is thrown
  - Prevents app from continuing without critical data

### 4. Added Localized Error Strings

Added error strings to all supported languages:
- **English** (values/strings.xml)
- **German** (values-de/strings.xml)
- **Spanish** (values-es/strings.xml)
- **French** (values-fr/strings.xml)
- **Italian** (values-it/strings.xml)

New strings:
- `error_missing_repository_data_title`: Dialog title
- `error_missing_repository_data_message`: Error message
- `error_missing_repository_data_categories`: Categories header
- `error_missing_repository_data_reinstall`: Suggested action

## Impact

### Code Size Reduction
- **Total lines removed**: ~900 lines
- **EditorStorage.kt**: -517 lines
- **Level.kt**: -84 lines
- **MapGenerator.kt**: -430 lines (entire file deleted)
- **New code added**: ~150 lines (validation + dialog)
- **Net reduction**: ~750 lines

### Behavior Changes

#### Before
1. App checked if gamedata directory was empty
2. If empty, tried to load from repository
3. If repository missing, generated default maps/levels procedurally
4. App always started successfully

#### After
1. App checks if gamedata directory is empty
2. If empty, tries to load from repository
3. If repository missing, throws exception with error dialog
4. User must reinstall or restore data to continue
5. App validates all data categories are present and not empty

### User Experience

#### Normal Case (Repository Files Present)
- No change - app loads normally
- All levels and maps loaded from repository files
- Faster startup (no generation overhead)

#### Error Case (Repository Files Missing)
- User sees clear error dialog
- Dialog lists specific missing categories
- Suggests reinstalling or restoring data
- Prevents silent failures or corrupted state

## Files Changed

### Modified Files
- `composeApp/src/commonMain/kotlin/de/egril/defender/App.kt`
- `composeApp/src/commonMain/kotlin/de/egril/defender/editor/EditorModels.kt`
- `composeApp/src/commonMain/kotlin/de/egril/defender/editor/EditorStorage.kt`
- `composeApp/src/commonMain/kotlin/de/egril/defender/model/Level.kt`
- `composeApp/src/commonMain/composeResources/values/strings.xml`
- `composeApp/src/commonMain/composeResources/values-de/strings.xml`
- `composeApp/src/commonMain/composeResources/values-es/strings.xml`
- `composeApp/src/commonMain/composeResources/values-fr/strings.xml`
- `composeApp/src/commonMain/composeResources/values-it/strings.xml`
- `docs/features/MAP_GENERATION_REFACTORING.md`

### New Files
- `composeApp/src/commonMain/kotlin/de/egril/defender/ui/MissingRepositoryDataDialog.kt`

### Deleted Files
- `composeApp/src/commonMain/kotlin/de/egril/defender/editor/MapGenerator.kt`

## Repository Files

The following repository files are now **required** for the game to run:
- `composeResources/files/repository/maps/*.json` - Map definitions
- `composeResources/files/repository/levels/*.json` - Level definitions
- `composeResources/files/repository/sequence.json` - Level sequence
- `composeResources/files/repository/worldmap.json` - World map data

These files must be included in the application bundle. The application will not generate fallback data if they are missing.

## Testing Recommendations

1. **Normal startup**: Verify app loads correctly with repository files
2. **Missing repository**: Simulate missing repository files to test error dialog
3. **Empty data**: Test with empty maps/levels directories
4. **Corrupted data**: Test with invalid JSON in repository files
5. **Localization**: Verify error messages in all supported languages

## Future Considerations

- Consider adding a "Restore Data" feature in the error dialog
- Add more detailed error messages for specific file issues
- Consider checksums to detect corrupted repository files
- Add telemetry to track how often this error occurs in production
