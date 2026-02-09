# Trap and Barricade Button Preservation Implementation

## Overview
Implemented UX improvement to keep trap and barricade placement buttons enabled when towers have remaining action points.

## Problem
Previously, after placing a trap or barricade, the placement button would be disabled immediately, requiring the player to click the button again to place another trap/barricade. This was inefficient for high-level towers with multiple actions per turn.

## Solution
Modified GamePlayScreen.kt to conditionally preserve placement mode based on whether the tower has remaining action points after placement.

## Changes

### 1. Helper Function
Added `shouldKeepPlacementMode()` helper function:
```kotlin
private fun shouldKeepPlacementMode(
    gameState: GameState,
    defenderId: Int
): Boolean {
    val defender = gameState.defenders.find { it.id == defenderId } ?: return false
    return defender.actionsRemaining.value > 0
}
```

This function checks if the tower still has actions remaining and should keep the placement mode active.

### 2. Trap Placement Handler
Modified dwarven mine trap placement (line ~551):
```kotlin
if (onMineBuildTrap?.invoke(selectedDefender.id, position) == true) {
    // Keep trap placement mode active if tower has actions remaining
    if (!shouldKeepPlacementMode(gameState, selectedDefender.id)) {
        selectedMineAction = null
        showMineActionDialog = false
    }
}
```

### 3. Barricade Placement Handler
Modified barricade placement (line ~594):
```kotlin
if (onBuildBarricade?.invoke(selectedDefender.id, position) == true) {
    // Keep barricade placement mode active if tower has actions remaining
    if (!shouldKeepPlacementMode(gameState, selectedDefender.id)) {
        selectedBarricadeAction = null
    }
}
```

### 4. Magical Trap Unchanged
Magical trap placement (wizard towers) continues to always clear the mode after placement because:
- Magical traps have a 10-turn cooldown mechanism
- The button would be disabled anyway due to the cooldown
- Keeping it selected would be confusing for the player

## Behavior

### Before
- Place trap/barricade → Button disabled → Must click button again → Place another trap/barricade
- Required 2 clicks per trap/barricade even when tower had multiple actions

### After
- Place trap/barricade → Button stays enabled (if actions > 0) → Immediately click map → Place another trap/barricade
- Requires only 1 click per trap/barricade for multi-action towers
- Button automatically disables when actions reach 0

## Affected Tower Types

### Dwarven Mine (Traps)
- Level 10+: 2 actions per turn
- Can now place 2 traps consecutively without re-clicking button

### Spike Tower (Barricades)
- Level 20+: Can build barricades
- Level 20: 3 actions per turn
- Level 30: 4 actions per turn
- Can now place multiple barricades consecutively

### Spear Tower (Barricades)
- Level 10+: Can build barricades
- Level 15+: 2 actions per turn
- Can now place/upgrade barricades consecutively

### Wizard Tower (Magical Traps)
- Level 10+: Can place magical traps
- **Behavior unchanged** - button always disabled after placement due to cooldown

## Testing
Comprehensive test suite added in `TrapBarricadePlacementModeTest.kt`:
- Action consumption verification
- Multiple placement scenarios
- Edge cases (no actions, cooldown)
- All tests passing ✓

## Pattern Consistency
This implementation follows the same pattern as attack button selection preservation:
- `shouldKeepTargetSelection()` for attacks
- `shouldKeepPlacementMode()` for traps/barricades

Both patterns check if the tower has remaining actions and conditionally preserve the active mode.

## User Experience Benefits
1. **Efficiency**: Fewer clicks needed for multi-action towers
2. **Flow**: More fluid gameplay when placing multiple traps/barricades
3. **Consistency**: Matches behavior of attack buttons
4. **Intuitive**: Button state reflects tower's action availability

## Files Modified
- `composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/GamePlayScreen.kt`
  - Added `shouldKeepPlacementMode()` helper
  - Modified trap placement handler
  - Modified barricade placement handler

## Files Added
- `composeApp/src/commonTest/kotlin/de/egril/defender/game/TrapBarricadePlacementModeTest.kt`
  - Comprehensive test coverage
  - 10 test cases covering all scenarios
