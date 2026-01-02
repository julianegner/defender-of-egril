# Game Data Transfer Implementation

## Summary
This implementation adds optional game data transfer functionality, allowing players to choose whether to include world map progress (locked/unlocked/won level statuses) when downloading savegames or to export just their progress separately.

## Key Design Decision
**World map progress is NOT automatically included in saves.** Instead, users have explicit control via:
1. A toggle switch to enable/disable inclusion when downloading saves
2. A separate "Export Game Progress" button to download only progress
3. Automatic conflict detection when importing progress files

## Changes Made

### 1. Data Model Changes (`SaveModels.kt`)
- Added optional `worldMapSave: WorldMapSave?` field to `SavedGame` data class
- Maintains backward compatibility with existing save files (field defaults to null)
- Field is populated only when explicitly requested via download with game data transfer enabled

### 2. Serialization (`SaveJsonSerializer.kt`)
- Updated `serializeSavedGame()` to include world map data in JSON output (when present)
- Updated `deserializeSavedGame()` to parse world map data from JSON
- Handles null values for backward compatibility with old saves
- World map data is serialized at the end of the save file

### 3. Save Logic (`SaveFileStorage.kt`)
- `convertGameStateToSavedGame()` sets `worldMapSave = null` by default (no automatic inclusion)
- Added `getSaveGameWithWorldMapJson(saveId)`: Gets a save with world map included for export
- Added `exportWorldMapProgress()`: Exports only the world map progress
- Added `importWorldMapProgress(json)`: Imports world map and detects conflicts
- Added `applyWorldMapProgress()`: Applies imported world map to current levels

### 4. Game Data Transfer UI (`LoadGameScreen.kt`)
- Added **Game Data Transfer toggle** at top of screen
  - Labeled switch with description
  - Controls whether downloads include world map
  - Affects both individual and bulk downloads
- Added **Export Game Progress button**
  - Downloads only player progress (no level data)
  - Useful for transferring progress between devices
- Modified **Upload handling**
  - Automatically detects game progress files vs save files
  - Routes to appropriate import handler

### 5. Conflict Detection (`GameViewModel.kt`)
- `WorldMapConflict` data class supports optional `savedGame` and `level` (null when importing just progress)
- Removed automatic conflict checking from `loadGame()` workflow
- Added `importWorldMapProgress(json)`: Manual import with conflict detection
- Modified `resolveWorldMapConflict()`: Handles both save-with-progress and standalone progress imports
- Download functions updated:
  - `downloadSaveGame(saveId, includeGameState)`: Optional game state parameter
  - `downloadAllSaveGames(includeGameState)`: Optional game state parameter
  - `downloadGameState()`: Export just progress

### 6. UI Dialog (`WorldMapConflictDialog.kt`)
- Reused for both save conflicts and standalone progress imports
- Shows side-by-side comparison of saved vs current progress
- Lists all differences with level IDs and their statuses
- Provides three options:
  - Use Saved Progress (overwrites current progress)
  - Keep Current Progress (ignores saved progress)
  - Cancel (aborts import)

### 7. Integration (`App.kt`)
- Updated LoadGameScreen parameters to pass new callbacks
- Added `onExportGameProgress` handler
- Added `onImportGameProgress` handler
- Updated download handlers with `includeGameState` parameters

### 8. Localization
Added strings to all 5 supported languages (English, German, Spanish, French, Italian):

**Game Data Transfer:**
- `game_data_transfer` - Toggle label
- `game_data_transfer_description` - Toggle description
- `export_game_progress` - Export button label
- `export_game_progress_description` - Export button description
- `import_game_progress` - Import label

**Conflict Resolution:**
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

### 9. Testing (`SaveDataTest.kt`, `WorldMapSaveIntegrationTest.kt`)
Added comprehensive tests:
- `testSavedGameWithWorldMapSave()` - Verifies serialization/deserialization with world map data
- `testBackwardCompatibilityWithoutWorldMapSave()` - Ensures old saves without world map field still load correctly
- Integration tests for conflict detection and world map export/import

## Workflows

### Saving a Game (Regular)
1. User saves game during gameplay
2. `SaveFileStorage.convertGameStateToSavedGame()` is called
3. **World map is NOT included** (`worldMapSave = null`)
4. JSON is serialized and written to disk
5. Save contains only level data, no progress information

### Downloading a Save with Game Data Transfer Enabled
1. User enables "Game Data Transfer" toggle
2. User clicks download on a save or "Download All"
3. `getSaveGameWithWorldMapJson()` is called
4. Current world map progress is loaded and added to save JSON
5. Modified JSON with world map is exported
6. File name includes "_with_progress" suffix (for individual saves)

### Exporting Game Progress Only
1. User clicks "Export Game Progress" button
2. `exportWorldMapProgress()` is called
3. Current world map status is serialized to JSON
4. JSON contains only `levelStatuses` map
5. File named `game-progress-{timestamp}.json` is exported

### Importing Game Progress
1. User clicks "Upload" and selects files
2. System detects file contains only `levelStatuses` (game progress file)
3. `importWorldMapProgress()` is called
4. If progress differs from current, conflict dialog appears
5. User sees side-by-side comparison of progress
6. User chooses:
   - **Use Saved Progress**: Overwrites current progress
   - **Keep Current Progress**: Discards imported data
   - **Cancel**: Aborts import
7. If chosen to use, `applyWorldMapProgress()` updates levels and saves

### Loading a Regular Save (No Progress Included)
1. User loads a save file
2. `GameViewModel.loadGame()` deserializes the save
3. No world map data present (`worldMapSave = null`)
4. Game loads normally without any conflict checks
5. Current world map progress remains unchanged

## Backward Compatibility
- Old save files without `worldMapSave` field load normally (field defaults to null)
- No automatic conflict detection (removed from load workflow)
- Conflicts only occur when explicitly importing game progress files
- Regular saves never include world map data unless downloaded with transfer enabled

## Future Extensibility
The implementation is designed to easily accommodate future "user values" in the game state file:
1. Add new fields to `WorldMapSave` data class
2. Update serialization/deserialization in `SaveJsonSerializer`
3. Export/import functions will automatically handle new fields
4. Update `WorldMapConflictDialog` to display new fields in the comparison

## Technical Notes
- Manual JSON serialization is used (consistent with existing codebase patterns)
- World map data is serialized as nested JSON object under `worldMapSave` key
- Conflict detection uses simple equality check on level status maps
- Dialog uses Material3 AlertDialog with custom layout
- All strings follow the project's localization guidelines
