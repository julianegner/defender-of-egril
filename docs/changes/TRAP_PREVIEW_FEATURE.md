# Trap Preview Feature

## Overview
This feature adds visual feedback when placing traps by showing a semi-transparent preview of the trap icon on valid placement tiles when hovering with the mouse.

## Implementation Details

### What was changed
Modified `composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/GameMap.kt` to add trap preview rendering.

### How it works

1. **Detection**: The system detects when the player is in trap placement mode:
   - `selectedMineAction == MineAction.BUILD_TRAP` (Dwarven Mine trap)
   - `selectedWizardAction == WizardAction.PLACE_MAGICAL_TRAP` (Wizard Tower magical trap)

2. **Validation**: When hovering over a tile, the system checks if it's a valid trap placement location:
   - Must be on the path (`isOnPath`)
   - Must be within range of the selected tower (`distance <= sel.range`)
   - Must not have an enemy currently on it (`!hasEnemy`)
   - Must not already have a trap (`!hasTrap`)

3. **Display**: If all conditions are met, a semi-transparent (50% alpha) trap icon is shown:
   - **Dwarven Trap**: HoleIcon (🕳️) - brown hole icon
   - **Magical Trap**: PentagramIcon (⭐) - purple/magenta pentagram

### Code Structure

The implementation follows the same pattern as the tower placement preview:

```kotlin
// Calculate hover preview for trap placement
val isHoveringForTrapPreview = hoveredPosition == position
val isTrapPlacementMode = selectedMineAction == MineAction.BUILD_TRAP || 
                          selectedWizardAction == WizardAction.PLACE_MAGICAL_TRAP

// Check if this tile is valid for trap placement
val isValidTrapPlacement = if (isTrapPlacementMode && isHoveringForTrapPreview && selectedDefenderId != null) {
    val selectedDefender = gameState.defenders.find { it.id == selectedDefenderId }
    selectedDefender?.let { sel ->
        val distance = sel.position.value.distanceTo(position)
        val hasEnemy = attacker != null
        val hasTrap = trap != null
        isOnPath && distance <= sel.range && !hasEnemy && !hasTrap
    } ?: false
} else {
    false
}
```

### Visual Behavior

- **Dwarven Mine Traps**:
  - Click the "Build Trap" button on a Dwarven Mine
  - Hover over path tiles within the mine's range
  - See a semi-transparent hole icon (🕳️) appear on valid tiles
  - Click to place the trap

- **Magical Traps** (Wizard Tower level 10+):
  - Click the "Place Magical Trap" button on a level 10+ Wizard Tower
  - Hover over path tiles within the wizard's range
  - See a semi-transparent purple pentagram icon appear on valid tiles (only where no enemy or trap exists)
  - Click to place the magical trap

## User Benefits

1. **Improved UX**: Players can clearly see where traps can be placed before clicking
2. **Reduced Errors**: Prevents confusion about why a trap can't be placed on certain tiles
3. **Consistency**: Matches the existing tower placement preview behavior
4. **Visual Clarity**: The semi-transparent icons don't obscure other game elements

## Technical Notes

- The preview uses the same alpha transparency (0.5f / 50%) as tower placement previews
- The icons are rendered in the `GridCellContent` composable, after existing content but before target circles
- No new icons were created - reuses existing `HoleIcon` and `PentagramIcon` components
- The feature automatically works on all platforms (Desktop, Android, iOS, Web/WASM)

## Testing

To test the feature:

1. Start a game and place a Dwarven Mine
2. Once built, click the "Build Trap" button
3. Move the mouse over path tiles within range
4. Verify that a semi-transparent hole icon appears on valid tiles
5. For magical traps, upgrade a Wizard Tower to level 10+
6. Click the "Place Magical Trap" button
7. Move the mouse over path tiles within range
8. Verify that a semi-transparent pentagram icon appears on valid tiles (where no enemy or trap exists)
