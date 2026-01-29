# Manual Testing Guide: Barricade Button Fix

## Issues Fixed
1. **Barricade button now has yellow border when active** (like trap buttons)
2. **Clicking barricade button again deselects it** (toggle behavior)
3. **Spear towers can now attack enemies when barricade mode is active** (barricade mode can be canceled)

## Test Scenarios

### Scenario 1: Visual Verification - Button Highlighting
**Goal:** Verify the barricade button shows a yellow border when active

**Steps:**
1. Start a new game (any level)
2. Build a Spear Tower
3. Upgrade the Spear Tower to level 10+ (use cheat code `cash` if needed)
4. Select the Spear Tower
5. Click the "Build Barricade" button (brown button with wood icon)
6. **Expected:** The button should now have a thick yellow border (3dp wide)
7. Click the button again
8. **Expected:** The yellow border should disappear (button is deselected)

### Scenario 2: Attack Functionality with Barricade Mode
**Goal:** Verify spear towers can attack enemies even with barricade mode available

**Steps:**
1. Start a new game (any level with enemies)
2. Build a Spear Tower and upgrade to level 10+
3. Start the battle (enemies should appear)
4. Select the Spear Tower
5. **Without clicking barricade button:**
   - Click on an enemy within range
   - **Expected:** Attack button appears, allowing you to attack the enemy
6. Click "Build Barricade" button (activates barricade mode - yellow border appears)
7. Click the "Build Barricade" button again (deselects barricade mode - yellow border disappears)
8. Click on an enemy within range
9. **Expected:** Attack button appears, allowing you to attack the enemy

### Scenario 3: Toggle Behavior
**Goal:** Verify clicking the barricade button toggles the mode on/off

**Steps:**
1. Build and upgrade a Spear Tower to level 10+
2. Select the tower
3. Click "Build Barricade" button
4. **Expected:** Yellow border appears, map shows yellow highlights on valid barricade positions
5. Click "Build Barricade" button again
6. **Expected:** Yellow border disappears, map highlights disappear
7. Click "Build Barricade" button again
8. **Expected:** Yellow border appears again, map shows highlights again

### Scenario 4: Mode Clearing
**Goal:** Verify barricade mode is cleared when selecting different towers/enemies

**Steps:**
1. Build two towers (both upgraded to level 10+)
2. Select first tower, click "Build Barricade" button
3. **Expected:** Yellow border appears on barricade button
4. Click on the second tower (different tower)
5. **Expected:** Yellow border should disappear (mode cleared)
6. Click back to first tower
7. **Expected:** No yellow border (mode not preserved)

### Scenario 5: Comparison with Trap Buttons
**Goal:** Verify barricade button behavior matches trap button behavior

**Steps:**
1. Build a Dwarven Mine
2. Select the mine
3. Click the "Trap" button
4. **Expected:** Yellow border appears on trap button
5. Build a Spear Tower and upgrade to level 10+
6. Select the Spear Tower
7. Click "Build Barricade" button
8. **Expected:** Yellow border appears on barricade button (identical style to trap button)

## Visual Indicators

### Before Fix:
- Barricade button: No yellow border when clicked
- User confusion: Is barricade mode active or not?
- Cannot attack enemies while barricade mode is "stuck" active

### After Fix:
- Barricade button: Thick yellow border (3dp) when active
- Clear visual feedback matching trap button style
- Can toggle barricade mode off by clicking button again
- Can attack enemies after deselecting barricade mode

## Code Changes Summary

### Files Modified:
1. **DefenderInfo.kt**
   - Added `selectedBarricadeAction: BarricadeAction?` parameter
   - Added border logic to BarricadeButton: shows yellow border when `selectedBarricadeAction == BarricadeAction.BUILD_BARRICADE`
   - Pass selectedBarricadeAction to BarricadeButton

2. **GameControls.kt**
   - Added `selectedBarricadeAction: BarricadeAction?` parameter
   - Pass selectedBarricadeAction to DefenderInfo (both call sites)

3. **GamePlayScreen.kt**
   - Modified handleBarricadeAction to toggle mode (if already active, set to null)
   - Pass selectedBarricadeAction to GameControlsPanel (INITIAL_BUILDING and PLAYER_TURN phases)
   - Clear barricade mode when selecting different defenders (like trap modes)

## Testing Checklist
- [ ] Barricade button shows yellow border when clicked
- [ ] Barricade button removes yellow border when clicked again (toggle)
- [ ] Spear tower can attack enemies after deselecting barricade mode
- [ ] Barricade mode is cleared when selecting a different tower
- [ ] Visual style matches trap button (same border width and color)
- [ ] No regression: barricade placement still works correctly
- [ ] No regression: barricade reinforcement still works correctly
