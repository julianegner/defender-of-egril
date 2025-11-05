# Save/Load System Implementation Summary

## Overview
This implementation adds a comprehensive save/load system to Defender of Egril, allowing players to:
1. Persist world map progress (which levels are locked, unlocked, or won)
2. Save game state during gameplay
3. Load previously saved games
4. Manage multiple save files

## Architecture

### File Structure
```
composeApp/src/commonMain/kotlin/com/defenderofegril/
├── save/
│   ├── SaveModels.kt          # Data models for saves
│   ├── SaveJsonSerializer.kt  # JSON serialization
│   └── SaveFileStorage.kt     # File storage and conversion
└── ui/
    ├── LoadGameScreen.kt      # UI for loading games
    ├── GamePlayScreen.kt      # Added "Save Game" button
    ├── WorldMapScreen.kt      # Added "Load Game" button
    ├── GameViewModel.kt       # Save/load integration
    └── App.kt                 # Navigation updates
```

### Data Models

#### WorldMapSave
Stores the status of each level (LOCKED, UNLOCKED, or WON):
```kotlin
data class WorldMapSave(
    val levelStatuses: Map<Int, LevelStatus>
)
```

#### SavedGame
Complete game state snapshot including:
- Level information (ID, name)
- Game progress (turn number, coins, health)
- Game phase (INITIAL_BUILDING, PLAYER_TURN, ENEMY_TURN)
- All defenders with their positions, levels, and build status
- All attackers with their positions, health, and status
- Field effects (fireballs, acid)
- Traps from dwarven mines

#### SaveGameMetadata
Lightweight representation for the load game screen:
- Save ID and timestamp
- Level name and ID
- Turn number, tower count, enemy count

### Storage

#### File Locations
- **World Map Status**: `~/.defender-of-egril/savefiles/worldmap.json`
- **Saved Games**: `~/.defender-of-egril/savefiles/savegame_<timestamp>.json`

The system uses the existing `FileStorage` infrastructure which provides platform-specific implementations:
- Desktop: Local file system in user home directory
- Android: App-specific internal storage
- iOS: App-specific documents directory

### Serialization

The implementation uses manual JSON serialization (similar to the level editor) because:
1. Consistency with existing codebase patterns
2. Avoids kotlinx.serialization multiplatform issues
3. Simple and readable JSON format
4. Easy to debug and inspect

Example saved game JSON structure:
```json
{
  "id": "savegame_1234567890",
  "timestamp": 1234567890,
  "levelId": 1,
  "levelName": "The First Wave",
  "turnNumber": 5,
  "coins": 150,
  "healthPoints": 8,
  "phase": "PLAYER_TURN",
  "defenders": [...],
  "attackers": [...],
  "fieldEffects": [...],
  "traps": [...]
}
```

## User Interface

### GamePlayScreen - Save Button
- Located next to the "Back to Map" button
- Only visible during actual gameplay
- Shows confirmation dialog after saving
- Does not interrupt gameplay

### WorldMapScreen - Load Game Button
- Positioned alongside "Rules" and "Back to Menu" buttons
- Opens the LoadGameScreen

### LoadGameScreen
- Lists all saved games sorted by timestamp (newest first)
- Each entry shows:
  - Level name
  - Date/time saved
  - Turn number
  - Tower count (🏰 icon)
  - Enemy count (👹 icon)
- Click to load a game
- Trash icon (🗑️) to delete with confirmation dialog
- Back button to return to world map

## Integration Points

### GameViewModel
New methods:
- `saveCurrentGame()` - Saves active game state, returns save ID
- `loadGame(saveId)` - Loads and restores game state
- `navigateToLoadGame()` - Shows load game screen
- `deleteSavedGame(saveId)` - Removes a save file
- `refreshSavedGames()` - Updates the list of available saves

Auto-save triggers:
- World map status saves automatically when:
  - A level is completed (won)
  - Next level is unlocked
  - Cheat codes unlock levels

### State Restoration
The `SaveFileStorage.convertSavedGameToGameState()` method recreates:
1. Game state with correct level reference
2. All defenders with mutable states
3. All attackers with mutable states
4. Field effects and traps
5. Game phase and counters

## Testing

### Unit Tests
`SaveDataTest.kt` verifies:
- WorldMapSave serialization/deserialization
- SavedGame serialization/deserialization
- Data model structure integrity
- Position data preservation

### Manual Testing
See `SAVE_LOAD_TESTING.md` for comprehensive manual test scenarios.

## Design Decisions

### Why Manual JSON Serialization?
1. Matches existing pattern in EditorJsonSerializer
2. Avoids kotlinx.serialization issues with enums in multiplatform
3. Produces human-readable JSON for debugging
4. Simple and maintainable

### Why Not Use Database?
1. Minimal dependencies - uses existing FileStorage
2. JSON files are portable and inspectable
3. Simple backup/restore by copying files
4. Sufficient for the expected number of saves

### Why Save Entire Game State?
1. Allows resuming at exact point
2. Simpler than delta/incremental saves
3. File sizes are reasonable (typically < 50KB)
4. Fast to save and load

### World Map Auto-Save
World map status is automatically saved to ensure progress is never lost. This happens:
- After completing a level
- After unlocking levels via cheat codes
- The save is invisible to the user (no confirmation dialog)

## Future Enhancements

Possible improvements:
1. Save game preview/thumbnails
2. Multiple save slots with names
3. Auto-save at regular intervals
4. Cloud save synchronization
5. Import/export save files
6. Save game statistics (play time, etc.)

## Compatibility Notes

### Save Format Versioning
Currently no version field in saves. Future changes should:
1. Add a `version` field to SavedGame
2. Implement migration logic for old saves
3. Gracefully handle incompatible saves

### Level Editor Integration
If level structure changes in the editor:
- Saved games reference levels by ID
- Loading will fail gracefully if level doesn't exist
- Consider adding level structure hash for validation
