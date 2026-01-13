# Map Editor Fix - Spacing Synchronization

## Problem
The map editor was broken - dragging the mouse over tiles to change their type didn't work correctly. The issue was described as "large gaps between hexagons" but this was actually a symptom of incorrect mouse position calculation, not a visual rendering issue.

## Root Cause
There was a **mismatch between two critical files**:

### HexagonalMapView.kt (Rendering)
```kotlin
// Line 319: Vertical spacing
verticalArrangement = Arrangement.spacedBy((-hexHeight + verticalSpacing - 7f).dp)

// Line 328: Horizontal spacing  
horizontalArrangement = Arrangement.spacedBy((-10).dp)
```

### HexUtils.kt (Mouse Position Conversion)
```kotlin
// Line 37: Vertical spacing - WRONG!
val rowSpacing = -hexHeight + verticalSpacing - 15f  // Should be -7f

// Line 38: Horizontal spacing - WRONG!
val colSpacing = -18f  // Should be -10f
```

The `screenToHexGridPosition` function in `HexUtils.kt` was using different spacing values than the actual rendering, causing mouse drag positions to be incorrectly translated to tile positions.

## Solution
**Fixed `HexUtils.kt` to match `HexagonalMapView.kt`:**
```kotlin
// Line 37: Vertical spacing - FIXED
val rowSpacing = -hexHeight + verticalSpacing - 7f  // Now matches rendering

// Line 38: Horizontal spacing - FIXED
val colSpacing = -10f  // Now matches rendering
```

## Why This Matters
The `screenToHexGridPosition` function converts screen coordinates (where the user clicks/drags) into hex grid coordinates (x, y tile positions). For this to work correctly, it must use the **exact same spacing values** as the rendering code. Any mismatch causes mouse positions to map to the wrong tiles.

## Previous Incorrect Approach
My initial PR attempted to "fix" the spacing values in `HexagonalMapView.kt` by removing the `-7f` and changing the horizontal spacing, thinking these were bugs. This was wrong - the values in `HexagonalMapView.kt` were correct; it was `HexUtils.kt` that had incorrect values.

## Files Changed
- `composeApp/src/commonMain/kotlin/de/egril/defender/utils/HexUtils.kt` - Fixed spacing values to match rendering
- `composeApp/src/commonMain/kotlin/de/egril/defender/ui/HexagonalMapView.kt` - Reverted to original (correct) values

## Impact
- ✅ Map editor drag-to-paint now works correctly
- ✅ Gameplay map rendering unchanged (was already correct)
- ✅ Mouse position to tile conversion now accurate
