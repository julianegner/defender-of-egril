# Trap/Barricade Button Behavior Comparison

## Visual Flow Comparison

### Before (Old Behavior)
```
┌─────────────────────────────────────────────────────────┐
│ Level 10 Dwarven Mine (2 actions remaining)            │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│ Player clicks "Set Trap" button                        │
│ Button shows yellow border (active mode)               │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│ Player clicks map position                             │
│ Trap is placed, action consumed (1 remaining)          │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│ ❌ Button is DISABLED (mode cleared)                   │
│ ❌ Must click button AGAIN to continue                 │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│ Player clicks "Set Trap" button AGAIN                  │
│ Button shows yellow border (active mode)               │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│ Player clicks map position                             │
│ Second trap placed, action consumed (0 remaining)       │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│ Button is disabled (no actions left)                   │
└─────────────────────────────────────────────────────────┘

Total clicks: 4 (2 button clicks + 2 map clicks)
```

### After (New Behavior)
```
┌─────────────────────────────────────────────────────────┐
│ Level 10 Dwarven Mine (2 actions remaining)            │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│ Player clicks "Set Trap" button                        │
│ Button shows yellow border (active mode)               │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│ Player clicks map position                             │
│ Trap is placed, action consumed (1 remaining)          │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│ ✅ Button STAYS ENABLED (mode preserved)               │
│ ✅ Can immediately place next trap                     │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│ Player clicks map position directly                    │
│ Second trap placed, action consumed (0 remaining)       │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│ Button is disabled (no actions left)                   │
└─────────────────────────────────────────────────────────┘

Total clicks: 3 (1 button click + 2 map clicks)
```

## Impact Analysis

### Click Reduction
| Actions Per Turn | Traps/Barricades | Old Clicks | New Clicks | Saved |
|-----------------|------------------|------------|------------|-------|
| 2               | 2                | 4          | 3          | 25%   |
| 3               | 3                | 6          | 4          | 33%   |
| 4               | 4                | 8          | 5          | 37.5% |
| 5               | 5                | 10         | 6          | 40%   |

### Tower Types Affected

#### Dwarven Mine (Traps)
- **Level 10**: 2 actions → 2 traps (save 1 click)
- **Level 15**: 2 actions → 2 traps (save 1 click)
- **Level 20**: 3 actions → 3 traps (save 2 clicks)
- **Level 30**: 4 actions → 4 traps (save 3 clicks)

#### Spike Tower (Barricades)
- **Level 20**: 3 actions → 3 barricades (save 2 clicks)
- **Level 30**: 4 actions → 4 barricades (save 3 clicks)
- **Level 40**: 5 actions → 5 barricades (save 4 clicks)

#### Spear Tower (Barricades)
- **Level 15**: 2 actions → 2 barricades (save 1 click)
- **Level 20**: 3 actions → 3 barricades (save 2 clicks)
- **Level 30**: 4 actions → 4 barricades (save 3 clicks)

## UI State Indicators

### Button States
1. **Disabled**: Gray, cannot click (no actions OR on cooldown)
2. **Enabled**: Blue/Brown, can click to activate mode
3. **Active**: Yellow border, placement mode active

### Button Behavior by Type

#### Dwarven Mine Trap Button
```
Initial: [SET TRAP] (enabled, 2 actions)
         ↓ Click button
Active:  [SET TRAP] (yellow border)
         ↓ Click map
After 1: [SET TRAP] (yellow border, 1 action) ← STAYS ACTIVE
         ↓ Click map
After 2: [SET TRAP] (disabled, 0 actions)
```

#### Barricade Button
```
Initial: [BARRICADE] (enabled, 3 actions)
         ↓ Click button
Active:  [BARRICADE] (yellow border)
         ↓ Click map
After 1: [BARRICADE] (yellow border, 2 actions) ← STAYS ACTIVE
         ↓ Click map
After 2: [BARRICADE] (yellow border, 1 action) ← STAYS ACTIVE
         ↓ Click map
After 3: [BARRICADE] (disabled, 0 actions)
```

#### Magical Trap Button (Unchanged)
```
Initial: [MAGICAL TRAP] (enabled, 2 actions, 0 cooldown)
         ↓ Click button
Active:  [MAGICAL TRAP] (yellow border)
         ↓ Click map
After 1: [MAGICAL TRAP] (disabled, 10 cooldown) ← ALWAYS CLEARS
```

## Code Flow Comparison

### Before
```kotlin
if (onMineBuildTrap?.invoke(selectedDefender.id, position) == true) {
    selectedMineAction = null  // Always clear
    showMineActionDialog = false
}
```

### After
```kotlin
if (onMineBuildTrap?.invoke(selectedDefender.id, position) == true) {
    // Keep trap placement mode active if tower has actions remaining
    if (!shouldKeepPlacementMode(gameState, selectedDefender.id)) {
        selectedMineAction = null
        showMineActionDialog = false
    }
}
```

### Helper Function
```kotlin
private fun shouldKeepPlacementMode(
    gameState: GameState,
    defenderId: Int
): Boolean {
    val defender = gameState.defenders.find { it.id == defenderId } ?: return false
    return defender.actionsRemaining.value > 0
}
```

## User Experience Flow

### Typical Game Scenario (Level 20+ Gameplay)

**Before:**
1. Level 20 Spike Tower (3 actions)
2. Click "Barricade" button
3. Click map position 1 → Barricade placed
4. ❌ Click "Barricade" button AGAIN
5. Click map position 2 → Barricade placed
6. ❌ Click "Barricade" button AGAIN
7. Click map position 3 → Barricade placed

**After:**
1. Level 20 Spike Tower (3 actions)
2. Click "Barricade" button
3. Click map position 1 → Barricade placed
4. ✅ Click map position 2 directly → Barricade placed
5. ✅ Click map position 3 directly → Barricade placed

Result: **33% fewer clicks** for the same outcome!

## Player Feedback Expectations

### Positive Impacts
- **Faster gameplay**: Less clicking = more fluid experience
- **Less repetitive**: Don't need to keep re-enabling the same mode
- **More strategic**: Can focus on placement decisions, not UI navigation
- **Consistent**: Matches attack button behavior

### Edge Cases Handled
1. **Actions exhausted**: Button automatically disables
2. **Cooldown active**: Button stays disabled (magical traps)
3. **Tower sold**: Selection and mode cleared
4. **Turn ended**: All modes reset for new turn

## Technical Notes

### Pattern Consistency
This implementation follows the same pattern as attack button preservation:
- Both use `shouldKeep*()` helper functions
- Both check `actionsRemaining.value > 0`
- Both preserve selection/mode when condition is true
- Both clear when condition is false

### Exception: Magical Traps
Magical traps do NOT preserve mode because:
- 10-turn cooldown after placement
- Button would be disabled anyway
- Preserving mode would be confusing
- User would see active mode but disabled button
