# Smooth Tile Transitions - Visual Guide

## Before and After

### Without Tile Blending (Original)
```
┌─────────┐ ┌─────────┐ ┌─────────┐
│  PATH   │ │  PATH   │ │  PATH   │
│  Tile   │ │  Tile   │ │  Tile   │
│ (Beige) │ │ (Beige) │ │ (Beige) │
└─────────┘ └─────────┘ └─────────┘
│         │ │         │ │         │  <-- Sharp edge boundary
┌─────────┐ ┌─────────┐ ┌─────────┐
│ BUILD   │ │ BUILD   │ │ BUILD   │
│  AREA   │ │  AREA   │ │  AREA   │
│ (Green) │ │ (Green) │ │ (Green) │
└─────────┘ └─────────┘ └─────────┘
```
**Issue**: Hard, jarring transitions between different terrain types

### With Tile Blending (New)
```
┌─────────┐ ┌─────────┐ ┌─────────┐
│  PATH   │ │  PATH   │ │  PATH   │
│  Tile   │ │  Tile   │ │  Tile   │
│ (Beige) │ │ (Beige) │ │ (Beige) │
└─────────┘ └─────────┘ └─────────┘
│  blend  │ │  blend  │ │  blend  │  <-- Smooth gradient transition
│  zone   │ │  zone   │ │  zone   │
┌─────────┐ ┌─────────┐ ┌─────────┐
│ BUILD   │ │ BUILD   │ │ BUILD   │
│  AREA   │ │  AREA   │ │  AREA   │
│ (Green) │ │ (Green) │ │ (Green) │
└─────────┘ └─────────┘ └─────────┘
```
**Benefit**: Natural, smooth transitions create a more polished look

## Hexagonal Tile Blending

### Single Tile View
```
       ╱ ╲
      ╱   ╲         Pointy-top hexagon
     ╱     ╲        6 neighbors (E, NE, NW, W, SW, SE)
    ╱       ╲       
   ╱  MAIN   ╲      Main tile image fills hexagon
  ╱   TILE    ╲     
 ╱             ╲    
╱_______________╲   
```

### With Neighbor Blending
```
       ╱ ╲
      ╱NE ╲        NE = North-East neighbor blend
     ╱▒▒▒▒▒╲       W  = West neighbor blend
  NW▒▒▒▒▒▒▒▒E      etc.
   ╱▒▒▒▒▒▒▒▒╲      
  ╱▒▒ MAIN ▒▒╲     ▒ = Blend zones (gradient alpha)
 ╱▒▒▒ TILE ▒▒▒╲    Center shows mainly the main tile
╱W▒▒▒▒▒▒▒▒▒▒▒▒E    Edges show blend of main + neighbors
▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒
 ╲▒▒▒▒▒▒▒▒▒▒▒╱     
  ╲SW▒▒▒▒▒SE╱      
   ╲▒▒▒▒▒▒▒╱       
    ╲__S__╱        S  = South blend (if 6 neighbors)
```

### Gradient Mask Details
```
Edge Center (neighbor direction)
      │
      ▼
    ╔═══╗           ░ = Fully transparent (0% alpha)
    ║░░░║           ▒ = Semi-transparent (30% alpha) 
    ║▒▒▒║           █ = Neighbor tile visible
    ║███║           
    ╚═══╝
      │
      └─→ Blend Width (40% of hex radius)
      
Radial gradient:
- Center: Edge midpoint
- Radius: 0.4 * hexSize
- At edge: 30% alpha (neighbor visible)
- Away from edge: 0% alpha (fully transparent)
```

## Tile Type Combinations

### Supported Blending
✓ PATH ↔ BUILD_AREA
✓ PATH ↔ NO_PLAY
✓ BUILD_AREA ↔ ISLAND
✓ BUILD_AREA ↔ NO_PLAY
✓ ISLAND ↔ NO_PLAY
✓ RIVER ↔ PATH (shows riverbank)
✓ RIVER ↔ BUILD_AREA (shows riverbank)
✓ RIVER ↔ NO_PLAY (shows riverbank)

