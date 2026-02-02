# Initial Setup Tab Refactoring

## Overview
Complete refactoring of the Initial Setup tab in the level editor to provide a better workflow for placing game elements (towers, enemies, traps, and barricades) before a level starts.

## Problem Statement

### Issues with Old Implementation
1. **Dialog-based workflow**: Each placement required opening a dialog, configuring, selecting position, confirming, and closing
2. **Single placement**: Could only place one element at a time
3. **No visual feedback**: Already placed elements were not visible on the map
4. **No validation**: Could accidentally place overlapping elements
5. **Cumbersome UX**: Too many clicks and dialogs for multiple placements

## New Implementation

### UI Structure
```
┌─────────────────────────────────────────┬─────────────────────────┐
│                                         │                         │
│  Map (2/3 width)                        │  Sidebar (1/3 width)    │
│  - All tiles visible                    │  - Element type         │
│  - Color-coded placement areas          │    selector             │
│  - Existing elements displayed          │  - Configuration panel  │
│  - Interactive hover effects            │  - Placement mode       │
│  - Click to place                       │  - Placed elements      │
│                                         │    summary              │
│                                         │                         │
└─────────────────────────────────────────┴─────────────────────────┘
```

### Key Features

#### 1. Visual Display of Existing Elements
- **Towers**: Blue circles on the map
- **Enemies**: Red circles on the map
- **Traps**: Triangles (brown for dwarven, purple for magical)
- **Barricades**: Brown squares

#### 2. Interactive Map
- **Hover effects**: Shows which tile will be selected
- **Valid placement areas**: Color-coded (green for valid)
- **Conflict detection**: Red highlighting for invalid placements
- **Selected element**: Gold highlighting

#### 3. Improved Workflow
1. Click element type button (Towers/Enemies/Traps/Barricades)
2. Configure element properties in sidebar
3. Click on map to place - can place multiple times
4. Switch to selection mode to remove elements
5. No dialogs needed for normal operation

