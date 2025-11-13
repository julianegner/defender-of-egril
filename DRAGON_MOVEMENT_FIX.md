# Dragon Movement Fix

## Issue Description

The dragon unit was moving incorrectly. It should move as follows:
- **Turn 1**: 1 tile on the path (walking)
- **Turn 2+**: Up to 5 tiles, flying over any terrain, but **must end on a path tile**

The previous implementation allowed the dragon to end on any tile when flying, not just path tiles.

## Root Cause

The flying movement logic in `EnemyMovementSystem.moveDragon()` used a greedy approach:
- It moved one hex at a time towards the target
- It didn't validate that the final position was on the path
- This could result in the dragon landing on non-path tiles (build areas, islands, etc.)

## Solution

Replaced the greedy movement with a proper BFS (Breadth-First Search) algorithm:

1. **BFS Exploration**: Starting from the dragon's current position, explore all tiles within 5 hexagonal distance
2. **Path Filtering**: Identify which of these reachable positions are on the path
3. **Target Proximity**: Filter to only positions that are closer to the target than the current position
4. **Best Selection**: Choose the path position that is closest to the target

This ensures:
- ✅ Dragon can fly over any terrain (islands, build areas, non-path tiles)
- ✅ Dragon always ends on a path tile
- ✅ Dragon moves optimally towards the target
- ✅ Dragon respects the maximum flying distance of 5 tiles

## Code Changes

### File: `composeApp/src/commonMain/kotlin/com/defenderofegril/game/EnemyMovementSystem.kt`

**Before (lines 253-298):**
```kotlin
// For flying, we can move directly towards target ignoring path
if (dragon.isFlying.value) {
    var currentPos = dragon.position.value
    var remainingSpeed = speed
    
    while (remainingSpeed > 0) {
        val neighbors = currentPos.neighbors()
        val nextPos = neighbors.minByOrNull { it.distanceTo(target) } ?: break
        // ... greedy movement that doesn't ensure ending on path
    }
    dragon.position.value = currentPos
}
```

**After (lines 252-334):**
```kotlin
// For flying, we can move up to 5 tiles but must end on path
if (dragon.isFlying.value) {
    // BFS to find all positions within 5 hexagonal distance
    val visited = mutableSetOf(currentPos)
    val queue = mutableListOf(Pair(currentPos, 0))
    
    while (queue.isNotEmpty()) {
        val (pos, dist) = queue.removeAt(0)
        
        // Check if this position is on path and gets us closer to target
        if (pos != currentPos && 
            state.level.isOnPath(pos) && 
            pos.distanceTo(target) < currentDistToTarget) {
            reachablePathPositions.add(Pair(pos, dist))
        }
        
        // Explore neighbors if we haven't reached max flying distance
        if (dist < speed) {
            for (neighbor in pos.getHexNeighbors()) {
                // ... BFS exploration
            }
        }
    }
    
    // Choose the path position that gets us closest to target
    val bestPosition = reachablePathPositions.minByOrNull { (pos, _) ->
        pos.distanceTo(target)
    }?.first
    
    dragon.position.value = bestPosition ?: currentPos
}
```

## Tests Added

Created `DragonMovementTest.kt` with 5 comprehensive tests:

1. **testDragonWalksOnFirstTurn**: Validates that dragons walk 1 tile on their first turn
2. **testDragonFliesOnSecondTurn**: Validates that dragons fly up to 5 tiles on subsequent turns
3. **testDragonFliesOverObstacles**: Validates that dragons can fly over islands/obstacles
4. **testDragonMustEndOnPath**: Validates dragons always end on path tiles across multiple turns
5. **testDragonCannotEndOffPath**: Specific test that fails with old implementation, passes with new one

All tests pass with the new implementation.

## Verification

Run the tests:
```bash
./gradlew :composeApp:testDebugUnitTest --tests "com.defenderofegril.game.DragonMovementTest"
```

All existing tests continue to pass:
```bash
./gradlew :composeApp:testDebugUnitTest
```

## Impact

- **Gameplay**: Dragons now move correctly according to the design requirements
- **Balance**: Flying dragons can take more direct paths while still respecting the path constraint
- **Performance**: BFS is efficient for small search spaces (5 tile radius)
- **Compatibility**: No breaking changes to game save format or other systems