### No Blending
✗ Same tile type (no transition needed)
✗ SPAWN_POINT (always distinct)
✗ TARGET (always distinct)
✗ Tiles with ready towers (tower visibility priority)

## Rendering Layers

### Layer Order (bottom to top)
1. **Background Color** - Solid color fallback
2. **Main Tile Image** - Primary texture
3. **Neighbor Blend 1** - First neighbor (e.g., East)
4. **Neighbor Blend 2** - Second neighbor (e.g., North-East)
5. **... up to 6 neighbors** - Remaining neighbors
6. **Game Entities** - Towers, enemies, effects
7. **UI Overlays** - Borders, selection, circles

### Clipping
All rendering is clipped to hexagon shape:
```
     ╱ ╲
    ╱   ╲         Hexagon Shape = Clipping Path
   ╱  A  ╲        Only pixels inside hexagon are visible
  ╱   L   ╲       Everything outside is clipped
 ╱    L    ╲      
╱___________╲     This ensures clean hexagonal tiles
```

## Visual Examples

### Example 1: Path with Build Area
```
┌──────────┬──────────┐
│   PATH   │   PATH   │  PATH tiles (beige stone texture)
└──────────┴──────────┘
 blend zone (gradient)   ← Smooth transition here
┌──────────┬──────────┐
│  BUILD   │  BUILD   │  BUILD_AREA tiles (green grass)
└──────────┴──────────┘
```

At the boundary:
- PATH tile shows 100% at its center
- BUILD tile shows 100% at its center  
- At edge: ~70% PATH, ~30% BUILD (gradient blend)

### Example 2: River with Riverbank
```
┌──────────┐
│  NO_PLAY │           NO_PLAY (gray background)
└──────────┘
 blend shows
 riverbank   ←          ← River shows grassy bank at edge
┌──────────┐
│  RIVER   │           RIVER (blue water texture)
└──────────┘
```

### Example 3: Island Surrounded by Background
```
        ┌──────────┐
        │ NO_PLAY  │       NO_PLAY surrounds island
        └──────────┘
   ┌──────────┬──────────┐
   │  ISLAND  │  ISLAND  │  ISLAND (2x2 build area)
   ├──────────┼──────────┤  All edges blend with NO_PLAY
   │  ISLAND  │  ISLAND  │
   └──────────┴──────────┘
        ┌──────────┐
        │ NO_PLAY  │
        └──────────┘
```

## Performance Characteristics

### Before Blending
- 1 draw call per tile
- Simple hexagon clip
- ~60 FPS typical

### After Blending  
- 1 + N draw calls per tile (N = different neighbors)
- Hexagon clip + gradient masks
- Still ~60 FPS (negligible impact)
- Memory: Slight increase from neighbor caching

### Optimizations
1. Precompute neighbor types (cached with `remember()`)
2. Skip blending for same-type neighbors
3. Skip blending when images disabled
4. Reuse gradient calculations

## User Experience

### Toggle Feature
Settings → Tile Images → ON/OFF
- ON: Blended transitions (new)
- OFF: Sharp transitions (original)

### Visual Quality
- Subtle effect (30% alpha at edges)
- Professional appearance
- Natural terrain flow
- No gameplay impact

### Click Detection
Unaffected - still uses hexagon shape for hit testing

## Technical Notes

### Blend Mode
Uses `BlendMode.DstIn`:
- Destination: Neighbor tile pixels
- Source: Gradient mask
- Result: Neighbor visible only where gradient is opaque

### Color Space
- RGB color blending
- Alpha compositing
- sRGB color space

### Cross-Platform
Works on all platforms:
- Desktop (JVM)
- Android
- iOS  
- Web (WASM)

Simplified algorithm ensures compatibility without platform-specific APIs.
