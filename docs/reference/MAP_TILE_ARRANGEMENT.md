# Map Tile Arrangement Documentation

This document describes the hexagonal tile arrangement system used in Defender of Egril maps, including all sizes, measurements, and tile types. This information is intended for external applications that need to generate map images matching the game's layout.

## Table of Contents

1. [Overview](#overview)
2. [Hexagon Geometry](#hexagon-geometry)
3. [Grid Layout System](#grid-layout-system)
4. [Tile Types](#tile-types)
5. [Map Structure](#map-structure)
6. [Calculating Image Dimensions](#calculating-image-dimensions)
7. [Example Calculations](#example-calculations)

## Overview

Defender of Egril uses a **pointy-top hexagonal grid** with an **odd-row offset** layout (also known as "odd-q vertical layout" in the Red Blob Games hexagon guide).

Key characteristics:
- Hexagons have points at top and bottom, flat sides on left and right
- Odd-numbered rows (y % 2 == 1) are offset to the right
- Coordinates use standard (x, y) positions starting at (0, 0)
- Maps are stored in JSON format with tile positions as "x,y" keys

## Hexagon Geometry

### Base Size

The fundamental measurement is the **hexagon radius** (distance from center to any corner):

```
hexSize = 40 (in display points/pixels at 1x scale)
```

### Derived Measurements

From the hexagon radius, all other dimensions are calculated:

```
sqrt(3) = 1.732050808...

hexWidth = hexSize × sqrt(3)
         = 40 × 1.732050808
         = 69.28203230 pixels (flat-to-flat width)

hexHeight = hexSize × 2
          = 40 × 2
          = 80 pixels (point-to-point height)
```

### Hexagon Shape

A pointy-top hexagon has 6 vertices positioned around the center:

```
       Top (0°)
         •
        / \
   NW  /   \  NE
  •   |     |   •
      |     |
  •   |     |   •
   SW  \   /  SE
        \ /
         •
    Bottom (180°)
```

Vertex positions (relative to center at 0,0):
- Top: (0, -hexSize)
- Top-Right: (hexSize × sqrt(3)/2, -hexSize/2)
- Bottom-Right: (hexSize × sqrt(3)/2, hexSize/2)
- Bottom: (0, hexSize)
- Bottom-Left: (-hexSize × sqrt(3)/2, hexSize/2)
- Top-Left: (-hexSize × sqrt(3)/2, -hexSize/2)

With hexSize = 40:
- Top: (0, -40)
- Top-Right: (34.64, -20)
- Bottom-Right: (34.64, 20)
- Bottom: (0, 40)
- Bottom-Left: (-34.64, 20)
- Top-Left: (-34.64, -20)

## Grid Layout System

### Spacing Constants

The hexagons overlap to create proper tessellation. The following constants from `HexagonalGridConstants.kt` control the spacing:

```kotlin
VERTICAL_SPACING_ADJUSTMENT = -7.0 pixels
HORIZONTAL_SPACING = -10.0 pixels
ODD_ROW_OFFSET_RATIO = 0.42
```

### Vertical Spacing

Hexagon rows overlap vertically. The vertical spacing between row centers is:

```
verticalSpacing = hexHeight × 0.75
                = 80 × 0.75
                = 60 pixels

actualVerticalSpacing = -hexHeight + verticalSpacing + VERTICAL_SPACING_ADJUSTMENT
                      = -80 + 60 + (-7)
                      = -27 pixels (negative = overlap)
```

This means each row starts 27 pixels ABOVE the bottom of the previous row, creating a 27-pixel overlap.

The center-to-center vertical distance between rows is:

```
rowCenterDistance = hexHeight + actualVerticalSpacing
                  = 80 + (-27)
                  = 53 pixels
```

However, due to the way the UI framework handles spacing, the effective vertical distance from the top of one row to the top of the next is:

```
effectiveRowSpacing = verticalSpacing
                    = 60 pixels
```

### Horizontal Spacing

Within a row, hexagons overlap horizontally:

```
horizontalSpacing = HORIZONTAL_SPACING
                  = -10 pixels
```

The center-to-center horizontal distance between adjacent hexagons in the same row is:

```
hexCenterDistance = hexWidth + horizontalSpacing
                  = 69.28203230 + (-10)
                  = 59.28203230 pixels
```

### Odd Row Offset

Odd-numbered rows (y = 1, 3, 5, ...) are shifted to the right to create the hexagonal pattern:

```
oddRowOffset = hexWidth × ODD_ROW_OFFSET_RATIO
             = 69.28203230 × 0.42
             = 29.09845357 pixels
```

## Tile Types

Maps use the `TileType` enum with the following values:

### TileType.PATH
- **Purpose**: Path where enemies walk
- **Gameplay**: Enemies traverse these tiles to reach the target
- **Visual**: Typically shown with a distinct path texture/color
- **Placement**: Forms continuous routes from spawn points to targets

### TileType.BUILD_AREA
- **Purpose**: Areas where towers can be built
- **Gameplay**: Players can place defensive towers on these tiles
- **Visual**: Usually shown as buildable terrain adjacent to paths
- **Placement**: Typically placed next to paths for strategic tower positioning

### TileType.ISLAND
- **Purpose**: Build islands - isolated buildable areas
- **Gameplay**: Special build areas that can be placed within or separate from the main path
- **Visual**: Often 2×2 or larger island formations
- **Placement**: Can be in the middle of paths or standalone areas
- **Note**: Islands are always buildable regardless of path adjacency

### TileType.NO_PLAY
- **Purpose**: Non-playable area (void/blocked)
- **Gameplay**: Not accessible to enemies or towers
- **Visual**: Typically shown as empty space, water, or impassable terrain
- **Placement**: Used to create interesting map shapes and boundaries

### TileType.SPAWN_POINT
- **Purpose**: Enemy spawn locations
- **Gameplay**: Enemies appear on these tiles at the start of their turn
- **Visual**: Marked with special spawn point indicators
- **Placement**: Usually at the left edge (x=0 or x=1) of the map
- **Note**: Spawn points are also walkable by enemies

### TileType.TARGET
- **Purpose**: Goal position for enemies
- **Gameplay**: When enemies reach this tile, the player loses health points
- **Visual**: Marked as the target/goal location
- **Placement**: Usually at the right edge (x=width-1) of the map
- **Note**: Targets are also walkable by enemies

### TileType.RIVER
- **Purpose**: River tiles with flow mechanics
- **Gameplay**: Movable with bridges (built by certain enemy types like Ork, Evil Wizard, Ewhad)
- **Visual**: Shown with flowing water and directional indicators
- **Additional Properties**:
  - `flowDirection`: One of NORTH_EAST, EAST, SOUTH_EAST, SOUTH_WEST, WEST, NORTH_WEST, NONE, MAELSTROM
  - `flowSpeed`: Integer 1 or 2 (determines number of flow arrows)
- **Placement**: Creates water obstacles that can be crossed with bridges
- **Note**: River tiles are stored separately in the `riverTiles` map with additional RiverTile data

## Map Structure

### JSON Format

Maps are stored as JSON files with the following structure:

```json
{
  "id": "map_example",
  "name": "Example Map",
  "nameKey": "map_example_name",
  "width": 30,
  "height": 20,
  "readyToUse": true,
  "tiles": {
    "x,y": "TILE_TYPE",
    "0,0": "SPAWN_POINT",
    "1,0": "PATH",
    "29,19": "TARGET"
  },
  "riverTiles": {
    "15,10": {"flowDirection": "EAST", "flowSpeed": 1}
  }
}
```

### Tile Position Format

Tiles are keyed by their grid coordinates in the format `"x,y"` where:
- `x` = column index (0 to width-1)
- `y` = row index (0 to height-1)

If a position is not in the `tiles` map, it defaults to `TileType.NO_PLAY`.

### River Tiles

River tiles require two entries:
1. In `tiles` map: `"x,y": "RIVER"`
2. In `riverTiles` map: `"x,y": {"flowDirection": "DIRECTION", "flowSpeed": 1 or 2}`

## Calculating Image Dimensions

To create a map image that encompasses all tiles with a rectangular border, you need to calculate the bounding box.

### Pixel Position Calculation

For any tile at grid position (x, y):

```javascript
// Check if row is odd
const isOddRow = (y % 2 === 1);

// Calculate the offset for odd rows
const rowOffset = isOddRow ? (hexWidth * ODD_ROW_OFFSET_RATIO) : 0;

// Calculate the center position of the hexagon
const centerX = (x * (hexWidth + HORIZONTAL_SPACING)) + rowOffset + (hexWidth / 2);
const centerY = (y * verticalSpacing) + (hexHeight / 2);

// The hexagon's bounding box
const left = centerX - (hexWidth / 2);
const right = centerX + (hexWidth / 2);
const top = centerY - (hexHeight / 2);
const bottom = centerY + (hexHeight / 2);
```

### Map Bounding Box

For a map with dimensions `width × height`:

```javascript
const hexSize = 40;
const sqrt3 = Math.sqrt(3);
const hexWidth = hexSize * sqrt3;  // 69.28203230
const hexHeight = hexSize * 2;     // 80
const verticalSpacing = hexHeight * 0.75;  // 60
const HORIZONTAL_SPACING = -10;
const ODD_ROW_OFFSET_RATIO = 0.42;

// Calculate maximum extent
const lastRow = height - 1;
const lastCol = width - 1;
const isLastRowOdd = (lastRow % 2 === 1);

// Rightmost hexagon position
const maxRowOffset = isLastRowOdd ? (hexWidth * ODD_ROW_OFFSET_RATIO) : 0;
const rightmostCenterX = (lastCol * (hexWidth + HORIZONTAL_SPACING)) + maxRowOffset + (hexWidth / 2);

// Bottommost hexagon position
const bottommostCenterY = (lastRow * verticalSpacing) + (hexHeight / 2);

// Bounding box of all hexagons
const minX = 0;
const maxX = rightmostCenterX + (hexWidth / 2);
const minY = 0;
const maxY = bottommostCenterY + (hexHeight / 2);

// Total map content size (before padding)
const contentWidth = maxX - minX;
const contentHeight = maxY - minY;
```

### Rectangular Image Dimensions

To create a rectangular image that fully contains all hexagons with some padding:

```javascript
const padding = 20;  // pixels of padding around the map

const imageWidth = Math.ceil(contentWidth + (padding * 2));
const imageHeight = Math.ceil(contentHeight + (padding * 2));
```

When rendering hexagons in the image, offset all positions by the padding:

```javascript
const renderX = centerX + padding;
const renderY = centerY + padding;
```

## Example Calculations

### Small Map (15 × 8)

Example: Tutorial Map

```
Map dimensions: 15 columns × 8 rows

Constants:
- hexSize = 40
- hexWidth = 69.28
- hexHeight = 80
- verticalSpacing = 60
- HORIZONTAL_SPACING = -10
- ODD_ROW_OFFSET_RATIO = 0.42

Last column: 14
Last row: 7 (odd row, so offset applies)

Rightmost hexagon:
- Row offset = 69.28203230 × 0.42 = 29.09845357
- Center X = (14 × 59.28203230) + 29.09845357 + 34.64101615 = 893.69791577
- Right edge = 893.69791577 + 34.64101615 = 928.33893192

Bottommost hexagon:
- Center Y = (7 × 60) + 40 = 460
- Bottom edge = 460 + 40 = 500

Content size: 928.34 × 500.00 pixels

With 20px padding:
Image size: 969 × 540 pixels
```

### Large Map (30 × 30)

Example: The Creek

```
Map dimensions: 30 columns × 30 rows

Last column: 29
Last row: 29 (odd row, so offset applies)

Rightmost hexagon:
- Row offset = 69.28203230 × 0.42 = 29.09845357
- Center X = (29 × 59.28203230) + 29.09845357 + 34.64101615 = 1782.91836642
- Right edge = 1782.91836642 + 34.64101615 = 1817.55938257

Bottommost hexagon:
- Center Y = (29 × 60) + 40 = 1780
- Bottom edge = 1780 + 40 = 1820

Content size: 1817.56 × 1820.00 pixels

With 20px padding:
Image size: 1858 × 1860 pixels
```

### Medium Map (40 × 40)

Example: Large battle map

```
Map dimensions: 40 columns × 40 rows

Last column: 39
Last row: 39 (odd row, so offset applies)

Rightmost hexagon:
- Row offset = 69.28203230 × 0.42 = 29.09845357
- Center X = (39 × 59.28203230) + 29.09845357 + 34.64101615 = 2375.13881707
- Right edge = 2375.13881707 + 34.64101615 = 2409.77983322

Bottommost hexagon:
- Center Y = (39 × 60) + 40 = 2380
- Bottom edge = 2380 + 40 = 2420

Content size: 2409.78 × 2420.00 pixels

With 20px padding:
Image size: 2450 × 2460 pixels
```

## Additional Notes

### Coordinate System

- Origin (0, 0) is at the top-left
- X increases to the right
- Y increases downward
- Row 0 is the topmost row (even row, no offset)
- Row 1 is the second row (odd row, with right offset)

### Hexagon Neighbors

Each hexagon has 6 neighbors. The neighbor positions depend on whether the current row is even or odd:

**Even rows (y % 2 == 0):**
- East: (x+1, y)
- North-East: (x, y-1)
- North-West: (x-1, y-1)
- West: (x-1, y)
- South-West: (x-1, y+1)
- South-East: (x, y+1)

**Odd rows (y % 2 == 1):**
- East: (x+1, y)
- North-East: (x+1, y-1)
- North-West: (x, y-1)
- West: (x-1, y)
- South-West: (x, y+1)
- South-East: (x+1, y+1)

### Map Validation

A valid "ready to use" map must:
1. Have at least one SPAWN_POINT tile
2. Have at least one TARGET tile
3. Have a continuous path from all spawn points to at least one target
4. Path continuity includes PATH, SPAWN_POINT, TARGET, and optionally RIVER tiles

### Visual Rendering Recommendations

When rendering map images:

1. **Layer Order** (bottom to top):
   - NO_PLAY tiles (background/void)
   - RIVER tiles with flow indicators
   - PATH tiles
   - BUILD_AREA and ISLAND tiles
   - SPAWN_POINT markers
   - TARGET markers

2. **Colors/Textures**:
   - Use tile-specific textures from `composeResources/files/tiles/{TileType}/`
   - Or use color codes similar to the game's rendering

3. **Borders**:
   - Each hexagon has a thin border to distinguish tile boundaries
   - Border width: typically 1-2 pixels at 1x scale

4. **Padding**:
   - Include at least 20 pixels of padding around the map
   - Ensures hexagons at the edges are fully visible
   - Creates a clean rectangular image

### Performance Considerations

For large maps (40×40 or bigger):
- Image dimensions can exceed 2400×2400 pixels
- Consider rendering at lower resolution and scaling up if needed
- Use efficient rendering techniques (canvas, WebGL, etc.)

## References

- Red Blob Games Hexagon Guide: https://www.redblobgames.com/grids/hexagons/
- Source code: `composeApp/src/commonMain/kotlin/de/egril/defender/ui/hexagon/`
- Hexagon constants: `HexagonalGridConstants.kt`
- Map models: `composeApp/src/commonMain/kotlin/de/egril/defender/editor/EditorModels.kt`
- Repository maps: `composeApp/src/commonMain/composeResources/files/repository/maps/`

---

**Document Version**: 1.0  
**Last Updated**: 2026-02-16  
**Game Version**: Defender of Egril (Kotlin Multiplatform)
