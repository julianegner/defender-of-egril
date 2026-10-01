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
 * @param tiles      Alternative tile type per position. Only [TileType.PATH] and [TileType.RIVER]
 *                   are offered in the editor.
 * @param riverTiles Flow direction/speed for every position in [tiles] that becomes [TileType.RIVER].
 */
data class TileZone(
    val id: String,
    val name: String = "",
    val tiles: Map<Position, TileType> = emptyMap(),
    val riverTiles: Map<Position, RiverTile> = emptyMap(),
) {
    /** Display label: the name when set, otherwise the id. */
    val displayName: String get() = name.ifBlank { id }

    companion object {
        /** Tile types a zone may switch a tile to. */
        val SUPPORTED_TILE_TYPES: List<TileType> = listOf(TileType.PATH, TileType.RIVER)
    }
}
