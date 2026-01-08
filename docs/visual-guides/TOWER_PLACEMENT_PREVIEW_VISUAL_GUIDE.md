# Tower Placement Range Preview - Visual Examples

This document provides visual descriptions and ASCII art representations of the tower placement preview feature since actual screenshots cannot be captured in the CI environment.

## Example 1: Spike Tower Preview (Range 1)

```
Legend:
  [P] = Path tile (enemy walkway)
  [B] = Build area (empty, can place tower)
  [T] = Tower already placed
  [H] = Hovered build tile (YELLOW background, YELLOW dashed border)
  [R] = Range preview tile (LIGHT GREEN background, GREEN dashed border)

Map Layout:
┌─────┬─────┬─────┬─────┬─────┐
│     │     │ [B] │     │     │
├─────┼─────┼─────┼─────┼─────┤
│ [B] │ [B] │ [P] │ [B] │ [B] │  
├─────┼─────┼─────┼─────┼─────┤
│     │     │ [P] │     │     │
├─────┼─────┼─────┼─────┼─────┤
│ [B] │ [B] │ [P] │ [B] │ [B] │
└─────┴─────┴─────┴─────┴─────┘

When hovering over build tile at (1,1):
┌─────┬─────┬─────┬─────┬─────┐
│     │     │ [B] │     │     │
├─────┼─────┼─────┼─────┼─────┤
│[H]╱╲│ [B] │[R]╱╲│ [B] │ [B] │  <- Spike Tower selected
│  ╲╱│     │ ╲╱│     │     │  <- Hovered at (1,1), shows range on (2,1)
├─────┼─────┼─────┼─────┼─────┤
│     │     │ [P] │     │     │
├─────┼─────┼─────┼─────┼─────┤
│ [B] │ [B] │ [P] │ [B] │ [B] │
└─────┴─────┴─────┴─────┴─────┘

Visual Elements:
- Tile (1,1): YELLOW background (40% alpha) + YELLOW dashed border
- Tile (2,1): LIGHT GREEN background (20% alpha) + GREEN dashed border
- Path tile within range is highlighted as attackable
```

## Example 2: Bow Tower Preview (Range 3)

```
Map Layout:
┌─────┬─────┬─────┬─────┬─────┬─────┬─────┐
│     │     │ [B] │     │ [P] │     │     │
├─────┼─────┼─────┼─────┼─────┼─────┼─────┤
│ [B] │ [B] │ [P] │ [P] │ [P] │ [B] │ [B] │  
├─────┼─────┼─────┼─────┼─────┼─────┼─────┤
│     │     │ [P] │     │ [P] │     │     │
├─────┼─────┼─────┼─────┼─────┼─────┼─────┤
│ [B] │ [B] │ [P] │ [P] │ [P] │ [B] │ [B] │
└─────┴─────┴─────┴─────┴─────┴─────┴─────┘

When hovering over build tile at (1,1) with Bow Tower (range 3):
┌─────┬─────┬─────┬─────┬─────┬─────┬─────┐
│     │     │ [B] │     │[R]╱╲│     │     │  <- Range 2 away
│     │     │     │     │ ╲╱│     │     │
├─────┼─────┼─────┼─────┼─────┼─────┼─────┤
│[H]╱╲│ [B] │[R]╱╲│[R]╱╲│[R]╱╲│ [B] │ [B] │  <- Range 1-3 away
│ ╲╱│     │ ╲╱│ ╲╱│ ╲╱│     │     │
├─────┼─────┼─────┼─────┼─────┼─────┼─────┤
│     │     │[R]╱╲│     │[R]╱╲│     │     │  <- Range 1-2 away
│     │     │ ╲╱│     │ ╲╱│     │     │
├─────┼─────┼─────┼─────┼─────┼─────┼─────┤
│ [B] │ [B] │[R]╱╲│[R]╱╲│[R]╱╲│ [B] │ [B] │  <- Range 2-3 away
│     │     │ ╲╱│ ╲╱│ ╲╱│     │     │
└─────┴─────┴─────┴─────┴─────┴─────┴─────┘

Visual Elements:
- Tile (1,1): YELLOW background (40% alpha) + YELLOW dashed border
- All path tiles within 3 hexes: LIGHT GREEN (20% alpha) + GREEN dashed border
- Multiple path tiles highlighted showing long-range coverage
```

## Example 3: Ballista Tower Preview (Range 5, Min Range 3)

