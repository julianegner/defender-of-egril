# Tile Zone Events (Tides and Shifting Rivers)

Level events can switch parts of a map between **PATH** and **RIVER** at runtime. This is used to
depict tides, floods or rivers that change their course during a level.

## Concepts

### Tile zones (map)

A tile zone (`model/TileZone.kt`) is defined on a map in the map editor. It stores, per position, the
tile type the tile takes while the zone is active (PATH or RIVER, including river flow direction and
speed). The base map describes the normal state. Only tiles that are PATH or RIVER on the base map can
be part of a zone.

Map JSON (`tileZones` is optional; `zoneTiles` is used instead of `tiles` so the manual parser cannot
confuse zone tiles with the base map):

```json
"tileZones": [
  {
    "id": "zone_1",
    "name": "High tide",
    "zoneTiles": {
      "12,4": {"type": "RIVER", "flowDirection": "SOUTH_EAST", "flowSpeed": 1},
      "13,4": {"type": "PATH"}
    }
  }
]
```

### Event actions

| Action | Effect |
|--------|--------|
| `APPLY_TILE_ZONE` | Activate the zone `zoneId` |
| `REVERT_TILE_ZONE` | Deactivate the zone `zoneId` (tiles fall back to earlier active zones or the base map) |
| `TOGGLE_TILE_ZONE` | Activate the zone when inactive, otherwise deactivate it |
| `STOP_EVENT_LOOP` | Stop the running loop of the event `targetEventId` |

When several active zones cover the same tile, the zone activated last wins.

Events and loop steps with "No message" apply their actions without opening a popup. The editor
offers "high tide" and "low tide" text presets and every `message_background_*` resource as a
selectable message frame. The selected frame is saved as `messageFrame`; omitted or unknown frames
use the standard story frame, while known frames reuse the story/villain dialog's text placement.

### Event loops

An event can carry a `loop` that starts when the event fires (`model/LevelEvent.kt`):

- A loop has `steps` and a `repeatCount` (`0` = endless).
- Each step waits `waitTurns` player turns, then applies its `actions` and shows its optional
  `messageKey`. A step with `waitTurns = 0` runs immediately.
- A step can contain a `nestedLoop`, which runs completely before the enclosing loop continues.
  Nesting depth is not limited at runtime (the editor offers up to four levels).
- Endless loops are allowed at every level. A loop whose pass would not wait at least one turn is
  invalid (it would run forever within a single turn) and is not started; a runtime guard
  (`EventScriptSystem.MAX_LOOP_STEPS_PER_TURN`) additionally protects against this.
- Loops advance at the start of each player turn, before events are evaluated. A running loop is not
  restarted when its (repeatable) event fires again.

```json
"loop": {"repeatCount": 0, "steps": [{"waitTurns": 3, "actions": [{"type": "TOGGLE_TILE_ZONE", "zoneId": "zone_1"}], "messageKey": "event_msg_..."}]}
```

## Consequences of changing tiles

Implemented in `game/TileZoneSystem.kt`.

### Flooding (land becomes river)

- Enemies that can neither swim, fly nor hover **drown**: they are removed without reward and do not
  count as kills.
- Enemies that survive submersion (currently the **Troll**, `AttackerType.survivesSubmersion`) sink
  to the river bed. They are listed in the enemy list under *Submerged*, cannot be attacked, and do
  not count towards winning the level. When the water recedes they resurface on their tile, or on the
  first free walkable neighbor if the tile is occupied.
- Traps, fiefs and mushrooms on the tile are destroyed. Barricades are washed away.
- A tower on the tile (for example on a barricade tower base) is lifted onto a **raft**. The raft
  keeps the barricade's health (capped at 150); a tower without a barricade gets a full raft.
  Dwarven mines and dragon's lairs cannot float and are destroyed.

### Drying (river becomes land)

- Water-only enemies (e.g. the Kraken) strand and die without reward.
- Bridges on the tile are removed.
- Stranded **rafts become barricades** carrying their tower. Rafts have 150 health; stranding deals
  30 damage. If the barricade is left with less than 100 health, it can no longer carry the tower:
  the tower is destroyed without refund and the barricade remains. A raft with no health left after
  stranding leaves nothing behind.

## Rendering

Active zone tiles are tracked in `GameState.zonePaintedTiles` / `zonePaintedRiverTiles`. On map save,
each zone is rendered as a full map variant with the same generator and noise seed as the base PNG,
then masked to a transparent PNG with a feathered edge around its tiles (including tiles that
restore the original terrain after an earlier overlapping zone)
(`<map-id>.zone-<index>.png`). Gameplay loads these PNGs and layers active zones in activation order
over the base image without generating images mid-turn. If an image is missing, tile visuals remain
available as a fallback. A custom uploaded background cannot be automatically matched by the
generated zone art; the editor warns about this. Downloaded community maps generate their base and
zone PNGs before gameplay, including any alternate map ID used by a level. Pathfinding and movement
read the level's tiles live.

## Persistence

Save games store the active zone ids (`activeTileZoneIds`), the progress of running loops
(`activeEventLoops`, one frame per nesting level), raft health and whether an enemy is submerged. On
load, the zone tiles are re-applied without re-running flood/dry consequences, since the saved
objects already reflect them. Older saves load with defaults (no active zones or loops, full raft
health).

## Tests

- `game/TileZoneSystemTest.kt`: flooding, drying, submerging, resurfacing, rafts and barricades
- `game/EventLoopTest.kt`: waits, repetitions, endless and nested loops, stopping, validation
- `editor/TileZoneSerializationTest.kt`: map zone and event loop JSON round trips
- `save/TileZoneSaveLoadTest.kt`: save game round trips and backward compatibility
