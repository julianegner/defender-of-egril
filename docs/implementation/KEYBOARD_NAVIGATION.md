# Keyboard Navigation Feature

## Overview
The game now supports keyboard navigation for panning the hexagonal map view in both gameplay and map editor screens.

## Controls

### Gameplay Screen
- **Arrow Keys** (↑ ↓ ← →): Pan the map in the corresponding direction
- **WASD Keys**: Alternative pan controls (W=up, S=down, A=left, D=right)
- **Mouse Wheel**: Zoom in/out (existing feature)
- **Mouse Drag**: Pan the map (existing feature)
- **Pinch Gesture**: Zoom on mobile devices (existing feature)

### Map Editor Screen
- **Mouse Wheel**: Zoom in/out
- **Zoom Buttons**: Use the +/- buttons in the header to zoom
- **Mouse Drag**: Pan the map
- **Keyboard Navigation**: Disabled to prevent interference with brush painting

## Implementation Details

### Universal HexagonalMapView Component
A new universal component (`HexagonalMapView.kt`) has been created that:
- Provides a reusable hexagonal map view with pan and zoom capabilities
- Supports configurable keyboard navigation via `HexagonalMapConfig.enableKeyboardNavigation`
- Maintains all existing mouse wheel zoom and touch gesture support
- Uses focus management to capture keyboard events only when navigation is enabled

### Component Usage

#### GameGrid (Gameplay)
```kotlin
HexagonalMapView(
    gridWidth = gameState.level.gridWidth,
    gridHeight = gameState.level.gridHeight,
    config = HexagonalMapConfig(
        hexSize = 40f,
        enableKeyboardNavigation = true  // Enabled for gameplay
    ),
    scale = scale,
    offsetX = offsetX,
    offsetY = offsetY,
    onScaleChange = { newScale -> scale = newScale },
    onOffsetChange = { newOffsetX, newOffsetY -> ... }
) { hexWidth, hexHeight, verticalSpacing ->
    // Render hexagonal grid content
}
```

#### MapEditorView (Editor)
```kotlin
HexagonalMapView(
    gridWidth = map.width,
    gridHeight = map.height,
    config = HexagonalMapConfig(
        hexSize = 32f,
        enableKeyboardNavigation = false  // Disabled for editor
    ),
    scale = zoomLevel,
    offsetX = offsetX,
    offsetY = offsetY,
    onScaleChange = { newScale -> zoomLevel = newScale },
    onOffsetChange = { newX, newY -> ... }
) { hexWidthParam, hexHeightParam, verticalSpacing ->
    // Render hexagonal grid content
}
```

### Configuration Parameters

**HexagonalMapConfig**:
- `hexSize`: Radius of hexagon in pixels (default: 40f)
- `enableKeyboardNavigation`: Enable/disable keyboard navigation (default: true)
- `keyboardPanSpeed`: Pixels to pan per key press (default: 30f)
- `minScale`: Minimum zoom level (default: 0.5f)
- `maxScale`: Maximum zoom level (default: 3.0f)
- `zoomDelta`: Amount to zoom per button press (default: 0.1f)

## Benefits

1. **Accessibility**: Keyboard navigation provides an alternative control method for users who prefer or require keyboard-only interaction
2. **Precision**: Arrow keys allow for precise, incremental panning of the map
3. **Code Reusability**: The universal component reduces code duplication between gameplay and editor screens
4. **Maintainability**: Changes to pan/zoom behavior can be made in one place
5. **Configurability**: Each screen can customize the navigation behavior to suit its needs

## Testing

### Manual Testing
1. **Gameplay Navigation**: 
   - Start a game level
   - Press arrow keys or WASD to pan the map
   - Verify smooth panning in all directions
   - Test that pan constraints work (can't pan beyond map bounds)

2. **Map Editor**:
   - Open the map editor
   - Verify keyboard navigation is disabled (arrow keys don't move the map)
   - Test zoom buttons in the header work correctly
   - Test mouse wheel zoom still works
   - Test brush painting still works when dragging

3. **Combined Controls**:
   - Test mixing keyboard navigation with mouse wheel zoom
   - Test mixing keyboard navigation with mouse drag
   - Verify all controls work together smoothly

### Automated Testing
The existing unit tests continue to pass, ensuring no regression in core game logic.

## Future Enhancements

Possible improvements for future versions:
- Add keyboard shortcuts for zoom (e.g., +/- keys, Ctrl+scroll)
- Add keyboard shortcuts for other game actions
- Make pan speed configurable through game settings
- Add smooth keyboard panning animation
