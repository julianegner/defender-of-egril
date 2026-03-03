# Visual Examples for Map Tile Arrangement

This document provides visual ASCII diagrams to supplement the technical documentation in `MAP_TILE_ARRANGEMENT.md`.

## Hexagonal Grid Pattern

### Small 5×3 Grid Example

```
     /\    /\    /\    /\    /\     Row 0 (even - no offset)
    /  \  /  \  /  \  /  \  /  \
   |0,0||1,0||2,0||3,0||4,0|
    \  /  \  /  \  /  \  /  \  /
     \/    \/    \/    \/    \/
       /\    /\    /\    /\    /\   Row 1 (odd - offset right)
      /  \  /  \  /  \  /  \  /  \
     |0,1||1,1||2,1||3,1||4,1|
      \  /  \  /  \  /  \  /  \  /
       \/    \/    \/    \/    \/
     /\    /\    /\    /\    /\     Row 2 (even - no offset)
    /  \  /  \  /  \  /  \  /  \
   |0,2||1,2||2,2||3,2||4,2|
    \  /  \  /  \  /  \  /  \  /
     \/    \/    \/    \/    \/
```

Notice how row 1 (the middle row) is shifted to the right compared to rows 0 and 2.

## Tile Type Layout Example

Legend:
- `S` = SPAWN_POINT
- `P` = PATH
- `B` = BUILD_AREA
- `T` = TARGET
- `.` = NO_PLAY

### Tutorial Map Pattern (Simplified 15×8)

```
   . . . . .B.B. . . . . . . .     Row 0
    . . . . . .B.B. . . . . . .    Row 1
   . .B.B.B.B.B.B.B.B.B.B.B.B.B.   Row 2
    .P.P.P.P.P.P.P.P.P.P.P.P.P.P.  Row 3
   .S.P.P.P.P.P.P.P.P.P.P.P.P.T.   Row 4
    .P.P.P.P.P.P.P.P.P.P.P.P.P.P.  Row 5
   . .B.B.B.B.B.B.B.B.B.B.B.B.B.   Row 6
    . . . . . . . . . .B.B. . . .  Row 7
```

## Pixel Coordinate System

### Coordinate Origin
```
(0,0) ────────────────> X-axis (right)
  │
  │
  │
  │
  ▼
Y-axis (down)
```

### Hexagon Center Positions

For a 3×2 grid, the hexagon centers are positioned at:

```
Row 0 (even):
  Hex (0,0): center at (34.64, 40.00)
  Hex (1,0): center at (93.92, 40.00)
  Hex (2,0): center at (153.20, 40.00)

Row 1 (odd - with offset):
  Hex (0,1): center at (63.74, 100.00)
  Hex (1,1): center at (123.02, 100.00)
  Hex (2,1): center at (182.30, 100.00)
```

Notice the 29.10 pixel rightward offset for row 1 hexagons.

## Bounding Box Visualization

For a map of width W and height H, the bounding box extends from:

```
┌─────────────────────────────┐
│ Padding (20px)              │
│   ┌─────────────────────┐   │
│   │                     │   │
│   │   Hexagon Grid      │   │
│   │   Content Area      │   │
│   │                     │   │
│   └─────────────────────┘   │
│                             │
└─────────────────────────────┘
```

### Rectangular Image Dimensions

The image is rectangular (not following hexagon edges) to allow easy embedding:

```
Without padding (hexagons at edges):
┌───╱╲───╱╲───╱╲───┐
│  ╱  ╲ ╱  ╲ ╱  ╲  │
│ │    │    │    │ │ <- Content width
│  ╲  ╱ ╲  ╱ ╲  ╱  │
│   ╲╱   ╲╱   ╲╱   │
└──────────────────┘
      ↑
   Content height

With padding (full rectangular image):
┌──────────────────────┐
│      (padding)       │
│  ┌───╱╲───╱╲───╱╲───┐│
│  │  ╱  ╲ ╱  ╲ ╱  ╲  ││
│  │ │    │    │    │ ││ <- Image width
│  │  ╲  ╱ ╲  ╱ ╲  ╱  ││
│  │   ╲╱   ╲╱   ╲╱   ││
│  └──────────────────┘│
│      (padding)       │
└──────────────────────┘
         ↑
     Image height
```

## Hexagon Neighbor Relationships

### Even Row (y % 2 == 0) Neighbors

```
     NW    NE
      ╲   ╱
       ╲ ╱
   W ── ◯ ── E    (Current hexagon at even row)
       ╱ ╲
      ╱   ╲
     SW    SE
```

Position offsets for hexagon at (x, y) where y is even:
- E (East): (x+1, y)
- NE (North-East): (x, y-1)
- NW (North-West): (x-1, y-1)
- W (West): (x-1, y)
- SW (South-West): (x-1, y+1)
- SE (South-East): (x, y+1)

### Odd Row (y % 2 == 1) Neighbors

```
     NW    NE
      ╲   ╱
       ╲ ╱
   W ── ◯ ── E    (Current hexagon at odd row)
       ╱ ╲
      ╱   ╲
     SW    SE
```

Position offsets for hexagon at (x, y) where y is odd:
- E (East): (x+1, y)
- NE (North-East): (x+1, y-1)
- NW (North-West): (x, y-1)
- W (West): (x-1, y)
- SW (South-West): (x, y+1)
- SE (South-East): (x+1, y+1)

Note: The neighbor positions differ between even and odd rows due to the offset!

## River Tile Flow Directions

River tiles can have flow in any of the 6 hexagonal directions:

```
      NORTH_WEST    NORTH_EAST
            ╲           ╱
             ╲         ╱
              ╲       ╱
               ╲     ╱
                ╲   ╱
                 ╲ ╱
    WEST ──────── ◯ ──────── EAST
                 ╱ ╲
                ╱   ╲
               ╱     ╲
              ╱       ╲
             ╱         ╲
            ╱           ╲
      SOUTH_WEST    SOUTH_EAST
```

Plus special cases:
- NONE: Still water (no flow)
- MAELSTROM: Whirlpool/vortex

Flow speed can be 1 or 2 (shown as one or two arrows in the game).

## Island Pattern Example

Islands are often arranged in 2×2 groups:

```
     /\    /\
    /  \  /  \
   | I || I |     Row 0 (even)
    \  /  \  /
     \/    \/
       /\    /\
      /  \  /  \
     | I || I |  Row 1 (odd - offset)
      \  /  \  /
       \/    \/
```

This creates a cohesive island formation that spans multiple hexagons.

## Path Continuity

For a valid map, there must be a continuous path from spawn points to targets:

```
S → P → P → P → P → P → T
```

Where:
- S = Spawn point (also walkable)
- P = Path tile
- T = Target (also walkable)

The path can include waypoints for complex routing:

```
S → P → P → W₁ → P → W₂ → P → T
```

Where W₁ and W₂ are waypoints that enemies must visit in sequence.

---

These visualizations complement the technical specifications in `MAP_TILE_ARRANGEMENT.md`.
