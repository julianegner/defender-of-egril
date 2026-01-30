# Tile Icon Size Adjustment

## Overview
Adjusted trap icon sizes on game tiles to match barricade icon size for visual consistency.

## Changes Made

### Icon Size Updates
- **Dwarven trap icon**: Increased from 20dp to 48dp
- **Magical trap icon**: Increased from 24dp to 48dp
- **Trap preview icon**: Increased from 24dp to 48dp
- **Barricade icon**: Unchanged at 48dp (reference size)

### Implementation Details

#### 1. GamePlayConstants.kt
Created new `TileIconSizes` object to centralize tile-based icon sizes:
```kotlin
object TileIconSizes {
    /** Icon size for traps on tiles (dwarven traps, magical traps) */
    val Trap = 48.dp
    
    /** Icon size for barricades on tiles */
    val Barricade = 48.dp
    
    /** Icon size for trap preview when hovering during placement mode */
    val TrapPreview = 48.dp
}
```

#### 2. GameMap.kt
Updated all trap and barricade icon references to use the new constants:
- Line 884: `PentagramIcon(size = GamePlayConstants.TileIconSizes.Trap)` (magical trap)
- Line 889: `TrapIcon(size = GamePlayConstants.TileIconSizes.Trap)` (dwarven trap)
- Line 909: `WoodIcon(size = GamePlayConstants.TileIconSizes.Barricade)` (barricade)
- Line 963: `WoodIcon(size = GamePlayConstants.TileIconSizes.Barricade)` (barricade preview)
- Line 1046: `TrapIcon(size = GamePlayConstants.TileIconSizes.TrapPreview)` (dwarven trap preview)
- Line 1050: `PentagramIcon(size = GamePlayConstants.TileIconSizes.TrapPreview)` (magical trap preview)

#### 3. Copilot Instructions
Updated `.github/copilot-instructions.md` with new section "Tile Icon Sizes" documenting:
- All tile-based icon sizes must be defined in `GamePlayConstants.TileIconSizes`
- Convention that trap and barricade icons should be the same size (48dp)
- Guidelines for adding new tile-based elements

## Visual Impact
- **Before**: Trap icons were noticeably smaller than barricade icons (20dp/24dp vs 48dp)
- **After**: Trap icons are now the same size as barricade icons (48dp), providing better visual consistency and making traps more visible on the game map

## Benefits
1. **Visual Consistency**: All tile-based obstacles (traps and barricades) now have uniform icon sizing
2. **Better Visibility**: Larger trap icons are easier to see during gameplay
3. **Maintainability**: Centralized constants make future icon size adjustments easier
4. **Convention Documentation**: Future tile-based elements will follow the established pattern

## Testing
- Code compiles successfully with no errors
- All icon references updated consistently throughout the codebase
- No breaking changes to existing functionality

## Related Files
- `composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/GamePlayConstants.kt`
- `composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/GameMap.kt`
- `.github/copilot-instructions.md`
