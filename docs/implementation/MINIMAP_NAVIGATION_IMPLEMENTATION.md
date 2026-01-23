# Minimap Navigation Implementation

## Overview

This implementation adds drag-to-navigate functionality to the minimap in Defender of Egril. Users can now click/tap and drag on the minimap to move the viewport, making it easier to navigate the game map when zoomed in.

## Changes Made

### 1. HexagonMinimap.kt

Added drag gesture detection to the minimap with the following changes:

#### New Import
- Added `import androidx.compose.foundation.gestures.detectDragGestures`
- Added `import androidx.compose.ui.input.pointer.pointerInput`

#### New Parameter
- Added `onViewportDrag: ((Float, Float) -> Unit)?` parameter to both `HexagonMinimap()` and `HexagonMinimapContent()` functions
- This callback is invoked when the user drags on the minimap, passing the new viewport offsets

#### Drag Gesture Implementation
Added a `pointerInput` modifier to the viewport indicator Box that:

1. **Detects drag gestures** on the minimap using `onDragStart` and `onDrag` callbacks
2. **Captures initial offset** at drag start to prevent jumping behavior
3. **Converts minimap coordinates to viewport offsets** using the following calculation:
   ```kotlin
   // On drag start: capture the current viewport offset
   onDragStart = {
       dragStartOffsetX = offsetX ?: 0f
       dragStartOffsetY = offsetY ?: 0f
   }
   
   // On drag: accumulate drag amount from start position
   onDrag = { _, dragAmount ->
       // Convert drag in minimap coordinates to viewport offsets
       val dragXFraction = dragAmount.x / (config.minimapSizeDp * (1f - viewportWidthRatio))
       val dragYFraction = dragAmount.y / (config.minimapSizeDp * (1f - viewportHeightRatio))
       
       // Convert drag fraction to normalized offset change (-1 to 1 range)
       val deltaNormalizedX = dragXFraction * 2f
       val deltaNormalizedY = dragYFraction * 2f
       
       // Convert normalized offset change to actual offset change
       val deltaOffsetX = -deltaNormalizedX * maxOffsetX
       val deltaOffsetY = -deltaNormalizedY * maxOffsetY
       
       // Apply incrementally from drag start position
       val newOffsetX = dragStartOffsetX + deltaOffsetX
       val newOffsetY = dragStartOffsetY + deltaOffsetY
       
       // Update drag start for next increment
       dragStartOffsetX = newOffsetX
       dragStartOffsetY = newOffsetY
   }
   ```

4. **Constrains the offsets** to valid range: `coerceIn(-maxOffsetX, maxOffsetX)`
5. **Calls the callback** with the new viewport position

### 2. GameMap.kt

Updated the `GameGrid` composable to handle minimap drag events:

#### Added Callback
```kotlin
HexagonMinimap(
    // ... existing parameters ...
    onViewportDrag = { newOffsetX, newOffsetY ->
        offsetX = newOffsetX
        offsetY = newOffsetY
    }
)
```

This immediately updates the viewport position when the user drags on the minimap.

### 3. MinimapNavigationTest.kt

Created comprehensive tests for the minimap navigation functionality:

1. **testMinimapDragToViewportOffsetConversion()**: Validates the coordinate conversion math
2. **testMinimapDragConstraints()**: Tests that offsets are properly constrained
3. **testMinimapDragWhenFullyZoomedOut()**: Verifies behavior when fully zoomed out
4. **testMinimapDragWithHighZoom()**: Tests behavior at 3x zoom level

All tests pass successfully.

## How It Works

### Coordinate System

The minimap uses a different coordinate system than the main viewport:
- **Minimap**: Fixed size (120dp × 120dp)
- **Viewport**: Variable size based on zoom level and container size

### Conversion Logic

1. **Viewport Ratio**: Calculate what fraction of the map is visible
   - When zoomed in: smaller ratio (e.g., 0.5 = 50% visible)
   - When zoomed out: larger ratio (up to 1.0 = 100% visible)

2. **Movable Area**: The viewport indicator can move within `(1.0 - viewportRatio)` of the minimap
   - Example: At 0.5 ratio, viewport can move within 50% of minimap size (60dp out of 120dp)

3. **Drag Conversion**: 
   - Drag amount in minimap pixels → fraction of movable area
   - Fraction → normalized offset change (-1 to 1)
   - Normalized change → actual viewport offset (pixels)

4. **Constraint**: Ensure offsets stay within valid range to prevent showing off-map areas

## Platform Support

This feature works on all platforms:
- ✅ **Desktop**: Click and drag with mouse
- ✅ **Web/WASM**: Click and drag with mouse
- ✅ **Mobile**: Tap and drag with finger

The gesture detection is handled by Compose Multiplatform's `detectDragGestures`, which automatically handles platform-specific input.

## User Experience

### Before
- Users could only navigate by dragging the main map
- Minimap was view-only, showing current position

### After
- Users can click/tap and drag on the minimap to navigate
- Dragging the yellow viewport indicator moves the view
- Provides quick, precise navigation to any part of the map
- Especially useful when highly zoomed in

## Testing

### Automated Tests
All unit tests pass, validating:
- Coordinate conversion math
- Constraint handling
- Behavior at different zoom levels

### Manual Testing Required
To test the feature:
1. Run the game (desktop or web)
2. Start any level
3. Zoom in on the map (mouse wheel or pinch gesture)
4. The minimap appears in the bottom-right corner
5. Click/tap and drag on the minimap
6. Observe the main map viewport moving accordingly

## Technical Details

### Performance
- Minimal performance impact
- Drag gestures are efficiently handled by Compose
- Calculations are simple floating-point operations
- No heavy rendering or complex logic

### Backwards Compatibility
- The `onViewportDrag` parameter is optional (nullable)
- Existing minimap usage without the callback continues to work
- No breaking changes to the public API

### Code Quality
- Clean separation of concerns
- Well-documented calculations
- Comprehensive test coverage
- Follows existing code patterns in the codebase
