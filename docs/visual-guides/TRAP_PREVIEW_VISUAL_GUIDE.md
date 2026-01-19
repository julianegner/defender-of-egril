# Trap Preview Visual Guide

## Feature Overview
This document provides a visual guide to the trap preview feature implemented in the game.

## Dwarven Trap Preview

When placing a dwarven trap from a mine:

```
┌─────────────────────────────────────────────────────┐
│  Step 1: Select a Dwarven Mine                     │
│  ┌────┐                                            │
│  │ ⛏️ │ [Selected Mine]                            │
│  └────┘                                            │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│  Step 2: Click "Build Trap" button                 │
│  [Build Trap] ← Click this                         │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│  Step 3: Hover over path tiles within range        │
│                                                     │
│     ⛏️                                              │
│  [Mine] ───> Path Tiles                            │
│                                                     │
│  Valid tiles show: 🕳️ (50% transparent)            │
│  - Must be on path                                  │
│  - Must be within mine range (3 tiles)             │
│  - Must not have enemy                              │
│  - Must not have existing trap                      │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│  Step 4: Click to place trap                        │
│  🕳️ -10 damage ← Trap placed!                      │
└─────────────────────────────────────────────────────┘
```

## Magical Trap Preview

When placing a magical trap from a wizard tower:

```
┌─────────────────────────────────────────────────────┐
│  Step 1: Select a Wizard Tower (level 10+)         │
│  ┌────┐                                            │
│  │ 🔮 │ [Selected Wizard Tower]                    │
│  └────┘                                            │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│  Step 2: Click "Place Magical Trap" button         │
│  [Place Magical Trap] ← Click this                 │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│  Step 3: Hover over path tiles within range        │
│                                                     │
│     🔮                                              │
│  [Wizard] ───> Path Tiles                          │
│                                                     │
│  Valid tiles show: ⭐ (50% transparent purple)      │
│  - Must be on path                                  │
│  - Must be within wizard range (3 tiles)           │
│  - Must not have enemy                              │
│  - Must not have existing trap                      │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│  Step 4: Click to place magical trap                │
│  ⭐ ← Magical trap placed! (teleports enemies)      │
└─────────────────────────────────────────────────────┘
```

## Visual Comparison

### Before (No Preview)
```
Player hovers over path tile
  ↓
No visual feedback
  ↓
Click to place (may fail silently if invalid)
```

### After (With Preview)
```
Player hovers over path tile
  ↓
Preview icon appears (50% transparent) if valid
  OR
No preview if invalid (out of range, has enemy, etc.)
  ↓
Click to place (with confidence!)
```

## Icon Details

### Dwarven Trap Icon (HoleIcon)
- Visual: 🕳️ (hole/pit icon)
- Color: Brown/neutral
- Size: 24dp
- Alpha: 0.5 (50% transparent)

### Magical Trap Icon (PentagramIcon)
- Visual: ⭐ (purple pentagram)
- Color: Purple/Magenta (#AA00FF)
- Size: 24dp
- Alpha: 0.5 (50% transparent)

## Placement Rules Visualized

```
Valid Placement:
┌─────┬─────┬─────┬─────┬─────┐
│     │     │  ⛏️  │     │     │  ← Mine position
├─────┼─────┼─────┼─────┼─────┤
│     │ 🕳️  │ 🕳️  │ 🕳️  │     │  ← Path tiles in range
├─────┼─────┼─────┼─────┼─────┤  ← Shows preview
│     │ 🕳️  │ 🕳️  │ 🕳️  │     │
├─────┼─────┼─────┼─────┼─────┤
│     │ 🕳️  │ 🕳️  │ 🕳️  │     │
└─────┴─────┴─────┴─────┴─────┘

Invalid Placement (No Preview Shown):
┌─────┬─────┬─────┬─────┬─────┐
│     │     │  ⛏️  │     │     │  ← Mine position
├─────┼─────┼─────┼─────┼─────┤
│     │ 👹  │     │     │ ❌  │  ← Enemy (no preview)
├─────┼─────┼─────┼─────┼─────┤  ← Out of range (no preview)
│     │ 🕳️  │     │     │     │  ← Existing trap (no preview)
├─────┼─────┼─────┼─────┼─────┤
│ ❌  │ ❌  │ ❌  │ ❌  │ ❌  │  ← Not on path (no preview)
└─────┴─────┴─────┴─────┴─────┘
```

## Implementation Details

The preview is implemented in `GameMap.kt` using the following logic:

1. **Detection**: Check if player is in trap placement mode
   - `selectedMineAction == MineAction.BUILD_TRAP`
   - `selectedWizardAction == WizardAction.PLACE_MAGICAL_TRAP`

2. **Validation**: For hovered tile, check:
   - `isOnPath` (must be on the path)
   - `distance <= tower.range` (within tower range)
   - `!hasEnemy` (no enemy on tile)
   - `!hasTrap` (no existing trap on tile)

3. **Display**: If valid, show semi-transparent icon:
   - Dwarven: `HoleIcon(size = 24.dp)` with `alpha = 0.5f`
   - Magical: `PentagramIcon(size = 24.dp)` with `alpha = 0.5f`

## Testing

All placement rules are validated with unit tests:
- ✓ Range restrictions (dwarven: 3 tiles, wizard: 3 tiles)
- ✓ Path tile requirement
- ✓ Enemy blocking detection
- ✓ Existing trap blocking detection
- ✓ Trap type existence validation

See `TrapPlacementPreviewTest.kt` for complete test suite (9 tests, all passing).
