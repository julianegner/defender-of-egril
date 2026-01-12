# Smooth Tile Transitions Implementation

## Overview

The smooth tile transitions feature adds visual blending between adjacent hexagonal tiles of different types, creating a more natural and polished appearance for the game map.

## Problem

Previously, tiles were rendered as images cropped into hexagonal shapes using `HexagonShape()`. This created hard, sharp edges where tiles of different types met, making the transitions between terrain types visually jarring.

## Solution

### Approach

The implementation uses a layered rendering approach with alpha gradients:

1. **Base Layer**: The main tile image is drawn, clipped to the hexagon shape
2. **Neighbor Layers**: For each of the 6 hexagonal neighbors:
   - If the neighbor is a different tile type that should blend
   - Draw the neighbor's tile image over the main tile
   - Apply a radial alpha gradient mask centered on the shared edge
   - Use `BlendMode.DstIn` to create transparency gradient

### Architecture

#### New Components

1. **`BlendedTileCell.kt`** - New composable that handles blended tile rendering
   - `BlendedTileCell`: Main composable that wraps tile rendering
   - `BlendedTileBackground`: Renders the tile with neighbor blending
   - `drawNeighborBlend`: Draws a single neighbor with gradient mask
   - `shouldBlendTileType`: Determines which tile types support blending
   - `shouldBlendWithNeighbor`: Determines if two tile types should blend

2. **Modified `GameMap.kt`**
   - `GridCell`: Enhanced to use `BlendedTileCell` when tile images are enabled
   - `GridCellContent`: Extracted content rendering for reuse
   - Precomputes neighbor tile types and painters for performance

### Tile Types Supporting Blending

According to requirements, blending is enabled for:
- `PATH` - Enemy pathways
- `BUILD_AREA` - Tower placement zones adjacent to paths
- `ISLAND` - Build islands (2x2 tiles)
- `NO_PLAY` - Non-playable background areas
- `RIVER` - River tiles (special case: can show riverbank at edges)

Tiles that don't blend:
- `SPAWN_POINT` - Enemy spawn locations
- `TARGET` - Target destination

### Special Cases

#### River Tiles
River tiles have special handling:
- Can blend with non-river tiles to show riverbank transitions
- Adjacent river tiles of the same type don't blend (no transition needed)

#### Tower Coverage
When a tower is placed on a `BUILD_AREA` or `ISLAND` tile and is ready (fully constructed):
- The tile background is hidden to make the tower more visible
- Neighbor tiles don't blend with this position

### Algorithm Details

#### Edge Blending Parameters

```kotlin
val edgeDistance = hexSizePx * 0.75f   // Distance from center to edge
val blendWidth = hexSizePx * 0.4f      // Width of blend zone
val edgeAlpha = 0.3f                   // Maximum alpha at edge center
```

#### Gradient Calculation

For each neighbor:
1. Calculate the angle from current tile center to neighbor center
2. Compute edge center position along that angle
3. Create radial gradient:
   - Center: Edge center point
   - Radius: Blend width
   - Colors: Semi-transparent white at center, fully transparent at edges

#### Drawing Process

```kotlin
// 1. Clip to hexagon shape
clipPath(hexPath) {
    // 2. Draw main tile
    draw(mainTileImage)
    
    // 3. For each neighbor
    for (neighbor in neighbors) {
        if (shouldBlend(neighbor)) {
            // Draw neighbor with gradient mask
            drawNeighborBlend(neighborPainter)
        }
    }
}
```

### Performance Considerations

1. **Neighbor Precomputation**: Tile types for neighbors are computed once using `remember()`
2. **Conditional Rendering**: Blending only occurs when:
   - Tile images are enabled in settings
   - Current tile type supports blending
   - Neighbor exists and supports blending
   - Tile types are different
3. **Canvas Optimization**: All drawing is done in a single Canvas pass

### Backward Compatibility

The feature is fully backward compatible:
- When tile images are disabled in settings, uses original `BaseGridCell`
- Click/tap detection unchanged (uses same hexagon shape)
- No impact on game logic or mechanics

## Usage

### Enabling/Disabling

Tile blending automatically activates when:
1. Tile images are enabled in settings (`AppSettings.useTileImages.value == true`)
2. A tile painter is available for the current tile type

### Adjusting Blend Strength

To modify blend appearance, edit constants in `BlendedTileCell.kt`:

```kotlin
// In drawNeighborBlend()
val edgeDistance = hexSizePx * 0.75f   // Higher = blend closer to edge
val blendWidth = hexSizePx * 0.4f      // Higher = wider blend zone
Color.White.copy(alpha = 0.3f)         // Higher alpha = stronger blend
```

## Future Enhancements

Potential improvements:
1. **Platform-Specific Blending**: Use native layer APIs for better performance
2. **Dynamic Blend Strength**: Make blend parameters configurable in settings
3. **Additional Blend Modes**: Experiment with different blend modes for various effects
4. **Directional Gradients**: Use linear gradients perpendicular to edges for different visual styles

## Testing

### Visual Testing
- Load a level with mixed tile types
- Verify smooth transitions at tile boundaries
- Test with different zoom levels
- Check all tile type combinations

### Functional Testing
- Verify click/tap detection works correctly on all tiles
- Confirm tower placement and selection unchanged
- Test with tile images enabled and disabled

### Performance Testing
- Monitor frame rate during gameplay
- Check for any lag during pan/zoom operations
- Verify no memory leaks from painter caching

## Files Modified

- `composeApp/src/commonMain/kotlin/de/egril/defender/ui/BlendedTileCell.kt` (new)
- `composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/GameMap.kt` (modified)

## References

- Hexagonal Grid Guide: https://www.redblobgames.com/grids/hexagons/
- Compose Canvas API: https://developer.android.com/jetpack/compose/graphics/draw/overview
- Blend Modes: https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/BlendMode
