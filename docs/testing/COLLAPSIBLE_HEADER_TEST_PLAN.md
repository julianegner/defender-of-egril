# Collapsible Map Editor Header - Test Plan

## Manual Testing Checklist

### Test Environment Setup
- [ ] Desktop platform (Windows, Mac, or Linux)
- [ ] Game launched successfully
- [ ] Level Editor accessed from World Map
- [ ] Map Editor tab selected
- [ ] At least one map opened for editing

---

## Test Cases

### TC1: Initial State - Header is Expanded by Default
**Steps:**
1. Open any map in Map Editor
2. Observe the header state

**Expected Result:**
- ✓ Header is in expanded state showing all controls
- ✓ Header height is approximately 280dp
- ✓ Collapse button visible with ▲ icon and "Collapse" text
- ✓ All tile type buttons visible in a row
- ✓ Map name input field visible
- ✓ Zoom controls visible

---

### TC2: Collapse Header
**Steps:**
1. Open any map in Map Editor (header should be expanded)
2. Click the [▲ Collapse] button in the top-right of header

**Expected Result:**
- ✓ Header transitions to collapsed state
- ✓ Header height reduces to approximately 56dp
- ✓ Compact card appears on left side
- ✓ Tile type dropdown visible showing current tile type
- ✓ Expand button visible with ▼ icon
- ✓ Map area gains significant vertical space (~224dp more)
- ✓ Map remains interactive

---

### TC3: Expand Header
**Steps:**
1. Ensure header is in collapsed state
2. Click the [▼] expand button

**Expected Result:**
- ✓ Header transitions back to expanded state
- ✓ All original controls are visible again
- ✓ Map area returns to original size
- ✓ Current tile type selection is preserved
- ✓ Map name is preserved
- ✓ All functionality restored

---

### TC4: Tile Type Selection - Collapsed State
**Steps:**
1. Collapse the header
2. Click on the tile type dropdown button
3. Observe the dropdown menu

**Expected Result:**
- ✓ Dropdown menu opens
- ✓ All 7 tile types are visible: PATH, BUILD_AREA, ISLAND, NO_PLAY, SPAWN_POINT, TARGET, RIVER
- ✓ Current selection is indicated
- ✓ Menu is properly styled

---

### TC5: Change Tile Type in Collapsed Mode (Non-River)
**Steps:**
1. Collapse the header
2. Open tile type dropdown
3. Select "PATH" (or any non-RIVER tile)
4. Click on map hexagons

**Expected Result:**
- ✓ Dropdown closes
- ✓ Button text updates to show "PATH"
- ✓ Clicking on hexagons paints them as PATH tiles
- ✓ Map updates correctly

---

### TC6: Change Tile Type to RIVER in Collapsed Mode
**Steps:**
1. Collapse the header
2. Open tile type dropdown
3. Select "RIVER"

**Expected Result:**
- ✓ Dropdown closes
- ✓ Button text updates to show "RIVER"
- ✓ River Properties dialog automatically opens
- ✓ Dialog shows flow direction options (NONE, MAELSTROM, EAST, SE, SW, WEST, NW, NE)
- ✓ Dialog shows flow speed options (1 - Slow, 2 - Fast)
- ✓ Current selections are highlighted
- ✓ [OK] button is visible

---

### TC7: Configure River Properties in Collapsed Mode
**Steps:**
1. Collapse header and select RIVER tile type (dialog should open)
2. Select "EAST" for flow direction
3. Select "2 (Fast)" for flow speed
4. Click [OK]
5. Click on map hexagons

**Expected Result:**
- ✓ Dialog closes after clicking OK
- ✓ Selections are saved
- ✓ Clicking hexagons paints RIVER tiles
- ✓ River tiles show correct flow indicator (arrow pointing east)
- ✓ River tiles show correct speed indicator (double arrows for speed 2)

---

### TC8: River Properties Dialog - Cancel/Dismiss
**Steps:**
1. Collapse header and select RIVER tile type
2. Change flow direction
3. Click outside the dialog or press ESC (if supported)

**Expected Result:**
- ✓ Dialog closes
- ✓ Changes may or may not be saved (depending on implementation)
- ✓ RIVER tile type remains selected

---

### TC9: Map Editing in Collapsed State
**Steps:**
1. Collapse the header
2. Pan the map (drag)
3. Zoom in/out (mouse wheel or zoom controls if visible)
4. Paint tiles by clicking/dragging
5. Use keyboard navigation (if supported)

**Expected Result:**
- ✓ All map interactions work normally
- ✓ Pan gesture works correctly
- ✓ Zoom functionality works
- ✓ Tile painting works (brush mode)
- ✓ Keyboard navigation works
- ✓ No visual glitches or overlaps

---

### TC10: State Persistence within Session
**Steps:**
1. Open map, collapse header
2. Paint some tiles
3. Expand header
4. Make changes (e.g., change map name)
5. Collapse header again

**Expected Result:**
- ✓ Header collapses/expands correctly each time
- ✓ Tile changes are preserved
- ✓ Map name changes are preserved
- ✓ Current tile type selection is preserved
- ✓ No data loss during state changes

---

### TC11: Save Map with Collapsed Header
**Steps:**
1. Open map, collapse header
2. Make some tile changes
3. Click [Save] button (below map)

**Expected Result:**
- ✓ Map saves successfully
- ✓ Changes are persisted to file
- ✓ No errors occur
- ✓ Editor returns to map list

---

