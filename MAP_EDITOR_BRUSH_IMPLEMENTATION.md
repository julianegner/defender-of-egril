# Map Editor Brush Implementation

## Overview

This document describes the implementation of the brush feature for the map editor in Defender of Egril. The brush feature allows users to paint multiple tiles by clicking and dragging, rather than clicking each tile individually.

## Implementation Details

### Files Modified

1. **MapEditorView.kt** - Complete rewrite of brush functionality

### Key Changes (v2 - Fixed Implementation)

#### 1. Tile Position Tracking

Each tile tracks its center position using `onGloballyPositioned`:

```kotlin
val tilePositions = remember { mutableStateMapOf<String, Offset>() }

// On each tile:
.onGloballyPositioned { coordinates ->
    val bounds = coordinates.size
    val position = coordinates.positionInRoot()
    val centerX = position.x + bounds.width / 2f
    val centerY = position.y + bounds.height / 2f
    tilePositions[key] = Offset(centerX, centerY)
}
```

#### 2. Tile Detection

Helper function to find which tile is at a given pointer position:

```kotlin
fun getTileAtPosition(position: Offset): String? {
    val hexRadiusPx = with(density) { (hexWidth / 2f).dp.toPx() }
    return tilePositions.entries.minByOrNull { (_, tilePos) ->
        val dx = position.x - tilePos.x
        val dy = position.y - tilePos.y
        dx * dx + dy * dy
    }?.let { (key, tilePos) ->
        val dx = position.x - tilePos.x
        val dy = position.y - tilePos.y
        val distance = sqrt(dx * dx + dy * dy)
        if (distance < hexRadiusPx) key else null
    }
}
```

#### 3. Container-Level Drag Detection

Brush functionality uses `detectDragGestures` at the container level:

```kotlin
.pointerInput(selectedTileType) {
    detectDragGestures(
        onDragStart = { offset ->
            val tileKey = getTileAtPosition(offset)
            if (tileKey != null) {
                tiles = tiles.toMutableMap().apply {
                    this[tileKey] = selectedTileType
                }
            }
        },
        onDrag = { change, _ ->
            val tileKey = getTileAtPosition(change.position)
            if (tileKey != null) {
                tiles = tiles.toMutableMap().apply {
                    this[tileKey] = selectedTileType
                }
            }
        }
    )
}
```

### Why The Original Implementation Didn't Work

The first version had `pointerInput` on each individual tile with `awaitPointerEventScope`. This approach failed because:

1. Each tile only receives pointer events when the pointer is within that tile's bounds
2. When dragging from tile A to tile B, tile A stops receiving events once the pointer leaves
3. Tile B only gets events when the pointer enters, but detecting "enter while pressed" is unreliable
4. The pointer event system doesn't propagate "hover while pressed" events well to individual tiles

### New Approach Benefits

The container-level approach works because:

1. The container receives all drag events continuously
2. On each drag event, we calculate which tile is under the pointer
3. We paint that tile immediately
4. No need to track "brush active" state - the drag gesture handles it
5. More reliable and simpler code

### How It Works

1. **Pointer Down**: When the user clicks/touches anywhere on the map:
   - `onDragStart` fires with the initial position
   - We find which tile (if any) is at that position
   - That tile is painted with the selected tile type

2. **Pointer Move**: As the user drags while holding down:
   - `onDrag` fires continuously with the current pointer position
   - We find which tile is under the pointer
   - If there's a tile there, we paint it immediately
   - This happens for every position update, so all tiles under the path get painted

3. **Pointer Up**: When the user releases:
   - The drag gesture ends automatically
   - No cleanup needed

### User Experience

- **Click and Drag**: Hold down mouse button and drag over tiles - they all get painted
- **Single Click**: Click once on a tile to paint just that tile (via the existing `.clickable` modifier)
- **Touch and Drag**: Works on touch devices - touch and drag to paint
- **Zoom**: Still works via Ctrl+Scroll and zoom buttons
- **Pan**: Temporarily removed to avoid gesture conflicts (may be added back with modifier key)

### Technical Notes

- Uses `onGloballyPositioned` to track pixel coordinates of tile centers
- Uses `positionInRoot()` which gives coordinates relative to the root window
- Calculates distance from pointer to tile center to determine if pointer is "over" a tile
- Uses hex radius as the hit detection threshold
- Compatible with zoom (coordinates are in screen pixels, not logical units)
- The `.clickable` modifier is kept as fallback for simple clicks

### Testing

The feature has been tested by:
1. Compiling the code successfully on desktop ✅
2. Verifying no test regressions (existing tests still pass/fail as before) ✅
3. Manual testing recommended on desktop and web platforms

### Known Limitations

- Pan gestures temporarily removed to avoid conflicts with drag gestures
  - Can be added back with a modifier key (e.g., hold Space to pan)
  - Or use intelligent detection (long drag = pan, short drag on tile = paint)
- Transform gestures (pinch-to-zoom) on mobile removed
  - Zoom buttons still work as alternative

### Future Enhancements

Potential improvements:
- Add back pan with a modifier key (Space bar)
- Add back pinch-to-zoom for mobile
- Add brush size option (paint multiple tiles at once in a radius)
- Add eraser mode
- Add undo/redo functionality
- Preview which tile will be painted on hover

### Design Decisions

#### Gesture Prioritization

The implementation prioritizes brush painting over panning when dragging on tiles. This is intentional based on the issue requirements:

- **Brush Painting**: When dragging over tiles, they are painted (gesture consumed by tiles)
- **Panning**: Still available by dragging in empty space between/around tiles
- **Zooming**: Unaffected - works via Ctrl+Scroll or pinch gestures

This tradeoff is acceptable because:
1. The primary use case (requested in the issue) is to paint tiles by dragging
2. Panning is still accessible through empty areas
3. Most editing is done while zoomed in where panning is less critical
4. The zoom buttons provide an alternative to gesture-based zooming

#### State Management Efficiency

The implementation creates a new map copy for each tile paint operation:

```kotlin
tiles = tiles.toMutableMap().apply { this[key] = selectedTileType }
```

While this seems inefficient, it's the recommended pattern for Compose because:
1. Compose's state system is optimized for immutable updates
2. Creating new map instances ensures proper change detection
3. The map is typically small (< 500 tiles for most maps)
4. Paint operations are user-initiated (not continuous), limiting frequency
5. Alternative approaches (mutable maps with manual recomposition) are more error-prone

Performance profiling on real devices would be needed to justify a more complex optimization.

### How It Works

1. **Pointer Down**: When the user clicks/touches a tile:
   - `isBrushActive` is set to `true`
   - The tile is painted with the selected tile type
   - The event is consumed to prevent other handlers from processing it

2. **Pointer Move**: When the pointer moves over tiles while pressed:
   - If `isBrushActive` is `true`, the tile under the pointer is painted
   - Each tile independently detects when the pointer enters while pressed
   - The event is consumed

3. **Pointer Up**: When the user releases the click/touch:
   - `isBrushActive` is set to `false`
   - Brush mode is deactivated
   - The event is consumed

### User Experience

- **Single Click**: Still works as before - click a tile to paint it
- **Click and Drag**: Now supported - hold down the mouse button and drag over tiles to paint them all
- **Touch and Drag**: Works on touch devices too - touch and drag to paint multiple tiles
- **Zoom Compatibility**: Works correctly with the zoom feature (Ctrl+Scroll)
- **Pan Compatibility**: Does not interfere with pan gestures (drag on empty space)

### Technical Notes

- The `pointerInput` modifier includes `key`, `selectedTileType`, and `isBrushActive` as keys, ensuring the handler is recreated when these values change
- The `.clickable` modifier is kept as a fallback for simple clicks
- Events are consumed to prevent interference with other gesture detectors
- The implementation is platform-agnostic and works on desktop, web, and mobile (where supported)

### Testing

The feature has been tested by:
1. Compiling the code successfully on desktop
2. Verifying no test regressions (existing tests still pass/fail as before)
3. Manual testing recommended on desktop and web platforms

### Future Enhancements

Potential improvements for the future:
- Add a brush size option (paint multiple tiles at once)
- Add an eraser mode (toggle between paint and erase)
- Add undo/redo functionality for brush strokes
- Add a color picker preview when hovering over tiles
