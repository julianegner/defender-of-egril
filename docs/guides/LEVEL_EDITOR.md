# Level Editor

The Level Editor is a desktop-only feature that allows you to create and edit custom maps and levels for Defender of Egril.

## Accessing the Editor

1. Launch the game on **desktop** (Windows, Mac, or Linux)
2. From the main menu, click "Start Game" to go to the World Map
3. Look for the orange **Level Editor** button with the wrench (🛠️) symbol
4. Click the button to enter the editor

**Note**: The Level Editor button only appears on desktop platforms. It is not available on Android or iOS.

## File Storage Location

All editor data is stored in JSON format on your local filesystem:

- **Linux/Mac**: `~/.defender-of-egril/gamedata/`
- **Windows**: `%USERPROFILE%\.defender-of-egril\gamedata\`

### Directory Structure

```
~/.defender-of-egril/gamedata/
├── maps/
│   ├── map_30x8.json
│   ├── map_35x9.json
│   └── ... (your custom maps)
├── levels/
│   ├── level_1.json
│   ├── level_2.json
│   └── ... (your custom levels)
└── sequence.json
```

## Editor Features

### Map Editor Tab

- View existing maps
- Maps define the grid layout, spawn points, paths, and build areas
- Each map is saved as a separate JSON file in the `maps/` directory

#### Collapsible Header

The Map Editor header can be collapsed to provide more screen space for editing the map:

- **Expanded State** (default): Shows full controls including:
  - Map name input field
  - Complete tile type selection (PATH, BUILD_AREA, ISLAND, NO_PLAY, SPAWN_POINT, TARGET, RIVER)
  - River properties (flow direction and speed)
  - "Change All NO_PLAY to PATH" button
  - Zoom controls
  - Collapse button (▲ icon)

- **Collapsed State**: Shows compact controls on the left side:
  - Tile type dropdown with all tile types (including RIVER)
  - Expand button (▼ icon)
  - River properties dialog (opens when RIVER is selected)

To toggle between states, click the collapse/expand button in the header.

### Level Editor Tab

- View existing levels
- Each level references a map and defines:
  - Enemy spawns and their timing
  - Starting coins and health points
  - Available tower types
  - Level title and subtitle

### Level Sequence Tab

- Arrange the order in which levels appear in the game
- Use "↑" and "↓" buttons to move levels up or down
- The sequence is saved in `sequence.json`

## Default Content

The game comes with 6 pre-converted levels:

1. **The First Wave** - 30 Goblins (map_30x8)
2. **Mixed Forces** - Goblins, Skeletons, and Orks (map_35x9)
3. **The Ork Invasion** - Heavy Ork presence (map_40x10)
4. **Dark Magic Rises** - Wizards and Witches (map_45x11)
5. **The Final Stand** - Mixed endgame enemies (map_50x12)
6. **Ewhad's Challenge** - Boss fight (map_50x12)

These levels are automatically created and saved to disk the first time you run the game.

## JSON Format

### Map Format

```json
{
  "id": "map_30x8",
  "name": "Generated Map 30x8",
  "width": 30,
  "height": 8,
  "tiles": {
    "0,1": "SPAWN_POINT",
    "0,4": "SPAWN_POINT",
    "1,1": "PATH",
    "29,4": "TARGET",
    "10,5": "ISLAND"
  }
}
```

### Tile Types

- `PATH` - Where enemies walk
- `BUILD_AREA` - Adjacent to paths (not currently used, calculated automatically)
- `ISLAND` - 2x2 build islands
- `NO_PLAY` - Not playable area (default for unset tiles)
- `SPAWN_POINT` - Enemy spawn locations
- `TARGET` - Where enemies are trying to reach
- `WAYPOINT` - For future pathfinding (not yet implemented)

### Level Format

```json
{
  "id": "level_1",
  "mapId": "map_30x8",
  "title": "The First Wave",
  "subtitle": "",
  "startCoins": 100,
  "startHealthPoints": 10,
  "enemySpawns": [
    {"attackerType": "GOBLIN", "level": 1, "spawnTurn": 1},
    {"attackerType": "GOBLIN", "level": 1, "spawnTurn": 1}
  ],
  "availableTowers": ["SPIKE_TOWER", "SPEAR_TOWER", "BOW_TOWER"]
}
```

### Level Sequence Format

```json
{
  "sequence": ["level_1", "level_2", "level_3", "level_4", "level_5", "level_6"]
}
```

## Editing Files Manually

You can edit the JSON files manually with any text editor. The game will reload the data from disk each time you:

1. Navigate to the World Map
2. Start a level

Changes to the sequence are loaded when you enter the Level Editor.

## Future Enhancements

The following features are planned for future versions:

- **Map Editor**: Full graphical tile editing with drag-and-drop
- **Level Editor**: Visual enemy configuration and tower selection UI
- **Create New**: Buttons to create new maps and levels
- **Delete**: Remove unwanted maps and levels
- **Export/Import**: Share levels with other players
- **Validation**: Automatic checks for valid paths and balanced levels

## Technical Notes

- Files use manual JSON serialization (not kotlinx.serialization) for better multiplatform compatibility
- File operations are platform-specific (JVM File I/O on desktop)
- The editor storage initialization only runs once (checks for `sequence.json` existence)
- All 6 default levels are recreated from the original hardcoded level data

## Deploying Levels with the App

You can package custom levels with the app by placing them in the repository directory:

```
composeApp/src/commonMain/composeResources/files/repository/
├── maps/
│   └── your_map.json
├── levels/
│   └── your_level.json
└── sequence.json
```

When the app starts:
1. If no levels exist in the platform-specific storage, it checks the repository
2. If repository files exist, they are copied to the storage directory
3. Otherwise, default levels are generated programmatically

This allows you to:
- Create levels using the desktop editor
- Copy the JSON files from `~/.defender-of-egril/gamedata/` to the repository
- Rebuild the app to include your levels on all platforms

See `composeApp/src/commonMain/composeResources/files/repository/README.md` for detailed documentation on the repository format and usage.
