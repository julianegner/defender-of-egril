# Fix for Map Editor Hexagon Gaps

## Issue
The map editor had large gaps between hexagons, making it difficult to paint tiles by dragging the mouse over them.

## Root Cause
In `HexagonalMapView.kt`, the spacing calculations for hexagon tessellation had incorrect values:

### Vertical Spacing (Line 319)
```kotlin
// BEFORE (incorrect):
verticalArrangement = Arrangement.spacedBy((-hexHeight + verticalSpacing - 7f).dp)

// AFTER (correct):
verticalArrangement = Arrangement.spacedBy((-hexHeight + verticalSpacing).dp)
```

**Problem**: The `-7f` value was adding an extra 7 density-independent pixels of negative spacing, creating a 7dp gap between rows of hexagons.

**Calculation**:
- `hexHeight = hexSize * 2 = 40 * 2 = 80dp`
- `verticalSpacing = hexHeight * 0.75 = 60dp`
- Before: `-80 + 60 - 7 = -27dp` (too much overlap, creating gaps)
- After: `-80 + 60 = -20dp` (correct overlap for pointy-top hexagons)

### Horizontal Spacing (Line 329)
```kotlin
// BEFORE (incorrect):
horizontalArrangement = Arrangement.spacedBy((-10).dp)

// AFTER (correct):
horizontalArrangement = Arrangement.spacedBy((-(hexWidth * 0.25f)).dp)
```

**Problem**: The hardcoded `-10dp` value didn't provide enough overlap for proper hexagon tessellation.

**Calculation**:
- `hexWidth = hexSize * sqrt(3) = 40 * 1.732 ≈ 69.28dp`
- Before: `-10dp` (not enough overlap)
- After: `-(69.28 * 0.25) ≈ -17.32dp` (correct overlap for pointy-top hexagons)

## Mathematical Background

For pointy-top hexagons in an offset coordinate system (even-q vertical layout):

1. **Vertical Spacing**: Adjacent rows should overlap by 25% of the hexagon height
   - Formula: `verticalSpacing = hexHeight * 0.75`
   - Arrangement spacing: `-hexHeight + verticalSpacing = -hexHeight * 0.25`

2. **Horizontal Spacing**: Adjacent hexagons in the same row should overlap by 25% of the hexagon width
   - Formula: `hexWidth * 0.25`
   - Arrangement spacing: `-(hexWidth * 0.25)`

## Impact

This fix affects both:
- **Map Editor**: Hexagons now tessellate properly, allowing smooth tile painting by dragging
- **Game Play**: Uses the same `HexagonalMapView` component, so hexagons also display correctly

## Testing

Added `HexagonalSpacingTest.kt` with 4 unit tests:
1. `testVerticalSpacingCalculation` - Verifies vertical spacing is -20dp
2. `testHorizontalSpacingCalculation` - Verifies horizontal spacing is approximately -17.32dp
3. `testNoExtraVerticalGap` - Confirms the -7f gap is removed
4. `testHorizontalSpacingFormula` - Validates the spacing formula

All tests pass. Full test suite (58 test suites) passes.

## Files Changed
- `composeApp/src/commonMain/kotlin/de/egril/defender/ui/HexagonalMapView.kt` (2 lines changed)
- `composeApp/src/commonTest/kotlin/de/egril/defender/ui/HexagonalSpacingTest.kt` (new file)
