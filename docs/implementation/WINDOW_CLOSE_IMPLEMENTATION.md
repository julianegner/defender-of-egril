# Window Close with Unsaved Data - Implementation Summary

## Overview

This feature implements checking for unsaved data when a player tries to close the window/tab, matching the behavior of the existing "Back to Map" button.

## Problem Statement

**Issue:** Desktop and web: before closing the window when the player clicks on the X on the window, the app should check for any unsaved data, just the same as if the player would have clicked on the "back to map" button.

## Solution Architecture

### Core Components

#### 1. WindowCloseHandler (Common)
**File:** `composeApp/src/commonMain/kotlin/de/egril/defender/utils/WindowCloseHandler.kt`

A singleton object that coordinates unsaved changes state between the UI layer and platform-specific window handlers.

**Key Methods:**
- `setUnsavedChangesChecker(checker: (() -> Boolean)?)` - Sets the callback to check for unsaved changes
- `setSaveGameCallback(callback: (() -> Unit)?)` - Sets the callback to save the game
- `hasUnsavedChanges(): Boolean` - Checks if there are unsaved changes
- `saveGame()` - Triggers the save game callback

**Design Decision:** Uses simple nullable function properties instead of MutableState for simplicity, as reactive observation is not needed.

#### 2. App.kt Integration (Common)
**File:** `composeApp/src/commonMain/kotlin/de/egril/defender/App.kt`

Registers the unsaved changes checker and save callback when navigating to/from the GamePlay screen.

```kotlin
LaunchedEffect(currentScreen) {
    when (currentScreen) {
        is Screen.GamePlay -> {
            WindowCloseHandler.setUnsavedChangesChecker { viewModel.hasUnsavedChanges() }
            WindowCloseHandler.setSaveGameCallback { viewModel.saveCurrentGame() }
        }
        else -> {
            WindowCloseHandler.setUnsavedChangesChecker(null)
            WindowCloseHandler.setSaveGameCallback(null)
        }
    }
}
```

This ensures:
- Callbacks are only active during gameplay
- Callbacks are properly cleaned up when leaving gameplay
- No memory leaks from dangling callbacks

#### 3. Desktop Implementation (JVM)
**File:** `composeApp/src/desktopMain/kotlin/de/egril/defender/main.kt`

Implements a custom AlertDialog when the user tries to close the window with unsaved changes.

**Key Features:**
- Intercepts `onCloseRequest` before calling `exitApplication()`
- Shows dialog with 3 buttons:
  - **Cancel** (secondary color): Closes dialog, stays in game
  - **Discard Changes** (error/red color): Closes window without saving
  - **Save & Exit** (primary color): Saves game automatically then closes
- Uses existing localized strings: `unsaved_changes_title`, `unsaved_changes_message`, `cancel`, `discard_changes`, `save_and_exit`

**Implementation:**
```kotlin
Window(
    onCloseRequest = {
        if (WindowCloseHandler.hasUnsavedChanges()) {
            showUnsavedChangesDialog = true
        } else {
            exitApplication()
        }
    },
    // ... other properties
) {
    App()
    
    if (showUnsavedChangesDialog) {
        // AlertDialog with 3 button options
    }
}
```

#### 4. Web/WASM Implementation
**File:** `composeApp/src/wasmJsMain/kotlin/de/egril/defender/main.kt`

Sets up a JavaScript `beforeunload` event handler to show browser's native confirmation dialog.

**Key Features:**
- Uses JavaScript interop to access `window.onbeforeunload`
- Returns a confirmation message when there are unsaved changes
- Browser shows standard confirmation dialog (security requirement)

**Limitations:**
- Modern browsers show generic messages for security (cannot customize text)
- Some browsers require user interaction before showing the dialog
- Cannot show custom dialogs with multiple buttons (browser security)

**Implementation:**
```kotlin
private fun setupBeforeUnloadHandler() {
    window.onbeforeunload = { event ->
        if (WindowCloseHandler.hasUnsavedChanges()) {
            event.returnValue = "You have unsaved changes. Are you sure you want to leave?"
            "You have unsaved changes. Are you sure you want to leave?"
        } else {
            null
        }
    }
}
```

## How It Works

### Sequence Diagram

```
User Action: Click Window Close (X)
    |
    v
Desktop: onCloseRequest triggered
Web: beforeunload event fired
    |
    v
WindowCloseHandler.hasUnsavedChanges() called
    |
    v
Checks registered callback (if GamePlay screen)
    |
    +--> If on GamePlay screen: viewModel.hasUnsavedChanges()
    |        |
    |        +--> Compares current state snapshot with last save snapshot
    |        |
    |        +--> Returns true if different, false if same
    |
    +--> If NOT on GamePlay screen: Returns false (no callback)
    |
    v
Desktop:
    If hasUnsavedChanges() == true:
        Show AlertDialog with 3 buttons
    Else:
        exitApplication()

Web:
    If hasUnsavedChanges() == true:
        Return confirmation message
        Browser shows standard dialog
    Else:
        Return null (no dialog shown)
```

### State Tracking

The unsaved changes detection uses the existing `GameViewModel.hasUnsavedChanges()` logic:

1. **Initial Snapshot:** When starting a level, a snapshot of the game state is created
2. **Last Save Snapshot:** When saving the game, this snapshot is updated
3. **Comparison:** On close, current state is compared with the last save snapshot
4. **Detection:** If snapshots differ, there are unsaved changes

Snapshot includes:
- Turn number
- Coins
- Health points
- Game phase
- Defender positions, types, and levels
- Attacker positions, types, and health
- Field effects
- Traps

## Testing

A comprehensive testing guide is available in `docs/guides/WINDOW_CLOSE_TESTING.md` with:
- 7 desktop test scenarios
- 4 web test scenarios  
- 4 edge case scenarios
- Browser compatibility checklist
- Troubleshooting section

## Known Limitations

### Desktop
- Dialog only appears during active gameplay (GamePlay screen)
- Other screens don't trigger the dialog (by design - no game state to save)

### Web
- Cannot customize dialog text (browser security)
- Some browsers may suppress dialog if user hasn't interacted with page
- Cannot provide "Save & Exit" option (browser API limitation)

## Future Enhancements

Potential improvements not included in this implementation:
1. iOS/Android platform support (requires platform-specific lifecycle handling)
2. Auto-save before closing (would eliminate need for dialog in most cases)
3. Periodic auto-save during gameplay
4. Cloud save synchronization
5. Multiple save slots with named saves

## Compatibility

- **Desktop:** All JVM-based platforms (Windows, macOS, Linux)
- **Web:** All modern browsers (Chrome, Firefox, Safari, Edge)
- **Not Implemented:** Mobile platforms (Android, iOS) - would require different approach

## Related Files

- `GameViewModel.kt` - Contains `hasUnsavedChanges()` logic
- `SaveFileStorage.kt` - Handles save game operations
- `GamePlayScreen.kt` - Contains existing in-game "Back to Map" unsaved changes dialog
- `GameDialogs.kt` - Contains `UnsavedChangesDialog` component

## Localization

All strings are localized and available in:
- English (default)
- German
- Spanish
- French
- Italian

String keys used:
- `unsaved_changes_title`
- `unsaved_changes_message`
- `cancel`
- `discard_changes`
- `save_and_exit`
