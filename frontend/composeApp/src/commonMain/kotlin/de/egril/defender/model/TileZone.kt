package de.egril.defender.model

import de.egril.defender.editor.TileType

/**
 * A named area of a map with an alternative terrain state (e.g. "high tide" or "new river bed").
 *
 * The base map describes the normal state. A zone lists, for each of its tiles, the tile type the
 * tile takes while the zone is active. Zones are switched on and off at runtime by scripted level
 * events (see [EventActionType.APPLY_TILE_ZONE], [EventActionType.REVERT_TILE_ZONE] and
 * [EventActionType.TOGGLE_TILE_ZONE]) to depict tides or shifting river courses.
 *
 * @param id         Unique identifier within the map (referenced by events).
 * @param name       Display name shown in the editors.
 * @param tiles      Alternative tile type per position.
 * @param riverTiles Flow direction/speed for every position in [tiles] that becomes [TileType.RIVER].
 * @param movable    When true the zone is a region that can be moved as a whole by
 *                   [EventActionType.MOVE_TILE_ZONE], carrying its units, towers and terrain along.
 *                   Only the positions in [tiles] matter; the terrain is taken from the level.
 * @param rotatable  Marks a movable zone that is meant to rotate. It must have at least
 *                   [MIN_ROTATABLE_REGION_TILES] tiles, otherwise the map is not ready to use.
 */
data class TileZone(
    val id: String,
    val name: String = "",
    val tiles: Map<Position, TileType> = emptyMap(),
    val riverTiles: Map<Position, RiverTile> = emptyMap(),
    val movable: Boolean = false,
    /** Movable zones that are also rotated need at least [MIN_ROTATABLE_REGION_TILES] tiles, otherwise the map is invalid. */
    val rotatable: Boolean = false,
) {
    /** Display label: the name when set, otherwise the id. */
    val displayName: String get() = name.ifBlank { id }

    companion object {
        /** Tile types a zone may switch a tile to. */
        val SUPPORTED_TILE_TYPES: List<TileType> = TileType.entries
    }
}

/**
 * Terrain carried by a moved [TileZone] region at one of its current positions.
 *
 * @param homePosition The position the tile had in the zone before any move; used to re-map
 *                     level data (waypoints, target info) when a save game is loaded.
 */
data class RegionTile(
    val type: TileType,
    val riverTile: RiverTile?,
    val homePosition: Position,
)
