# Map Editor Zoom Fix - Visual Guide

## Overview

This guide provides a visual explanation of the map editor zoom fix that enables zoom functionality while maintaining accurate brush painting.

## The Problem

### Before the Fix

```
Map Editor (Zoom Disabled)
┌─────────────────────────────────────┐
│  Zoom: 1.0x only ❌                 │
│  [+] [-] (buttons disabled)         │
│                                     │
│  🖱️ Brush painting works           │
│  🔍 Zoom breaks brush painting      │
│  ⚠️  enableZoomMode = false         │
└─────────────────────────────────────┘
```

**User Experience Issues:**
- Could not zoom in to see tile details
- Could not zoom out to see whole map
- Had to work at 1.0x zoom always
- Inefficient for large maps

### Why It Was Broken

The coordinate conversion didn't account for zoom when centering:

```kotlin
// OLD CODE (WRONG!)
val adjustedX = pointerPos.x - (containerSize.width - actualContentSize.width) / 2f
                                                        ^^^^^^^^^^^^^^^^^^^^^^
                                                        Uses UNSCALED size!
```

**Visual Example at Zoom 2.0x:**
```
Container: 800px wide
Content (unscaled): 400px wide
Content (visual at 2.0x): 800px wide

Old calculation:
  centeringOffset = (800 - 400) / 2 = 200px  ❌ WRONG!
  
  The content actually fills the container (800px),
  so centering offset should be 0, not 200!
  
Result:
  User clicks at x=300
  System thinks: "That's 300 - 200 = 100px into content"
  But actually: "That's 300 - 0 = 300px into content"
  
  Brush paints WRONG TILE!
```

## The Solution

### After the Fix

```
Map Editor (Zoom Enabled)
┌─────────────────────────────────────┐
│  Zoom: 0.5x to 3.0x ✅              │
│  [+] [-] (buttons work!)            │
│                                     │
│  🖱️ Brush painting works            │
│  🔍 Zoom works with brush painting  │
│  ✅ enableZoomMode = true           │
└─────────────────────────────────────┘
```

**User Experience Improvements:**
- ✅ Zoom in to work on fine details
- ✅ Zoom out to see entire map layout
- ✅ Brush painting accurate at all zoom levels
- ✅ Efficient workflow for any map size

### How It Was Fixed

The coordinate conversion now accounts for zoom when centering:

```kotlin
// NEW CODE (CORRECT!)
val scaledWidth = actualContentSize.width * zoomLevel
val scaledHeight = actualContentSize.height * zoomLevel
val adjustedX = pointerPos.x - (containerSize.width - scaledWidth) / 2f
                                                      ^^^^^^^^^^^
                                                      Uses SCALED size!
```

**Visual Example at Zoom 2.0x:**
```
Container: 800px wide
Content (unscaled): 400px wide
Content (visual at 2.0x): 800px wide

New calculation:
  scaledWidth = 400 * 2.0 = 800px
  centeringOffset = (800 - 800) / 2 = 0px  ✅ CORRECT!
  
  The content fills the container, so no centering needed.
  
Result:
  User clicks at x=300
  System thinks: "That's 300 - 0 = 300px into content"
  Actually is: "That's 300 - 0 = 300px into content"
  
  Brush paints CORRECT TILE!
```

## Visual Comparison: Different Zoom Levels

### Zoom 0.5x (Zoomed Out)

```
┌─────────────────────────────────────────────┐ Container (800px)
│                                             │
│              [  Map  ]                      │ Content visual size: 200px
│             Centered at                     │ Centered at x=300
│             x=300 to x=500                  │
│                                             │
└─────────────────────────────────────────────┘

Centering offset = (800 - 200) / 2 = 300px ✅
```

### Zoom 1.0x (Normal)

```
┌─────────────────────────────────────────────┐ Container (800px)
│                                             │
│         [      Map      ]                   │ Content visual size: 400px
│        Centered at                          │ Centered at x=200
│        x=200 to x=600                       │
│                                             │
└─────────────────────────────────────────────┘

Centering offset = (800 - 400) / 2 = 200px ✅
```

### Zoom 2.0x (Zoomed In)

```
┌─────────────────────────────────────────────┐ Container (800px)
│[          Zoomed  Map          ]            │ Content visual size: 800px
│                                             │ Fills container
│          No centering needed                │ No offset
│                                             │
└─────────────────────────────────────────────┘

Centering offset = (800 - 800) / 2 = 0px ✅
```

### Zoom 3.0x (Max Zoom)

```
┌─────────────────────────────────────────────┐ Container (800px)
 [       Large Map Portion      ]               Content visual size: 1200px
         Exceeds container                      Content larger than container
         Pan to see other parts                 Negative centering offset
                                                 (handled by pan constraints)

Centering offset = (800 - 1200) / 2 = -200px ✅
```