```
When hovering over build tile at (1,1) with Ballista (range 3-5):
┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
│     │     │     │     │[R]╱╲│[R]╱╲│[R]╱╲│     │     │  <- Range 4-5
│     │     │     │     │ ╲╱│ ╲╱│ ╲╱│     │     │
├─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┤
│     │     │ [B] │     │[R]╱╲│[R]╱╲│[R]╱╲│     │     │  <- Range 3-5
│     │     │     │     │ ╲╱│ ╲╱│ ╲╱│     │     │
├─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┤
│[H]╱╲│ [B] │ [P] │ [P] │[R]╱╲│[R]╱╲│[R]╱╲│ [B] │ [B] │  <- Range 3-5
│ ╲╱│     │     │     │ ╲╱│ ╲╱│ ╲╱│     │     │
├─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┤
│     │     │ [P] │     │[R]╱╲│[R]╱╲│[R]╱╲│     │     │  <- Range 3-5
│     │     │     │     │ ╲╱│ ╲╱│ ╲╱│     │     │
├─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┤
│     │     │     │     │[R]╱╲│[R]╱╲│[R]╱╲│     │     │  <- Range 4-5
└─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┘

Visual Elements:
- Tile (1,1): YELLOW background + YELLOW dashed border
- Tiles within range 1-2: NO PREVIEW (below minimum range)
- Tiles within range 3-5: LIGHT GREEN + GREEN dashed border
- Demonstrates min range restriction visualization
```

## Example 4: Wizard Tower (Area Attack) Preview

```
When hovering over build tile with Wizard Tower (area attack, range 3):
┌─────┬─────┬─────┬─────┬─────┬─────┬─────┐
│     │     │ [B] │     │[R]╱╲│[R]╱╲│     │
│     │     │     │     │ ╲╱│ ╲╱│     │
├─────┼─────┼─────┼─────┼─────┼─────┼─────┤
│ [B] │[H]╱╲│[R]╱╲│[R]╱╲│[R]╱╲│[R]╱╲│ [B] │  <- Can target path AND river
│     │ ╲╱│ ╲╱│ ╲╱│ ╲╱│ ╲╱│     │
├─────┼─────┼─────┼─────┼─────┼─────┼─────┤
│     │ ~~~ │[R]╱╲│     │[R]╱╲│[R]╱╲│     │  <- ~~~ = River tile
│     │     │ ╲╱│     │ ╲╱│ ╲╱│     │
├─────┼─────┼─────┼─────┼─────┼─────┼─────┤
│ [B] │ [B] │[R]╱╲│[R]╱╲│[R]╱╲│ [B] │ [B] │
└─────┴─────┴─────┴─────┴─────┴─────┴─────┘

Visual Elements:
- AREA attack types show range on BOTH path tiles AND river tiles
- This differs from single-target attacks which only show path tiles
- All tiles get the same visual treatment (green dashed borders)
```

## Color Scheme Details

### Light Mode
- **Hovered Build Tile**: 
  - Background: `Color(0xFFFFEB3B).copy(alpha = 0.4f)` (light yellow, 40% transparent)
  - Border: `Color(0xFFFFEB3B)` (yellow, solid color with dashed pattern)
  
- **Range Preview Tile**:
  - Background: `Color(0xFF4CAF50).copy(alpha = 0.2f)` (light green, 20% transparent)
  - Border: `Color(0xFF4CAF50)` (green, solid color with dashed pattern)

### Dark Mode
- **Hovered Build Tile**: 
  - Background: `Color(0xFFC4A000).copy(alpha = 0.4f)` (darker yellow, 40% transparent)
  - Border: `Color(0xFFC4A000)` (darker yellow)
  
- **Range Preview Tile**:
  - Background: `Color(0xFF2E7D32).copy(alpha = 0.2f)` (darker green, 20% transparent)
  - Border: `Color(0xFF2E7D32)` (darker green)

### Dashed Border Pattern
- **Dash Length**: 10 pixels
- **Gap Length**: 5 pixels
- **Border Width**: 3dp
- Implementation: `PathEffect.dashPathEffect(floatArrayOf(10f, 5f), phase = 0f)`

## Comparison with Existing Tower Range Display

### Placed Tower (Existing Feature)
- **Background**: Blue (`GamePlayColors.Info`) - solid color
- **Border**: Dark blue (`GamePlayColors.InfoDark`) - solid line, 3-5dp
- **Range Display**: Green border (`GamePlayColors.Success`) - solid line, 4dp on path tiles

### Tower Preview (New Feature)
- **Background**: Light yellow (40% alpha) for build tile, light green (20% alpha) for range
- **Border**: Yellow/Green - dashed line (10px dash, 5px gap), 3dp
- **Transparency**: Much lighter to indicate "not yet placed"

The key distinction is:
1. **Solid vs. Dashed**: Placed towers use solid borders, preview uses dashed
2. **Opacity**: Preview uses transparent backgrounds, placed towers use solid colors
3. **Colors**: Different color scheme (yellow/light green vs. blue/green)