### TC12: Multiple Toggle Cycles
**Steps:**
1. Open map
2. Collapse header
3. Expand header
4. Collapse header
5. Expand header
6. Repeat 10 times

**Expected Result:**
- ✓ Header toggles smoothly each time
- ✓ No visual glitches
- ✓ No performance degradation
- ✓ All controls remain functional
- ✓ No memory leaks (observe app performance)

---

### TC13: Tile Type Selection - All Types
**Steps:**
1. Collapse header
2. For each tile type (PATH, BUILD_AREA, ISLAND, NO_PLAY, SPAWN_POINT, TARGET, RIVER):
   a. Select from dropdown
   b. Paint 2-3 hexagons
   c. Verify correct color and behavior

**Expected Result:**
- ✓ All 7 tile types are selectable
- ✓ Each tile type paints with correct color
- ✓ RIVER type opens properties dialog
- ✓ Non-RIVER types don't show dialog
- ✓ No errors or crashes

---

### TC14: River Flow Directions - All Options
**Steps:**
1. Collapse header, select RIVER
2. For each flow direction (NONE, MAELSTROM, EAST, SE, SW, WEST, NW, NE):
   a. Select direction in dialog
   b. Click [OK]
   c. Paint a tile
   d. Verify flow indicator

**Expected Result:**
- ✓ All 8 flow directions are selectable
- ✓ Each direction shows correct indicator:
  - NONE: dot or simple marker
  - MAELSTROM: whirlpool icon
  - EAST/SE/SW/WEST/NW/NE: arrows pointing in correct direction
- ✓ Indicators are visible on blue RIVER background

---

### TC15: River Flow Speeds - Both Options
**Steps:**
1. Collapse header, select RIVER
2. Select speed 1 (Slow), OK, paint tile → observe
3. Open dropdown again, select RIVER
4. Select speed 2 (Fast), OK, paint tile → observe

**Expected Result:**
- ✓ Speed 1: single arrow indicator
- ✓ Speed 2: double arrow indicator
- ✓ Visual difference is clear

---

### TC16: Responsive Layout - Various Window Sizes
**Steps:**
1. Open map editor
2. Resize window to small size (e.g., 800x600)
3. Collapse header → observe
4. Resize to large size (e.g., 1920x1080)
5. Observe layout

**Expected Result:**
- ✓ Collapsed header remains at 280dp width
- ✓ Header doesn't overflow or clip
- ✓ Dropdown menu is fully visible
- ✓ Dialog is properly centered
- ✓ Map area adjusts appropriately

---

### TC17: Dark/Light Mode Compatibility
**Steps:**
1. Open map editor in light mode
2. Collapse/expand header → observe styling
3. Switch to dark mode (if supported)
4. Collapse/expand header → observe styling

**Expected Result:**
- ✓ Header colors follow theme in both modes
- ✓ Text is readable in both modes
- ✓ Icons are visible in both modes
- ✓ No contrast issues

---

### TC18: Keyboard Accessibility
**Steps:**
1. Open map editor
2. Use TAB to navigate to collapse button → press ENTER
3. Use TAB to navigate to tile type dropdown → press ENTER
4. Use arrow keys to select tile type → press ENTER
5. (If dialog opens) TAB through dialog options → select with ENTER/SPACE

**Expected Result:**
- ✓ All interactive elements are keyboard-accessible
- ✓ Tab order is logical
- ✓ Visual focus indicators are visible
- ✓ ENTER/SPACE activates controls correctly

---

### TC19: Error Handling - Edge Cases
**Steps:**
1. Collapse header
2. Rapidly click expand/collapse button multiple times
3. Click dropdown while dialog is open
4. Press ESC while dropdown is open

**Expected Result:**
- ✓ No crashes or errors
- ✓ UI remains responsive
- ✓ State management handles rapid changes gracefully
- ✓ Dialogs/dropdowns close appropriately

---

### TC20: Integration with Other Features
**Steps:**
1. Open map, collapse header
2. Click minimap (if visible)
3. Use zoom controls (if visible in collapsed state)
4. Click "Change All NO_PLAY to PATH" (requires expanded state)
5. Save/Save As/Cancel buttons

**Expected Result:**
- ✓ Minimap works correctly
- ✓ All other map editor features remain functional
- ✓ No feature conflicts or breaking changes
- ✓ Bottom buttons (Save, Save As, Cancel) always accessible

---

## Regression Testing

### Existing Functionality to Verify
- [ ] Map name editing works (expanded state)
- [ ] All tile types paintable (both states)
- [ ] Zoom controls work (expanded state)
- [ ] "Change All NO_PLAY to PATH" button works (expanded state)
- [ ] River properties in expanded state work same as before
- [ ] Map saving works
- [ ] Map cancellation works
- [ ] Minimap works
- [ ] Pan and zoom gestures work

---

## Performance Testing

### Metrics to Monitor
- [ ] Header collapse/expand transition is smooth (<100ms)
- [ ] Dropdown menu opens instantly
- [ ] Dialog appears without delay
- [ ] No frame drops when toggling header rapidly
- [ ] Memory usage remains stable after multiple toggles

---

## Known Issues / Limitations

Document any issues found during testing:
1. 
2. 
3. 

---

## Test Results Summary

- Total Test Cases: 20
- Passed: ___
- Failed: ___
- Blocked: ___
- Not Tested: ___

**Overall Status**: [ ] PASS / [ ] FAIL

**Tester Name**: ________________
**Date**: ________________
**Platform**: ________________
**Build Version**: ________________

---

## Notes

Additional observations or comments:
