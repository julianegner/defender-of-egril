# Movable Tile Regions

A movable tile region is a tile zone that can be translated or rotated during a level. The terrain
of the region and everything standing on it (units, towers, traps, barricades, and other objects)
move together, for example to make a floating island drift.

## Editor

- **Map editor**: in the Tile zones panel, tick "Movable region" for the selected zone. A zone that
  covers a spawn point cannot be made movable. Tick "Rotatable" to mark the zone as one that will be
  rotated by a level event (only available for movable zones).
- Overlapping movable zones show a warning in the Tile zones panel.
- A map with a rotatable zone of fewer than 7 tiles can still be saved, but it is invalid. The red dot
  on the map editor tab shows this, and its dialog gives the reason.
- **Level editor, Events tab**: add an action of type "Move tile zone". Pick a movable zone, then set
  the rotation (0-5 steps of 60 degrees), and the horizontal and vertical shift in grid cells.

## Runtime rules

- Rotation happens around the region's centre. The shift is applied to the region's anchor (its top-most,
  then left-most tile after rotation).
- Vacated tiles revert to the base map. Destination tiles take the region's terrain.
- Objects on destination tiles are destroyed without reward, as for flooding.
- Rotation needs at least 7 tiles (`MIN_ROTATABLE_REGION_TILES`). A rotation of a smaller region is
  rejected and logged; translations of any size are allowed. The Events tab shows a warning when a
  rotation is set for a smaller zone.
- A move is rejected (and logged) if it leaves the map, lands on a spawn point, or overlaps another
  movable region.
- Overlaps between movable regions are only warned about in the map editor. At runtime the conflicting
  move is rejected.
- Waypoints, the single-hit target order and target info are remapped to the new tile positions.
- The moved state is saved with the game and restored on load.

## Known limitations

- Rotation pivots on the rounded centre of the current tiles. Regions with an exact centre (such as a
  seven-hex flower) return to their start after six spins. Smaller regions are not allowed to rotate
  because they drift when spun repeatedly. Irregular regions of seven or more tiles may still drift;
  a fixed stored pivot would remove this.
