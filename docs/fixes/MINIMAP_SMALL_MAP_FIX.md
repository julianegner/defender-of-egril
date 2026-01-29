# Minimap Navigation Fix for Small Maps

## Issue
Minimap navigation by dragging did not work on small maps, specifically the "Straight Map" (45×11 tiles). The minimap would display the viewport indicator but dragging it had no effect.

## Root Cause
In `HexagonMinimap.kt` at line 381, the drag gesture handler was only enabled when **both** viewport dimensions needed scrolling:

```kotlin
if (onViewportDrag != null && viewportWidthRatio < 1.0f && viewportHeightRatio < 1.0f)
```

On maps with very different width-to-height ratios (like the Straight Map: 45 wide × 11 tall), it's possible for one dimension to fit fully in the viewport while the other dimension extends beyond it. When this happens:
- Width may need scrolling: `viewportWidthRatio < 1.0f` ✓
- Height may fit fully: `viewportHeightRatio = 1.0f` ✗
- The AND condition fails, so dragging is disabled ✗

## Solution
Changed the condition to use OR instead of AND:

```kotlin
if (onViewportDrag != null && (viewportWidthRatio < 1.0f || viewportHeightRatio < 1.0f))
```

Now dragging is enabled when **either** dimension needs scrolling, which is the correct behavior.

## Changes Made

### 1. HexagonMinimap.kt
**File**: `composeApp/src/commonMain/kotlin/de/egril/defender/ui/hexagon/HexagonMinimap.kt`

**Line 381**: Changed the condition from AND to OR:
```diff
- if (onViewportDrag != null && viewportWidthRatio < 1.0f && viewportHeightRatio < 1.0f) {
+ if (onViewportDrag != null && (viewportWidthRatio < 1.0f || viewportHeightRatio < 1.0f)) {
```

### 2. MinimapNavigationTest.kt
**File**: `composeApp/src/commonTest/kotlin/de/egril/defender/ui/MinimapNavigationTest.kt`

Added two new test cases to verify the fix:

#### Test 1: `testMinimapDragOnSmallMap_OnlyWidthScrollable()`
Tests a wide but short map (2000×400 pixels) where:
- Width extends beyond viewport (needs scrolling)
- Height fits fully in viewport (no scrolling needed)
- Verifies that horizontal dragging works correctly

#### Test 2: `testMinimapDragOnSmallMap_OnlyHeightScrollable()`
Tests a tall but narrow map (600×2000 pixels) where:
- Height extends beyond viewport (needs scrolling)
- Width fits fully in viewport (no scrolling needed)
- Verifies that vertical dragging works correctly

Both tests ensure that the minimap navigation works in both single-dimension scrolling scenarios.

## Test Results
✅ All 6 tests in `MinimapNavigationTest` pass:
1. `testMinimapDragToViewportOffsetConversion` - Basic drag conversion
2. `testMinimapDragConstraints` - Boundary constraints
3. `testMinimapDragWhenFullyZoomedOut` - No scrolling needed
4. `testMinimapDragWithHighZoom` - High zoom with both dimensions scrollable
5. `testMinimapDragOnSmallMap_OnlyWidthScrollable` - NEW: Wide map test
6. `testMinimapDragOnSmallMap_OnlyHeightScrollable` - NEW: Tall map test

## Impact

### Before Fix
- Minimap navigation worked on square or near-square maps
- Did not work on maps with extreme width-to-height ratios
- Specifically failed on the "Straight Map" (45×11)
- Failed on any map where one dimension fit fully while the other needed scrolling

### After Fix
- Minimap navigation works on all map sizes and aspect ratios
- Works correctly on wide maps (like Straight Map)
- Works correctly on tall maps
- Works correctly on square maps (no regression)
- Users can now drag the minimap viewport indicator to navigate on any map where scrolling is possible

## Affected Levels
The following levels use the "map_straight" and will benefit from this fix:
- The First Wave
- Mixed Forces
- The Ork Invasion
- Dark Magic Rises
- The Witches

## Technical Details

### Viewport Ratio Calculation
The viewport ratio represents what fraction of the map is visible:
- `viewportWidthRatio = containerWidth / (contentWidth × scale)`
- `viewportHeightRatio = containerHeight / (contentHeight × scale)`
- Values are clamped to maximum 1.0

### Edge Cases
1. **Both dimensions fit (ratio = 1.0)**: Dragging disabled (correct - no scrolling needed)
2. **Both dimensions extend (ratio < 1.0)**: Dragging enabled (correct - worked before)
3. **One dimension fits, one extends**: Dragging NOW enabled (fixed - previously broken)

### Minimap Behavior
- The minimap shows a yellow viewport indicator
- When zoomed in, the indicator shrinks to show the visible portion
- Users can drag the indicator to quickly navigate
- Dragging only affects the dimension(s) that need scrolling
- If only width needs scrolling, dragging works horizontally
- If only height needs scrolling, dragging works vertically
- If both need scrolling, dragging works in both directions

## Backward Compatibility
✅ **Fully backward compatible**
- No API changes
- No breaking changes to existing behavior
- Only fixes a bug - improves usability without changing correct functionality
- All existing tests continue to pass
- New tests add coverage for previously untested scenarios
