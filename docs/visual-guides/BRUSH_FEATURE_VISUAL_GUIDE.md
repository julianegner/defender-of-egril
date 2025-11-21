# Map Editor Brush Feature - Visual Guide

## How the Brush Works

### Before: Click-Only Mode
```
User clicks on Tile A
   ↓
Tile A changes color
   ↓
User must click Tile B separately
   ↓
Tile B changes color
```

**Problem**: Tedious for painting large areas

### After: Click-and-Drag Brush Mode
```
User clicks on Tile A (pointer down)
   ↓
isBrushActive = true
   ↓
Tile A changes color
   ↓
User drags to Tile B (pointer still down)
   ↓
Tile B detects pointer while pressed + isBrushActive
   ↓
Tile B changes color automatically
   ↓
User drags to Tile C (pointer still down)
   ↓
Tile C changes color automatically
   ↓
User releases pointer
   ↓
isBrushActive = false
```

**Result**: Paint multiple tiles in one stroke!

## Visual Example

### Map Editor Interface

```
┌─────────────────────────────────────────────────────┐
│ Editing: My Custom Map                              │
│                                                      │
│ Map Name: [My Custom Map____________]               │
│                                                      │
│ Select Tile Type:                                   │
│ [PATH] [BUILD_AREA] [ISLAND] [NO_PLAY] [SPAWN_POINT]│
│ [TARGET] [WAYPOINT]                                 │
│                                                      │
│ Click or drag to paint hexagons (30x8). Ctrl+Scroll │
│ to zoom: [-] 100% [+]                               │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│                    Map Grid                         │
│                                                      │
│    ⬡ ⬡ ⬡ ⬡ ⬡ ⬡ ⬡ ⬡                                │
│   ⬡ ⬡ ⬡ ⬡ ⬡ ⬡ ⬡ ⬡ ⬡                               │
│  ⬡ ⬡ ⬡ ⬡ ⬡ ⬡ ⬡ ⬡ ⬡                                │
│   ⬡ ⬡ ⬡ ⬡ ⬡ ⬡ ⬡ ⬡ ⬡                               │
│                                                      │
│  User clicks here and drags ──────────────→         │
│    (all hexagons along path get painted)            │
│                                                      │
└─────────────────────────────────────────────────────┘

[Save Map] [Save As New] [Cancel]
```

## Gesture Behavior

### Scenario 1: Painting Tiles (Desired Behavior)
```
1. User clicks on PATH tile
2. User drags over adjacent tiles
3. Result: All tiles under cursor are painted PATH
```

### Scenario 2: Panning the Map (Still Works)
```
1. User clicks on EMPTY SPACE (between tiles or on edges)
2. User drags
3. Result: Map pans (tiles don't consume the gesture)
```

### Scenario 3: Zooming (Still Works)
```
1. User holds Ctrl and scrolls mouse wheel
   OR
2. User pinches on touchscreen
3. Result: Map zooms in/out (handled by parent container)
```

## State Flow Diagram

```
┌──────────────────┐
│  Initial State   │
│ isBrushActive =  │
│      false       │
└────────┬─────────┘
         │
         │ User clicks tile
         ↓
┌──────────────────┐
│  Brush Active    │
│ isBrushActive =  │
│      true        │◄───────┐
└────────┬─────────┘        │
         │                  │
         │ User drags       │ Pointer moves
         │ over tiles       │ over new tile
         ↓                  │
┌──────────────────┐        │
│  Paint Tile(s)   ├────────┘
│                  │
└────────┬─────────┘
         │
         │ User releases
         │ pointer
         ↓
┌──────────────────┐
│  Brush Inactive  │
│ isBrushActive =  │
│      false       │
└──────────────────┘
```

## Technical Implementation Highlights

### Pointer Event Detection (Per Tile)
```kotlin
.pointerInput(key, selectedTileType, isBrushActive) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            event.changes.forEach { change ->
                when {
                    // Pointer pressed on this tile
                    change.pressed && !change.previousPressed -> {
                        isBrushActive = true
                        paintTile()
                    }
                    // Pointer released
                    !change.pressed && change.previousPressed -> {
                        isBrushActive = false
                    }
                    // Pointer moving over this tile while pressed
                    change.pressed && isBrushActive -> {
                        paintTile()
                    }
                }
            }
        }
    }
}
```

### Key Features
- ✅ Works on mouse (desktop)
- ✅ Works on touch (mobile/tablet)
- ✅ Compatible with existing zoom/pan
- ✅ Non-destructive (single clicks still work)
- ✅ Platform-agnostic (desktop, web, mobile)

## User Instructions

### To use the brush feature:

1. **Open the Level Editor**
   - From the world map, click the Level Editor button (🛠️)
   - Navigate to the "Map Editor" tab

2. **Select a map to edit**
   - Choose an existing map or create a new one

3. **Choose your tile type**
   - Click on one of the tile type buttons (PATH, ISLAND, etc.)

4. **Paint with the brush**
   - **Single tile**: Just click on a hexagon
   - **Multiple tiles**: Click and hold, then drag over hexagons
   - Release to stop painting

5. **Save your changes**
   - Click "Save Map" to update the existing map
   - Click "Save As New" to create a copy with a new name

### Tips:
- Zoom in for precision work (Ctrl+Scroll or zoom buttons)
- Pan the map by dragging empty space around the tiles
- Different colors represent different tile types
- Red hexagons = spawn points, Blue = targets, Brown = paths, etc.
