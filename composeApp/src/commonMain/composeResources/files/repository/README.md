# Level Repository

This directory contains pre-built maps and levels that will be deployed with the app. If files exist in this repository, the app will load them instead of generating default levels programmatically.

## Directory Structure

```
repository/
├── maps/               # Map files (one per map)
│   ├── map_tutorial.json
│   └── ...
├── levels/             # Level files (one per level)
│   ├── welcome_to_defender_of_egril.json
│   └── ...
├── sequence.json       # Level sequence (order of levels)
└── dragon_names.json   # Dragon names (200 names for spawned dragons)
```

## File Formats

### sequence.json

Defines the order in which levels appear in the game.

```json
{
  "sequence": ["level_id_1", "level_id_2", "level_id_3"]
}
```

### dragon_names.json

Defines the list of dragon names that will be randomly assigned to spawned dragons.

```json
{
  "names": [
    "Flameheart", "Shadowwing", "Frostfang", "..."
  ]
}
```

You can customize this file to add your own dragon names. The game will randomly select from this list when spawning dragons from the Dragon's Lair.

### Map File (maps/{map_id}.json)

Defines the grid layout, spawn points, target, paths, and build areas.

```json
{
  "id": "map_tutorial",
  "name": "Tutorial Map",
  "width": 15,
  "height": 8,
  "readyToUse": true,
  "tiles": {
    "x,y": "TILE_TYPE"
  }
}
```

Tile types:
- `SPAWN_POINT`: Enemy spawn locations
- `TARGET`: Level goal
- `PATH`: Where enemies walk
- `BUILD_AREA`: Where towers can be built (adjacent to path)
- `ISLAND`: 2x2 tower platforms
- `WAYPOINT`: Path control points
- `NO_PLAY`: Non-playable area (default)

### Level File (levels/{level_id}.json)

Defines the level configuration, enemies, resources, and available towers.

```json
{
  "id": "level_id",
  "mapId": "map_id",
  "title": "Level Title",
  "subtitle": "Level Subtitle",
  "startCoins": 60,
  "startHealthPoints": 10,
  "enemySpawns": [
    {"attackerType": "GOBLIN", "level": 1, "spawnTurn": 1}
  ],
  "availableTowers": ["SPIKE_TOWER", "SPEAR_TOWER", "BOW_TOWER"],
  "waypoints": []
}
```

Enemy types:
- `GOBLIN`, `ORK`, `OGRE`, `SKELETON`, `EVIL_WIZARD`, `WITCH`
- `BLUE_DEMON`, `RED_DEMON`, `EVIL_MAGE`, `RED_WITCH`, `GREEN_WITCH`
- `EWHAD`, `DRAGON`

Tower types:
- `SPIKE_TOWER`, `SPEAR_TOWER`, `BOW_TOWER`
- `WIZARD_TOWER`, `ALCHEMY_TOWER`, `BALLISTA_TOWER`
- `DWARVEN_MINE`, `DRAGONS_LAIR`

## Creating New Levels

You can create new levels using the in-game Level Editor (desktop and web/wasm only):

1. Run the desktop or web version of the game
2. Click "Level Editor" from the world map
3. Create/edit maps in the Map Editor tab
4. Create/edit levels in the Level Editor tab
5. Arrange levels in the Level Sequence tab
6. Files are automatically saved to your local editor directory:
   - Linux/Mac: `~/.defender-of-egril/gamedata/`
   - Windows: `%USERPROFILE%\.defender-of-egril\gamedata\`

To add these files to the repository:

1. Copy the desired files from your local editor directory
2. Paste them into the corresponding `repository/` subdirectories
3. Update `sequence.json` to include your new levels
4. Rebuild the app

## How It Works

On app startup, `EditorStorage` will:

1. Check if files exist in the platform-specific storage directory
2. If not, attempt to load from the repository (these resources)
3. If repository files exist, copy them to the platform storage
4. If repository is empty, fall back to programmatic level generation

## Notes

- Repository files are embedded in the app at compile time
- They work on all platforms (desktop, Android, iOS, web/wasm)
- The Level Editor can still be used to create and modify levels at runtime
- Runtime-created levels are separate from repository levels
- To update deployed levels, modify repository files and rebuild the app
