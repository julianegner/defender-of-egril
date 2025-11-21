# Icon Implementation for Web/WASM Compatibility

## Overview

This document describes how Unicode emoji icons have been replaced with Canvas-drawn icons to ensure compatibility with web/WASM platforms where Unicode emojis may not render correctly.

## Problem

Unicode emojis (⚡, ⏱, ⚔️, etc.) used throughout the UI were not rendering properly on web/WASM platforms. These emojis were used for:
- Lightning bolt (⚡) - indicating available actions
- Timer (⏱) - indicating build time remaining
- Sword (⚔️) - indicating attack actions

## Solution

Created custom Canvas-drawn icons that work consistently across all platforms including desktop, Android, iOS, and web/WASM.

## Implementation Details

### New File: `IconUtils.kt`

Created three Composable icon functions:

1. **LightningIcon** - Draws a yellow lightning bolt
   - Default size: 16dp
   - Color: Yellow
   - Used to indicate available tower actions

2. **TimerIcon** - Draws a clock/timer
   - Default size: 10dp
   - Color: Orange (0xFFFFA500)
   - Shows hour and minute hands
   - Used to indicate build time remaining

3. **SwordIcon** - Draws a sword
   - Default size: 14dp or 24dp (depending on context)
   - Features: Silver blade, gold crossguard and pommel, brown handle
   - Used to indicate attack actions

### Files Modified

1. **UnitIcons.kt**
   - Replaced `Text("⚡")` with `LightningIcon()`
   - Replaced `Text("⏱...")` with Row containing `TimerIcon()` and text

2. **GamePlayScreen.kt**
   - Removed `ATTACK_ICON` constant
   - Replaced all `Text(ATTACK_ICON)` with `SwordIcon()`
   - Replaced lightning and timer emojis in info displays with icon components

3. **LoadGameScreen.kt**
   - Replaced timer emoji with `TimerIcon()` in save game display

4. **RulesScreen.kt**
   - Replaced emoji references with text descriptions
   - Changed "⚡" to "Lightning icon"
   - Changed "⏱" to "Timer icon"

5. **MenuScreens.kt**
   - Replaced emoji icons (👑, ⚔️, 💀) with text labels
   - "Crown", "Sword", "Defeat"

6. **WorldMapScreen.kt**
   - Replaced status emojis (🔒, ⚔️, ✓) with text
   - "Locked", "Available", "Completed"

## Visual Design

### LightningIcon
```
    *
   ***
  * * *
   ***
  * *
 *   *
*     *
```
- Filled yellow path with white outline
- Recognizable lightning bolt shape

### TimerIcon
```
   ___
  /   \
 |  |  |
 | _|  |
  \___/
```
- Clock circle outline
- Hour hand pointing up (12 o'clock position)
- Minute hand pointing right (3 o'clock position)
- Center dot

### SwordIcon
```
      /
     /
    /  (blade)
   /___
  |____|  (crossguard)
   |__|   (handle)
    ()    (pommel)
```
- Silver blade
- Gold crossguard
- Brown handle
- Gold pommel

## Benefits

1. **Cross-platform compatibility** - Icons render consistently on all platforms
2. **No font dependencies** - Don't rely on system emoji fonts
3. **Customizable** - Can easily adjust colors and sizes
4. **Scalable** - Vector-based drawing scales to any size
5. **Performance** - Lightweight Canvas drawing

## Testing

- ✅ Desktop build compiles successfully
- ✅ WasmJS build compiles successfully
- ✅ All existing tests pass
- ✅ Icons are used consistently throughout the UI

## Future Improvements

Potential enhancements:
- Add more icon animations (pulse effect for lightning, rotating for timer)
- Create a comprehensive icon library for other game elements
- Add icon themes (different color schemes)
- Implement icon caching for better performance
