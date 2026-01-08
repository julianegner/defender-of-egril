# Tower Placement Range Preview Feature

## Overview
This feature provides visual feedback when placing towers by showing the attack range preview when hovering the mouse over buildable tiles.

## Implementation Date
January 1, 2026

## Feature Description
When a player selects a tower type to build, hovering the mouse over an empty buildable tile (build area or build island) will display:

1. **Build Tile Highlight**: The hovered tile is highlighted with a light yellow background (40% alpha) to indicate where the tower will be placed.

2. **Range Preview**: All tiles within the tower's attack range are highlighted with a very light green background (20% alpha) to show which enemies can be attacked from that position.

3. **Dashed Borders**: Both the build tile and range tiles display dashed hexagonal borders (10px dash, 5px gap) to distinguish the preview from actual placed towers (which have solid borders).

## Visual Design

### Colors
- **Hovered Build Tile**: `GamePlayColors.Yellow.copy(alpha = 0.4f)` - Light yellow background
- **Range Preview Tiles**: `GamePlayColors.Success.copy(alpha = 0.2f)` - Very light green background
- **Build Tile Border**: Yellow dashed border (3dp width)
- **Range Tile Border**: Green dashed border (3dp width)

### Distinction from Existing Towers
The preview is visually distinct from placed towers in several ways:
- **Transparency**: Preview uses much lighter, more transparent colors (20-40% alpha) vs. solid colors for placed towers
- **Dashed Borders**: Preview uses dashed borders vs. solid borders for placed towers
- **Different Tile Colors**: Build tile is yellow (vs. blue for placed towers), range is very light green (vs. solid green for tower range)

## Technical Implementation

### Modified Files
1. **HexagonalMapView.kt**
   - Added `onHover` callback parameter to `BaseGridCell`
   - Implemented hover detection using `pointerInput` with `PointerEventType.Enter`/`Exit`

2. **GameMap.kt**
   - Added `hoveredPosition` state in `GameGrid` to track mouse hover
   - Added hover preview calculation in `GridCell`:
     - `showPlacementPreview`: Determines if the current tile should show as the build location
     - `isInPreviewRange`: Calculates which tiles are within attack range
   - Added Canvas-based dashed border rendering using `PathEffect.dashPathEffect`
   - Respects tower-specific rules (min/max range, area attack capabilities)

### Game Logic Integration
The preview respects all game rules:
- **Only Buildable Tiles**: Preview only shows on build areas and build islands
- **Empty Tiles Only**: No preview if a tower or enemy already exists at the position
- **Tower-Specific Ranges**: Uses the tower type's `baseRange` and `minRange` properties
- **Attack Type Support**:
  - Area attacks (AREA, LASTING): Show range on both path AND river tiles
  - Single-target attacks (MELEE, RANGED): Show range only on path tiles
- **Special Structures**: Towers with no attack (NONE) don't show range preview

## User Experience Benefits
1. **Better Planning**: Players can see the exact range before committing resources
2. **Strategic Decisions**: Easier to compare different tower placements
3. **Reduced Mistakes**: Visual feedback prevents accidental placements in suboptimal locations
4. **Learning Aid**: New players can understand tower ranges without trial and error

## Platform Support
- ✅ Desktop (JVM)
- ✅ Web (WASM)
- ✅ Android
- ✅ iOS

The feature uses standard Compose Multiplatform APIs (`pointerInput`, `Canvas`, `PathEffect`) that work across all supported platforms.

## Testing Notes
Due to network/Maven repository access limitations in the CI environment, this feature was tested through code inspection and compilation verification. Manual testing should verify:

1. Hover detection works smoothly without lag
2. Preview disappears when mouse moves away
3. Dashed borders render correctly on all platforms
4. Colors are visually distinct from placed towers
5. Range preview respects tower-specific rules (min/max range, area attacks)
6. No preview shows on non-buildable tiles

## Future Enhancements
Potential improvements for future iterations:
- Add range preview during initial building phase
- Show different colors for minimum range restrictions (e.g., Ballista's 3-tile minimum)
- Add keyboard navigation support for preview
- Include tooltip showing tower stats when hovering
