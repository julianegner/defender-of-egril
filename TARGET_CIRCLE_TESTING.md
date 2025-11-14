# Target Circle Enhancement Testing Guide

## Overview
This document describes how to manually test the enhanced target circle rendering system.

## What Changed
- **Old System**: Target circles were drawn as a single overlay at the GameGrid level with complex positioning calculations
- **New System**: Each tile draws its own circle segments directly on itself

## Features to Test

### 1. Central Target Tile (All Attack Types)
When selecting an enemy to attack, the target tile should show:
- 1 filled circle (small, center)
- 2 stroke circles (medium and larger)
- Color depends on attack type:
  - **MELEE/RANGED**: Dark Gray
  - **AREA (Fireball)**: Deep Orange/Red (#FF5722)
  - **LASTING (Acid)**: Green (#4CAF50)

### 2. Neighbor Tiles (AREA and LASTING Only)
For AREA and LASTING attack types, neighbor PATH tiles should show:
- 3 concentric arc segments (parts of larger circles)
- Only tiles on the PATH show these arcs
- Arcs should align to form complete rings when viewed together
- Same color as central tile

### 3. Single-Target Attacks (MELEE/RANGED)
- Only the 3 inner circles on the target tile
- No outer ring segments on neighbors

## Test Scenarios

### Scenario 1: Melee Tower (Pike/Spike Tower)
1. Start game or level
2. Build a Spike Tower (MELEE attack)
3. Wait for enemies to approach
4. Select the tower
5. Click on an enemy in range
6. **Expected**: Only 3 dark gray circles on the enemy tile

### Scenario 2: Ranged Tower (Bow, Spear, Ballista)
1. Build a ranged tower (Bow, Spear, or Ballista)
2. Select the tower
3. Click on an enemy in range
4. **Expected**: Only 3 dark gray circles on the enemy tile

### Scenario 3: Area Attack (Wizard Tower - Fireball)
1. Build a Wizard Tower (AREA attack)
2. Wait for enemies
3. Select the tower
4. Click on an enemy on the PATH
5. **Expected**:
   - 3 red/orange circles on the target enemy tile
   - Arc segments on surrounding PATH tiles (6 neighbors max)
   - Arc segments should be parts of 3 larger circles
   - Non-PATH neighbors should NOT show arcs

### Scenario 4: Lasting Attack (Alchemy Tower - Acid)
1. Build an Alchemy Tower (LASTING attack)
2. Wait for enemies
3. Select the tower
4. Click on an enemy on the PATH
5. **Expected**:
   - 3 green circles on the target enemy tile
   - Arc segments on surrounding PATH tiles
   - Arc segments form parts of 3 larger circles
   - Non-PATH neighbors should NOT show arcs

### Scenario 5: Pan and Zoom
1. Select any tower with an enemy
2. Zoom in and out using mouse wheel
3. Pan around using drag
4. **Expected**: Circles stay correctly positioned on their tiles

### Scenario 6: Multiple Platforms
Test on:
- Desktop (Linux, Windows, macOS)
- Web/WASM
- Android
- iOS (if available)

## Visual Verification Points

### Inner Circles (All Attack Types)
- ✓ Centered on target tile
- ✓ Radii: 6px (filled), 14px (stroke), 22px (stroke)
- ✓ Stroke width: 3px
- ✓ Visible and properly colored

### Outer Ring Segments (AREA/LASTING Only)
- ✓ Arc segments appear on neighbor PATH tiles
- ✓ Arcs are parts of circles centered on target tile
- ✓ Radii: 80px, 110px, 140px
- ✓ Stroke width: 3px
- ✓ Arcs align to form complete rings when viewed together
- ✓ No arcs on non-PATH neighbors

### Positioning
- ✓ No platform-specific positioning issues
- ✓ Works correctly on mobile and desktop
- ✓ Circles stay with tiles during pan/zoom

## Known Improvements
- Removed complex platform-specific positioning logic
- Removed overlay layer (cleaner architecture)
- Centralized circle size constants
- Uses same neighbor detection logic as fireball/acid effects
