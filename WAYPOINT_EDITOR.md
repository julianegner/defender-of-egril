# Waypoint Editor Implementation

This document describes the waypoint editing feature added to the Level Editor.

## Overview

Waypoints allow level designers to create complex enemy paths by defining specific positions enemies must pass through before reaching the final target. This enables sophisticated level designs like circular paths, spirals, or zigzag patterns.

## Features

### UI Components

The waypoint editor is accessible as the 4th tab in the Level Editor interface, labeled "Waypoints".

#### Waypoints Tab

The Waypoints tab displays:
- **Description**: Explains waypoint functionality
- **Available Tiles**: Shows count of WAYPOINT tiles available from the map
- **Validation Status**: Green checkmark (✓) for valid configuration, red error (✗) for invalid
- **Add Waypoint Button**: Opens dialog to create a new waypoint connection
- **Remove All Button**: Clears all waypoint connections (with confirmation)
- **Connection List**: Cards showing each waypoint connection (source → target)

#### Add Waypoint Dialog

When adding a waypoint, users select:
- **Source Position**: Must be a spawn point or waypoint tile
- **Target Position**: Must be a waypoint tile or the final target
- The system validates that no duplicate source positions exist

### Data Model

Waypoints are represented by the `EditorWaypoint` data class:
```kotlin
data class EditorWaypoint(
    val position: Position,           // The waypoint's position on the map
    val nextTargetPosition: Position  // Where enemies should go next
)
```

### Validation

The system validates waypoints to ensure:
1. No circular dependencies (waypoints that loop back on themselves)
2. All waypoint chains eventually lead to the final target
3. Source positions are unique (one waypoint per source position)

Validation is performed using `EditorLevel.validateWaypoints(targetPosition)` which:
- Returns `true` if waypoints are valid or if there are no waypoints
- Returns `false` if circular dependencies are detected
- Detects loops by tracking visited positions while following waypoint chains

### Persistence

Waypoints are persisted in the level JSON file format:
```json
{
  "id": "level_9",
  "waypoints": [
    {
      "position": {"x": 38, "y": 20},
      "nextTargetPosition": {"x": 20, "y": 2}
    },
    {
      "position": {"x": 20, "y": 2},
      "nextTargetPosition": {"x": 2, "y": 20}
    }
  ]
}
```

The serialization is handled by `EditorJsonSerializer` with:
- **serializeLevel()**: Converts waypoints to JSON format
- **deserializeLevel()**: Parses waypoints from JSON (backward compatible)

### Localization

The waypoint editor is fully localized with support for:
- English (en)
- German (de)
- Spanish (es)
- French (fr)
- Italian (it)

All UI strings are defined in `values/strings.xml` and translated files.

## Usage Guide

### Creating a Map with Waypoints

1. **Open Map Editor**: Navigate to the Map Editor tab
2. **Place Waypoint Tiles**: Use the WAYPOINT tile type to mark positions on the map where you want enemies to pause
3. **Save the Map**: Ensure the map is marked as "ready to use"

### Configuring Waypoints in a Level

1. **Open Level Editor**: Select or create a level
2. **Select Map**: Choose a map that has WAYPOINT tiles
3. **Navigate to Waypoints Tab**: Click the 4th tab labeled "Waypoints"
4. **Add Connections**:
   - Click "➕ Add Waypoint"
   - Select a source position (spawn point or waypoint)
   - Select a target position (waypoint or final target)
   - Click "Add Waypoint" to confirm
5. **Verify**: Check that the validation status shows ✓ (green checkmark)
6. **Save Level**: Click "Save Level" to persist changes

### Example: The Dance Level

The "Dance" level (level_9) demonstrates waypoints with a three-ring circular path:
- **Outer Ring**: 4 waypoints forming a circle at radius ~18
- **Middle Ring**: 4 waypoints forming a circle at radius ~10
- **Inner Ring**: 4 waypoints forming a circle at radius ~6
- **Center**: Final target at the center

Enemies spawn at the outer ring, circle clockwise, transition inward through each ring, and finally reach the center target.

## Implementation Details

### Files Modified

1. **LevelEditor.kt** (~500 lines added)
   - Added WaypointsTab composable
   - Added AddWaypointDialog composable
   - Added WaypointConnectionCard composable
   - Integrated waypoints into level save/load

2. **EditorJsonSerializer.kt** (~60 lines added)
   - Added waypoint serialization
   - Added waypoint deserialization with robust nested JSON parsing
   - Maintained backward compatibility

3. **Localization Files** (~20 strings × 5 languages)
   - Added waypoint-related UI strings
   - Fully translated for all supported languages

### Tests

`WaypointSerializationTest.kt` provides comprehensive test coverage:
- `testSerializeLevelWithWaypoints`: Verifies waypoints are included in JSON
- `testDeserializeLevelWithWaypoints`: Verifies waypoints are parsed correctly
- `testDeserializeLevelWithoutWaypointsBackwardCompatibility`: Ensures old levels still load
- `testSerializeDeserializeRoundTrip`: Validates complete serialize/deserialize cycle

### Backward Compatibility

Levels created before the waypoint editor feature:
- Will load successfully (waypoints field is optional)
- Will have an empty waypoints list by default
- Can be edited to add waypoints through the UI

## Technical Considerations

### Waypoint Tile Placement

WAYPOINT tiles should be:
- Part of the traversable path (enemies can walk on them)
- Connected to spawn points and the final target via PATH tiles or other WAYPOINT tiles
- Positioned strategically to create desired enemy movement patterns

### Performance

The waypoint validation algorithm:
- Uses a visited set to detect cycles
- Has O(n) time complexity where n is the number of waypoints
- Runs only when saving or displaying validation status
- Does not impact gameplay performance

### Limitations

Current implementation does not support:
- Multiple waypoint chains from the same source
- Conditional waypoints (branching based on enemy type)
- Dynamic waypoint activation/deactivation

These could be added in future enhancements if needed.

## Future Enhancements

Potential improvements:
1. **Visual Map Preview**: Show waypoint connections on a minimap
2. **Drag-and-Drop Editing**: Click tiles to create connections directly on the map
3. **Waypoint Templates**: Pre-defined patterns (circle, spiral, zigzag)
4. **Per-Enemy Waypoints**: Different paths for different enemy types
5. **Waypoint Testing**: Preview enemy movement through waypoint chain

## Related Documentation

- [LEVEL_EDITOR.md](LEVEL_EDITOR.md): General level editor guide
- [GAMEPLAY.md](GAMEPLAY.md): Game mechanics including enemy movement
- [EditorModels.kt](composeApp/src/commonMain/kotlin/de/egril/defender/editor/EditorModels.kt): Data model definitions
- [WaypointValidationTest.kt](composeApp/src/commonTest/kotlin/de/egril/defender/editor/WaypointValidationTest.kt): Waypoint validation tests
