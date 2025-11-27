# Level ID Save System Fix

## Issue
The world map save system (`worldmap.json`) was using numeric level IDs (1, 2, 3...) to track which levels are LOCKED, UNLOCKED, or WON. These numeric IDs were assigned sequentially based on the position of levels in the sequence.

### Problem
When a new level is added or the level sequence is reordered in the editor, the numeric IDs shift:
- Level that was at position 2 might now be at position 3
- This causes the wrong level to be locked/unlocked
- Players could lose progress or have inappropriate access to levels

### Example Scenario
Before:
```json
{
  "levelStatuses": {
    "1": "WON",
    "2": "UNLOCKED",
    "3": "LOCKED"
  }
}
```

If a new level is inserted at position 2:
- Old level 2 is now level 3
- Old level 3 is now level 4
- Player's progress is now applied to wrong levels

## Solution
Changed the save system to use stable editor level IDs (Strings like "level_1732546789123") instead of sequential numeric IDs.

### Implementation Changes

#### 1. Data Model (`SaveModels.kt`)
```kotlin
// Before
data class WorldMapSave(
    val levelStatuses: Map<Int, LevelStatus>  // levelId -> status
)

// After
data class WorldMapSave(
    val levelStatuses: Map<String, LevelStatus>  // editorLevelId -> status
)
```

#### 2. Serialization (`SaveJsonSerializer.kt`)
- Updated `serializeWorldMapSave()` to use String keys
- Updated `deserializeWorldMapSave()` to parse String keys (removed `.toInt()` call)

#### 3. Storage (`SaveFileStorage.kt`)
```kotlin
// Before
fun saveWorldMapStatus(worldLevels: List<WorldLevel>) {
    val statusMap = worldLevels.associate { it.level.id to it.status }
    // ...
}

// After
fun saveWorldMapStatus(worldLevels: List<WorldLevel>) {
    val statusMap = worldLevels.mapNotNull { worldLevel ->
        worldLevel.level.editorLevelId?.let { editorLevelId ->
            editorLevelId to worldLevel.status
        }
    }.toMap()
    // ...
}
```

Return type changed:
```kotlin
// Before
fun loadWorldMapStatus(): Map<Int, LevelStatus>?

// After
fun loadWorldMapStatus(): Map<String, LevelStatus>?
```

#### 4. Game View Model (`GameViewModel.kt`)
Updated level status lookup to use `editorLevelId`:
```kotlin
private fun initializeWorldMap() {
    val levels = LevelData.createLevels()
    val savedStatuses = SaveFileStorage.loadWorldMapStatus()
    
    _worldLevels.value = levels.mapIndexed { index, level ->
        val status = if (level.editorLevelId != null) {
            savedStatuses?.get(level.editorLevelId) ?: 
                if (index == 0) LevelStatus.UNLOCKED else LevelStatus.LOCKED
        } else {
            if (index == 0) LevelStatus.UNLOCKED else LevelStatus.LOCKED
        }
        
        WorldLevel(level = level, status = status)
    }
}
```

#### 5. Tests (`SaveDataTest.kt`)
Updated test to use String level IDs:
```kotlin
@Test
fun testWorldMapSaveSerialization() {
    val worldMapSave = WorldMapSave(
        levelStatuses = mapOf(
            "level_001" to LevelStatus.WON,
            "level_002" to LevelStatus.UNLOCKED,
            "level_003" to LevelStatus.LOCKED
        )
    )
    // ...
}
```

### New JSON Format
After the fix, `worldmap.json` looks like:
```json
{
  "levelStatuses": {
    "level_1732546789123": "WON",
    "level_1732546789456": "UNLOCKED",
    "level_1732546789789": "LOCKED"
  }
}
```

These editor level IDs are:
- Stable (don't change when sequence is reordered)
- Unique (timestamp-based generation)
- Automatically assigned when levels are created in the editor

## Backward Compatibility
**Note**: This change is NOT backward compatible. Existing save files with numeric IDs will not be recognized.

This is acceptable because:
- The app is not yet released (as stated in the issue)
- The fix prevents a more serious data corruption issue
- Players in development/testing can simply unlock levels again

## Testing
All existing tests pass with the new implementation:
- `SaveDataTest.testWorldMapSaveSerialization()` - Updated to test String IDs
- All other save/load tests pass without modification
- Build succeeds on all platforms

## Benefits
1. **Stability**: Level progress is tied to actual level content, not position
2. **Flexibility**: Developers can reorder levels without affecting saves
3. **Safety**: Adding/removing levels doesn't corrupt existing progress
4. **Editor Integration**: Uses the same stable IDs as the level editor

## Related Files Modified
- `composeApp/src/commonMain/kotlin/de/egril/defender/save/SaveModels.kt`
- `composeApp/src/commonMain/kotlin/de/egril/defender/save/SaveJsonSerializer.kt`
- `composeApp/src/commonMain/kotlin/de/egril/defender/save/SaveFileStorage.kt`
- `composeApp/src/commonMain/kotlin/de/egril/defender/ui/GameViewModel.kt`
- `composeApp/src/commonTest/kotlin/de/egril/defender/save/SaveDataTest.kt`
- `docs/implementation/SAVE_LOAD_IMPLEMENTATION.md`
