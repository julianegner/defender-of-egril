# Unsaved Changes Detection - Implementation Summary

## Overview
This document summarizes the implementation of the unsaved changes detection feature for the Defender of Egril game.

## Problem Statement
When the player leaves a level and clicks "return to map", all progress is lost. The app should check if something was changed since the last save and if so, ask the player if they want to save now or dismiss the changes.

## Solution
Implemented a change detection system that:
1. Captures a snapshot of the game state when starting or loading a level
2. Updates the snapshot when the game is saved
3. Compares the current state with the last saved state when the player tries to leave
4. Shows a dialog with three options if changes are detected:
   - **Save and Exit**: Saves the game and returns to the world map
   - **Discard Changes**: Returns to the world map without saving
   - **Cancel**: Closes the dialog and stays in the level

## Implementation Details

### 1. State Tracking (GameViewModel.kt)

#### New Fields
```kotlin
private var initialGameStateSnapshot: String? = null
private var lastSaveSnapshot: String? = null
```

#### Snapshot Creation
The `createGameStateSnapshot()` method creates a string-based fingerprint of the game state that includes:
- Turn number, coins, health points, game phase
- Defenders (towers): ID, type, position, level, build time remaining
- Attackers (enemies): ID, type, position, current health, defeated status
- Field effects (fireballs, acid) count
- Traps count

#### Change Detection
```kotlin
fun hasUnsavedChanges(): Boolean {
    val currentState = _gameState.value ?: return false
    val currentSnapshot = createGameStateSnapshot(currentState)
    val referenceSnapshot = lastSaveSnapshot ?: initialGameStateSnapshot ?: return false
    return currentSnapshot != referenceSnapshot
}
```

#### Snapshot Updates
- `startLevel()`: Captures initial snapshot when starting a new level
- `loadGame()`: Captures snapshot after loading a saved game
- `saveCurrentGame()`: Updates the last save snapshot after successful save

### 2. UI Components

#### UnsavedChangesDialog (GameDialogs.kt)
A new dialog component with three action buttons:
- Primary action (right): "Save and Exit"
- Destructive action (middle): "Discard Changes" (red button)
- Cancel action (left): "Cancel" (secondary color)

All buttons are displayed in a horizontal row for clarity.

#### GamePlayScreen Integration
- Added `hasUnsavedChanges` parameter to receive the check callback
- Added `showUnsavedChangesDialog` state variable
- Added `unsavedChangesEnabled` local variable to check if feature is enabled
- Modified the back button handler in `GameHeader` to check for unsaved changes
- Shows the `UnsavedChangesDialog` when unsaved changes are detected

### 3. Localization

Added 4 new string resources in all 5 supported languages:

**English:**
- unsaved_changes_title: "Unsaved Changes"
- unsaved_changes_message: "You have unsaved progress in this level. What would you like to do?"
- save_and_exit: "Save and Exit"
- discard_changes: "Discard Changes"

**German:**
- Nicht gespeicherte Änderungen
- Sie haben nicht gespeicherten Fortschritt in diesem Level. Was möchten Sie tun?
- Speichern und Beenden
- Änderungen verwerfen

**Spanish:**
- Cambios no guardados
- Tienes progreso no guardado en este nivel. ¿Qué te gustaría hacer?
- Guardar y Salir
- Descartar Cambios

**French:**
- Modifications non enregistrées
- Vous avez des progrès non enregistrés dans ce niveau. Que souhaitez-vous faire ?
- Enregistrer et Quitter
- Ignorer les Modifications

**Italian:**
- Modifiche non salvate
- Hai progressi non salvati in questo livello. Cosa vorresti fare?
- Salva ed Esci
- Scarta Modifiche

## Files Modified

1. **composeApp/src/commonMain/kotlin/de/egril/defender/ui/GameViewModel.kt**
   - Added snapshot fields and methods
   - Updated level start/load/save methods

2. **composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/GamePlayScreen.kt**
   - Added unsaved changes parameter
   - Added dialog state management
   - Modified back button handler

3. **composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/GameDialogs.kt**
   - Added UnsavedChangesDialog component

4. **composeApp/src/commonMain/kotlin/de/egril/defender/App.kt**
   - Passed hasUnsavedChanges callback to GamePlayScreen

5. **String resource files** (5 files - all languages)
   - values/strings.xml
   - values-de/strings.xml
   - values-es/strings.xml
   - values-fr/strings.xml
   - values-it/strings.xml

## Design Decisions

### String-Based Snapshot
**Decision**: Use string concatenation to create a fingerprint of the game state
**Rationale**: 
- Simple to implement and debug
- Sufficient performance for typical game state sizes (dozens of units)
- Easy to understand and maintain
- No external dependencies required

**Alternative Considered**: Hash-based approach
**Why Not**: More complex to implement, minimal performance gain for typical use cases

### Button Layout
**Decision**: Place all three buttons in a horizontal row in the confirmButton slot
**Rationale**:
- All three options are equally important to the user
- Horizontal layout provides clear visual separation
- AlertDialog's dismissButton typically supports only one button
- Better UX on desktop where horizontal space is abundant

**Alternative Considered**: Vertical button stack
**Why Not**: Takes more vertical space, less clear separation between actions

### Feature Toggle
**Decision**: Only enable the feature when both `hasUnsavedChanges` and `onSaveGame` callbacks are available
**Rationale**:
- Save functionality might be disabled in some contexts (e.g., tutorial mode)
- Graceful degradation - feature silently disables if dependencies are missing
- No additional configuration needed

## Testing
A comprehensive manual test plan is available at:
`docs/testing/UNSAVED_CHANGES_TEST_PLAN.md`

The plan covers:
- No changes scenario
- All three dialog actions (Save, Discard, Cancel)
- Changes after saving
- Battle start detection
- Loading saved games
- Localization testing

## Performance Considerations
- Snapshot creation is only triggered when:
  - Starting a new level
  - Loading a saved game
  - Saving the current game
  - Clicking the "Back to Map" button
- No continuous polling or reactive updates
- String comparison is O(n) but n is typically < 1KB
- No noticeable performance impact expected

## Future Improvements
Potential enhancements (not implemented):
1. More granular change tracking (e.g., only specific properties)
2. Hash-based snapshot for very large game states
3. Visual indicator in UI showing unsaved changes status
4. Auto-save functionality
5. Change history/undo functionality

## Backward Compatibility
- Feature is completely optional and non-breaking
- Existing save files work without modification
- No database schema changes required
- Works with all existing game modes and levels

## Security Considerations
- No sensitive data in snapshots (only game state)
- Snapshots are stored in memory only, not persisted
- No network communication involved
- No user input validation needed (state is generated internally)
