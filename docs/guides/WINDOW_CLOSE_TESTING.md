# Window Close with Unsaved Data Testing Guide

This guide explains how to test the window close check for unsaved data feature on desktop and web platforms.

## Feature Overview

When a player tries to close the window/tab while playing a game with unsaved changes, the application will:
- **Desktop**: Show a dialog with options to Cancel, Discard Changes, or Save & Exit
- **Web**: Show browser's standard confirmation dialog

This behavior matches the existing "Back to Map" button functionality.

## Running the Game

### Desktop (Recommended for Testing)
```bash
./gradlew :composeApp:run
```

### Web/Browser
```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```
Then open http://localhost:8080 in your browser

## Test Scenarios

### Desktop Platform Testing

#### Test 1: No Unsaved Changes - Window Should Close Normally
1. Launch the desktop application
2. Navigate through menu screens (Main Menu → World Map)
3. Click the window X button to close
4. **Expected**: Window closes immediately without any dialog

#### Test 2: Fresh Game - No Unsaved Changes
1. Launch the desktop application
2. Start a new game (level 1)
3. Do NOT make any moves or changes
4. Click the window X button to close
5. **Expected**: Window closes immediately (no changes were made)

#### Test 3: Game with Changes - Show Dialog
1. Launch the desktop application
2. Start a new game (level 1)
3. Place at least one tower or make a move (this creates unsaved changes)
4. Click the window X button to close
5. **Expected**: Dialog appears with three buttons:
   - "Cancel" - Closes the dialog and stays in the game
   - "Discard Changes" - Closes the window without saving
   - "Save & Exit" - Saves the game and closes the window

#### Test 4: Cancel Button - Stay in Game
1. Follow steps 1-4 from Test 3
2. Click "Cancel" button in the dialog
3. **Expected**: 
   - Dialog closes
   - Game continues normally
   - Window remains open

#### Test 5: Discard Changes Button - Exit Without Saving
1. Follow steps 1-4 from Test 3
2. Click "Discard Changes" button
3. **Expected**: 
   - Window closes immediately
   - Changes are NOT saved

#### Test 6: Save & Exit Button - Save and Close
1. Follow steps 1-4 from Test 3
2. Click "Save & Exit" button
3. **Expected**: 
   - Game is automatically saved
   - Window closes
4. Relaunch the application
5. Go to Load Game screen
6. **Expected**: A saved game appears with timestamp

#### Test 7: After Manual Save - No Dialog
1. Start a new game and make changes
2. Press Ctrl+S or use the save button to save the game
3. Click the window X button to close
4. **Expected**: Window closes immediately (no unsaved changes after save)

### Web Platform Testing

#### Test 8: No Unsaved Changes - Tab Should Close Normally
1. Open the game in a web browser
2. Navigate through menu screens
3. Close the browser tab (Ctrl+W or click X)
4. **Expected**: Tab closes immediately without any confirmation

#### Test 9: Game with Changes - Browser Confirmation
1. Open the game in a web browser
2. Start a new game and make changes (place a tower)
3. Try to close the tab (Ctrl+W) or navigate away
4. **Expected**: 
   - Browser shows standard confirmation dialog
   - Dialog text varies by browser (browser security prevents custom messages)
   - Options are typically "Leave" or "Stay"

#### Test 10: Web Refresh with Unsaved Changes
1. Open the game in a web browser
2. Start a new game and make changes
3. Press F5 or Ctrl+R to refresh the page
4. **Expected**: 
   - Browser shows confirmation dialog
   - Choosing "Leave" refreshes and loses changes
   - Choosing "Stay" cancels the refresh

#### Test 11: Web Navigate Away with Unsaved Changes
1. Open the game in a web browser
2. Start a new game and make changes
3. Click browser back button or type a new URL
4. **Expected**: Browser shows confirmation dialog

## Edge Cases to Test

### Edge Case 1: Loaded Game - Check Changes from Load Point
1. Load an existing saved game
2. Make NO changes
3. Try to close window
4. **Expected**: No dialog (state matches saved state)

### Edge Case 2: Loaded Game - Modified After Load
1. Load an existing saved game
2. Make changes (place a tower, attack, etc.)
3. Try to close window
4. **Expected**: Dialog appears (changes since last save)

### Edge Case 3: Multiple Tabs (Web Only)
1. Open game in two browser tabs
2. In Tab 1: Start a game and make changes
3. In Tab 2: Stay on main menu
4. Try to close Tab 1
5. **Expected**: Confirmation for Tab 1 only
6. Try to close Tab 2
7. **Expected**: No confirmation for Tab 2

### Edge Case 4: Level Complete Screen
1. Complete a level (win or lose)
2. See the Level Complete screen
3. Try to close window
4. **Expected**: Window closes without dialog (game is complete)

## Browser Compatibility (Web Platform)

Test on multiple browsers to ensure beforeunload works correctly:
- [ ] Chrome/Chromium
- [ ] Firefox
- [ ] Safari
- [ ] Edge

**Note**: Modern browsers show their own generic confirmation message for security reasons. We cannot customize the dialog text on web platforms.

## Known Limitations

### Desktop
- The dialog only appears during active gameplay (GamePlay screen)
- Other screens (Main Menu, World Map, Level Complete) don't show the dialog

### Web
- Browser security prevents custom dialog messages
- Dialog text is controlled by the browser
- Some browsers may not show the dialog in certain situations (e.g., if user hasn't interacted with the page)

## Troubleshooting

**Dialog doesn't appear on desktop:**
1. Ensure you're actually in a game (not menu screens)
2. Make sure you've made changes (placed a tower, attacked, etc.)
3. Check console for any JavaScript/Kotlin errors

**Web confirmation doesn't appear:**
1. Ensure you've interacted with the page (clicked something)
2. Check browser console for errors
3. Try a different browser (some browsers are more strict)
4. Make sure you've actually made changes in the game

**Dialog appears when it shouldn't:**
1. Check if you actually made changes (even small changes count)
2. Verify the game state tracking is working correctly

## Success Criteria

The implementation is successful if:
- ✅ Desktop shows custom dialog with 3 options when closing with unsaved changes
- ✅ Desktop "Cancel" button keeps game open
- ✅ Desktop "Discard Changes" button closes without saving
- ✅ Desktop "Save & Exit" button saves and closes
- ✅ Desktop closes immediately when no unsaved changes exist
- ✅ Web shows browser confirmation when closing with unsaved changes
- ✅ Web closes immediately when no unsaved changes exist
- ✅ Both platforms work correctly after manual save (no dialog)
- ✅ Behavior matches "Back to Map" button functionality

## Reporting Issues

When reporting issues, please include:
1. Platform (Desktop or Web browser name/version)
2. Test scenario number
3. Expected behavior
4. Actual behavior
5. Any console errors
6. Screenshots if applicable
