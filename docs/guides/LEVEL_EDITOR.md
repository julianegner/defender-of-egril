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

```text
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

### Spawn Loops in the Enemy Spawns Tab

Loops are edited directly in the normal turn list, so normal turns and loops can
be mixed freely. Click **Create loop** on a turn to wrap it in a new loop.

- Turns inside a loop are indented and marked by a colored line on the left.
  Loops can be nested; every nesting level uses its own color.
- Each loop has its own header above its first turn. It is collapsed by default
  and shows the loop ID, the repeat mode, and the turn range. Expand it to edit:
  - the unique **Loop ID**,
  - the repeat mode: **Fixed count** (total iterations, including the first),
    **While unit alive**, or **Infinite**,
  - the first and last turn of the loop (a boundary can only move if the loops
    still nest completely, without partial overlaps),
  - **Add turn to loop**, **Remove loop (keep turns)**, and **Delete loop with turns**.
- **Copy loop** (in the header) inserts a copy of the whole loop, including its
  turns, enemies, and nested loops, directly after it. Copied loops get new IDs;
  copied unit IDs are renamed, and conditions inside the copy refer to the copies.
- When loops exist, each enemy has a **Loop options** button to set a unique
  **Unit ID** and **First iteration only**. For a boss loop, select **While unit
  alive** and choose the unit from the **Target unit ID** dropdown, which lists
  all units with an ID. The unit must appear before or inside the loop. The
  condition is checked at the end of each cycle.
- **First iteration only** refers to the loop that directly contains the enemy;
  when an outer loop repeats, the inner loop starts again with its first iteration.
  Villains remain unique and must not respawn, so loops around a villain's loop
  must not repeat.
- Villains (e.g. Ewhad) always have **First iteration only** enabled and always use
  their name as unit ID (both are locked), because a villain may exist only once
  per level. They therefore always appear in the **Target unit ID** dropdown.
- An event must end every **Infinite** loop with a **Stop spawn loop** action
  (Events tab); otherwise the level is invalid and cannot be saved. Turns and
  loops after an infinite loop are allowed and start once it has been stopped.
- **Stop spawn loop** can end any spawn loop, including counted or
  "while unit alive" loops. A running loop stops immediately and spawning
  continues after it; a loop that has not started yet is skipped. The dropdown
  lists all spawn loops of the level.
- Spawn-loop problems are marked with a red dot on the **Enemy Spawns** and
  **Events** tab headers; click the dot to see the explanation.

Turn numbers in the editor are positions in the list; during the game a loop
repeats its turns before the following turns start. All loop edits support
**Undo/Redo**. Invalid settings are listed at the top of the tab and block saving
and playtesting. Levels without loops are still saved as the classic linear
`enemySpawns` list; levels with loops are saved as nested `spawnGroups`, where a
group contains `entries` (turns and nested groups). The design preview expands
finite loops up to 10,000 turns and 100,000 enemies, otherwise one cycle per loop
is shown when possible. Dynamic loop duration is not predictable, and loop
playtests always run the complete plan.

### Map Editor Tab

- View existing maps
- Maps define the grid layout, spawn points, paths, and build areas
- In spawn turns, enemy types without a compatible land/water spawn point are disabled, even
  when the map has only one spawn point. Changing an enemy type in table mode keeps its assigned
  spawn point only when that point remains compatible.
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

#### Tile Zones

Tile zones store an alternative terrain state for part of a map, e.g. *high tide* or a *new river
bed*. Level events switch them on and off at runtime (see the Events tab below and
[Tile Zone Events](../features/TILE_ZONE_EVENTS.md)).

- Click **Tile zones** in the collapsed header (next to the tile type selector) to open the zone panel.
- **Add zone** creates a zone; click a zone to select it (click again to deselect) and rename it.
- Select a zone and click **Start zone drawing** to switch from normal map drawing to zone drawing.
  While this mode is active, both clicks and drags edit only the selected zone; the regular tile
  type selector chooses what to paint. Use **Remove tiles from zone** to erase zone overrides and
  **Finish zone drawing** to return to normal map editing. River tiles use the selected flow
  direction and speed.
- Every base tile type, including implicit NO_PLAY tiles, can belong to a zone and change to any
  tile type. Zone tiles are shown with their alternative type and a magenta border while selected.
- Zones are shifted along when the map is resized and are part of undo/redo.
- When editing a zone, the editor warns that changes cannot automatically match the visual style
  of an uploaded map background; changed areas may stand out.
- The zone panel shows where the background PNG and map JSON (including tile zones) are stored.
  On save, each zone also gets a pre-rendered transparent PNG alongside the base image
  (`<map-id>.zone-<index>.png`). The image-generation dialog shows generation and optimization
  for each image on one line; unchanged images are skipped individually. All files are in app
  storage (`~/.defender-of-egril/` on desktop); gameplay loads them without generating new ones.

### Level Editor Tab

- View existing levels
- Each level references a map and defines:
  - Enemy spawns and their timing
  - Starting coins and health points
  - Available tower types
  - Level title and subtitle

#### Level Generator

The **Level Generator** button in the Level Editor tab opens a dialog that creates a complete level
draft from a few inputs. Everything is chosen before the generation starts:

- **Difficulty** (easy, medium, hard, nightmare): controls the number of waves, the enemy levels and
  the starting coins and health points.
- **Villains**: optional; any number of villains can be selected. Every villain has a themed roster
  (for example, Araxxa the Giant Spider leads spiders, undead villains lead undead minions) which
  pre-sets the roster dropdowns below. The coven twins Haga and Zussa are not offered, since they
  only make sense together with Grand Coven-Mother Sybilla. Each selected villain is spawned exactly
  once, at the end of the first third of the waves, so it has time to build up its potential
  (summoning, auras) instead of arriving right before the end.
- **Enemy rosters**: a primary roster (horde, undead, demons, witches, pirates, spiders or wilds)
  and an optional secondary roster define which enemies the waves are drawn from. Selecting villains
  pre-sets both dropdowns with their themes, but they remain editable.
- **Map**: either an existing map is selected, or a new map is generated. For a generated map the
  general size (small 20x20, medium 30x30, large 40x40, gigantic 50x50 — based on the sizes of the
  existing maps) is chosen first; the exact width and height can then be adjusted. The map layout
  follows the chosen rosters: the spiders roster gets a spider-web map, the pirates roster a river
  crossing, and otherwise a random layout is used.

Every wave covers two consecutive turns and both of them get enemies, so a generated level never
contains a spawn turn without enemies. Wave sizes scale with the difficulty and grow over the
course of the level.

While the level is being generated a loading indicator is shown. Afterwards the generated level
(and, if requested, the generated map) is saved and opened in the level editor so it can be refined
like any other level.

#### Events Tab (within Level Editor)

The **Events** tab lets you script events for a level. Each event pairs a *condition* with one or
more *actions* and an optional predefined story message:

- **Conditions**: beginning of a player turn, beginning of an enemy turn, a number of enemies
  killed, a number of enemies of a specific type killed, a unit (any or of a specific type)
  reaching a defined tile, the player having health/mana/coins at or below a threshold, or a required
  number of Altars activated simultaneously. Every
  condition can be gated with a "from turn N onwards" value so it is only checked from a given turn.
- **Actions**: give coins, give mana, grant a support object, grant a support spell, destroy a
  dwarven mine at a specified tile, activate/deactivate/toggle a tile zone of the level's map, or
  stop the loop of another event, or win the level. When a *destroy mine* action targets a tile that has no
  pre-placed dwarven mine, or a zone/loop action references a missing zone/event, the editor shows
  a warning below the field.
- **Message**: optionally display a predefined story message (selected via dropdown) when the event
  fires. A message dialog is always shown when an event fires — even if no story message is selected —
  and lists the granted elements (coins, mana, support objects/spells) with their symbols, names and
  amounts so the player knows what they gained.
- **Repeatable**: by default an event fires only once; enable *repeatable* to let it fire on every
  future evaluation whenever its condition is met.
- **Loop**: optionally start a loop when the event fires. A loop is a list of steps; each step waits
  a number of player turns and then applies its actions (and optionally shows a message). A loop
  repeats a given number of times or endlessly, and a step can contain a nested loop that runs
  completely before the next step. Every loop pass must wait at least one turn; otherwise the editor
  shows an error and the loop is not started. An endless nested loop is allowed but the steps after
  it are never reached (the editor shows a hint). Typical use: an endless loop that toggles a
  *high tide* zone every few turns.

Events are evaluated at the start of each player and enemy turn, and also immediately during the
player's turn when a relevant state change happens (an enemy is killed or coins are spent), so
threshold-based events fire as soon as their condition is met rather than waiting for the next turn.
For a Rune Network objective, choose the activated-Altars condition, set its threshold, add the
win-level action, and select the Rune Network victory message. Altars must be activated in the same
player turn; their channeling resets at the start of the next player turn.

Each event is shown as a collapsible card. Collapsed cards display a short summary (the condition and
the number of actions) and a delete button; click the card header to expand it and edit its details.

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

### Spawn Groups

Levels can define an ordered `spawnGroups` array instead of the linear `enemySpawns` list.
When groups are present, they control spawning; levels without groups retain their existing
turn-by-turn behavior. Groups are configured in the level JSON.

Each group has a `groupId`, a `repeatMode`, and a `turns` sequence. Turn offsets are relative to
the beginning of each repetition and start at 1. Empty spawn turns and gaps between offsets
represent pauses. After a cycle finishes, the engine either repeats the group or starts the
next group:

- `COUNT`: runs the entire sequence `repeatCount` times.
- `CONDITION`: repeats while its condition is true at the cycle boundary.
  `UNIT_ALIVE` checks the particular unit identified by `targetUnitId`, not every enemy of its type.
- `INFINITE`: repeats indefinitely. Clearing the current enemies does not automatically win
  the level while this group remains active; the level needs an external completion mechanism.

Saving a game preserves loop progress and named-unit references, so loading resumes the current
cycle rather than restarting the wave.

Spawn entries use the existing `attackerType`, `level`, and optional `spawnPoint` fields, plus
`count` (default 1). A `unitId` names the actual spawned unit for conditions. Set
`firstIterationOnly` on a boss entry so it spawns once while its reinforcements keep repeating.
For example, add this field to a level:

```json
{
  "spawnGroups": [
    {
      "groupId": "opening",
      "repeatMode": "COUNT",
      "repeatCount": 3,
      "turns": [
        {"turnOffset": 1, "spawns": [{"attackerType": "GOBLIN", "count": 2, "level": 1}]},
        {"turnOffset": 2, "spawns": [{"attackerType": "ORK", "level": 1}]}
      ]
    },
    {
      "groupId": "boss_phase",
      "repeatMode": "CONDITION",
      "condition": "UNIT_ALIVE",
      "targetUnitId": "ewhad_boss",
      "turns": [
        {"turnOffset": 1, "spawns": [{"attackerType": "EWHAD", "unitId": "ewhad_boss", "firstIterationOnly": true}]},
        {"turnOffset": 2, "spawns": []},
        {"turnOffset": 3, "spawns": [{"attackerType": "SKELETON", "count": 3}]}
      ]
    }
  ]
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

```text
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
