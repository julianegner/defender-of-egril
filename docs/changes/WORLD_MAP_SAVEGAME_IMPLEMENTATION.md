# World Map Progress in Savegame Files Implementation

## Summary
This implementation extends savegame files to include world map progress (locked/unlocked/won level statuses), enabling conflict detection and resolution when loading a save with different progress.

## Changes Made

### 1. Data Model Changes (`SaveModels.kt`)
- Added optional `worldMapSave: WorldMapSave?` field to `SavedGame` data class
- Maintains backward compatibility with existing save files (field defaults to null)

### 2. Serialization (`SaveJsonSerializer.kt`)
- Updated `serializeSavedGame()` to include world map data in JSON output
- Updated `deserializeSavedGame()` to parse world map data from JSON
- Handles null values for backward compatibility with old saves
- World map data is serialized at the end of the save file as specified

### 3. Save Logic (`SaveFileStorage.kt`)
- Updated `convertGameStateToSavedGame()` to load and include current world map status when creating a save
- World map progress is automatically captured at save time

### 4. Conflict Detection (`GameViewModel.kt`)
- Added `WorldMapConflict` data class to hold conflict information
- Added `worldMapConflict` state flow for UI to observe
- Implemented `checkAndHandleWorldMapConflict()` to detect differences between saved and current world map progress
- Integrated conflict checking into `loadGame()` workflow
- Added `resolveWorldMapConflict()` to apply user's choice
- Added `cancelWorldMapConflict()` to dismiss the dialog

### 5. UI Dialog (`WorldMapConflictDialog.kt`)
- Created new dialog component to display world map conflicts
- Shows side-by-side comparison of saved vs current progress
- Lists all differences with level IDs and their statuses
- Provides three options:
  - Use Saved Progress (overwrites current progress)
  - Keep Current Progress (ignores saved progress)
  - Cancel (aborts load operation)

### 6. Integration (`App.kt`)
- Added `worldMapConflict` state observation
- Integrated `WorldMapConflictDialog` into app-level dialogs
- Dialog appears automatically when conflict is detected during game load

### 7. Localization
Added strings to all 5 supported languages (English, German, Spanish, French, Italian):
- `world_map_conflict_title` - Dialog title
- `world_map_conflict_message` - Explanation message
- `world_map_conflict_differences` - Header for differences table
- `world_map_conflict_level` - "Level" column header
- `world_map_conflict_saved` - "Saved" column header
- `world_map_conflict_current` - "Current" column header
- `world_map_conflict_use_saved` - Button to use saved progress
- `world_map_conflict_use_current` - Button to keep current progress
- `level_status_locked` - Display text for locked levels
- `level_status_unlocked` - Display text for unlocked levels
- `level_status_won` - Display text for won levels
- `level_status_unknown` - Display text for unknown status

### 8. Testing (`SaveDataTest.kt`)
Added comprehensive tests:
- `testSavedGameWithWorldMapSave()` - Verifies serialization/deserialization with world map data
- `testBackwardCompatibilityWithoutWorldMapSave()` - Ensures old saves without world map field still load correctly

## Workflow

### Saving a Game
1. User saves game during gameplay
2. `SaveFileStorage.convertGameStateToSavedGame()` is called
3. Current world map progress is loaded from `level_progress.json`
4. World map data is included in the `SavedGame` object
5. JSON is serialized with world map data at the end
6. Save file is written to disk

### Loading a Game with No Conflict
1. User loads a save file
2. `GameViewModel.loadGame()` deserializes the save
3. `checkAndHandleWorldMapConflict()` compares saved vs current world map
4. If identical (or no saved world map), game loads normally
5. No dialog is shown

### Loading a Game with Conflict
1. User loads a save file
2. `GameViewModel.loadGame()` deserializes the save
3. `checkAndHandleWorldMapConflict()` detects differences
4. `WorldMapConflict` object is created and published to state
5. `WorldMapConflictDialog` automatically appears
6. User sees side-by-side comparison of differences
7. User chooses:
   - **Use Saved Progress**: Current world map is updated to match save, game loads
   - **Keep Current Progress**: Saved world map is ignored, game loads
   - **Cancel**: Dialog closes, load operation is aborted
8. After resolution, game loads normally

## Backward Compatibility
- Old save files without `worldMapSave` field load normally (field defaults to null)
- No conflicts are detected for old saves (null treated as "no saved progress")
- New saves always include world map data going forward

## Future Extensibility
The implementation is designed to easily accommodate future "user values" in the game state file:
1. Add new fields to `WorldMapSave` data class
2. Update serialization/deserialization in `SaveJsonSerializer`
3. Update conflict detection logic in `checkAndHandleWorldMapConflict()`
4. Update `WorldMapConflictDialog` to display new fields in the comparison

## Technical Notes
- Manual JSON serialization is used (consistent with existing codebase patterns)
- World map data is serialized as nested JSON object under `worldMapSave` key
- Conflict detection uses simple equality check on level status maps
- Dialog uses Material3 AlertDialog with custom layout
- All strings follow the project's localization guidelines
