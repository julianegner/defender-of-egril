# Waypoint Minimap Connections Enhancement

## Overview
This document describes the enhancement made to the waypoint minimap in the level editor to display connections with different colors for each target and ensure all connections are properly visible.

## Problem Statement
The original issue (#issue-number) requested:
1. Display the connections on the minimap with different colors (one color for each target)
2. Check why sometimes the connections are not shown on the minimap

## Root Cause Analysis

### Why connections weren't always shown
The original implementation (lines 242-261 in WaypointMinimap.kt) had the following issues:
1. **Connection drawing was inside the tile iteration loop** - Connections were only drawn when iterating through map tiles
2. **Bounds checking prevented valid connections** - The code checked `if (targetCol in 0 until mapWidth && targetRow in 0 until mapHeight)` which could exclude valid waypoint positions
3. **Waypoints not on tiles were skipped** - If a waypoint's position didn't correspond to an iterated tile, no connection was drawn

### Why all connections used the same color
The original implementation used a hardcoded yellow color (`Color.Companion.Yellow`) for all waypoint connections, with no logic to differentiate between different target destinations.

## Solution Implementation

### 1. Color Palette for Targets
Added a palette of 8 distinct colors to represent different target destinations:
```kotlin
private val TARGET_COLORS = listOf(
    Color(0xFFFFD700), // Gold
    Color(0xFF00FFFF), // Cyan
    Color(0xFFFF00FF), // Magenta
    Color(0xFF00FF00), // Lime
    Color(0xFFFF6600), // Orange
    Color(0xFF9966FF), // Purple
    Color(0xFFFF0066), // Pink
    Color(0xFF66FF99), // Mint
)
```

### 2. Ultimate Target Detection
Created a `findUltimateTarget()` function that traces waypoint chains to determine which final target each waypoint ultimately leads to:

```kotlin
private fun findUltimateTarget(
    waypoint: EditorWaypoint,
    allWaypoints: List<EditorWaypoint>,
    targets: List<Position>
): Position?
```

The function:
- Follows the chain of waypoints (waypoint -> waypoint -> waypoint -> target)
- Detects circular dependencies and returns null
- Returns the final target position if found
- Handles edge cases (no target, invalid chains)

### 3. Connection Drawing Refactoring
Moved connection drawing outside the tile iteration loop:

**Before:**
```kotlin
for (row in 0 until map.height) {
    for (col in 0 until map.width) {
        // Draw tile
        // Draw connection IF waypoint at this position
    }
}
```

**After:**
```kotlin
for (row in 0 until map.height) {
    for (col in 0 until map.width) {
        // Draw tile only
    }
}

// Draw ALL waypoint connections
for (waypoint in existingWaypoints) {
    // Draw connection with target-specific color
}
```

### 4. Color Assignment Logic
Each waypoint connection is colored based on its ultimate target:
```kotlin
val waypointTargetColors = existingWaypoints.associate { waypoint ->
    val ultimateTarget = findUltimateTarget(waypoint, existingWaypoints, targets)
    val targetIndex = ultimateTarget?.let { targets.indexOf(it) } ?: -1
    val color = if (targetIndex >= 0) {
        TARGET_COLORS[targetIndex % TARGET_COLORS.size]
    } else {
        Color.Gray // For waypoints that don't lead to a valid target
    }
    waypoint.position to color
}
```

## Testing

Created comprehensive unit tests in `WaypointMinimapConnectionTest.kt`:

1. **testFindUltimateTargetDirectConnection** - Simple waypoint -> target
2. **testFindUltimateTargetChain** - Two-waypoint chain
3. **testFindUltimateTargetLongChain** - Three-waypoint chain
4. **testFindUltimateTargetCircularDependency** - Detects circular references
5. **testFindUltimateTargetNoTarget** - Waypoint not leading to target
6. **testFindUltimateTargetMultipleTargets** - Multiple targets with specific destination
7. **testFindUltimateTargetBranchingChains** - Multiple waypoints to same target

All tests pass successfully.

## Benefits

1. **Visual Clarity** - Different colors make it easy to see which waypoints lead to which targets
2. **Reliability** - All waypoint connections are now guaranteed to be drawn
3. **Scalability** - Supports up to 8 different targets (can be extended by adding more colors)
4. **Error Detection** - Gray color indicates waypoints with circular dependencies or no valid target
5. **Better UX** - Thicker stroke width (2.0f vs 1.5f) improves visibility

## Edge Cases Handled

1. **Circular dependencies** - Detected and colored gray
2. **Missing targets** - Waypoints not leading to a valid target are colored gray
3. **Multiple targets** - Each target gets its own color from the palette
4. **Long chains** - Waypoints can chain through multiple intermediate waypoints
5. **More than 8 targets** - Colors wrap around using modulo operator

## Files Modified

- `composeApp/src/commonMain/kotlin/de/egril/defender/ui/editor/level/waypoint/WaypointMinimap.kt`

## Files Added

- `composeApp/src/commonTest/kotlin/de/egril/defender/ui/WaypointMinimapConnectionTest.kt`

## Compatibility

- Backward compatible - no changes to data models or serialization
- Works with existing waypoint configurations
- No impact on other minimap implementations (HexagonMinimap, SpawnPointMinimap)
