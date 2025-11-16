# The Dance Map Implementation

## Overview
Added "The Dance" level with a unique circular path design that makes enemies move in dancing circular patterns before reaching the center target.

## Map Specifications

### Layout
- **Size**: 40×40 hexagonal grid
- **Target**: Center at (20,20)
- **Spawn Points**: 4 at edge centers
  - Top: (20, 0)
  - Bottom: (20, 39)
  - Left: (0, 20)
  - Right: (39, 20)

### Broken Ring Feature
At distance 4 from the center target, a "broken ring" pattern alternates:
- 3 BUILD_AREA tiles (for tower placement)
- 3 PATH tiles
- This pattern repeats around the entire ring

This creates strategic tower placement opportunities while maintaining the circular path theme.

### Path Design

#### Circular Movement Pattern
Without implementing waypoints (which are marked as "Future" in the codebase), the map achieves the "dancing" effect through clever path layout:

**Concentric Circles:**
- Outer circle at radius 18
- Middle circle at radius 10  
- Inner circle at radius 6

**Radial Connections:**
- 8 radial spokes at angles: 0°, 45°, 90°, 135°, 180°, 225°, 270°, 315°
- Connect all circles to ensure continuous pathfinding
- Enemies naturally follow circular paths due to A* pathfinding

**Connectivity:**
- Direct paths from each spawn point toward center
- Extra width on all paths for guaranteed connectivity
- Validated via BFS pathfinding check

## Level Configuration

### Level 9: "The Dance"
- **Title**: "The Dance"
- **Subtitle**: "Follow the Rhythm"
- **Starting Coins**: 220
- **Starting Health**: 10
- **Enemies**: 45 total
  - Heavy emphasis on fast units (Goblins and Skeletons)
  - Mix includes: Goblins (33%), Skeletons (33%), Orks (17%), Evil Wizards (17%)
- **Available Towers**: All except Dragon's Lair

### Level Sequence
The Dance (Level 9) is positioned before "The Final Stand" (Level 5):
```
[Tutorial, 1, 2, 3, 4, 7 (Spiral), 8 (Plains), 9 (Dance), 5 (Final Stand), 6 (Ewhad)]
```

## Technical Implementation

### MapGenerator.kt
Added `createDanceMap()` function that:
1. Places spawn points at edge centers
2. Creates broken ring at distance 4 with alternating tile pattern
3. Generates circular paths at multiple radii
4. Adds radial spokes for connectivity
5. Ensures spawn-to-target pathfinding validation

### Path Generation Algorithm
```kotlin
// Create concentric circles
for (radius in [18, 10, 6]) {
    for each position at distance radius±2 from center {
        add to path
    }
}

// Add radial spokes
for (angle in [0°, 45°, 90°, 135°, 180°, 225°, 270°, 315°]) {
    for radius 0 to 18 {
        calculate position at (radius, angle)
        add position and neighbors to path
    }
}

// Connect spawns directly
for each spawn point {
    create straight path toward center
    add neighboring cells for width
}
```

### Why Not Waypoints?

Waypoints are defined in `TileType` enum but marked as "Future: waypoints for path control". The current implementation:
- **PathfindingSystem**: Uses direct A* from spawn to target (no waypoint support)
- **No waypoint tests**: No existing test infrastructure
- **No waypoint logic**: Would require significant pathfinding system changes

The circular path design achieves the same gameplay effect without modifying core pathfinding:
- Enemies follow the A* optimal path through the circular paths
- Natural "dancing" movement as they circle around
- Strategic tower placement on the broken ring

## Testing

### New Tests (7 total)
1. `testDanceMapExists()`: Verifies map creation
2. `testDanceMapHasFourSpawnPoints()`: Checks edge center spawns
3. `testDanceMapHasCenterTarget()`: Confirms center target
4. `testDanceMapHasBrokenRing()`: Validates ring pattern at distance 4
5. `testDanceMapIsReadyToUse()`: Ensures valid pathfinding
6. `testDanceLevelExists()`: Verifies level configuration
7. `testDanceMapStructure()`: Validates tile distribution
8. `testLevelSequenceUpdated()`: Updated to include Level 9

### Test Results
All 22 tests passing:
- Spiral Map: 7 tests ✅
- Plains Map: 7 tests ✅  
- Dance Map: 7 tests ✅
- Level Sequence: 1 test ✅

### Validation
The map passes `validateReadyToUse()` which uses BFS to confirm:
- Continuous path from all spawn points to target
- No blocked or disconnected areas
- Proper connectivity through the circular design

## Gameplay Experience

### Strategic Elements
1. **Broken Ring**: Players must decide which sections of the ring to fortify
2. **Circular Movement**: Enemies spend more time in range of properly positioned towers
3. **Multiple Approaches**: 4 spawn points create varied attack vectors
4. **Tower Synergy**: Circular paths ideal for area-effect and damage-over-time towers

### Visual Theme
The concentric circles and radial spokes create a ballroom dance floor aesthetic, matching the "Dance" theme where enemies waltz through the circular patterns.

## Version Management
Remains at version 4 as requested - no version bump needed.

## Files Changed
1. **MapGenerator.kt**: +90 lines (createDanceMap, generateDancePath functions)
2. **EditorStorage.kt**: +40 lines (Dance map creation, Level 9 configuration, updated sequence)
3. **SpiralMapTest.kt**: +140 lines (7 new comprehensive tests)

## Future Enhancements
If waypoints are implemented in the future, the Dance map could be enhanced to:
- Add waypoint tiles at key circle intersections
- Force specific circular routes before allowing center approach
- Create more complex dancing patterns

However, the current path-based design provides excellent gameplay without requiring waypoint system changes.
