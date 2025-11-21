# Arc Segment Row Offset Fix

## Problem

Arc segments were not being drawn for neighbor tiles in different rows than the target tile. The debug circles showed the tiles were being found correctly, but the arcs weren't visible.

## Root Cause

The `drawArcSegment` function was calculating offsets incorrectly for tiles in different rows. In a hexagonal grid with odd-row offset layout:
- Even rows (y % 2 == 0): No horizontal offset
- Odd rows (y % 2 == 1): Offset to the right by `hexWidth * 0.42f`

The original code tried to adjust grid coordinates by ±0.5, but this didn't match the actual pixel-based layout used by `HexagonalMapView`.

## Solution

Changed the offset calculation in `CircularSegmentDrawer.drawArcSegment()` to:

```kotlin
// Calculate base pixel offsets
var offsetX = dx * hexWidth
val offsetY = dy * verticalSpacing

// Adjust for hexagonal grid row offset
// In HexagonalMapView, odd rows (y % 2 == 1) are offset to the right by hexWidth * 0.42f
val neighborRowOffset = if (neighborPos.y % 2 == 1) hexWidth * 0.42f else 0f
val centerRowOffset = if (centerPos.y % 2 == 1) hexWidth * 0.42f else 0f
offsetX += (centerRowOffset - neighborRowOffset)
```

This correctly accounts for the actual pixel offset applied in the layout, ensuring arc segments are positioned correctly regardless of row parity differences.

## Example

If center is at (5, 5) [odd row] and neighbor is at (5, 4) [even row]:
- Grid distance: dx = 0, dy = 1
- Base offset: offsetX = 0, offsetY = verticalSpacing
- Row adjustment: offsetX += (hexWidth * 0.42f - 0) = hexWidth * 0.42f
- The arc is now correctly positioned to account for the visual offset between the rows

## Additional Fix

Fixed undefined `center` variable in debug circle drawing code by calculating the tile center:
```kotlin
val centerX = size.width / 2
val centerY = size.height / 2
```

## Result

Arc segments now render correctly on all neighbor tiles, forming complete rings around the target regardless of row positions.
