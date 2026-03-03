# Map Tile Arrangement - Quick Reference

**For:** External applications generating map images for Defender of Egril

**Main Documentation:** [MAP_TILE_ARRANGEMENT.md](./MAP_TILE_ARRANGEMENT.md)  
**Visual Examples:** [MAP_TILE_ARRANGEMENT_VISUAL_EXAMPLES.md](./MAP_TILE_ARRANGEMENT_VISUAL_EXAMPLES.md)

## Essential Constants

```javascript
const HEX_SIZE = 40;                    // Hexagon radius (center to corner)
const SQRT_3 = 1.732050808;
const HEX_WIDTH = 69.28203230;          // HEX_SIZE × SQRT_3
const HEX_HEIGHT = 80;                  // HEX_SIZE × 2
const VERTICAL_SPACING = 60;            // HEX_HEIGHT × 0.75
const HORIZONTAL_SPACING = -10;
const ODD_ROW_OFFSET_RATIO = 0.42;
const VERTICAL_SPACING_ADJ = -7;
const PADDING = 20;                     // Recommended image padding
```

## Quick Formulas

### Hexagon Center Position
```javascript
function getHexagonCenter(x, y) {
    const isOddRow = (y % 2 === 1);
    const rowOffset = isOddRow ? (HEX_WIDTH * ODD_ROW_OFFSET_RATIO) : 0;
    
    const centerX = (x * (HEX_WIDTH + HORIZONTAL_SPACING)) + rowOffset + (HEX_WIDTH / 2);
    const centerY = (y * VERTICAL_SPACING) + (HEX_HEIGHT / 2);
    
    return { x: centerX, y: centerY };
}
```

### Image Dimensions
```javascript
function getImageDimensions(mapWidth, mapHeight, padding = 20) {
    const lastCol = mapWidth - 1;
    const lastRow = mapHeight - 1;
    const isLastRowOdd = (lastRow % 2 === 1);
    
    // Rightmost edge
    const maxRowOffset = isLastRowOdd ? (HEX_WIDTH * ODD_ROW_OFFSET_RATIO) : 0;
    const rightEdge = (lastCol * (HEX_WIDTH + HORIZONTAL_SPACING)) 
                      + maxRowOffset + HEX_WIDTH;
    
    // Bottom edge
    const bottomEdge = (lastRow * VERTICAL_SPACING) + HEX_HEIGHT;
    
    return {
        width: Math.ceil(rightEdge + padding * 2),
        height: Math.ceil(bottomEdge + padding * 2)
    };
}
```

## Tile Types (TileType Enum)

| Type | Purpose | Walkable | Buildable |
|------|---------|----------|-----------|
| `PATH` | Enemy walking path | ✓ | ✗ |
| `BUILD_AREA` | Tower build zones | ✗ | ✓ |
| `NO_PLAY` | Empty/void/blocked | ✗ | ✗ |
| `SPAWN_POINT` | Enemy spawn | ✓ | ✗ |
| `TARGET` | Enemy goal | ✓ | ✗ |
| `RIVER` | Water with flow | ✗* | ✗ |

*River tiles require bridges (built by certain enemies)

## Map JSON Structure

```json
{
  "id": "map_id",
  "name": "Map Name",
  "width": 30,
  "height": 20,
  "tiles": {
    "x,y": "TILE_TYPE"
  },
  "riverTiles": {
    "x,y": {
      "flowDirection": "EAST|WEST|NORTH_EAST|...",
      "flowSpeed": 1
    }
  }
}
```

## Common Map Sizes

| Map Type | Dimensions | Image Size (with padding) |
|----------|------------|---------------------------|
| Tutorial | 15 × 8 | 969 × 540 px |
| Small | 20 × 15 | 1248 × 940 px |
| Medium | 30 × 30 | 1858 × 1860 px |
| Large | 40 × 40 | 2451 × 2460 px |

## Hexagon Vertices (for drawing)

