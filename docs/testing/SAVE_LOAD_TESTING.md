# Save/Load System Manual Testing Guide

## Test Scenarios

### 1. World Map Status Persistence
**Test Steps:**
1. Launch the game
2. Complete level 1 successfully
3. Verify level 1 shows as "WON" and level 2 is "UNLOCKED"
4. Close the game completely
5. Relaunch the game
6. Navigate to World Map
7. **Expected:** Level 1 should still show as "WON" and level 2 should be "UNLOCKED"

### 2. Save Game During Play
**Test Steps:**
1. Start level 1
2. Place 2-3 towers
3. Start the game (click "Start Turn 1")
4. Let a few enemies spawn
5. Click "Save Game" button
6. **Expected:** A confirmation dialog should appear saying "Game Saved"
7. Click OK
8. Continue playing or return to world map

### 3. Load Saved Game
**Test Steps:**
1. From World Map, click "Load Game" button
2. **Expected:** A screen showing saved games should appear
3. Each saved game should show:
   - Level name (e.g., "The First Wave")
   - Date and time of save
   - Turn number
   - Number of towers (🏰)
   - Number of enemies (👹)
4. Click on a saved game to load it
5. **Expected:** The game should resume from the saved state with:
   - Same turn number
   - Same towers in same positions
   - Same enemies in same positions
   - Same coins and health points

### 4. Multiple Save Files
**Test Steps:**
1. Save game at turn 1
2. Continue playing to turn 3
3. Save game again
4. Go to Load Game screen
5. **Expected:** Both save games should be listed
6. They should be sorted with newest save at the top
7. Each should show different turn numbers

### 5. Delete Saved Game
**Test Steps:**
1. From Load Game screen, find a saved game
2. Click the trash icon (🗑️) next to it
3. **Expected:** A confirmation dialog should appear
4. Click "Delete"
5. **Expected:** The saved game should be removed from the list
6. Click "Cancel" on another save's delete dialog
7. **Expected:** That save should remain in the list

### 6. Load Game After Changes
**Test Steps:**
1. Save a game during level 1
2. Return to world map
3. Start level 2
4. Play level 2 for a bit (don't complete it)
5. Return to world map
6. Load the saved game from level 1
7. **Expected:** The game should load the level 1 state correctly

### 7. World Map Status After Unlocking Levels
**Test Steps:**
1. Use cheat code "unlock" to unlock all levels (click on World Map title)
2. Close and reopen the game
3. **Expected:** All levels should still be unlocked

### 8. Save File Location
**Test Files Created:**
- World map status: `~/.defender-of-egril/savefiles/worldmap.json`
- Saved games: `~/.defender-of-egril/savefiles/savegame_<timestamp>.json`

You can inspect these JSON files to verify the save data structure.

## Known Limitations

1. Save files are stored locally on the device
2. Platform-specific storage:
   - Desktop: `~/.defender-of-egril/savefiles/`
   - Android: App-specific storage
   - iOS: App-specific storage

## Edge Cases to Test

1. **Save during initial building phase** - Should work
2. **Save during enemy turn** - Should work (phase is saved)
3. **Save with no towers** - Should work
4. **Save with no enemies** - Should work
5. **Load game with level editor changes** - If the level structure has changed in the editor, loading might fail or produce unexpected results

## Troubleshooting

If save/load doesn't work:
1. Check console output for error messages
2. Verify the savefiles directory exists and is writable
3. Check JSON file format for corruption
4. Try deleting all save files and starting fresh