## Coordinate Transformation Flow

```
┌─────────────────────────────────────────────────────┐
│  User clicks/drags on map                           │
│  ↓                                                   │
│  Raw pointer position (screen coordinates)          │
│  Example: pointerPos = (350, 250)                   │
└─────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────┐
│  STEP 1: Adjust for centering (NEW FIX HERE!)       │
│  ↓                                                   │
│  scaledWidth = actualContentSize.width * zoomLevel  │
│  scaledHeight = actualContentSize.height * zoomLevel│
│  adjustedX = pointerPos.x - (containerWidth -       │
│              scaledWidth) / 2                       │
│  ↓                                                   │
│  Example at zoom 2.0x:                              │
│    scaledWidth = 400 * 2.0 = 800                    │
│    adjustedX = 350 - (800 - 800) / 2 = 350          │
└─────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────┐
│  STEP 2: Convert to content coordinates             │
│  (done in screenToHexGridPosition)                  │
│  ↓                                                   │
│  contentX = (adjustedX - offsetX) / zoomLevel       │
│  contentY = (adjustedY - offsetY) / zoomLevel       │
│  ↓                                                   │
│  Example:                                           │
│    contentX = (350 - 0) / 2.0 = 175                 │
└─────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────┐
│  STEP 3: Convert to hex grid position               │
│  (using hex geometry formulas)                      │
│  ↓                                                   │
│  Position(x, y) calculated based on:                │
│  - Hex width and height                             │
│  - Row and column spacing                           │
│  - Odd/even row offset                              │
│  ↓                                                   │
│  Result: Position(3, 2)  ← Paint this tile!         │
└─────────────────────────────────────────────────────┘
```

## Code Changes Summary

### MapEditorView.kt

**Change 1: Enable zoom**
```kotlin
// Before:
enableZoomMode = false // fixme: zoom deactivated because it breaks the brush painting

// After:
enableZoomMode = true  // Zoom now works with brush painting
```

**Change 2: Add zoomLevel dependency**
```kotlin
// Before:
.pointerInput(containerSize, actualContentSize) {

// After:
.pointerInput(containerSize, actualContentSize, zoomLevel) {
```

**Change 3: Fix centering calculation**
```kotlin
// Before:
val adjustedX = pointerPos.x - (containerSize.width - actualContentSize.width) / 2f
val adjustedY = pointerPos.y - (containerSize.height - actualContentSize.height) / 2f

// After:
val scaledWidth = actualContentSize.width * zoomLevel
val scaledHeight = actualContentSize.height * zoomLevel
val adjustedX = pointerPos.x - (containerSize.width - scaledWidth) / 2f
val adjustedY = pointerPos.y - (containerSize.height - scaledHeight) / 2f
```

## Test Coverage

New tests added to verify the fix:

### Test 1: Various Zoom Levels
```kotlin
testScreenToHexGridPosition_variousZoomLevels()
```
- Tests at zoom: 0.5x, 1.5x, 3.0x
- Verifies Position(0, 0) maps correctly at each zoom level
- ✅ All tests pass

### Test 2: Zoom with Centering
```kotlin
testScreenToHexGridPosition_withZoomAndCentering()
```
- Simulates realistic container/content sizes
- Tests complete transformation pipeline
- ✅ All tests pass

## Usage Instructions

### For Users

1. **Open the map editor**
   - Click "Editor" from world map
   - Go to "Map Editor" tab

2. **Use zoom controls**
   - Mouse wheel + Ctrl to zoom in/out
   - Or use [+] and [-] buttons in header
   - Zoom range: 0.5x to 3.0x

3. **Paint tiles with brush**
   - Select a tile type from the palette
   - Click and drag to paint tiles
   - Works accurately at any zoom level!

4. **Navigate large maps**
   - Zoom out to see the whole map
   - Zoom in to work on details
   - Use arrow keys or WASD to pan

### Best Practices

- **Zoom in** (2x-3x) when placing spawn points, waypoints, or small details
- **Zoom out** (0.5x-1x) when designing overall map layout
- **Use keyboard pan** (arrow keys/WASD) to move around while zoomed in
- **Minimap** shows your current viewport when zoomed in

## Conclusion

The map editor zoom feature is now fully functional with accurate brush painting at all zoom levels. The fix is minimal, well-tested, and mathematically sound. Users can now enjoy a much more efficient map editing experience!

## Related Files

- `MapEditorView.kt` - Main implementation
- `HexUtils.kt` - Coordinate conversion function
- `HexUtilsTest.kt` - Test coverage
- `MAP_EDITOR_ZOOM_FIX.md` - Detailed technical documentation