#### 4. Validation Rules
- **Towers**: Cannot overlap with other towers
- **Traps**: Cannot overlap with traps or barricades
- **Barricades**: Cannot overlap with traps or barricades
- **Enemies**: Can overlap (they're mobile)
- **Tile restrictions**: Each element type has valid tile types (e.g., towers on BUILD_AREA/ISLAND)

#### 5. Configuration Panels
Each element type has a dedicated configuration panel:
- **Towers**: Type selector, level, dragon name (for Dragon's Lair), show all towers toggle
- **Enemies**: Type selector, level, custom health, dragon name (for dragons)
- **Traps**: Type selector (dwarven/magical), damage
- **Barricades**: Health points

### Technical Implementation

#### Files Modified
1. **InitialSetupTab.kt** - Main tab component, completely refactored
   - New split-screen layout (Row with map and sidebar)
   - State management for placement mode and configurations
   - Validation logic for placements
   - Element selection logic

2. **InitialSetupMinimap.kt** - Enhanced minimap component
   - Added parameters for existing elements
   - Visual rendering of all placed elements (circles, triangles, squares)
   - Hover state management
   - Click handling for both placement and selection modes
   - Conflict highlighting

3. **InitialSetupSidebar.kt** - New sidebar component
   - Element type selector buttons
   - Configuration panels for each element type
   - Placement mode switcher
   - Selected element details panel
   - Placed elements summary

#### Files Deprecated
- `InitialDefendersSection.kt.old` - Old tower placement UI
- `InitialAttackersSection.kt.old` - Old enemy placement UI
- `InitialTrapsAndBarricadesSection.kt.old` - Old trap/barricade placement UI

These files are kept as `.old` backups but are no longer used.

#### New String Resources (17 total)
All added to English and all language files (German, Spanish, French, Italian):
- `initial_setup_map`: "Map"
- `initial_setup_click_to_place`: "Click on the map to place the selected element"
- `initial_setup_configuration`: "Configuration"
- `element_type`: "Element Type"
- `selection_mode`: "Selection Mode"
- `selection_mode_info`: "Click on an element on the map to select and remove it"
- `placed_elements`: "Placed Elements"
- `tower_configuration`: "Tower Configuration"
- `enemy_configuration`: "Enemy Configuration"
- `trap_configuration`: "Trap Configuration"
- `barricade_configuration`: "Barricade Configuration"
- `selected_element`: "Selected Element"
- `no_map_selected`: "No map selected. Please select or create a map first."
- `deselect`: "Deselect"
- `towers`: "Towers"
- `traps`: "Traps"
- `barricades`: "Barricades"

### Color Scheme
- **Selected element**: Gold (#FFD700)
- **Valid placement (hover)**: Cyan (#00FFFF)
- **Invalid placement (conflict)**: Red (#FF4444)
- **Valid placement area**: Green (varies by dark/light mode)
- **Towers**: Blue (#2196F3)
- **Enemies**: Red (#FF0000)
- **Dwarven trap**: Brown (#795548)
- **Magical trap**: Purple (#9C27B0)
- **Barricade**: Brown (#8D6E63)

### Data Flow

#### Placement Flow
1. User selects element type → `placementMode` state updated
2. User configures element properties → configuration state updated
3. User clicks on map tile → `onTileClick` callback
4. Validation check → `canPlaceDefender/Trap/Barricade`
5. If valid → element added to list → parent state updated
6. Minimap re-renders with new element displayed

#### Selection Flow
1. User clicks "Selection Mode" → `placementMode = null`
2. User clicks on element on map → `findElementAtPosition`
3. Element found → `selectedElement` state updated
4. Sidebar shows element details with Remove button
5. User clicks Remove → element removed from list → parent state updated

#### Removal Flow
1. In sidebar's selected element panel
2. User clicks "Remove" button → `onRemove` callback
3. Element removed from appropriate list (defenders/attackers/traps/barricades)
4. `selectedElement` cleared
5. Minimap re-renders without the element

### Validation Logic

```kotlin
// Tower placement
fun canPlaceDefender(position: Position, existingDefenders: List<InitialDefender>): Boolean {
    return existingDefenders.none { it.position == position }
}

// Trap/Barricade placement
fun canPlaceTrap(position: Position, traps: List<InitialTrap>, barricades: List<InitialBarricade>): Boolean {
    return traps.none { it.position == position } && barricades.none { it.position == position }
}

// Tile type validation
fun isValidPlacement(position: Position, mode: PlacementMode, map: EditorMap): Boolean {
    val tileType = map.getTileType(position.x, position.y)
    return when (mode) {
        PlacementMode.DEFENDER -> tileType == TileType.BUILD_AREA || tileType == TileType.ISLAND
        PlacementMode.ATTACKER -> tileType == TileType.PATH || tileType == TileType.SPAWN_POINT
        PlacementMode.TRAP -> tileType == TileType.PATH
        PlacementMode.BARRICADE -> tileType == TileType.PATH
    }
}
```

### Advantages of New Implementation

1. **Better UX**: Fewer clicks, no modal dialogs for placement
2. **Visual feedback**: See all placements immediately
3. **Faster workflow**: Place multiple elements without interruption
4. **Validation**: Prevents invalid placements automatically
5. **Discoverability**: All options visible in sidebar
6. **Intuitive**: Map + configuration is familiar pattern
7. **Flexible**: Easy to add/remove elements
8. **Clear state**: Placement mode indicator always visible

### Backward Compatibility
- All existing saved levels with initial setup data load correctly
- JSON serialization unchanged (already implemented in `EditorJsonSerializer`)
- Data models unchanged (`InitialDefender`, `InitialAttacker`, `InitialTrap`, `InitialBarricade`)

### Future Enhancements (Not Implemented)
- Drag and drop to move elements
- Copy/paste elements
- Keyboard shortcuts for element types
- Undo/redo for placements
- Bulk operations (select multiple, delete all of type)
- Preview of element stats on hover
- Zoom/pan controls for large maps

## Testing Checklist

### Basic Functionality
- [ ] Open level editor Initial Setup tab
- [ ] Verify map displays correctly
- [ ] Verify sidebar displays correctly

### Tower Placement
- [ ] Select Towers mode
- [ ] Configure tower type and level
- [ ] Click on BUILD_AREA tile → tower placed
- [ ] Click on ISLAND tile → tower placed
- [ ] Click on PATH tile → no placement (invalid)
- [ ] Click on existing tower position → no placement (conflict shown in red)
- [ ] Toggle "Show all towers" → all tower types available

### Enemy Placement
- [ ] Select Enemies mode
- [ ] Configure enemy type and level
- [ ] Click on PATH tile → enemy placed
- [ ] Click on SPAWN_POINT tile → enemy placed
- [ ] Click on BUILD_AREA tile → no placement (invalid)
- [ ] Place multiple enemies on same tile → all placed (enemies can overlap)

### Trap Placement
- [ ] Select Traps mode
- [ ] Configure trap type and damage
- [ ] Click on PATH tile → trap placed
- [ ] Click on barricade position → no placement (conflict)
- [ ] Click on existing trap position → no placement (conflict)

### Barricade Placement
- [ ] Select Barricades mode
- [ ] Configure health points
- [ ] Click on PATH tile → barricade placed
- [ ] Click on trap position → no placement (conflict)
- [ ] Click on existing barricade position → no placement (conflict)

### Selection and Removal
- [ ] Click "Selection Mode"
- [ ] Click on tower → selected element panel shows tower details
- [ ] Click "Remove" → tower removed
- [ ] Click on enemy → selected element panel shows enemy details
- [ ] Click "Remove" → enemy removed
- [ ] Click on trap → selected element panel shows trap details
- [ ] Click "Remove" → trap removed
- [ ] Click on barricade → selected element panel shows barricade details
- [ ] Click "Remove" → barricade removed
- [ ] Click "Deselect" → selection cleared

### Visual Feedback
- [ ] Hover over valid tile → cyan highlight
- [ ] Hover over invalid tile (placement mode) → no highlight
- [ ] Hover over conflict position → red highlight
- [ ] Selected element → gold highlight
- [ ] Placed towers → blue circles visible
- [ ] Placed enemies → red circles visible
- [ ] Placed traps → triangles visible
- [ ] Placed barricades → squares visible

### Save/Load
- [ ] Place various elements
- [ ] Save level
- [ ] Close editor
- [ ] Reopen level → all elements still present
- [ ] Play level → initial elements appear correctly

### Edge Cases
- [ ] Map with no BUILD_AREA → can't place towers (correct)
- [ ] Map with no PATH → can't place enemies/traps/barricades (correct)
- [ ] Very small map → UI still usable
- [ ] Very large map → elements render correctly
- [ ] Many elements → performance acceptable

## Conclusion
This refactoring significantly improves the user experience for setting up initial game elements in the level editor. The new interface is more intuitive, provides better visual feedback, and allows for faster workflows while maintaining all existing functionality and data compatibility.