Relative to center, with HEX_SIZE = 40:
```javascript
const vertices = [
    { x: 0,      y: -40 },     // Top
    { x: 34.64,  y: -20 },     // Top-Right
    { x: 34.64,  y: 20 },      // Bottom-Right
    { x: 0,      y: 40 },      // Bottom
    { x: -34.64, y: 20 },      // Bottom-Left
    { x: -34.64, y: -20 }      // Top-Left
];
```

## Grid Layout Pattern

```
Row 0 (even):  ⬡ ⬡ ⬡ ⬡ ⬡   (no offset)
Row 1 (odd):    ⬡ ⬡ ⬡ ⬡ ⬡ (offset right by 29.10px)
Row 2 (even):  ⬡ ⬡ ⬡ ⬡ ⬡   (no offset)
```

## Neighbor Offsets

**Even rows (y % 2 == 0):**
- E: (+1, 0) | NE: (0, -1) | NW: (-1, -1)
- W: (-1, 0) | SW: (-1, +1) | SE: (0, +1)

**Odd rows (y % 2 == 1):**
- E: (+1, 0) | NE: (+1, -1) | NW: (0, -1)
- W: (-1, 0) | SW: (0, +1) | SE: (+1, +1)

## River Flow Directions

- `EAST`, `WEST`
- `NORTH_EAST`, `NORTH_WEST`
- `SOUTH_EAST`, `SOUTH_WEST`
- `NONE` (still water)
- `MAELSTROM` (whirlpool)

Flow speed: 1 or 2 (number of arrows shown)

## Example Code (Python)

```python
import math

HEX_SIZE = 40
SQRT_3 = math.sqrt(3)
HEX_WIDTH = HEX_SIZE * SQRT_3
HEX_HEIGHT = HEX_SIZE * 2
VERTICAL_SPACING = HEX_HEIGHT * 0.75
HORIZONTAL_SPACING = -10
ODD_ROW_OFFSET_RATIO = 0.42

def draw_hexagon(ctx, x, y, tile_type):
    """Draw a hexagon at grid position (x, y)"""
    is_odd_row = (y % 2 == 1)
    row_offset = (HEX_WIDTH * ODD_ROW_OFFSET_RATIO) if is_odd_row else 0
    
    center_x = (x * (HEX_WIDTH + HORIZONTAL_SPACING)) + row_offset + (HEX_WIDTH / 2)
    center_y = (y * VERTICAL_SPACING) + (HEX_HEIGHT / 2)
    
    # Add padding offset
    center_x += 20
    center_y += 20
    
    # Draw hexagon with 6 vertices
    vertices = []
    for i in range(6):
        angle = math.pi / 3 * i - math.pi / 2
        vx = center_x + HEX_SIZE * math.cos(angle)
        vy = center_y + HEX_SIZE * math.sin(angle)
        vertices.append((vx, vy))
    
    # Draw polygon
    ctx.polygon(vertices)
    ctx.fill(get_color_for_tile(tile_type))
    ctx.stroke()
```

## Validation Checklist

✓ Map has at least one SPAWN_POINT  
✓ Map has at least one TARGET  
✓ Continuous path exists from all spawn points to at least one target  
✓ Path includes only walkable tiles (PATH, SPAWN_POINT, TARGET, optional RIVER)  
✓ River tiles have corresponding entries in riverTiles map  
✓ Image dimensions calculated correctly  
✓ Padding applied on all sides  
✓ Hexagons rendered at correct positions  

## Resources

- **Main Documentation:** `docs/reference/MAP_TILE_ARRANGEMENT.md`
- **Visual Examples:** `docs/reference/MAP_TILE_ARRANGEMENT_VISUAL_EXAMPLES.md`
- **Source Code:** `composeApp/src/commonMain/kotlin/de/egril/defender/ui/hexagon/`
- **Example Maps:** `composeApp/src/commonMain/composeResources/files/repository/maps/`
- **Red Blob Games Guide:** https://www.redblobgames.com/grids/hexagons/

---

**Last Updated:** 2026-02-16  
**Version:** 1.0
