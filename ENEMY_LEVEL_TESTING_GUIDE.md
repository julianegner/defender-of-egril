# Manual Testing Guide: Enemy Level Display

## Feature: Store and Display Enemy Unit Levels

This guide describes how to manually test the enemy level display feature.

## Prerequisites
- Build and run the desktop application: `./gradlew :composeApp:run`
- OR run on Android/iOS device

## Test Cases

### 1. Enemy Level in Enemy List (Planned Spawns)
**Expected Behavior**: Enemy units in the "Planned Spawns" section should display their level if > 1.

**Steps**:
1. Start the game
2. Select a level from the world map
3. Look at the right overlay panel (click the icon if not visible)
4. Check the "Planned Spawns" section
5. Look for enemies with "LvX" badge (where X > 1)

**What to verify**:
- Enemies with level 1 should NOT show a level badge
- Enemies with level > 1 should show "Lv2", "Lv3", etc. in red text next to their name
- The level badge should be visible and readable

### 2. Enemy Level in Active Enemy List (On Map)
**Expected Behavior**: Enemy units that are currently on the map should display their level if > 1.

**Steps**:
1. Start the game and progress to a turn where enemies have spawned
2. Look at the right overlay panel
3. Check the "On Map:" section
4. Look for enemies with "LvX" badge (where X > 1)

**What to verify**:
- Same as above - level badge should only appear for enemies with level > 1
- The display format should match the planned spawns display

### 3. Enemy Details When Clicking on Enemy Tile
**Expected Behavior**: Clicking on a tile with an enemy should show enemy details in the tower area.

**Steps**:
1. Start the game and progress to a turn where enemies are on the map
2. Click on a hexagon tile that contains an enemy unit
3. Observe the bottom panel

**What to verify**:
- The enemy details panel should appear on the left side
- The buy tower buttons should move to the right (4x2 grid)
- The panel should show:
  - Large enemy icon (matching tower icon size)
  - Enemy name
  - Level badge if level > 1 (e.g., "Lv3" in red)
  - Current HP / Max HP
  - Position coordinates
  - Special abilities (if any):
    - ⚡ Can summon minions
    - 💚 Can heal other enemies
    - 🔒 Can disable towers
    - 🛡️ Immune to acid
    - 🛡️ Immune to fireball

### 4. Enemy Details vs Tower Details
**Expected Behavior**: Enemy details and tower details should use the same layout pattern.

**Steps**:
1. Click on a tower tile
2. Observe the tower details panel
3. Click on an enemy tile
4. Observe the enemy details panel
5. Compare the layouts

**What to verify**:
- Both panels should appear in the same location (left side of bottom panel)
- Buy buttons should move to the right for both
- Icon size should be the same (96dp on desktop, 64dp on mobile)
- Overall layout should feel consistent

### 5. Deselecting Units
**Expected Behavior**: Clicking the same unit twice should deselect it.

**Steps**:
1. Click on an enemy tile
2. Verify enemy details appear
3. Click on the same enemy tile again
4. Verify the panel disappears and buy buttons return to normal layout

**What to verify**:
- Enemy can be deselected by clicking again
- Tower can be deselected by clicking again
- Selecting a tower deselects any selected enemy
- Selecting an enemy deselects any selected tower

### 6. Level Editor - Setting Enemy Levels
**Expected Behavior**: Level editor should allow setting enemy levels.

**Steps** (Desktop/WASM only):
1. Open Level Editor
2. Go to "Level Editor" tab
3. Create or edit a level
4. Add enemy spawns to a turn
5. Check the "Enemy Level" field

**What to verify**:
- Each enemy spawn should have a level field
- Level should default to 1
- Level can be changed to any positive integer
- Saved level should preserve the enemy levels

### 7. Save/Load - Enemy Levels Persist
**Expected Behavior**: Enemy levels should be saved and loaded correctly.

**Steps**:
1. Start a game with enemies that have levels > 1
2. Wait for some enemies to spawn
3. Save the game
4. Load the saved game
5. Check the enemy list and click on enemies

**What to verify**:
- Enemy levels are preserved after save/load
- Active enemies on the map retain their level
- Planned spawns retain their level

### 8. Localization - Enemy Abilities
**Expected Behavior**: Enemy ability descriptions should appear in the selected language.

**Steps**:
1. Change language in Settings (German, Spanish, French, or Italian)
2. Click on an enemy with special abilities (Evil Mage, Red Witch, Green Witch, Blue Demon, Red Demon)
3. Check the ability descriptions

**What to verify**:
- Ability descriptions appear in the selected language
- Translations are correct and readable

## Test Data

### Enemies with Special Abilities
- **Evil Mage**: Can summon minions
- **Red Witch**: Can disable towers
- **Green Witch**: Can heal other enemies
- **Blue Demon**: Immune to acid
- **Red Demon**: Immune to fireball
- **Ewhad**: Can summon minions (boss)

### Creating Test Levels with Different Enemy Levels
To test enemy levels, create custom levels in the Level Editor with enemies at various levels (1, 2, 3, 5, 10).

## Known Limitations
- Level editor is only available on Desktop and Web/WASM platforms
- Enemy level cannot be changed after the enemy has spawned

## Automated Tests
The following automated tests verify the core functionality:
- `EnemyLevelTest.kt` - Tests default level behavior and level storage
