# Unsaved Changes Feature - Manual Test Plan

## Overview
This document describes how to manually test the unsaved changes detection feature.

## Feature Description
When a player makes changes in a level and clicks "Back to Map", the game should:
1. Detect if there are unsaved changes
2. If there are changes, show a dialog with three options:
   - **Save and Exit**: Saves the game and returns to the world map
   - **Discard Changes**: Returns to the world map without saving
   - **Cancel**: Closes the dialog and stays in the level
3. If there are no changes, navigate directly to the world map

## Test Scenarios

### Scenario 1: No Changes
**Steps:**
1. Start the game and select a level
2. Do not make any changes (don't place towers, don't start battle)
3. Click the "Map" button

**Expected Result:**
- The game should navigate directly to the world map without showing any dialog

### Scenario 2: Unsaved Changes - Save and Exit
**Steps:**
1. Start the game and select a level
2. Place a tower on the map
3. Click the "Map" button
4. Verify the "Unsaved Changes" dialog appears
5. Click "Save and Exit"

**Expected Result:**
- The game should be saved
- The game should navigate to the world map
- If you go to "Load Game", you should see the saved game

### Scenario 3: Unsaved Changes - Discard Changes
**Steps:**
1. Start the game and select a level
2. Place a tower on the map
3. Click the "Map" button
4. Verify the "Unsaved Changes" dialog appears
5. Click "Discard Changes"

**Expected Result:**
- The game should navigate to the world map without saving
- The changes should be lost
- If you start the same level again, it should be in its initial state

### Scenario 4: Unsaved Changes - Cancel
**Steps:**
1. Start the game and select a level
2. Place a tower on the map
3. Click the "Map" button
4. Verify the "Unsaved Changes" dialog appears
5. Click "Cancel"

**Expected Result:**
- The dialog should close
- The game should remain in the level
- The tower you placed should still be visible

### Scenario 5: Changes After Saving
**Steps:**
1. Start the game and select a level
2. Place a tower on the map
3. Click the "Save" button and save the game
4. Place another tower on the map
5. Click the "Map" button

**Expected Result:**
- The "Unsaved Changes" dialog should appear (because you made changes after the last save)

### Scenario 6: No Changes After Saving
**Steps:**
1. Start the game and select a level
2. Place a tower on the map
3. Click the "Save" button and save the game
4. Do not make any more changes
5. Click the "Map" button

**Expected Result:**
- The game should navigate directly to the world map without showing the dialog

### Scenario 7: Starting Battle Counts as Change
**Steps:**
1. Start the game and select a level
2. Place a tower on the map
3. Click "Start Battle"
4. Wait for the first turn to complete
5. Click the "Map" button

**Expected Result:**
- The "Unsaved Changes" dialog should appear

### Scenario 8: Loading a Saved Game
**Steps:**
1. From the world map, click "Load Game"
2. Select a saved game and load it
3. Do not make any changes
4. Click the "Map" button

**Expected Result:**
- The game should navigate directly to the world map without showing the dialog

### Scenario 9: Localization Test
**Steps:**
1. Start the game
2. Click the settings button
3. Change the language to German/Spanish/French/Italian
4. Start a level
5. Make some changes
6. Click the "Map" button

**Expected Result:**
- The unsaved changes dialog should appear with text in the selected language
- The three buttons should show the correct translations:
  - German: "Speichern und Beenden", "Änderungen verwerfen", "Abbrechen"
  - Spanish: "Guardar y Salir", "Descartar Cambios", "Cancelar"
  - French: "Enregistrer et Quitter", "Ignorer les Modifications", "Annuler"
  - Italian: "Salva ed Esci", "Scarta Modifiche", "Annulla"

## Change Detection Coverage

The feature detects changes in the following game state properties:
- Turn number
- Coins
- Health points
- Game phase (Initial Building, Player Turn, Enemy Turn)
- Number and properties of defenders (towers):
  - Position
  - Type
  - Level
  - Build time remaining
- Number and properties of attackers (enemies):
  - Position
  - Type
  - Current health
  - Defeated status
- Field effects (fireballs, acid)
- Traps

## Implementation Details

### Files Modified
1. **GameViewModel.kt**
   - Added `initialGameStateSnapshot` and `lastSaveSnapshot` fields
   - Added `createGameStateSnapshot()` method to create state fingerprints
   - Added `hasUnsavedChanges()` method to compare current state with last save
   - Updated `startLevel()` to capture initial snapshot
   - Updated `loadGame()` to capture snapshot after loading
   - Updated `saveCurrentGame()` to update last save snapshot

2. **GamePlayScreen.kt**
   - Added `hasUnsavedChanges` parameter
   - Added `showUnsavedChangesDialog` state variable
   - Modified `onBackToMap` handler in `GameHeader` to check for unsaved changes
   - Added `UnsavedChangesDialog` component at the end of the composable

3. **GameDialogs.kt**
   - Added `UnsavedChangesDialog` component with three buttons

4. **App.kt**
   - Passed `hasUnsavedChanges` callback to `GamePlayScreen`

5. **String Resources**
   - Added `unsaved_changes_title`, `unsaved_changes_message`, `save_and_exit`, and `discard_changes` strings
   - Translations added for: English, German, Spanish, French, Italian

## Notes
- The feature only activates if the `onSaveGame` callback is available (i.e., when save functionality is enabled)
- The snapshot comparison is performed using a string-based fingerprint of the game state
- Changes are detected at a granular level, including tower upgrades, enemy defeats, etc.
