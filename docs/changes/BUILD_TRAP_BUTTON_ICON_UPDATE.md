# Build Trap Button Icon Update

## Overview
Updated the Build Trap button in the Dwarven Mine action panel to use the new `TrapIcon` (trap.png) instead of the old `HoleIcon` (emoji_hole.png).

## Change Location
**File:** `composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/DefenderInfo.kt`

### Before (Line 438)
```kotlin
HoleIcon(size = 24.dp)
Text(stringResource(Res.string.trap), fontSize = 14.sp, fontWeight = FontWeight.Bold)
```

### After (Line 438)
```kotlin
TrapIcon(size = 24.dp)
Text(stringResource(Res.string.trap), fontSize = 14.sp, fontWeight = FontWeight.Bold)
```

## Visual Comparison

### Before: HoleIcon (emoji_hole.png)
![Old Icon](https://github.com/user-attachments/assets/f64c12cf-a0f1-4229-a293-4f47552c687b)
- Simple emoji-style hole
- Basic circular design
- Generic appearance

### After: TrapIcon (trap.png)
![New Icon](https://github.com/user-attachments/assets/13354565-0cf2-44aa-9731-8c038a4360c3)
- Professional pit trap illustration
- Detailed with grass and flowers
- Matches game art quality

## Context
This change completes the full integration of trap.png across all trap-related UI elements:

1. ✅ **Trap display on game map** - Shows placed traps with trap.png
2. ✅ **Trap placement preview** - Semi-transparent trap.png during placement
3. ✅ **Build Trap button** - Action button icon on Dwarven Mine panel (NEW)

## User Impact
When players select a Dwarven Mine tower, they will see the Build Trap button with the new professional trap.png icon, providing:
- **Visual consistency** - Same icon used across all trap-related UI
- **Better clarity** - Professional artwork is more visually distinct
- **Enhanced UX** - Improved visual quality matches game standards

## Technical Details
- **Import added:** `import de.egril.defender.ui.icon.TrapIcon`
- **Lines changed:** 2 (1 import, 1 icon replacement)
- **Build status:** ✅ Successful
- **Compatibility:** All platforms (Desktop, Android, iOS, Web/WASM)
